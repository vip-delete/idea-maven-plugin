package com.github.zhve.ideaplugin;

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
