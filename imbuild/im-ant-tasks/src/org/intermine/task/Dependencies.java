package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.StringUtils;

/**
 *
 *
 * @author Thomas Riley
 */
public class Dependencies extends Task
{
    /** Base directory that all projects are relative to. */
    private String workspaceBaseDir;
    /** Compile classpath (does not include artifacts, includes class files and libs). */
    private Path compilePath;
    /** Execute classpath (includes libs and artifacts) */
    private Path executePath;
    /** Main classpath represented as FileSet. */
    private FileSet compileFileSet;
    /** Deploy classpath represented as FileSet. */
    private FileSet executeFileSet;
    /** Target to run for each dependent project (optional). */
    private String target;
    /**  */
    //private String pathid = "main.class.path";
    /** Whether or not to build project dependencies (default is true). */
    private boolean build = true;
    /** Whether or not to fully traverse dependency graph (default is false). */
    private boolean nofollow = false;
    /** Type of depenency to load from project.properties (default is "compile"). */
    private String type = "project";
    /** The properties from the project.properties file */
    private Properties projectProperties;

    /**
       The extra dependencies that this project needs as set by the EXTRA_DEPS property or
       from the project.properties.
       An example of when this is needed is flymine/postprocess which depends on
       bio/postprocess/core, however bio/postprocess/core needs a model project in order to work so
       we set EXTRA_DEPS (extra.project.dependencies) to "flymine/dbmodel".  We can't make
       bio/postprocess/core depend directly on flymine/dbmodel because bio/postprocess/core is
       supposed to be model/mine independent.
    */
    private String extraProjectDependencies = "";

    private String EXTRA_DEPS = "extra.project.dependencies";


    /**
     * Base directory that all projects are assumed relative to.
     *
     * @param basedir base directory that all projects are assumed relative to
     */
    public void setBasedir(String basedir) {
        workspaceBaseDir = basedir;
    }

    /**
     * Set the ant target to execute on each dependency. Targets are not executed
     * if any of the following are true:
     * <ul>
     * <li>buildDependencies is set to false</li>
     * <li>the property "no.dep" is set to "true" (the value of this
     * property is set by the Dependencies task and should not be set or altered)</li>
     * <li>nofollow is set to true</li>
     * </ul>
     *
     * @param target target to run on each dependency
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Set whether or not to build project dependencies (default is true).
     *
     * @param build false to disable building of dependencies
     */
    public void setBuildDependencies(boolean build) {
        this.build = build;
    }

    /**
     * Set whether or not to visit dependencies of dependencies. Default is false - the
     * entire dependency graph is searched.
     *
     * @param nofollow true to disable traversal of the dependency graph and to only
     *                 look at the immediate project dependencies
     */
    public void setNoFollow(boolean nofollow) {
        this.nofollow = nofollow;
    }

