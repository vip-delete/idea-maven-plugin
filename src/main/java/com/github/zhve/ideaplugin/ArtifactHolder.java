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
import org.apache.maven.artifact.resolver.*;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.*;

/**
 * @author Vasiliy Zhukov
 * @since 07/25/2010
 */
class ArtifactHolder {
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

    public ArtifactHolder(Log log, List<MavenProject> reactorProjects, ArtifactFactory artifactFactory, ArtifactResolver artifactResolver, ArtifactRepository localRepository, ArtifactMetadataSource artifactMetadataSource) throws MojoExecutionException {
        // collect
        reactorArtifacts = new HashSet<>();
        for (MavenProject reactorProject : reactorProjects)
            reactorArtifacts.add(reactorProject.getArtifact());
        reactorArtifacts = Collections.unmodifiableSet(reactorArtifacts);

        // Resolve dependencies
        Map<MavenProject, ArtifactDependencyHelper.DependencyData> dependencyDataMap;
        try {
            dependencyDataMap = ArtifactDependencyHelper.findDependencies(log, artifactFactory, reactorProjects);
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // Resolve transitively
        Map<MavenProject, ArtifactDependencyHelper.DependencyData> dependencyDataNewMap = new LinkedHashMap<>();
        for (Map.Entry<MavenProject, ArtifactDependencyHelper.DependencyData> entry : dependencyDataMap.entrySet()) {
            MavenProject project = entry.getKey();
            ArtifactDependencyHelper.DependencyData dependencyData = entry.getValue();
            ArtifactDependencyHelper.DependencyData dependencyDataNew = new ArtifactDependencyHelper.DependencyData();
            dependencyDataNew.getReactorDependencies().addAll(dependencyData.getReactorDependencies());
            dependencyDataNewMap.put(project, dependencyDataNew);

            if (!"pom".equals(project.getPackaging())) {
                if (!dependencyData.getRemoteDependencies().isEmpty()) {
                    // search
                    ArtifactResolutionResult resolutionResult;
                    log.info("");
                    log.info("Resolve Transitively: " + project.getArtifact().getId());
                    log.info("");
                    log.info("Before:");
                    for (Artifact a : dependencyData.getRemoteDependencies())
                    log.info("  " + a.getId() + ":" + a.getScope());
                    log.info("");
                    try {
                        resolutionResult = artifactResolver.resolveTransitively(
                                dependencyData.getRemoteDependencies(),
                                project.getArtifact(),
                                project.getManagedVersionMap(),
                                localRepository,
                                project.getRemoteArtifactRepositories(),
                                artifactMetadataSource
                        );
                        // save search result
                        log.info("After:");
                        for (Object resolutionNode : resolutionResult.getArtifactResolutionNodes()) {
                            Artifact art = ((ResolutionNode) resolutionNode).getArtifact();
                            log.info("  " + art.getId() + ":" + art.getScope());
                            dependencyDataNew.getRemoteDependencies().add(art);
                        }
                    } catch (ArtifactResolutionException | ArtifactNotFoundException e) {
                        // TODO: add flag to ignore resolution errors
                        throw new MojoExecutionException(e.getMessage(), e);
                    }
                }
            }
        }

        // Find common dependencies
        Set<Artifact> commonSet = new HashSet<>();
        Set<Artifact> fullSet = new HashSet<>();
        boolean added = false;
        for (ArtifactDependencyHelper.DependencyData data : dependencyDataNewMap.values()) {
            // add to full
            fullSet.addAll(data.getRemoteDependencies());

            // add to common
            if (added) {
                commonSet.retainAll(data.getRemoteDependencies());
            } else {
                commonSet.addAll(data.getRemoteDependencies());
                added = true;
            }
        }

        // Remove commonSet from dependencies
        for (ArtifactDependencyHelper.DependencyData data : dependencyDataNewMap.values())
            data.getRemoteDependencies().removeAll(commonSet);

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
        for (Map.Entry<MavenProject, ArtifactDependencyHelper.DependencyData> entry : dependencyDataNewMap.entrySet()) {
            MavenProject project = entry.getKey();
            List<Artifact> artifacts = new ArrayList<>();
            artifacts.addAll(entry.getValue().getRemoteDependencies());
            artifacts.addAll(entry.getValue().getReactorDependencies());
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
}
