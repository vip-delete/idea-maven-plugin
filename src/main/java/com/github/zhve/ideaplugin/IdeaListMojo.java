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
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vasiliy Zhukov
 * @since 5/18/2014.
 */
@Mojo(name = "list", aggregator = true)
public class IdeaListMojo extends AbstractMojo {
    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ArtifactFactory artifactFactory;

    @Component(role = ArtifactMetadataSource.class, hint = "maven")
    private ArtifactMetadataSource artifactMetadataSource;

    @Parameter(property = "reactorProjects", required = true, readonly = true)
    private List<MavenProject> reactorProjects;

    @Parameter(property = "localRepository", required = true, readonly = true)
    private ArtifactRepository localRepository;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArtifactDependencyResolver resolver = new ArtifactDependencyResolver(getLog(), artifactFactory, artifactResolver, localRepository, artifactMetadataSource);
        ArtifactHolder artifactHolder = new ArtifactHolder(getLog(), resolver, reactorProjects);

        for (MavenProject project : reactorProjects) {
            List<Artifact> list = new ArrayList<Artifact>(artifactHolder.getDependencies(project));
            Collections.sort(list, ArtifactComparator.INSTANCE);

            getLog().info("                                                                        \n" +
                    "[INFO] ------------------------------------------------------------------------\n" +
                    "[INFO] Listing " + project.getName() + " " + project.getId() + "\n" +
                    "[INFO] ------------------------------------------------------------------------\n" +
                    "[INFO] \n" +
                    "[INFO] --- \n" +
                    "[INFO] ");
            getLog().info("The following files have been resolved:");
            for (Artifact artifact : list)
                getLog().info("   " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + (artifact.getClassifier() != null ? artifact.getClassifier() + ":" : "") + artifact.getVersion() + ":" + artifact.getScope());
            getLog().info("");
        }
    }
}
