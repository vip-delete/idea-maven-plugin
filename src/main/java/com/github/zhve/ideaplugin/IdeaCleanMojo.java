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
            deleteFile(project, "iml");
            if (project.isExecutionRoot()) {
                deleteFile(project, "ipr");
                deleteFile(project, "iws");
            }
        }
    }

    private void deleteFile(MavenProject project, String extension) throws MojoFailureException {
        File file = new File(project.getBasedir(), project.getArtifactId() + "." + extension);
        if (file.exists() && !file.isDirectory()) {
            if (file.delete())
                getLog().info(" " + file.getAbsolutePath());
            else
                getLog().error(file.getAbsolutePath());
        }
    }
}
