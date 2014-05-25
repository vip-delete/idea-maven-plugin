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

import org.apache.maven.model.IssueManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * @author Vasiliy Zhukov
 * @since 07/25/2010
 */
@Mojo(name = "idea", aggregator = true)
public class IdeaPluginMojo extends IdeaPluginMojoBase {
    /**
     * Enables/disables the assembling modules in jars for web artifacts
     */
    @Parameter(property = "assembleModulesIntoJars", defaultValue = "true")
    private boolean assembleModulesIntoJars;

    /**
     * Name of the registered Java SDK
     */
    @Parameter(property = "jdkName", defaultValue = "1.7", required = true)
    private String jdkName;

    /**
     * Name of the project language level, possible values: JDK_1_3, JDK_1_4, JDK_1_5, JDK_1_6, JDK_1_7, JDK_1_8
     */
    @Parameter(property = "jdkLevel", defaultValue = "JDK_1_7", required = true)
    private String jdkLevel;

    /**
     * Path to Google App Engine, if it's GAE project
     */
    @Parameter(property = "gaeHome")
    private String gaeHome;

    /**
     * Resource pattern in wildcard format, for example "?*.xml;?*.properties".
     */
    @Parameter(property = "wildcardResourcePatterns", defaultValue = "!?*.java", required = true)
    private String wildcardResourcePatterns;

    /**
     * Compile in background checkbox
     */
    @Parameter(property = "compileInBackground", defaultValue = "true")
    private boolean compileInBackground;

    /**
     * Allow adding assertion @NotNull at runtime
     */
    @Parameter(property = "assertNotNull", defaultValue = "true")
    private boolean assertNotNull;

    /**
     * User specific name of the run configuration for the application server
     * If not set, than use name of the war-packaging module
     */
    @Parameter(property = "applicationServerTitle")
    private String applicationServerTitle;

    /**
     * Name of the web application server, possible values: Tomcat, Jetty
     */
    @Parameter(property = "applicationServerName", defaultValue = "Tomcat")
    private String applicationServerName;

    /**
     * Version of the web application server
     */
    @Parameter(property = "applicationServerVersion", defaultValue = "7.0.53")
    private String applicationServerVersion;

    /**
     * Full name of the application server
     */
    @Parameter(property = "applicationServerFullName")
    private String applicationServerFullName;

    /**
     * ArtifactId which will be used to create default Run Configuration
     * Only used if reactorProjects has more than two war-packaged artifacts
     */
    @Parameter(property = "selectedWarArtifactId")
    private String selectedWarArtifactId;

    /**
     * VM parameters for the web application server
     */
    @Parameter(property = "vmParameters")
    private String vmParameters;

    /**
     * Start browser after application run
     */
    @Parameter(property = "openInBrowser", defaultValue = "false")
    private boolean openInBrowser;

    /**
     * Start browser at this url
     */
    @Parameter(property = "openInBrowserUrl", defaultValue = "http://localhost:8080")
    private String openInBrowserUrl;

    /**
     * deployment context of the web application
     */
    @Parameter(property = "deploymentContextPath", defaultValue = "/")
    private String deploymentContextPath;

    /**
     * turn on/off "Compact Empty Middle Packages" at Project Pane
     */
    @Parameter(property = "hideEmptyPackages", defaultValue = "true")
    private boolean hideEmptyPackages;

    /**
     * turn on/off "Autoscroll to Source" at Project Pane
     */
    @Parameter(property = "autoscrollToSource", defaultValue = "false")
    private boolean autoscrollToSource;

    /**
     * turn on/off "Autoscroll from Source" at Project Pane
     */
    @Parameter(property = "autoscrollFromSource", defaultValue = "false")
    private boolean autoscrollFromSource;

    /**
     * turn on/off "Sort by Type" at Project Pane
     */
    @Parameter(property = "sortByType", defaultValue = "false")
    private boolean sortByType;

    /**
     * turn on/off "Optimize Imports" before commit
     */
    @Parameter(property = "optimizeImportsBeforeCommit", defaultValue = "true")
    private boolean optimizeImportsBeforeCommit;

