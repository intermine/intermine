package org.intermine.task;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;

import org.intermine.task.project.Project;
import org.intermine.task.project.ProjectXmlBinding;
import org.intermine.task.project.Source;
import org.intermine.task.project.SourceProperty;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

public class PostProcess extends Task
{
    private Reference classPathRef;
    private File projectXml, outputFile;
    private Project project;
    private String operation, action;
    private File workspaceBaseDir;

    public void setClassPathRef(Reference ref) {
        this.classPathRef = ref;
    }

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
     * Base directory that all projects are assumed relative to.
     *
     * @param basedir base directory that all projects are assumed relative to
     */
    public void setBasedir(File basedir) {
        workspaceBaseDir = basedir;
    }

    /**
     * @param action representing a source directory where we want to run the postprocessor task on.
     * */
    public void setAction(String action) {
        this.action = action;
    }
    /**
     * Sets the value of operation
     *
     * @param operation the operation to perform eg. 'Download publications'
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    
    public void execute() throws BuildException {
        if (projectXml == null) {
            throw new BuildException("no projectXml specified");
        }

        if (classPathRef == null) {
            throw new BuildException("no classPathRef specified");
        }

        project = ProjectXmlBinding.unmarshall(projectXml);
        System.out.println("Found " + project.getPostProcesses().size() + " post-processes");

        if ("${source}".equalsIgnoreCase(action)) { action = ""; }
        if ("${action}".equalsIgnoreCase(action)) { action = ""; }
        if (action == null) { action = ""; }

        //Default - do it all
        if ("".equals(action)) {
            Map postProcessMap = project.getPostProcesses();
            for (Iterator iter = postProcessMap.keySet().iterator(); iter.hasNext(); ) {
                String name = iter.next().toString();

                System.out.println(" executing post process:" + name);

                if (DO_SOURCES.equals(name)) {
                    doAllSourcePostProcessing();
                } else {
                    doCorePostProcess(name);
                }
            }
        // ok - do a specific task only
        } else {
            if (project.getPostProcesses().containsKey(action)) {
                doCorePostProcess(action);
            } else if (project.getSources().containsKey(action)) {
                doSourcePostProcess(action);
            } else {
                throw new BuildException("No postprocess/source found for:" + action);
            }
        }
    }

    private void doCorePostProcess (String postProcessName) {
        try {

            Object pp = newPostProcessTask();
            setProperty(pp, "operation", postProcessName);
            pp.getClass().getMethod("execute", new Class[0]).invoke(pp, new Object[0]);
        } catch (Exception err) {
            throw new BuildException("error setting operation on PostProcessTask (name:"
                    + postProcessName + ")", err);
        }
    }

    private void doAllSourcePostProcessing() {

        Iterator iter = project.getSources().entrySet().iterator();
        while (iter.hasNext()) {
            String thisSource = (String) ((Map.Entry) iter.next()).getKey();
            doSourcePostProcess(thisSource);
        }
    }

    private void doSourcePostProcess(String source) {
        Source s = (Source) project.getSources().get(source);
        File sourceDir = new File(new File(workspaceBaseDir, "/bio/sources"), s.getType());

        System.out.println("Performing postprocess on source:" + source + ", in dir: " + sourceDir);

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
        initAntProps(ant, s.getProperties().iterator());

        // source.name
        Property prop = ant.createProperty();
        prop.setName("source.name");
        prop.setValue(source);

        ant.execute();
    }

    private void initAntProps(Ant ant, Iterator propertyIter) {

        while (propertyIter.hasNext()) {
            SourceProperty sp = (SourceProperty) propertyIter.next();
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

    private Object newPostProcessTask() {

        ClassLoader cl = ClasspathUtils.getClassLoaderForPath(getProject(), classPathRef);
        Object pp = ClasspathUtils.newInstance("org.intermine.bio.postprocess.PostProcessTask", cl);

        try {
            setProperty(pp, "objectStore", "os.production");
            setProperty(pp, "objectStoreWriter", "osw.production");
            setProperty(pp, "project", getProject());
            //PropertyUtils.setProperty(pp, "outputFile", outputFile);
        } catch (Exception err) {
            throw new BuildException("error setting up PostProcessTask", err);
        }

        return pp;
    }

    private void setProperty(Object obj, String property, Object value) throws Exception {
        Method method = obj.getClass().getMethod("set" + property.substring(0, 1).toUpperCase() +
                property.substring(1), new Class[]{value.getClass()});
        method.invoke(obj, new Object[]{value});
    }
}
