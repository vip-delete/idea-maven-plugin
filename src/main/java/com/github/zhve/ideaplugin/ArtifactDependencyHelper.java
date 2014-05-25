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
        Set<Artifact> reactorArtifacts = new HashSet<Artifact>();
        log.info("");
        log.info("Reactor Artifacts");
        log.info("");
        for (MavenProject reactorProject : reactorProjects) {
            log.info(reactorProject.getArtifact().getId());
            reactorArtifacts.add(reactorProject.getArtifact());
        }

        // artifact -> all transitive dependencies
        Map<Artifact, DependencyData> dependencyMap = new HashMap<Artifact, DependencyData>();
        log.info("");
        log.info("Detect Dependencies");
        for (MavenProject project : reactorProjects) {
            log.info("");
            log.info(project.getId());
            List<Artifact> remoteData = new ArrayList<Artifact>();
            List<Artifact> reactorData = new ArrayList<Artifact>();
            for (Object object : project.getDependencies()) {
                Dependency dependency = (Dependency) object;
                Artifact dependencyArtifact = toDependencyArtifact(artifactFactory, dependency);
                boolean reactor = reactorArtifacts.contains(dependencyArtifact);
                String id = dependencyArtifact.getId() + ":" + dependencyArtifact.getScope();
                if ("jar".equals(dependencyArtifact.getType())) {
                    if (reactor) {
                        log.info("R " + id);
                        reactorData.add(dependencyArtifact);
                    } else {
                        log.info("  " + id);
                        remoteData.add(dependencyArtifact);
                    }
                } else {
                    log.info("O " + id + " (type=" + dependencyArtifact.getType() + ")");
                }
            }

            // save dependency data for project
            dependencyMap.put(project.getArtifact(), new DependencyData(remoteData, reactorData));
        }

        log.info("");
        log.info("Resolve Dependencies");
        Map<MavenProject, DependencyData> result = new LinkedHashMap<MavenProject, DependencyData>();
        for (MavenProject project : reactorProjects) {
            log.info("");
            log.info(project.getId());
            Map<String, Artifact> reactorData = new LinkedHashMap<String, Artifact>();
            Map<String, Artifact> remoteData = new LinkedHashMap<String, Artifact>();

            Queue<Artifact> queue = new LinkedList<Artifact>();
            queue.add(project.getArtifact());
            while (!queue.isEmpty()) {
                Artifact artifact = queue.poll();
                log.info("# " + artifact.getId() + ":" + artifact.getScope());
                DependencyData artifactDependencyData = dependencyMap.get(artifact);

                // analyze all remote dependencies for given level
                for (Artifact dependency : artifactDependencyData.getRemoteList()) {
                    String dependencyConflictId = dependency.getDependencyConflictId();
                    Artifact dependencyArtifact = toDependencyArtifact(artifactFactory, dependency, artifact.getScope());
                    if (dependencyArtifact != null) {
                        String fullName = dependencyArtifact.getId() + ":" + dependencyArtifact.getScope();
                        Artifact prevArtifact = remoteData.get(dependencyConflictId);
                        if (prevArtifact == null) {
                            // new remote dependency
                            log.info("  " + fullName);
                            remoteData.put(dependencyConflictId, dependencyArtifact);
                        } else {
                            // we have already added this remote dependency
                            if (prevArtifact.getId().equals(dependencyArtifact.getId())) {
                                log.info("D " + fullName);
                            } else {
                                log.info("C " + fullName);
                                log.info("  " + project.getArtifact().getId());
                                log.info("  " + "+-" + prevArtifact.getId() + ":" + prevArtifact.getScope());
                                log.info("  " + "+-" + artifact.getId() + ":" + artifact.getScope());
                                log.info("  " + "  \\-" + fullName);
                            }
                        }
                    } else {
                        log.info("O " + dependency.getId() + ":" + dependency.getScope() + " (inherit=" + artifact.getId() + ":" + artifact.getScope() + ")");
                    }
                }

                // analyze all reactor dependencies for given level
                for (Artifact dependency : artifactDependencyData.getReactorList()) {
                    String dependencyConflictId = dependency.getDependencyConflictId();
                    Artifact dependencyArtifact = toDependencyArtifact(artifactFactory, dependency, artifact.getScope());
                    if (dependencyArtifact != null) {
                        String fullName = dependencyArtifact.getId() + ":" + dependencyArtifact.getScope();
                        Artifact prevArtifact = reactorData.get(dependencyConflictId);
                        if (prevArtifact == null) {
                            // new reactor dependency
                            log.info("R " + fullName);
                            reactorData.put(dependencyConflictId, dependencyArtifact);
                            // go deep
                            queue.add(dependencyArtifact);
                        } else {
                            // we have already added this remote dependency
                            if (prevArtifact.getId().equals(dependencyArtifact.getId())) {
                                log.info("D " + fullName);
                            } else {
                                log.info("C " + fullName);
                                log.info("  " + project.getArtifact().getId());
                                log.info("  " + "+-" + prevArtifact.getId() + ":" + prevArtifact.getScope());
                                log.info("  " + "+-" + artifact.getId() + ":" + artifact.getScope());
                                log.info("  " + "  \\-" + fullName);
                            }
                        }
                    } else {
                        log.info("O " + dependency.getId() + ":" + dependency.getScope() + " (inherit=" + artifact.getId() + ":" + artifact.getScope() + ")");
                    }
                }
            }
            result.put(project, new DependencyData(new ArrayList<Artifact>(remoteData.values()), new ArrayList<Artifact>(reactorData.values())));
        }
        return result;
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
            List<String> exclusions = new ArrayList<String>();
            for (Exclusion exclusion : dependency.getExclusions())
                exclusions.add(exclusion.getGroupId() + ":" + exclusion.getArtifactId());
            dependencyArtifact.setDependencyFilter(new ExcludesArtifactFilter(exclusions));
        }

        // additional
        if (Artifact.SCOPE_SYSTEM.equalsIgnoreCase(dependency.getScope()))
            dependencyArtifact.setFile(new File(dependency.getSystemPath()));


        return dependencyArtifact;
    }

    private static Artifact toDependencyArtifact(ArtifactFactory artifactFactory, Artifact dependency, String inheritedScope) {
        Artifact dependencyArtifact = artifactFactory.createDependencyArtifact(dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersionRange(),
                dependency.getType(),
                dependency.getClassifier(),
                dependency.getScope(),
                inheritedScope,
                dependency.isOptional()
        );
        if (dependencyArtifact != null)
            dependencyArtifact.setDependencyFilter(dependency.getDependencyFilter());
        return dependencyArtifact;
    }

    // Classes

    public static class DependencyData {
        private final List<Artifact> remoteList;
        private final List<Artifact> reactorList;

        public DependencyData(List<Artifact> remoteList, List<Artifact> reactorList) {
            this.remoteList = Collections.unmodifiableList(remoteList);
            this.reactorList = Collections.unmodifiableList(reactorList);
        }

        public List<Artifact> getRemoteList() {
            return remoteList;
        }

        public List<Artifact> getReactorList() {
            return reactorList;
        }
    }
}
