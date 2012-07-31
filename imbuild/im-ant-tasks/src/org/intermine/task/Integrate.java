package org.intermine.task;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.util.StringUtils;
import org.intermine.task.project.Project;
import org.intermine.task.project.ProjectXmlBinding;
import org.intermine.task.project.Source;
import org.intermine.task.project.UserProperty;

/**
 * A task that can read a project.xml file and run a data integration build.
 *
 * @author tom
 */
public class Integrate extends Task
{
    private static final String ENDL = System.getProperty("line.separator");
    
    private String [] possibleActionsArray = {
        "retrieve",
        "load",
        "clean"
    };

    private Set<String> possibleActions = new HashSet<String>(Arrays.asList(possibleActionsArray));

    private File projectXml;
    private Project intermineProject;
    private String action, sourceAttribute;

    /**
     * Set the project.xml to use for this Task.
     * @param projectXml the File
     */
    public void setProjectXml(File projectXml) {
        this.projectXml = projectXml;
    }

    /**
     * Set the action (retrieve or load) for this task.  null means run all actions.
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
        this.sourceAttribute = source;
    }


    /**
     * Run the integration.
     * @throws BuildException if parameters aren't set or the source or action is invalid
     */
    public void execute() throws BuildException {
        if (projectXml == null) {
            throw new BuildException("no projectXml specified");
        }
        if (sourceAttribute == null || "".equals(sourceAttribute.trim())) {
            throw new BuildException("no source set, try \"ant -Dsource=all\" or "
                                     + "\"ant -Dsource=source1,source2\"");
        }

        System.err.print("action: " + action + ENDL);

        if (action != null && !"".equals(action) && !possibleActions.contains(action)) {
            StringBuffer sb = new StringBuffer();
            sb.append("Unknown action: ").append(action).append("  possible actions: ");
            for (int i = 0; i < possibleActionsArray.length - 1; i++) {
                sb.append(possibleActionsArray[i]).append(", ");
            }
            sb.append(possibleActionsArray[possibleActionsArray.length - 1]);
            throw new BuildException(sb.toString());
        }

        intermineProject = ProjectXmlBinding.unmarshall(projectXml);

        System.out.print("Found " + intermineProject.getSources().size() + " sources" + ENDL);

        List<String> sourceNames = new ArrayList<String>();

        if ("".equals(sourceAttribute) || sourceAttribute.equals("all")) {
            for (String thisSource : intermineProject.getSources().keySet()) {
                sourceNames.add(thisSource);
            }
        } else {
            Vector<String> bits = StringUtils.split(sourceAttribute, ',');
            for (String bit: bits) {
                sourceNames.add(bit);
            }
        }

        for (String thisSourceName: sourceNames) {
            Source sourceObject = intermineProject.getSources().get(thisSourceName);
            if (sourceObject == null) {
                throw new BuildException("can't find source in project definition file: "
                                         + thisSourceName);
            }

            if ("".equals(action)) {
                performAction(thisSourceName, sourceObject.getType());
            } else {
                performAction(action, thisSourceName, sourceObject.getType());
            }
        }
    }

    private void performAction(String sourceName, String sourceType) {
        performAction("retrieve", sourceName, sourceType);
        performAction("load", sourceName, sourceType);
    }

    private void performAction(String actionName, String sourceName, String sourceType) {
        Source s = (Source) intermineProject.getSources().get(sourceName);
        File sourceDir = s.getLocation();
        
        System.out.print("Performing integration action \"" + actionName + "\" for source \""
                         + sourceName + "\" (" + sourceType + ") in: " + sourceDir + ENDL);

        Ant ant = new Ant();
        ant.setDir(sourceDir);
        ant.setInheritAll(false);
        ant.setTarget(actionName);
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
        for (UserProperty sp : intermineProject.getProperties()) {
            Property prop = ant.createProperty();
            if (sp.getName() == null) {
                throw new BuildException("name not set for property in: " + sourceName);
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
        for (UserProperty sp : s.getUserProperties()) {
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
        Property nameProp = ant.createProperty();
        nameProp.setName("source.name");
        nameProp.setValue(sourceName);

        Property typeProp = ant.createProperty();
        typeProp.setName("source.type");
        typeProp.setValue(sourceType);

        Property allSourcesProp = ant.createProperty();
        allSourcesProp.setName("allsources.list");
        StringBuilder sb = new StringBuilder();
        boolean needComma = false;
        for (String sn : intermineProject.getSources().keySet()) {
            if (needComma) {
                sb.append(" ");
            }
            needComma = true;
            sb.append(sn);
        }
        allSourcesProp.setValue(sb.toString());

        ant.execute();
    }
}
