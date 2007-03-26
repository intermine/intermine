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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import org.intermine.task.project.Project;
import org.intermine.task.project.ProjectXmlBinding;
import org.intermine.task.project.Source;
import org.intermine.task.project.UserProperty;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;

/**
 * A task that can read a project.xml file and run an data integration build.
 * @author tom
 */
public class Integrate extends Task
{
    private String [] possibleActionsArray = {
        "retrieve",
        "translate",
        "load",
        "clean"
    };

    private Set possibleActions = new HashSet(Arrays.asList(possibleActionsArray));

    private File projectXml;
    private Project intermineProject;
    private String action, source;
    private File workspaceBaseDir;

    /**
     * Set the project.xml to use for this Task.
     * @param projectXml the File
     */
    public void setProjectXml(File projectXml) {
        this.projectXml = projectXml;
    }

    /**
     * Set the action (retrieve, translate or load) for this task.  null means run all actions.
     * @param action the action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Set the source to integrate.  null means integrate all sources.
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Base directory that all projects are assumed relative to.
     *
     * @param basedir base directory that all projects are assumed relative to
     */
    public void setBasedir(File basedir) {
        workspaceBaseDir = basedir;
    }

    public void execute() throws BuildException {
        if (projectXml == null) {
            throw new BuildException("no projectXml specified");
        }
        if (workspaceBaseDir == null) {
            throw new BuildException("no workspaceBaseDir specified");
        }

        System.err.println ("action: " + action);

        if (action != null && !action.equals("") && !possibleActions.contains(action)) {
            StringBuffer sb = new StringBuffer();
            sb.append("Unknown action: ").append(action).append("  possible actions: ");
            for (int i = 0; i < possibleActionsArray.length - 1; i++) {
                sb.append(possibleActionsArray[i]).append(", ");
            }
            sb.append(possibleActionsArray[2]);
            throw new BuildException(sb.toString());
        }

        intermineProject = ProjectXmlBinding.unmarshall(projectXml);

        System.out.println("Found " + intermineProject.getSources().size() + " sources");

        if (source.equals("")) {
            Iterator iter = intermineProject.getSources().entrySet().iterator();
            while (iter.hasNext()) {
                String thisSource = (String) ((Map.Entry) iter.next()).getKey();
                if (action.equals("")) {
                    performAction(thisSource);
                } else {
                    performAction(action, thisSource);
                }
            }
        } else {
            if (intermineProject.getSources().get(source) == null) {
                throw new BuildException("can't find source in project definition file: " + source);
            }

            if (action.equals("")) {
                performAction(source);
            } else {
                performAction(action, source);
            }
        }
    }

    private void performAction(String source) {
        performAction("retrieve", source);
        performAction("translate", source);
        performAction("load", source);
    }

    private void performAction(String action, String source) {
        Source s = (Source) intermineProject.getSources().get(source);
        File baseDir = new File(workspaceBaseDir,
                                intermineProject.getType() + File.separatorChar + "sources"); 
        File sourceDir = new File(baseDir, s.getType());

        System.out.println("Performing integration action \"" + action + "\" for source \""
                           + source + "\" in directory: " + sourceDir);

        Ant ant = new Ant();
        ant.setDir(sourceDir);
        ant.setInheritAll(false);
        ant.setTarget(action);
        ant.setProject(getProject());

        // Tell sub-invocation to execute targets on dependencies.  This is needed so that ant in
        // the source directories correctly runs ant on it's dependencies
        Property depProp = ant.createProperty();
        depProp.setName("no.dep");
        depProp.setValue("false");
        depProp.setProject(getProject());
        depProp.execute();

        // pass the integrate project's basedir to the source
        Property integrateBasedir = ant.createProperty();
        integrateBasedir.setName("integrate.basedir");
        integrateBasedir.setLocation(getProject().getBaseDir());
        integrateBasedir.setProject(getProject());
        integrateBasedir.execute();
        
        // Add global properties
        Iterator globalPropIter = intermineProject.getProperties().iterator();
        while (globalPropIter.hasNext()) {
            UserProperty sp = (UserProperty) globalPropIter.next();
            Property prop = ant.createProperty();
            if (sp.getName() == null) {
                throw new BuildException("name not set for property in: " + source);
            } else {
                prop.setName(sp.getName());
            }
            if (sp.isLocation()) {
                prop.setLocation(getProject().resolveFile(sp.getLocation()));
            } else {
                if (sp.getValue() == null) {
                    throw new BuildException("value null for property: " + sp.getName());
                } else {
                    prop.setValue(sp.getValue());
                }
            }
            prop.setProject(getProject());
            prop.execute();
        }

        // Add source properties
        Iterator propIter = s.getUserProperties().iterator();
        while (propIter.hasNext()) {
            UserProperty sp = (UserProperty) propIter.next();
            Property prop = ant.createProperty();
            prop.setName(sp.getName());
            if (sp.isLocation()) {
                prop.setLocation(getProject().resolveFile(sp.getLocation()));
            } else {
                prop.setValue(sp.getValue());
            }
            prop.setProject(getProject());
            prop.execute();
        }

        // source.name
        Property prop = ant.createProperty();
        prop.setName("source.name");
        prop.setValue(source);

        ant.execute();
    }
}
