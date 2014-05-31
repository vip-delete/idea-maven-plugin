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

    public ArtifactHolder(Log log, ArtifactDependencyResolver resolver, List<MavenProject> reactorProjects) throws MojoExecutionException {
        // collect
        reactorArtifacts = new HashSet<Artifact>();
        for (MavenProject reactorProject : reactorProjects)
            reactorArtifacts.add(reactorProject.getArtifact());
        reactorArtifacts = Collections.unmodifiableSet(reactorArtifacts);

        // Resolve
        Map<MavenProject, ArtifactDependencyResolver.DependencyData> dependencyDataNewMap;
        try {
            dependencyDataNewMap = resolver.findDependencies(reactorProjects);
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // Find common dependencies
        Set<Artifact> fullSet = new HashSet<Artifact>();
        for (ArtifactDependencyResolver.DependencyData data : dependencyDataNewMap.values())
            fullSet.addAll(data.getRemoteList());

        // Save allDependencies
        allDependencies = new ArrayList<Artifact>(fullSet);
        Collections.sort(allDependencies, ArtifactComparator.INSTANCE);
        allDependencies = Collections.unmodifiableList(allDependencies);

        log.info("");
        log.info("Full Dependencies");
        log.info("");
        for (Artifact artifact : allDependencies)
            log.info("  " + artifact.getId());

        // Save dependencyMap

        this.dependencyMap = new HashMap<MavenProject, List<Artifact>>();
        for (Map.Entry<MavenProject, ArtifactDependencyResolver.DependencyData> entry : dependencyDataNewMap.entrySet()) {
            MavenProject project = entry.getKey();
            // Remove commonSet from dependencies
            List<Artifact> remoteList = new ArrayList<Artifact>(entry.getValue().getRemoteList());
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
