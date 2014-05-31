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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * @author Vasiliy Zhukov
 * @since 5/20/2014.
 */
@Mojo(name = "clean", aggregator = true)
public class IdeaCleanMojo extends AbstractMojo {
    @Parameter(property = "reactorProjects", required = true, readonly = true)
    private List<MavenProject> reactorProjects;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Delete Workspace Files:");
        for (MavenProject project : reactorProjects) {
            Util.deleteFileOrDirectory(getLog(), new File(project.getBasedir(), project.getArtifactId() + ".iml"));
            if (project.isExecutionRoot()) {
                Util.deleteFileOrDirectory(getLog(), new File(project.getBasedir(), ".idea"));
                Util.deleteFileOrDirectory(getLog(), new File(project.getBasedir(), project.getArtifactId() + ".ipr"));
                Util.deleteFileOrDirectory(getLog(), new File(project.getBasedir(), project.getArtifactId() + ".iws"));
            }
        }
    }
}
