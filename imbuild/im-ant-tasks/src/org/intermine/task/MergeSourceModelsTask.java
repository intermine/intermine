package org.intermine.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.intermine.task.project.Project;
import org.intermine.task.project.ProjectXmlBinding;
import org.intermine.task.project.Source;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

/**
 * Task to merge additions files from all sources in the project.xml into an intermine XML model.
 *
 * @see org.intermine.modelproduction.ModelMerger
 * @author Kim Rutherford
 */

public class MergeSourceModelsTask extends Task
{
    private File modelFile;
    private String extraModelPathsStart;
    private String extraModelPathsEnd;
    /** Base directory that all projects are relative to. */
    private String workspaceBaseDir;
    private Reference classPathRef;
    private File projectXml;

    private static final String MODEL_MERGER_TASK = "org.intermine.task.ModelMergerTask";

    /**
     * Set the classpath to use for post processing.
     * @param ref the classpath reference
     */
    public void setClassPathRef(Reference ref) {
        this.classPathRef = ref;
    }

    /**
     * Base directory that all projects are assumed relative to.
     *
     * @param basedir base directory that all projects are assumed relative to
     */
    public void setBasedir(String basedir) {
        workspaceBaseDir = basedir;
    }

    /**
     * Set the project.xml file to use when post-processing.
     * @param projectXml the project xml file
     */
    public void setProjectXml(File projectXml) {
        this.projectXml = projectXml;
    }

    /**
     * Set the model to add additions to.
     * @param file path to model file
     */
    public void setModelFile(File file) {
        modelFile = file;
    }

    /**
     * The paths containing extra model additions that should be merged first.
     * @param extraModelPathsStart a space separated list of model addition paths
     */
    public void setExtraModelPathsStart(String extraModelPathsStart) {
        this.extraModelPathsStart = extraModelPathsStart;
    }

    /**
     * The paths containing extra model additions that should be merged last.
     * @param extraModelPathsEnd a space separated list of model addition paths
     */
    public void setExtraModelPathsEnd(String extraModelPathsEnd) {
        this.extraModelPathsEnd = extraModelPathsEnd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws BuildException {
        if (projectXml == null) {
            throw new BuildException("no projectXml specified");
        }
        if (workspaceBaseDir == null) {
            throw new BuildException("basedir not set");
        }
        if (classPathRef == null) {
            throw new BuildException("classPathRef not set");
        }

        Project imProject = ProjectXmlBinding.unmarshall(projectXml);

        List<File> pathsToMerge = new ArrayList<File>();

        String[] bits = extraModelPathsStart.split("\\s+");

        for (int i = 0; i < bits.length; i++) {
            if (bits[i].length() > 0) {
                addToAdditionsList(pathsToMerge,
                                   new File(workspaceBaseDir + File.separator + bits[i]));
            }
        }

        Collection<Source> sources = imProject.getSources().values();

        for (Source source: sources) {
            String additionsFileName = source.getType() + "_additions.xml";

            File additionsFile = new File(source.getLocation(), additionsFileName);
            addToAdditionsList(pathsToMerge, additionsFile);
        }

        bits = extraModelPathsEnd.split("\\s+");

        for (int i = 0; i < bits.length; i++) {
            if (bits[i].length() > 0) {
                File additionsFile = new File(workspaceBaseDir + File.separator + bits[i]);
                addToAdditionsList(pathsToMerge, additionsFile);
            }
        }

        Task mergeTask = newModelMergerTask();

        setProperty(mergeTask, "inputModelFile", modelFile);
        setProperty(mergeTask, "outputFile", modelFile);

        try {
            Method addFileSetMethod =
                mergeTask.getClass().getMethod("setAdditionsFiles", List.class);
            addFileSetMethod.invoke(mergeTask, pathsToMerge);
        } catch (Exception e) {
            throw new BuildException("exception while adding files to "
                                     + MODEL_MERGER_TASK, e);
        }

        try {
            Method method = mergeTask.getClass().getMethod("execute");
            method.invoke(mergeTask);
        } catch (Exception e) {
            throw new BuildException("exception while invoking execute on "
                                     + MODEL_MERGER_TASK, e);
        }
    }

    private void addToAdditionsList(List<File> pathsToMerge, File additionsFile)
        throws BuildException {
        try {
            File canonFile = additionsFile.getCanonicalFile();
            if (canonFile.exists()) {
                if (!pathsToMerge.contains(canonFile)) {
                    pathsToMerge.add(canonFile);
                }
            } else {
                System.err .println("warning: " + canonFile + " not found");
            }
        } catch (IOException e) {
            throw new BuildException("failed to find canonical file for: " + additionsFile, e);
        }
    }

    private Task newModelMergerTask() {
        ClassLoader cl = ClasspathUtils.getClassLoaderForPath(getProject(), classPathRef);

        Task mergeTask = (Task) ClasspathUtils.newInstance(MODEL_MERGER_TASK, cl);

        try {
            setProperty(mergeTask, "project", getProject());
        } catch (Exception err) {
            throw new BuildException("error setting up PostProcessTask", err);
        }

        return mergeTask;
    }

    private void setProperty(Object obj, String property, Object value) throws BuildException {
        try {
            Method method =
                obj.getClass().getMethod("set" + property.substring(0, 1).toUpperCase()
                                         + property.substring(1), new Class[]{value.getClass()});
            method.invoke(obj, new Object[]{value});
        } catch (Exception e) {
            throw new BuildException("exception while setting property " + property + " to "
                                     + value, e);
        }
    }
}
