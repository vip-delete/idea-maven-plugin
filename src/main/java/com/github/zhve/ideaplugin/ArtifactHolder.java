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
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.project.MavenProject;

import java.util.*;

/**
 * @author Vasiliy Zhukov
 * @since 07/25/2010
 */
public class ArtifactHolder {
    /**
     * Common artifacts for all modules
     */
    private List<Artifact> commonDependencies;

    /**
     * All dependencies of all modules except commonDependencies
     */
    private List<Artifact> allDependencies;

    /**
     * Maven Project -> Dependency Artifacts except commonDependencies
     */
    private Map<MavenProject, List<Artifact>> dependencyMap;

    /**
     * Artifacts for all modules (reactors)
     */
    private Set<Artifact> reactorArtifacts;

    public ArtifactHolder(List<MavenProject> reactorProjects, ArtifactFactory artifactFactory, ArtifactResolver artifactResolver, ArtifactRepository localRepository, ArtifactMetadataSource artifactMetadataSource) throws Exception {
        // collect
        reactorArtifacts = new HashSet<>();
        for (MavenProject reactorProject : reactorProjects)
            reactorArtifacts.add(reactorProject.getArtifact());
        reactorArtifacts = Collections.unmodifiableSet(reactorArtifacts);

        // Resolve dependencies
        Map<MavenProject, ArtifactDependencyHelper.DependencyData> dependencyDataMap = ArtifactDependencyHelper.findDependencies(artifactFactory, reactorProjects);

        // Resolve transitively
        for (Map.Entry<MavenProject, ArtifactDependencyHelper.DependencyData> entry : dependencyDataMap.entrySet()) {
            MavenProject project = entry.getKey();
            ArtifactDependencyHelper.DependencyData dependencyData = entry.getValue();

            if (!"pom".equals(project.getPackaging())) {
                if (!dependencyData.remoteDependencies.isEmpty()) {
                    // search
                    // TODO: add flag to ignore resolution errors
                    ArtifactResolutionResult resolutionResult = artifactResolver.resolveTransitively(
                            dependencyData.remoteDependencies,
                            project.getArtifact(),
                            project.getManagedVersionMap(),
                            localRepository,
                            project.getRemoteArtifactRepositories(),
                            artifactMetadataSource
                    );

                    // save search result
                    for (Object resolutionNode : resolutionResult.getArtifactResolutionNodes())
                        dependencyData.remoteDependencies.add(((ResolutionNode) resolutionNode).getArtifact());
                }
            }
        }

        // Find common dependencies
        Set<Artifact> commonSet = new HashSet<>();
        Set<Artifact> fullSet = new HashSet<>();
        boolean added = false;
        for (ArtifactDependencyHelper.DependencyData data : dependencyDataMap.values()) {
            // add to full
            fullSet.addAll(data.remoteDependencies);

            // add to common
            if (added) {
                commonSet.retainAll(data.remoteDependencies);
            } else {
                commonSet.addAll(data.remoteDependencies);
                added = true;
            }
        }

        // Remove commonSet from dependencies
        for (ArtifactDependencyHelper.DependencyData data : dependencyDataMap.values())
            data.remoteDependencies.removeAll(commonSet);

        // Remote commonSet from fullSet
        fullSet.removeAll(commonSet);

        // Save commonDependencies
        commonDependencies = new ArrayList<>(commonSet);
        Collections.sort(this.commonDependencies, ArtifactComparator.INSTANCE);
        commonDependencies = Collections.unmodifiableList(this.commonDependencies);

        // Save allDependencies
        allDependencies = new ArrayList<>(fullSet);
        Collections.sort(allDependencies, ArtifactComparator.INSTANCE);
        allDependencies = Collections.unmodifiableList(allDependencies);

        // Save dependencyMap

        this.dependencyMap = new HashMap<>();
        for (Map.Entry<MavenProject, ArtifactDependencyHelper.DependencyData> entry : dependencyDataMap.entrySet()) {
            MavenProject project = entry.getKey();
            List<Artifact> artifacts = new ArrayList<>();
            artifacts.addAll(entry.getValue().remoteDependencies);
            artifacts.addAll(entry.getValue().reactorDependencies);
            Collections.sort(artifacts, ArtifactComparator.INSTANCE);
            this.dependencyMap.put(project, Collections.unmodifiableList(artifacts));
        }
    }

    public List<Artifact> getDependencies(MavenProject project) {
        List<Artifact> artifacts = dependencyMap.get(project);
        return artifacts == null ? Collections.<Artifact>emptyList() : artifacts;
    }

    public List<Artifact> getCommonDependencies() {
        return commonDependencies;
    }

    public List<Artifact> getAllDependencies() {
        return allDependencies;
    }

    public boolean isReactorArtifact(Artifact artifact) {
        return reactorArtifacts.contains(artifact);
    }

    public List<MavenProject> getProjectsWithPackaging(String packaging) {
        List<MavenProject> projects = new ArrayList<>();
        for (MavenProject project : dependencyMap.keySet())
            if (project.getPackaging().equals(packaging))
                projects.add(project);
        return projects;
    }

    // Private

    private static class ArtifactComparator implements Comparator<Artifact> {
        public static final ArtifactComparator INSTANCE = new ArtifactComparator();

        @Override
        public int compare(Artifact o1, Artifact o2) {
            return o1.getId().compareTo(o2.getId());
        }
    }
}