    /**
     * Set the dependency type. This is basically a way to alter the property read from
     * project.properties identifying project dependencies. The name of the property
     * read will by TYPE.dependencies where, by default, TYPE is "compile". Setting this
     * property to some non-default also has the side-effect of stopping the following of
     * dependencies (in other words it sets nofollow to true).
     *
     * @param type type of dependencies to load from project.properties
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Execute the task.
     *
     * @throws BuildException if something goes wrong
     */
    public void execute() throws BuildException {
        if (workspaceBaseDir == null) {
            throw new BuildException("basedir attribute required");
        }

        String compilePathId = type + ".compile.path";
        String executePathId = type + ".execute.path";
        String artifactPathId = type + ".artifact.path";

        // Don't run twice if target not specified.
        if (getProject().getReference(compilePathId) != null && target == null) {
            return;
        }

        if (getProject().getUserProperty("top.ant.project.name") == null) {
            getProject().setUserProperty("top.ant.project.name", getProject().getName());
        }

        projectProperties = loadProjectProperties(getProject().getBaseDir());

        extraProjectDependencies = projectProperties.getProperty(EXTRA_DEPS);

        List<String> extraProjectDepsList = new ArrayList<String>();

        if (getProject().getProperty(EXTRA_DEPS) != null) {
            // add the extra project dependencies from the project that is calling/executing this
            // project
            if (extraProjectDependencies == null) {
                extraProjectDependencies = getProject().getProperty(EXTRA_DEPS);
            } else {
                extraProjectDependencies += ", " + getProject().getProperty(EXTRA_DEPS);
            }
        }

        if (extraProjectDependencies != null) {
            Vector bits = StringUtils.split(extraProjectDependencies, ',');

            for (Object bit: bits) {
                String dep = ((String) bit).trim();

                if (!extraProjectDepsList.contains(dep)) {
                    extraProjectDepsList.add(dep);
                }
            }
        }

        compilePath = new Path(getProject());
        executePath = new Path(getProject());
        Path artifactPath = new Path(getProject());
        compileFileSet = new FileSet();
        compileFileSet.setDir(new File(workspaceBaseDir.replace('/', File.separatorChar)));
        compileFileSet.setProject(getProject());
        executeFileSet = new FileSet();
        executeFileSet.setDir(new File(workspaceBaseDir.replace('/', File.separatorChar)));
        executeFileSet.setProject(getProject());

        String compileIncludes = "";
        String executeIncludes = "";

        FileSet artifactFileSet = new FileSet();
        artifactFileSet.setDir(new File(workspaceBaseDir.replace('/', File.separatorChar)));
        artifactFileSet.setProject(getProject());
        String artifactIncludes = "";

        getProject().addReference(compilePathId, compilePath);
        getProject().addReference(executePathId, executePath);
        getProject().addReference(artifactPathId, artifactPath);

        String projName = calcThisProjectName();

        // Gather list of projects, removing redundancy
        List allProjectNames = new ArrayList();
        followProjectDependencies(projName, allProjectNames, extraProjectDepsList);

        // Make a List of dependent projects excluding those from extraProjectDepsList (and
        // dependencies of it)
        List projectNamesNoExtraDeps = new ArrayList();
        followProjectDependencies(projName, projectNamesNoExtraDeps, new ArrayList());

        // Find out whether to run targets on dependencies
        // We only want the root level invocation to run targets
        boolean executeTargets = !("true".equalsIgnoreCase(getProject().getProperty("no.dep")));
        executeTargets = build && executeTargets && !nofollow;

        // Describe complete dependency set
        if (executeTargets) {
            describeDependencies(allProjectNames, "Dependency build order:");
        }

        // Deal with this projects libs

        // Add lib/main/*.jar
        FileSet fileset = new FileSet();
        fileset.setDir(getProject().getBaseDir());
        fileset.setIncludes("lib/*.jar");
        fileset.setProject(getProject());
        compilePath.addFileset(fileset);
        executePath.addFileset(fileset);

        compileIncludes += projName + "/lib/*.jar ";
        executeIncludes += projName + "/lib/*.jar ";

        for (int i = 0; i < allProjectNames.size(); i++) {
            String depName = (String) allProjectNames.get(i);
            File projDir = getProjectBaseDir(depName);

            String theTarget = target;
            if (theTarget == null) {
                theTarget = "jar";
            }

            if (executeTargets /*&& (mainDep || test)*/) {
                System .out.println("Executing \"" + theTarget + "\" for source \"" + depName
                                    + "\"  in directory: " + projDir);

                // Run target if specified
                Ant ant = new Ant();
                ant.setDir(projDir);
                ant.setInheritAll(false);
                ant.setTarget(theTarget);
                ant.setProject(getProject());

                // Tell sub-invocation not to execute targets on dependencies
                Property prop = ant.createProperty();
                prop.setName("no.dep");
                prop.setValue("true");
                prop.setProject(getProject());
                prop.execute();

                // Tell sub-invocation about extra dependencies
                if (extraProjectDependencies != null) {
                    Property extraDependenciesProp = ant.createProperty();
                    extraDependenciesProp.setName(EXTRA_DEPS);
                    extraDependenciesProp.setValue(extraProjectDependencies);
                    extraDependenciesProp.setProject(getProject());
                    extraDependenciesProp.execute();
                }

                ant.execute();
            }

            DirSet dirset = new DirSet();
            dirset.setDir(projDir);
            dirset.setIncludes("build/classes");
            compilePath.addDirset(dirset);

            executeIncludes += depName + "/dist/* ";

            // Add lib/*.jar
            FileSet libFileSet = new FileSet();
            libFileSet.setDir(projDir);
            libFileSet.setIncludes("lib/*.jar");
            libFileSet.setProject(getProject());
            compilePath.addFileset(libFileSet);

            compileIncludes += depName + "/lib/*.jar ";
            compileIncludes += depName + "/build/classes/ ";

            if (projectNamesNoExtraDeps.contains(depName)) {
                // Add dist/*.jar, dist/*.war
                FileSet distFileSet = new FileSet();
                distFileSet.setDir(projDir);
                distFileSet.setIncludes("dist/*");
                distFileSet.setProject(getProject());
                executePath.addFileset(distFileSet);

                executePath.addFileset(libFileSet);
                executeIncludes += depName + "/lib/*.jar ";
                artifactIncludes += depName + "/dist/* ";
            }
        }

        if (compileIncludes.length() > 0) {
            compileFileSet.setIncludes(compileIncludes);
            getProject().addReference(compilePathId + ".fileset", compileFileSet);
        }

        if (executeIncludes.length() > 0) {
            executeFileSet.setIncludes(executeIncludes);
            getProject().addReference(executePathId + ".fileset", executeFileSet);
            getProject().addReference(executePathId + ".fileset.text", executeIncludes);
        }

        if (artifactIncludes.length() > 0) {
            artifactFileSet.setIncludes(artifactIncludes);
        } else {
            artifactFileSet.setIncludes("nothing");
        }

        getProject().addReference(artifactPathId + ".fileset", artifactFileSet);
        getProject().addReference(artifactPathId + ".fileset.text", artifactIncludes);
    }

