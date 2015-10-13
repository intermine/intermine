package org.intermine.task;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.ClasspathUtils;
import org.intermine.task.project.PostProcess;
import org.intermine.task.project.Project;
import org.intermine.task.project.ProjectXmlBinding;
import org.intermine.task.project.Source;
import org.intermine.task.project.UserProperty;

/**
 * Class that operates on the PostProcessing section of a Mines project.xml file.
 *
 * The class will iterate over the post processes in the project.xml in the order that they are
 * listed in.
 *
 * If there is a do-source postprocess all the sources included in the project.xml will
 * be examined to see if they have a post process step of their own, if so, it will be called.
 *
 * @author Peter McLaren
 */
public class PostProcessTask extends Task
{
    /**
     * The property (from "project.properties") that sets the postprocessor class to use.
     */
    public static final String POSTPROCESSOR_CLASS = "postprocessor.class";

    private Reference classPathRef;
    private File projectXml;
    private Project project;
    private String action;

    /**
     * Set the classpath to use for post processing.
     * @param ref the classpath reference
     */
    public void setClassPathRef(Reference ref) {
        this.classPathRef = ref;
    }

    /**
     * Set the project.xml file to use when post-processing.
     * @param projectXml the project xml file
     */
    public void setProjectXml(File projectXml) {
        this.projectXml = projectXml;
    }

    /**
     * Used mainly in project.xml indicate when we should iterate over the source directories when
     * we are doing the postprocessing, as some postprocess tasks will have to run before the source
     * tasks and some other tasks afterwards.
     * */
    public static final String DO_SOURCES = "do-sources";

    /**
     * Set the action to perform - ie. the post-process
     * @param action representing a source directory where we want to run the postprocessor task on.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws BuildException {
        if (projectXml == null) {
            throw new BuildException("no projectXml specified");
        }

        if (classPathRef == null) {
            throw new BuildException("no classPathRef specified");
        }

        project = ProjectXmlBinding.unmarshall(projectXml);
        System.out.print("Found " + project.getPostProcesses().size() + " post-processes\n");

        if ("${source}".equalsIgnoreCase(action)) { action = ""; }
        if ("${action}".equalsIgnoreCase(action)) { action = ""; }
        if (action == null) { action = ""; }

        // Default - do it all
        if ("".equals(action)) {
            for (String name: project.getPostProcesses().keySet()) {
                System.out.print(" executing post process: " + name + "\n");

                if (DO_SOURCES.equals(name)) {
                    doAllSourcePostProcessing();
                } else {
                    doCorePostProcess(name);
                }
            }

        // ok - do a specific task only
        } else {
            if (DO_SOURCES.equals(action)) {
                doAllSourcePostProcessing();
            } else if (project.getPostProcesses().containsKey(action)) {
                doCorePostProcess(action);
            } else if (project.getSources().containsKey(action)) {
                doSourcePostProcess(action);
            } else {
                throw new BuildException("No postprocess/source found for: " + action);
            }
        }
    }

    private void doCorePostProcess (String postProcessName) {
        System.err.print("Performing postprocess: " + postProcessName + "\n");

        PostProcess p = project.getPostProcesses().get(postProcessName);
        try {
            Task pp = newPostProcessTask();
            setProperty(pp, "operation", postProcessName);

            for (UserProperty up: p.getUserProperties()) {
                if (up.isLocation()) {
                    pp.getProject().setUserProperty(up.getName(), up.getLocation());
                } else {
                    pp.getProject().setUserProperty(up.getName(), up.getValue());
                }
            }

            pp.getClass().getMethod("execute", new Class[0]).invoke(pp, new Object[0]);
        } catch (Exception err) {
            throw new BuildException("error running PostProcessTask (action: "
                    + postProcessName + ")", err);
        }
    }

    private void doAllSourcePostProcessing() {
        for (String thisSource : project.getSources().keySet()) {
            doSourcePostProcess(thisSource);
        }
    }

    private void doSourcePostProcess(String source) {
        // TODO this needs to work out a way to pass the current ClassLoader to the and subtarget
        // that is being executed (newPostProcessTask()). As it stands the do-sources step uses a
        // differenct ClassLoader for each source meaning database connections can't be shared and
        // may eventually run out.
        //
        // Creating the Ant object with the current ClassLoader doesn't seem to be sufficient to
        // force it's use in actual execution.

        Source s = project.getSources().get(source);
        File sourceDir = s.getLocation();

        Properties projProps = Dependencies.loadProjectProperties(sourceDir);

        if (projProps.get(POSTPROCESSOR_CLASS) == null) {
            // no post-process for this source
            System.err.print("no postprocess for source: " + source + ", in dir: " + sourceDir
                             + "\n");
            return;
        }

        System.err.print("Performing postprocess on source: " + source + ", in dir: "
                + sourceDir + "\n");

        Ant ant = new Ant();
        ant.setDir(sourceDir);
        ant.setInheritAll(false);
        ant.setTarget("postprocess");
        ant.setProject(getProject());

        // Tell sub-invocation to execute targets on dependencies.  This is needed so that ant in
        // the source directories correctly runs ant on it's dependencies
        Property depProp = ant.createProperty();
        depProp.setName("no.dep");
        depProp.setValue("false");
        depProp.setProject(getProject());
        depProp.execute();

        // Add global properties
        initAntProps(ant, project.getProperties().iterator());

        // Add source properties
        initAntProps(ant, s.getUserProperties().iterator());

        // source.name
        Property prop = ant.createProperty();
        prop.setName("source.name");
        prop.setValue(source);

        ant.execute();
    }

    private void initAntProps(Ant ant, Iterator<UserProperty> propertyIter) {
        while (propertyIter.hasNext()) {
            UserProperty sp = propertyIter.next();
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
    }

    private Task newPostProcessTask() {
        // use ant's ClasspathUtils to construct the postprocess operation task with the current
        // ClassLoader (and make sure all instances share a ClassLoader. This is important as
        // database connections are only reused within a ClassLoader - by default a different one
        // is used each time and we run out of database connections.
        Path path = (Path) classPathRef.getReferencedObject();
        ClassLoader cl = ClasspathUtils.getClassLoaderForPath(getProject(),
                path, path.toString(), false, true);
        String className = "org.intermine.bio.postprocess.PostProcessOperationsTask";
        // use reflection to avoid depending on the bio/postprocess project
        Object pp = ClasspathUtils.newInstance(className, cl);

        try {
            setProperty(pp, "objectStoreWriter", "osw.production");
            setProperty(pp, "project", getProject());
        } catch (Exception err) {
            throw new BuildException("error setting up PostProcessTask", err);
        }

        return (Task) pp;
    }

    private void setProperty(Object obj, String property, Object value) throws Exception {
        Method method =
            obj.getClass().getMethod("set" + property.substring(0, 1).toUpperCase()
                                     + property.substring(1), new Class[]{value.getClass()});
        method.invoke(obj, new Object[]{value});
    }
}
