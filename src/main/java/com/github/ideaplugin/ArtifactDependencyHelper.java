package com.github.ideaplugin;

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
    public static Map<MavenProject, DependencyData> findDependencies(ArtifactFactory artifactFactory, List<MavenProject> reactorProjects) throws InvalidVersionSpecificationException {
        // collect ids
        Set<Artifact> reactorArtifactIds = new HashSet<>();
        for (MavenProject reactorProject : reactorProjects)
            reactorArtifactIds.add(reactorProject.getArtifact());

        // artifact -> all transitive dependencies
        Map<Artifact, DependencyData> dependencyMap = new Hashtable<>();
        for (MavenProject project : reactorProjects) {
            DependencyData dependencyData = new DependencyData();
            for (Object dependency : project.getDependencies()) {
                // convert to artifact
                Artifact dependencyArtifact = toDependencyArtifact(artifactFactory, (Dependency) dependency);

                // add to dependency
                (reactorArtifactIds.contains(dependencyArtifact) ? dependencyData.reactorDependencies : dependencyData.remoteDependencies).add(dependencyArtifact);
            }

            // save dependency data for project
            dependencyMap.put(project.getArtifact(), dependencyData);
        }

        // transitive resolution
        Map<MavenProject, DependencyData> result = new HashMap<>();
        for (MavenProject project : reactorProjects) {
            DependencyData transitiveDependencyData = new DependencyData();
            findRecursiveArtifacts(project.getArtifact(), transitiveDependencyData, dependencyMap);
            result.put(project, transitiveDependencyData);
        }
        return result;
    }

    /**
     * Find all dependencies for given reactor artifact
     *
     * @param reactorArtifact          reactor artifact
     * @param transitiveDependencyData collector for transitive dependencies
     * @param reactorDependencyMap     only reactor dependency map: reactor artifact -> dependencyData
     */
    private static void findRecursiveArtifacts(Artifact reactorArtifact,
                                               DependencyData transitiveDependencyData,
                                               Map<Artifact, DependencyData> reactorDependencyMap) {
        // current dependency
        DependencyData artifactDependencyData = reactorDependencyMap.get(reactorArtifact);

        // save
        transitiveDependencyData.addAll(artifactDependencyData);

        // repeat for each reactor artifact dependency
        for (Artifact reactorArtifactDependency : artifactDependencyData.reactorDependencies)
            findRecursiveArtifacts(reactorArtifactDependency, transitiveDependencyData, reactorDependencyMap);
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
        Set<Artifact> remoteDependencies = new HashSet<>();  // non reactor artifact dependencies
        Set<Artifact> reactorDependencies = new HashSet<>(); // reactor artifact dependencies

        public void addAll(DependencyData data) {
            remoteDependencies.addAll(data.remoteDependencies);
            reactorDependencies.addAll(data.reactorDependencies);
        }
    }
}