    /**
     * turn on/off "Reformat Code" before commit
     */
    @Parameter(property = "reformatCodeBeforeCommit", defaultValue = "false")
    private boolean reformatCodeBeforeCommit;

    /**
     * turn on/off "Perform Code Analysis for affected files" before commit
     */
    @Parameter(property = "performCodeAnalysisBeforeCommit", defaultValue = "false")
    private boolean performCodeAnalysisBeforeCommit;

    protected void doExecute() throws Exception {
        // prepare
        ArtifactHolder artifactHolder = getArtifactHolder();
        VelocityWorker velocityWorker = getVelocityWorker();
        VelocityContext context = new VelocityContext();
        MavenProject project = getProject();

        // fill iml-attributes
        String buildDirectory = project.getBuild().getDirectory();
        String standardBuildDirectory = project.getFile().getParent() + File.separator + "target";
        context.put("buildDirectory", buildDirectory.startsWith(standardBuildDirectory) ? standardBuildDirectory : buildDirectory);
        context.put("context", this);
        context.put("gaeHome", gaeHome == null ? null : new File(gaeHome).getCanonicalPath());
        context.put("MD", "$MODULE_DIR$");
        context.put("packagingPom", "pom".equals(project.getPackaging()));
        context.put("packagingWar", "war".equals(project.getPackaging()));
        context.put("project", project);

        // generate iml file
        createFile(context, velocityWorker.getImlTemplate(), "iml");

        // for non execution root just exit
        if (!getProject().isExecutionRoot()) return;

        // fill ipr-attributes
        context.put("M", getLocalRepositoryBasePath());
        context.put("assembleModulesIntoJars", assembleModulesIntoJars);
        context.put("jdkName", jdkName);
        context.put("jdkLevel", jdkLevel);
        context.put("wildcardResourcePatterns", XMLUtil.escapeXmlAttribute(wildcardResourcePatterns));
        List<MavenProject> warProjects = artifactHolder.getProjectsWithPackaging("war");
        // check id uniques
        Set<String> used = new HashSet<String>();
        for (MavenProject item : warProjects)
            if (!used.add(item.getArtifactId()))
                throw new MojoExecutionException("Two or more war-packagins projects in reactor have the same artifactId, please make sure that <artifactId> is unique for each war-packagins project.");
        Collections.sort(warProjects, ProjectComparator.INSTANCE);
        context.put("warProjects", warProjects);

        IssueManagement issueManagement = getProject().getIssueManagement();
        if (issueManagement != null) {
            String system = issueManagement.getSystem();
            String url = issueManagement.getUrl();
            if ("Redmine".equalsIgnoreCase(system)) {
                context.put("issueNavigationExist", Boolean.TRUE);
                context.put("issueRegexp", "\\d+");
                context.put("linkRegexp", url + "/issues/$0");
            } else if ("JIRA".equalsIgnoreCase(system)) {
                context.put("issueNavigationExist", Boolean.TRUE);
                context.put("issueRegexp", "[A-Z]+\\-\\d+");
                context.put("linkRegexp", url + "/browse/$0");
            } else if ("YouTrack".equalsIgnoreCase(system)) {
                context.put("issueNavigationExist", Boolean.TRUE);
                context.put("issueRegexp", "[A-Z]+\\-\\d+");
                context.put("linkRegexp", url + "/issue/$0");
            } else if ("Google Code".equalsIgnoreCase(system)) {
                context.put("issueNavigationExist", Boolean.TRUE);
                context.put("issueRegexp", "\\d+");
                context.put("linkRegexp", url + "/issues/detail?id=$0");
            } else if ("GitHub".equalsIgnoreCase(system)) {
                context.put("issueNavigationExist", Boolean.TRUE);
                context.put("issueRegexp", "\\d+");
                context.put("linkRegexp", url + "/issues/$0");
            }
        }

        createFile(context, velocityWorker.getIprTemplate(), "ipr");

        // fill iws-attributes
        context.put("compileInBackground", compileInBackground);
        context.put("assertNotNull", assertNotNull);
        context.put("hideEmptyPackages", hideEmptyPackages);
        context.put("autoscrollToSource", autoscrollToSource);
        context.put("autoscrollFromSource", autoscrollFromSource);
        context.put("sortByType", sortByType);
        context.put("optimizeImportsBeforeCommit", optimizeImportsBeforeCommit);
        context.put("reformatCodeBeforeCommit", reformatCodeBeforeCommit);
        context.put("performCodeAnalysisBeforeCommit", performCodeAnalysisBeforeCommit);

        if (!warProjects.isEmpty()) {
            // fill war-attributes
            MavenProject warProject = getDefaultWarProject(warProjects);
            context.put("warProject", warProject);
            warProjects.remove(warProject);
            context.put("otherWarProjects", warProjects);
            context.put("applicationServerTitle", StringUtils.isEmpty(applicationServerTitle) ? warProject.getArtifactId() : XMLUtil.escapeXmlAttribute(applicationServerTitle));
            context.put("applicationServerName", gaeHome == null ? applicationServerName : "Google AppEngine Dev");
            context.put("applicationServerVersion", applicationServerVersion);
            context.put("openInBrowser", openInBrowser);
            context.put("openInBrowserUrl", XMLUtil.escapeXmlAttribute(openInBrowserUrl));
            context.put("vmParameters", vmParameters == null ? "" : XMLUtil.escapeXmlAttribute(vmParameters));
            context.put("deploymentContextPath", deploymentContextPath);

            if (gaeHome != null) {
                context.put("applicationServerConfigurationType", "GoogleAppEngineDevServer");
                context.put("applicationServerFullName", applicationServerFullName == null ? "AppEngine Dev" : applicationServerFullName);
            } else if ("Tomcat".equals(applicationServerName)) {
                context.put("applicationServerConfigurationType", "#com.intellij.j2ee.web.tomcat.TomcatRunConfigurationFactory");
                context.put("applicationServerFullName", applicationServerFullName == null ? applicationServerName + " " + applicationServerVersion : applicationServerFullName);
            } else if ("Jetty".equals(applicationServerName)) {
                context.put("applicationServerConfigurationType", "org.codebrewer.idea.jetty.JettyRunConfigurationType");
                context.put("applicationServerFullName", applicationServerFullName == null ? applicationServerName + " " + applicationServerVersion : applicationServerFullName);
            } else
                throw new MojoExecutionException("Unknown applicationServerName: " + applicationServerName + ", possible values: Tomcat, Jetty");
        }

        createFile(context, velocityWorker.getIwsTemplate(), "iws");
    }

