package org.intermine.plugin.integrate

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet
import org.intermine.plugin.BioSourceProperties
import org.intermine.plugin.TaskConstants
import org.intermine.plugin.dbmodel.DBModelUtils
import org.intermine.plugin.project.ProjectXmlBinding
import org.intermine.plugin.project.Source

class IntegratePlugin implements Plugin<Project> {
    String COMMON_OS_PREFIX = "common"

    void apply(Project project) {
        String projectXml = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"
        List<String> sourceNames = new ArrayList<String>()
        IntegrateAction action
        org.intermine.plugin.project.Project intermineProject
        DBModelUtils dbUtils
        IntegrateUtils integration
        BioSourceProperties bioSourceProperties

        project.configurations {
            integrateSource
            postProcesses
            bioCore
        }

        project.tasks.compileJava.dependsOn project.tasks.generateModel

        project.task('initIntegration') {
            dependsOn 'copyDefaultInterMineProperties', 'copyMineProperties', 'jar'

            doLast {
                intermineProject = ProjectXmlBinding.unmarshall(new File(projectXml));
                dbUtils = new DBModelUtils(project)
                integration = new IntegrateUtils(project, intermineProject)
                bioSourceProperties = new BioSourceProperties(intermineProject, project)
                //project.dependencies.add("compile", [group: "org.intermine", name: "bio-core", version: versions.imVersion, transitive: false])

                String sourceInput = project.hasProperty('source') ? project.property('source') : ""
                if ("".equals(sourceInput) || "all".equals(sourceInput)) {
                    println "Integrating ALL sources set project.xml"
                    intermineProject.sources.keySet().each { sourceName ->
                        sourceNames.add(sourceName)
                    }
                } else {
                    sourceNames = Arrays.asList(sourceInput.split("\\s*,\\s*"))
                }

                project.dependencies.add("bioCore", [group: "org.intermine", name: "bio-core", version: System.getProperty("bioVersion"), transitive: false])

                // keep track of the versions, only take the first one encountered
                Map<String, String> typesToVersions = new HashMap<String, String>()

                sourceNames.each { sourceName ->
                    Source source = intermineProject.sources.get(sourceName)
                    if (source == null) {
                        throw new InvalidUserDataException("Can't find source " + sourceName + " in gradleProject definition file")
                    }
                    String sourceType = source.type

                    // use default version
                    String version = System.getProperty("bioVersion")
                    // check if we have a custom version
                    if (typesToVersions.get(sourceType) != null) {
                        // we've seen this type before, use same version
                        version = typesToVersions.get(sourceType)
                    } else if (source.version != null) {
                        version = source.version
                        typesToVersions.put(sourceType, version)
                    }

                    project.dependencies.add("integrateSource", [group: "org.intermine", name: "bio-source-" + sourceType, version: version])

                    if ("so".equals(sourceType)) {
                        // extract SO.obo file and put on classpath for use by this data source

                        SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                        String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;

                        FileTree fileTree = project.zipTree(project.configurations.getByName("bioCore").singleFile)
                        PatternSet patternSet = new PatternSet()
                        patternSet.include("so.obo")
                        File file = fileTree.matching(patternSet).singleFile
                        String oboFilePath = buildResourcesMainDir + File.separator + "so.obo"
                        file.createNewFile()
                        file.renameTo(oboFilePath)
                    }
                }

                //when we have more than one source we can't split the integrate in the 2 steps: retrieve and load
                if (sourceNames.size() > 1) {
                    action = IntegrateAction.RETRIEVE_AND_LOAD
                } else {
                    String actionInput = project.hasProperty('action') ? project.property('action') : ""
                    try {
                        action = IntegrateAction.getAction(actionInput)
                    } catch (RuntimeException ex) {
                        throw new InvalidUserDataException("Unknown action: " + actionInput + ". Possible actions: load or retrieve")
                    }
                }
            }
        }

        project.task('integrate') {
            group TaskConstants.TASK_GROUP
            description "Integrates sources into production database. Optional input properties: source (source name) and action(possible values: pre-retrieve, retrieve or load). E.g. integrate -Psource=uniprot-malaria -Paction=load"
            dependsOn 'integrateSingleSource', 'integrateMultipleSources'
        }

        project.task('integrateSingleSource') {
            description "Integrates single source into production database."
            dependsOn 'initIntegration','preRetrieveSingleSource','retrieveSingleSource','loadSingleSource'
            onlyIf { sourceNames.size() == 1 }
        }

        project.task('integrateMultipleSources') {
            description "Integrates multiple sources into production database."
            dependsOn 'initIntegration'
            onlyIf { sourceNames.size() > 1 }

            doLast {
                sourceNames.each { sourceName ->
                    def osName = "os.$COMMON_OS_PREFIX-tgt-items-std"
                    def modelName = 'fulldata'

                    Properties props = bioSourceProperties.getBioSourceProperties(sourceName)
                    if (!props.containsKey("have.file.custom.direct")) {
                        println "Building tgt items database"
                        dbUtils.createSchema(osName)
                        dbUtils.createTables(osName, modelName)
                        dbUtils.storeMetadata(osName, modelName)
                    }

                    integration.preRetrieveSingleSource(sourceName)
                    println "Retrieving $sourceName in a tgt items database"
                    integration.retrieveSingleSource(sourceName)

                    if (!props.containsKey("have.file.custom.direct")) {
                        // need to do this after data is in items DB or loading is too slow
                        dbUtils.createIndexes(osName, false)
                        dbUtils.analyse(osName, modelName)
                    }

                    println "Loading $sourceName tgt items into production database"
                    integration.loadSingleSource(intermineProject.sources.get(sourceName))
                }
            }
        }

        project.task('preRetrieveSingleSource') {
            description "Pre-retrieves a single source"
            dependsOn 'initIntegration','buildTgtItems'
            onlyIf {
                sourceNames.size() == 1 && (action.equals(IntegrateAction.PRE_RETRIEVE) || action.equals(IntegrateAction.RETRIEVE) || action.equals(IntegrateAction.RETRIEVE_AND_LOAD))
            }

            doLast {
                String sourceName = sourceNames.get(0)
                integration.preRetrieveSingleSource(sourceName)
            }
        }

        project.task('retrieveSingleSource') {
            description "Retrieves a single source into tgt items database"
            dependsOn 'initIntegration','buildTgtItems', 'preRetrieveSingleSource'
            onlyIf {
                sourceNames.size() == 1 && (action.equals(IntegrateAction.RETRIEVE) || action.equals(IntegrateAction.RETRIEVE_AND_LOAD))
            }

            doLast {
                String sourceName = sourceNames.get(0)
                println "Retrieving " + sourceName + " in a tgt items database"
                integration.retrieveSingleSource(sourceName)
                Properties props = bioSourceProperties.getBioSourceProperties(sourceName)
                if (!props.containsKey("have.file.custom.direct")) {
                    // need to do this after data is in items DB or loading is too slow
                    def osName = "os.$COMMON_OS_PREFIX-tgt-items-std"
                    def modelName = 'fulldata'

                    dbUtils.createIndexes(osName, false)
                    dbUtils.analyse(osName, modelName)
                }
            }
        }

        project.task('buildTgtItems') {
            description "Builds tgt items database"
            dependsOn 'initIntegration'
            onlyIf { sourceNames.size() == 1 && !action.equals(IntegrateAction.LOAD)}

            doLast {
                Properties props = bioSourceProperties.getBioSourceProperties(sourceNames.get(0))
                if (!props.containsKey("have.file.custom.direct")) {
                    println "Building tgt items database"
                    def osName = "os.$COMMON_OS_PREFIX-tgt-items-std"
                    def modelName = 'fulldata'

                    dbUtils.createSchema(osName)
                    dbUtils.createTables(osName, modelName)
                    dbUtils.storeMetadata(osName, modelName)
                }
            }
        }

        project.task('loadSingleSource') {
            description "Loads items into production database"
            dependsOn 'initIntegration'
            mustRunAfter 'retrieveSingleSource'
            onlyIf {
                sourceNames.size() == 1 && (action.equals(IntegrateAction.LOAD) || action.equals(IntegrateAction.RETRIEVE_AND_LOAD))
            }

            doLast {
                String sourceName = sourceNames.get(0)
                println "Loading " + sourceName + " tgt items into production DB"
                integration.loadSingleSource(intermineProject.sources.get(sourceName))
            }
        }



    }
}