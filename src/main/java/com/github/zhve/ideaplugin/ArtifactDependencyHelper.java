package com.github.zhve.ideaplugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.*;

/**
 * @author Vasiliy Zhukov
 * @since 5/11/2014
 */
class ArtifactDependencyHelper {
    /**
     * Transitive resolve all dependencies for reactor projects
     *
     * @param artifactFactory standard Maven's factory to create artifacts
     * @param reactorProjects reactor projects
     * @return dependency map: reactor project -> dependency data
     * @throws InvalidVersionSpecificationException error
     */
    public static Map<MavenProject, DependencyData> findDependencies(Log log, ArtifactFactory artifactFactory, List<MavenProject> reactorProjects) throws InvalidVersionSpecificationException {
        // collect ids
        Set<Artifact> reactorArtifacts = new HashSet<>();
        log.info("");
        log.info("Reactor Artifacts");
        log.info("");
        for (MavenProject reactorProject : reactorProjects) {
            log.info(" " + reactorProject.getArtifact().getId());
            reactorArtifacts.add(reactorProject.getArtifact());
        }

        // artifact -> all transitive dependencies
        Map<Artifact, DependencyData> dependencyMap = new HashMap<>();
        log.info("");
        log.info("Resolve Dependencies");
        log.info("");
        for (MavenProject project : reactorProjects) {
            log.info(" " + project.getId());
            DependencyData dependencyData = new DependencyData();
            for (Object object : project.getDependencies()) {
                Dependency dependency = (Dependency) object;
                Artifact dependencyArtifact = toDependencyArtifact(artifactFactory, dependency);
                boolean reactor = reactorArtifacts.contains(dependencyArtifact);
                if (reactor)
                    log.info(" R " + dependencyArtifact.getId());
                else
                    log.info("   " + dependencyArtifact.getId() + ":" + dependencyArtifact.getScope());

                // add to dependency
                if (reactor)
                    dependencyData.getReactorDependencies().add(dependencyArtifact);
                else
                    dependencyData.getRemoteDependencies().add(dependencyArtifact);
            }

            // save dependency data for project
            dependencyMap.put(project.getArtifact(), dependencyData);
        }

        // transitive resolution
        log.info("");
        log.info("Transitive Resolve Dependencies");
        log.info("");
        Map<MavenProject, DependencyData> result = new LinkedHashMap<>();
        for (MavenProject project : reactorProjects) {
            log.info(" " + project.getId());
            Map<String, Artifact> remoteDependencies = new LinkedHashMap<>();
            Set<Artifact> reactorDependencies = new LinkedHashSet<>();
            findRecursiveArtifacts(log, 1, "", artifactFactory, project, project.getArtifact(), remoteDependencies, reactorDependencies, dependencyMap);
            DependencyData dependencyData = new DependencyData();
            dependencyData.getRemoteDependencies().addAll(remoteDependencies.values());
            dependencyData.getReactorDependencies().addAll(reactorDependencies);
            result.put(project, dependencyData);
        }
        return result;
    }

