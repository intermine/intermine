package org.intermine.plugin.postprocess

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.BuildException
import org.intermine.plugin.BioSourceProperties
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
    public static final String CREATE_SEARCH_INDEX = "create-search-index"
    public static final String CREATE_AUTO_INDEX = "create-autocomplete-index"

    void apply(Project project) {
        String projectXml = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"
        org.intermine.plugin.project.Project intermineProject
        List<String> processNames = new ArrayList<String>()
        BioSourceProperties bioSourceProperties

        project.task('initPostProcess') {
            doLast {
                intermineProject = ProjectXmlBinding.unmarshall(new File(projectXml))
                println "Found " + intermineProject.postProcesses.size() + " post-processes"
                bioSourceProperties = new BioSourceProperties(intermineProject, project)

                String processInput = project.hasProperty('process') ? project.property('process') : ""
                if ("".equals(processInput) || "all".equals(processInput)) {
                    println "Performing ALL " + intermineProject.postProcesses.size() + " post-processes"
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

                    // default version
                    String version = System.getProperty("bioVersion")
                    // check if we have a custom version
                    if (postProcess.version != null) {
                        version = postProcess.version
                    }

                    if (!DO_SOURCES.equals(processName)) {
                        project.dependencies.add("postProcesses", [group: "org.intermine", name: "bio-postprocess-" + processName, version: version])
                    }
                }
            }
        }

        project.task('postProcess') {
            group TaskConstants.TASK_GROUP
            description "Post processes. Optional input parameters: process (process name) and source(if process=do-sources). E.g. postprocess -Pprocess=create-references"
            dependsOn 'initPostProcess', 'compileJava', 'copyDefaultInterMineProperties', 'copyMineProperties'

            doLast{
                processNames.each { processName ->
                    println "Performing postprocess " + processName + "."
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
                        //validation + add dependency
                        sourceNames.each { sourceName ->
                            Source source = intermineProject.sources.get(sourceName)
                            if (source == null) {
                                throw new InvalidUserDataException("Can't find source " + sourceName + " in project definition file")
                            }
                            String version = System.getProperty("bioVersion")
                            if (source.getVersion() != null) {
                                version = source.getVersion()
                            }
                            project.dependencies.add("integrateSource", [group: "org.intermine", name: "bio-source-" + source.type, version: version])
                        }

                        sourceNames.each { sourceName ->
                            if (bioSourceProperties.postprocessorExists(sourceName)) {
                                println "Performing source postprocess on " + sourceName
                                String postprocessorClassName = bioSourceProperties.getPostprocessorClassname(sourceName)
                                Source source = intermineProject.sources.get(sourceName)
                                def ant = new AntBuilder()
                                source.userProperties.each { prop ->
                                    if (prop.location != null) {
                                        ant.project.setProperty(prop.name, prop.location)//TODO resolve path
                                    } else {
                                        ant.project.setProperty(prop.name, prop.value)
                                    }
                                }
                                ant.project.setProperty("source.name", sourceName)
                                ant.taskdef(name: "sourcePostProcess", classname: "org.intermine.task.PostProcessorTask") {
                                    classpath {
                                        dirset(dir: project.getBuildDir().getAbsolutePath())
                                        pathelement(path: project.configurations.getByName("compile").asPath)
                                        pathelement(path: project.configurations.getByName("integrateSource").asPath)
                                    }
                                }
                                ant.sourcePostProcess(clsName: postprocessorClassName, osName: "osw.production")
                            }
                        }
                    } else {
                        try {
                            def ant = new AntBuilder()
                            String postprocessorClassName = bioSourceProperties.getPostProcesserClassName(processName)
                            ant.taskdef(name: "corePostProcess", classname: "org.intermine.task.PostProcessorTask") {
                                classpath {
                                dirset(dir: project.getBuildDir().getAbsolutePath())
                                pathelement(path: project.configurations.getByName("compile").asPath)
                                pathelement(path: project.configurations.getByName("postProcesses").asPath)
                                }
                            }
                            ant.corePostProcess(clsName: postprocessorClassName, osName: "osw.production")
                        } catch(Exception | BuildException e) {
                            println "POSTPROCESS " + processName + " FAILED."
                            println e.message
                            if(CREATE_SEARCH_INDEX.equals(processName)
                                    || CREATE_AUTO_INDEX.equals(processName)) {
                                println "Please correct the error and run again ONLY THE POSTPROCESS"
                                println "./gradlew postprocess -Pprocess=" + processName
                                println "NO NEED TO RE-RUN THE ENTIRE BUILD"
                            }
                            return
                        }
                    }
                    println "Postprocess " + processName + " completed."
                }
            }
        }
    }
}
