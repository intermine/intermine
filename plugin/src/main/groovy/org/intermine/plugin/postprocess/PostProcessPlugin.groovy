package org.intermine.plugin.postprocess

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.intermine.plugin.TaskConstants
import org.intermine.plugin.project.PostProcess
import org.intermine.plugin.project.ProjectXmlBinding
import org.intermine.plugin.project.Source

class PostProcessPlugin implements Plugin<Project> {
    /**
     * Used mainly in project.xml indicate when we should iterate over the source directories when
     * we are doing the postprocessing, as some postprocess tasks will have to run before the source
     * tasks and some other tasks afterwards.
     * */
    public static final String DO_SOURCES = "do-sources"
    public static final String POSTPROCESSOR_CLASS = "postprocessor.class";

    void apply(Project project) {
        String projectXml = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"
        org.intermine.plugin.project.Project intermineProject
        List<String> processNames = new ArrayList<String>()

        project.task('initPostProcess') {
            doLast {
                intermineProject = ProjectXmlBinding.unmarshall(new File(projectXml))
                println "Found " + intermineProject.postProcesses.size() + " post-processes"

                String processInput = project.hasProperty('process') ? project.property('process') : ""
                if ("".equals(processInput) || "all".equals(processInput)) {
                    intermineProject.postProcesses.keySet().each { processName ->
                        processNames.add(processName)
                    }
                } else {
                    processNames = Arrays.asList(processInput.split("\\s*,\\s*"))
                }
                processNames.each { processName ->
                    PostProcess postProcess = intermineProject.postProcesses.get(processName)
                    if (postProcess == null) {
                        throw new InvalidUserDataException("Can't find postProcess " + processName + " in project definition file")
                    }
                }
            }
        }

        project.task('postProcess') {
            group TaskConstants.TASK_GROUP
            description "Post processes. Optional input parameters: process (process name) and source(if process=do-sources). E.g. postprocess -Pprocess=create-references"
            dependsOn 'initPostProcess'

            doLast{
                processNames.each { processName ->
                    println "Performing postprocess " + processName
                    if(DO_SOURCES.equals(processName)) {
                        //read source input property
                        List<String> sourceNames = new ArrayList<String>()
                        String sourceInput = project.hasProperty('source') ? project.property('source') : ""
                        if ("".equals(sourceInput)) {
                            intermineProject.sources.keySet().each { sourceName ->
                                sourceNames.add(sourceName)
                            }
                        } else {
                            sourceNames = Arrays.asList(sourceInput.split("\\s*,\\s*"))
                        }
                        //validation
                        sourceNames.each { sourceName ->
                            Source source = intermineProject.sources.get(sourceName)
                            if (source == null) {
                                throw new InvalidUserDataException("Can't find source " + sourceName + " in project definition file")
                            }
                        }

                        sourceNames.each { sourceName ->
                            println "Performing source postprocess on " + sourceName
                            //if POSTPROCESSOR_CLASS in source.properties
                            //doSourcePostProcess(sourceName)
                        }
                    } else {
                        //doCorePostProcess(processName)
                    }
                }
            }
        }
    }
}
