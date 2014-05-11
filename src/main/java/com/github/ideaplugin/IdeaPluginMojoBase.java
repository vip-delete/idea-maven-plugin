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
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Vasiliy Zhukov
 * @since 07/26/2010
 */
public abstract class IdeaPluginMojoBase extends AbstractMojo {
    private static final Object SYNC = new Object();
    private static volatile boolean initialized;
    private static ArtifactHolder artifactHolder;
    private static VelocityWorker velocityWorker;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ArtifactFactory artifactFactory;

    @Component
    private MavenProjectBuilder projectBuilder;

    @Component(role = ArtifactMetadataSource.class, hint = "maven")
    private ArtifactMetadataSource artifactMetadataSource;

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "reactorProjects", required = true, readonly = true)
    private List<MavenProject> reactorProjects;

    @Parameter(property = "localRepository", required = true, readonly = true)
    private ArtifactRepository localRepository;

    // Getters

    public List<MavenProject> getReactorProjects() {
        return reactorProjects;
    }

    protected MavenProject getProject() {
        return project;
    }

    protected ArtifactHolder getArtifactHolder() {
        return artifactHolder;
    }

    protected VelocityWorker getVelocityWorker() {
        return velocityWorker;
    }

    // AbstractMojo

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (!initialized) {
                synchronized (SYNC) {
                    if (!initialized) {
                        artifactHolder = new ArtifactHolder(reactorProjects, artifactFactory, artifactResolver, localRepository, artifactMetadataSource);
                        velocityWorker = new VelocityWorker();
                        initialized = true;
                    }
                }
            }
            doExecute();
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    protected abstract void doExecute() throws Exception;

    // Velocity Bindings

    public List<Artifact> getDependencies(MavenProject project) {
        return artifactHolder.getDependencies(project);
    }

    public List<Artifact> getCommonDependencies() {
        return artifactHolder.getCommonDependencies();
    }

    public List<Artifact> getAllDependencies() {
        return artifactHolder.getAllDependencies();
    }

    public boolean isReactorArtifact(Artifact artifact) {
        return artifactHolder.isReactorArtifact(artifact);
    }

    public String getScope(Artifact artifact) {
        if (Artifact.SCOPE_PROVIDED.equalsIgnoreCase(artifact.getScope())) return "PROVIDED";
        if (Artifact.SCOPE_TEST.equalsIgnoreCase(artifact.getScope())) return "TEST";
        if (Artifact.SCOPE_RUNTIME.equalsIgnoreCase(artifact.getScope())) return "RUNTIME";
        return "COMPILE";
    }

    public boolean isWebFriendlyScope(Artifact artifact) {
        return !Artifact.SCOPE_PROVIDED.equalsIgnoreCase(artifact.getScope()) && !Artifact.SCOPE_TEST.equalsIgnoreCase(artifact.getScope());
    }

    public Map<String, String> getVcsMapping() {
        return Collections.emptyMap();
    }

    public String getModuleLibraryJar(Artifact artifact) {
        return localRepository.pathOf(artifact);
    }

    public String getModuleLibraryJavadocs(Artifact artifact) {
        String path = localRepository.pathOf(artifact);
        return path.substring(0, path.length() - 4) + "-javadoc.jar";
    }

    public String getModuleLibrarySources(Artifact artifact) {
        String path = localRepository.pathOf(artifact);
        return path.substring(0, path.length() - 4) + "-sources.jar";
    }

    public String getLocalRepositoryBasePath() {
        return localRepository.getBasedir();
    }

    public String getReactorArtifactJarName(Artifact artifact) {
        return artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
    }

    public List<String> getReactorPaths() {
        List<String> list = new ArrayList<String>();
        list.add(new File(project.getFile().getParentFile(), project.getArtifactId() + ".iml").getAbsolutePath());
        for (MavenProject reactor : (List<MavenProject>) project.getCollectedProjects())
            list.add(new File(reactor.getFile().getParentFile(), reactor.getArtifactId() + ".iml").getAbsolutePath());
        return list;
    }
}
