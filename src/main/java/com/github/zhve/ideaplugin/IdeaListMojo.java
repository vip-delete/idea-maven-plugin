package com.github.zhve.ideaplugin;

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
        ArtifactHolder artifactHolder = new ArtifactHolder(getLog(), reactorProjects, artifactFactory, artifactResolver, localRepository, artifactMetadataSource);

        for (MavenProject project : reactorProjects) {
            List<Artifact> list = new ArrayList<Artifact>();
            list.addAll(artifactHolder.getCommonDependencies());
            list.addAll(artifactHolder.getDependencies(project));
            Collections.sort(list, ArtifactComparator.INSTANCE);

            getLog().info("\n" +
                    "[INFO] ------------------------------------------------------------------------\n" +
                    "[INFO] Listing " + project.getName() + " " + project.getId() + "\n" +
                    "[INFO] ------------------------------------------------------------------------\n" +
                    "[INFO] \n" +
                    "[INFO] --- \n" +
                    "[INFO]");
            getLog().info("The following files have been resolved:");
            for (Artifact artifact : list)
                getLog().info("   " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + (artifact.getClassifier() != null ? artifact.getClassifier() + ":" : "") + artifact.getVersion() + ":" + artifact.getScope());
            getLog().info("");
        }
    }
}
