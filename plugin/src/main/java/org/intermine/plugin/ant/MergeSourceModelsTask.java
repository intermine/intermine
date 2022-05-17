package org.intermine.plugin.ant;

/*
 * Copyright (C) 2002-2022 FlyMine
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

import org.intermine.plugin.project.Project;
import org.intermine.plugin.project.ProjectXmlBinding;
import org.intermine.plugin.project.Source;

import java.io.File;
import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.ClasspathUtils;

/**
 * Task to merge additions files from all sources in the project.xml into an intermine XML model.
 *
 * @author Kim Rutherford
 */

public class MergeSourceModelsTask extends Task
{
    private File modelFile;
    private String extraModelsStart;
    private String extraModelsEnd;
    private File projectXml;

    private static final String MODEL_MERGER_TASK = "org.intermine.task.ModelMergerTask";

    /**
     * Set the project.xml file to use when post-processing.
     * @param projectXml the project xml file
     */
    public void setProjectXmlPath(File projectXml) {
        this.projectXml = projectXml;
    }

    /**
     * Set the model to add additions to.
     * @param file path to model file
     */
    public void setModelFilePath(File file) {
        modelFile = file;
    }

    /**
     * The paths containing extra model additions that should be merged first.
     * @param extraModelsStart a space separated list of model addition paths
     */
    public void setExtraModelsStart(String extraModelsStart) {
        this.extraModelsStart = extraModelsStart;
    }

    /**
     * The paths containing extra model additions that should be merged last.
     * @param extraModelsEnd a space separated list of model addition paths
     */
    public void setExtraModelsEnd(String extraModelsEnd) {
        this.extraModelsEnd = extraModelsEnd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws BuildException {
        if (projectXml == null) {
            throw new BuildException("no projectXml specified");
        }

        Project imProject = ProjectXmlBinding.unmarshall(projectXml);

        List<String> pathsToMerge = new ArrayList<String>();

        if (extraModelsStart != null) {
            String[] bits = extraModelsStart.split("\\s+");

            for (int i = 0; i < bits.length; i++) {
                if (bits[i].length() > 0) {
                    addToAdditionsList(pathsToMerge, bits[i]);
                }
            }
        }

        Collection<Source> sources = imProject.getSources().values();

        for (Source source: sources) {
            String additionsFileName = source.getType() + "_additions.xml";

            addToAdditionsList(pathsToMerge, additionsFileName);
        }

        if (extraModelsEnd != null) {
            String[] bits = extraModelsEnd.split("\\s+");

            for (int i = 0; i < bits.length; i++) {
                if (bits[i].length() > 0) {
                    addToAdditionsList(pathsToMerge, bits[i]);
                }
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

    private void addToAdditionsList(List<String> pathsToMerge, String additionsFile)
        throws BuildException {
        if (MergeSourceModelsTask.class.getClassLoader().getResourceAsStream(additionsFile) != null) {
            pathsToMerge.add(additionsFile);
        } else {
            System.err.println("warning: " + additionsFile + " not found");
        }
    }

    private Task newModelMergerTask() {
        ClassLoader cl = MergeSourceModelsTask.class.getClassLoader();

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