    private MavenProject getDefaultWarProject(List<MavenProject> warProjects) {
        if (warProjects.size() > 1 && StringUtils.isNotEmpty(selectedWarArtifactId)) {
            int i = 0;
            while (i < warProjects.size() && !selectedWarArtifactId.equals(warProjects.get(i).getArtifactId())) i++;
            return i < warProjects.size() ? warProjects.get(i) : warProjects.get(0);
        } else
            return warProjects.get(0);
    }

    private void createFile(VelocityContext context, Template template, String extension) throws Exception {
        File file = new File(getProject().getBasedir(), getProject().getArtifactId() + "." + extension);
        FileOutputStream output = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
        template.merge(context, writer);
        writer.close();
    }

    public List<String> getGaeApiJars() {
        List<String> list = new ArrayList<String>();
        for (String file : new File(gaeHome, "/lib/user").list())
            if (file.endsWith(".jar"))
                list.add(file);
        return list;
    }

    public List<String> getGaeOrmJars() {
        List<String> list = new ArrayList<String>();
        for (String file : new File(gaeHome, "/lib/user/orm").list())
            if (file.endsWith(".jar"))
                list.add(file);
        return list;
    }

    public List<String> getGaeSourceLibs() {
        List<String> list = new ArrayList<String>();
        for (String file : new File(gaeHome, "/src/orm").list()) {
            if (file.startsWith("datanucleus-core-") || file.startsWith("datanucleus-jpa"))
                list.add(file + "!/" + file.substring(0, file.length() - "-src.zip".length()) + "/src/java");
            else
                list.add(file + "!/");
        }
        return list;
    }
}