    /**
     * Find all dependencies for given reactor artifact
     *
     * @param reactorArtifact      reactor artifact
     * @param remoteDependencies   collector for remote transitive dependencies
     * @param reactorDependencies  collector for reactor transitive dependencies
     * @param reactorDependencyMap only reactor dependency map: reactor artifact -> dependencyData
     */
    private static void findRecursiveArtifacts(Log log, int level, String indent, ArtifactFactory artifactFactory, MavenProject project, Artifact reactorArtifact,
                                               Map<String, Artifact> remoteDependencies,
                                               Set<Artifact> reactorDependencies,
                                               Map<Artifact, DependencyData> reactorDependencyMap) {
        // current dependency
        DependencyData artifactDependencyData = reactorDependencyMap.get(reactorArtifact);

        // add remote artifacts
        for (Artifact dependency : artifactDependencyData.getRemoteDependencies()) {
            String dependencyConflictId = dependency.getDependencyConflictId();
            Artifact dependencyArtifact;
            if (level == 0)
                dependencyArtifact = dependency;
            else {
                dependencyArtifact = artifactFactory.createDependencyArtifact(dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersionRange(),
                        dependency.getType(),
                        dependency.getClassifier(),
                        dependency.getScope(),
                        reactorArtifact.getScope(),
                        dependency.isOptional()
                );
                if (dependencyArtifact != null)
                    dependencyArtifact.setDependencyFilter(dependency.getDependencyFilter());
            }

            if (dependencyArtifact != null) {
                String fullName = dependencyArtifact.getId() + ":" + dependencyArtifact.getScope();
                Artifact remoteArtifact = remoteDependencies.get(dependencyConflictId);
                if (remoteArtifact == null) {
                    // new remote dependency
                    log.info(indent + "    " + dependencyArtifact.getId() + ":" + dependencyArtifact.getScope());
                    remoteDependencies.put(dependencyConflictId, dependencyArtifact);
                } else {
                    // we have already added this remote dependency
                    if (remoteArtifact.getId().equals(dependencyArtifact.getId())) {
                        log.info(indent + " D " + fullName);
                    } else {
                        log.info(indent + " E " + fullName);
                        log.info(indent + "    " + project.getArtifact().getId());
                        log.info(indent + "    " + "+- " + remoteArtifact.getId() + ":" + remoteArtifact.getScope());
                        log.info(indent + "    " + "+- " + reactorArtifact.getId());
                        log.info(indent + "    " + "   \\-" + fullName);
                    }
                }
            } else {
                log.info(indent + " O " + dependency.getId() + ":" + dependency.getScope() + " (level=" + level + ")");
            }
        }

        // repeat for each reactor artifact dependency
        for (Artifact reactorArtifactDependency : artifactDependencyData.getReactorDependencies()) {
            log.info(indent + " R " + reactorArtifact.getId());
            reactorDependencies.add(reactorArtifactDependency);
            findRecursiveArtifacts(log, level + 1, indent + "  ", artifactFactory, project, reactorArtifactDependency, remoteDependencies, reactorDependencies, reactorDependencyMap);
        }
    }

    /**
     * Convert Dependency to Artifact
     *
     * @param artifactFactory standard Maven's factory to create artifacts
     * @param dependency      dependency
     * @return artifact
     * @throws InvalidVersionSpecificationException if VersionRange is invalid
     */
    private static Artifact toDependencyArtifact(ArtifactFactory artifactFactory, Dependency dependency) throws InvalidVersionSpecificationException {
        // instantiate
        Artifact dependencyArtifact = artifactFactory.createDependencyArtifact(dependency.getGroupId(),
                dependency.getArtifactId(),
                VersionRange.createFromVersionSpec(dependency.getVersion()),
                dependency.getType(),
                dependency.getClassifier(),
                dependency.getScope() == null ? Artifact.SCOPE_COMPILE : dependency.getScope(),
                null,
                dependency.isOptional()
        );

        // apply exclusions is needed
        if (!dependency.getExclusions().isEmpty()) {
            List<String> exclusions = new ArrayList<>();
            for (Exclusion exclusion : dependency.getExclusions())
                exclusions.add(exclusion.getGroupId() + ":" + exclusion.getArtifactId());
            dependencyArtifact.setDependencyFilter(new ExcludesArtifactFilter(exclusions));
        }

        // additional
        if (Artifact.SCOPE_SYSTEM.equalsIgnoreCase(dependency.getScope()))
            dependencyArtifact.setFile(new File(dependency.getSystemPath()));


        return dependencyArtifact;
    }

    // Classes

    public static class DependencyData {
        private final Set<Artifact> remoteDependencies = new LinkedHashSet<>();  // non reactor artifact dependencies
        private final Set<Artifact> reactorDependencies = new LinkedHashSet<>(); // reactor artifact dependencies

        public Set<Artifact> getRemoteDependencies() {
            return remoteDependencies;
        }

        public Set<Artifact> getReactorDependencies() {
            return reactorDependencies;
        }
    }
}