    /**
     * Return the project name calculated from the path of this project.
     * @return the project name
     * @throws BuildException if something goes wrong
     */
    public String calcThisProjectName() throws BuildException {
        try {
            File dir = getProject().getBaseDir().getCanonicalFile();
            File wspace =
                new File(workspaceBaseDir.replace('/', File.separatorChar)).getCanonicalFile();
            String projName = "";
            while (!dir.equals(wspace)) {
                if (projName.length() > 0) {
                    projName = "/" + projName;
                }
                projName = dir.getName() + projName;
                dir = dir.getParentFile();
            }
            return projName;
        } catch (IOException err) {
            throw new BuildException(err);
        }
    }

    /**
     * @param projects
     */
    private void describeDependencies(List projects, String label) {
        System .out.println("---- " + label
                + " ---------------------------------------------".substring(label.length()));
        for (int i = 0; i < projects.size(); i++) {
            System .out.println(" " + projects.get(i));
        }
        if (projects.size() == 0) {
            System .out.println(" None.");
        }
        System .out.println("---------------------------------------------------");
    }

    /**
     * Load dependencies for a project and iterate over them.  Also add the contents of
     * extra.project.dependencies to the start of the projects List.
     * @param projName the name of the current project to example
     * @param projects accumulation of project names found
     */
    protected void followProjectDependencies(String projName, List projects,
                                             List<String> extraDeps) {
        // System .out.println("following " + projDir.getAbsolutePath());
        // Load project properties
        File projDir = getProjectBaseDir(projName);
        Properties properties = loadProjectProperties(projDir);
        String projectType = type;
        if (projectType.equals("project")) {
            projectType = "compile";
        }

        String depString = properties.getProperty(projectType + ".dependencies");

        List<String> deps = new ArrayList<String>();

        Vector bits = StringUtils.split(depString, ',');

        for (Object bit: bits) {
            deps.add(((String) bit).trim());
        }

        deps.addAll(extraDeps);

        if (deps.size() > 0) {
            // Visit dependencies
            iterateOverDependencies(projName, deps, projects);
        }
    }

    /**
     * Step over each dependency mentioned in depsString and record it. Also follow
     * each project once.
     *
     * @param deps list of project dependencies
     * @param projects accumulation of project names found
     */
    protected void iterateOverDependencies(String projName, List<String> deps, List projects) {
        for (String dep: deps) {
            if (dep.length() > 0) {
                if (projName.equals(dep)) {
                    continue;
                }

                if (!nofollow) {
                    followProjectDependencies(dep, projects, new ArrayList<String>());
                }

                if (projects.contains(dep)) {
                    // remove from current position and add to start
                    //System .out.println("Ignoring " + dep);
                } else {
                    //System .out.println("Adding   " + dep);
                    projects.add(dep);
                }
            }
        }
    }

    /**
     * Load project properties for given project.
     *
     * @param projDir project directory
     * @return Properties object containing project properties
     */
    protected Properties loadProjectProperties(File projDir) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(new File(projDir, "project.properties")));
        } catch (IOException e) {
            throw new BuildException("Failed to load project properties from "
                    + projDir.getAbsolutePath(), e);
        }
        return properties;
    }

    /**
     * Given a project name, returning the corresponding project directory.
     *
     * @param projName the project name
     * @return the project directory
     * @throws BuildException if the project directory cannot be located
     */
    protected File getProjectBaseDir(String projName) throws BuildException {
        String absPath = workspaceBaseDir + "/" + projName;
        File projDir = new File(absPath.replace('/', File.separatorChar));
        if (!projDir.exists()) {
            throw new BuildException("Expected project " + projName + " to be located at "
                    + projDir.getAbsolutePath() + " but location doesn't exist");
        }
        return projDir;
    }
}
