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
        reactorArtifacts = new HashSet<Artifact>();
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
        Map<MavenProject, ArtifactDependencyHelper.DependencyData> dependencyDataNewMap = new LinkedHashMap<MavenProject, ArtifactDependencyHelper.DependencyData>();
        for (Map.Entry<MavenProject, ArtifactDependencyHelper.DependencyData> entry : dependencyDataMap.entrySet()) {
            MavenProject project = entry.getKey();
            ArtifactDependencyHelper.DependencyData dependencyData = entry.getValue();

            List<Artifact> remoteData = new ArrayList<Artifact>();
            List<Artifact> reactorData = new ArrayList<Artifact>(dependencyData.getReactorList());
            if (!dependencyData.getRemoteList().isEmpty()) {
                // search
                ArtifactResolutionResult resolutionResult;
                log.info("");
                log.info("Resolve Transitively: " + project.getArtifact().getId());
                log.info("");
                log.info("Before:");
                for (Artifact a : dependencyData.getRemoteList())
                    log.info("  " + a.getId() + ":" + a.getScope());
                log.info("");
                try {
                    resolutionResult = artifactResolver.resolveTransitively(
                            new LinkedHashSet<Artifact>(dependencyData.getRemoteList()),
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
                        if (reactorArtifacts.contains(art)) {
                            if (!reactorData.contains(art)) {
                                reactorData.add(art);
                                log.info("R " + art.getId() + ":" + art.getScope());
                            } else {
                                log.info("D " + art.getId() + ":" + art.getScope());
                            }
                        } else {
                            log.info("  " + art.getId() + ":" + art.getScope());
                            remoteData.add(art);
                        }
                    }
                } catch (ArtifactResolutionException e) {
                    // TODO: add flag to ignore resolution errors
                    throw new MojoExecutionException(e.getMessage(), e);
                } catch (ArtifactNotFoundException e) {
                    // TODO: add flag to ignore resolution errors
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
            dependencyDataNewMap.put(project, new ArtifactDependencyHelper.DependencyData(remoteData, reactorData));
        }

        // Find common dependencies
        Map<String /*id+scope*/, Artifact> commonMap = new TreeMap<String, Artifact>();
        Set<Artifact> fullSet = new HashSet<Artifact>();
        boolean added = false;
        for (ArtifactDependencyHelper.DependencyData data : dependencyDataNewMap.values()) {
            Map<String, Artifact> remoteMap = new HashMap<String, Artifact>();
            for (Artifact artifact : data.getRemoteList())
                remoteMap.put(artifact.getId() + ":" + artifact.getScope(), artifact);

            // add to full
            fullSet.addAll(data.getRemoteList());

            // add to common
            if (added) {
                commonMap.keySet().retainAll(remoteMap.keySet());
            } else {
                commonMap.putAll(remoteMap);
                added = true;
            }
        }

        // Remote commonSet from fullSet
        fullSet.removeAll(commonMap.values());

        log.info("");
        log.info("Common Dependencies");
        log.info("");
        for (String id : commonMap.keySet())
            log.info("  " + id);

        log.info("");
        log.info("Full Dependencies");
        log.info("");
        for (Artifact artifact : fullSet)
            log.info("  " + artifact.getId());

        // Save commonDependencies
        commonDependencies = new ArrayList<Artifact>(commonMap.values());
        Collections.sort(this.commonDependencies, ArtifactComparator.INSTANCE);
        commonDependencies = Collections.unmodifiableList(this.commonDependencies);

        // Save allDependencies
        allDependencies = new ArrayList<Artifact>(fullSet);
        Collections.sort(allDependencies, ArtifactComparator.INSTANCE);
        allDependencies = Collections.unmodifiableList(allDependencies);

        // Save dependencyMap

        this.dependencyMap = new HashMap<MavenProject, List<Artifact>>();
        for (Map.Entry<MavenProject, ArtifactDependencyHelper.DependencyData> entry : dependencyDataNewMap.entrySet()) {
            MavenProject project = entry.getKey();
            // Remove commonSet from dependencies
            List<Artifact> remoteList = new ArrayList<Artifact>();
            for (Artifact artifact : entry.getValue().getRemoteList())
                if (!commonMap.containsKey(artifact.getId() + ":" + artifact.getScope()))
                    remoteList.add(artifact);
            List<Artifact> reactorList = new ArrayList<Artifact>(entry.getValue().getReactorList());
            Collections.sort(remoteList, ArtifactComparator.INSTANCE);
            Collections.sort(reactorList, ArtifactComparator.INSTANCE);
            List<Artifact> artifacts = new ArrayList<Artifact>();
            artifacts.addAll(reactorList);
            artifacts.addAll(remoteList);
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
        List<MavenProject> projects = new ArrayList<MavenProject>();
        for (MavenProject project : dependencyMap.keySet())
            if (project.getPackaging().equals(packaging))
                projects.add(project);
        return projects;
    }
}
