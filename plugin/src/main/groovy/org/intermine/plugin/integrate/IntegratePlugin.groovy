package org.intermine.plugin.integrate

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.intermine.plugin.TaskConstants
import org.intermine.plugin.VersionConfig
import org.intermine.plugin.dbmodel.DBUtils
import org.intermine.project.ProjectXmlBinding
import org.intermine.project.Source

class IntegratePlugin implements Plugin<Project> {
    String COMMON_OS_PREFIX = "common"

    void apply(Project project) {
        String projectXml = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"
        List<String> sourceNames = new ArrayList<String>()
        IntegrateAction action
        org.intermine.project.Project intermineProject
        DBUtils dbUtils
        IntegrateUtils integration
        VersionConfig versions = project.extensions.create('integrationVersionConfig', VersionConfig)

        project.configurations {
            integrateSource
        }

        project.task('initIntegration') {
            dependsOn 'copyDefaultInterMineProperties','copyMineProperties'

            doLast {
                intermineProject = ProjectXmlBinding.unmarshall(new File(projectXml));
                dbUtils = new DBUtils(project)
                integration = new IntegrateUtils(project, intermineProject)
                project.dependencies.add("compile", [group: "org.intermine", name: "bio-core", version: versions.bioVersion, transitive: false])

                String sourceInput = project.hasProperty('source') ? project.property('source') : ""
                if ("".equals(sourceInput) || "all".equals(sourceInput)) {
                    intermineProject.sources.keySet().each { sourceName ->
                        sourceNames.add(sourceName)
                    }
                } else {
                    sourceNames = Arrays.asList(sourceInput.split("\\s*,\\s*"))
                }
                sourceNames.each { sourceName ->
                    Source source = intermineProject.sources.get(sourceName)
                    if (source == null) {
                        throw new InvalidUserDataException("Can't find source " + sourceName + " in project definition file")
                    }
                    String sourceType = source.type
                    project.dependencies.add("integrateSource", [group: "org.intermine", name: "bio-source-" + sourceType, version: versions.bioVersion, transitive: false])
                }

                //when we have more than one source we can't split the integrate in the 2 steps: retrieve and load
                if (sourceNames.size() > 1) {
                    action = IntegrateAction.getAction(null)
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
            description "Integrates sources into production database. Optional input properties: source (source name) and action(possible values: retrieve or load). E.g. integrate -Psource=uniprot-malaria -Paction=load"
            dependsOn 'integrateSingleSource', 'integrateMultipleSources'
        }

        project.task('integrateSingleSource') {
            description "Integrates single source into production database."
            dependsOn 'initIntegration','retrieveSingleSource','loadSingleSource'
            onlyIf { sourceNames.size() == 1 }
        }

        project.task('integrateMultipleSources') {
            description "Integrates multiple sources into production database."
            dependsOn 'initIntegration'
            onlyIf { sourceNames.size() > 1 }

            doLast {
                sourceNames.each { sourceName ->
                    Properties bioSourceProperties = integration.getBioSourceProperties(sourceName)
                    if (!bioSourceProperties.containsKey("have.file.custom.direct")) {
                        println "Building tgt items database"
                        dbUtils.createSchema("os." + COMMON_OS_PREFIX + "-tgt-items-std", "fulldata")
                        dbUtils.createTables("os." + COMMON_OS_PREFIX + "-tgt-items-std", "fulldata")
                        dbUtils.storeMetadata("os." + COMMON_OS_PREFIX + "-tgt-items-std", "fulldata")
                        dbUtils.analyse("os." + COMMON_OS_PREFIX + "-tgt-items-std", "fulldata")
                    }
                    println "Retrieving " + sourceName + " in a tgt items database"
                    integration.retrieveSingleSource(sourceName)
                    println "Loading " + sourceName + " tgt items into production database"
                    integration.loadSingleSource(intermineProject.sources.get(sourceName))
                }
            }

        }

        project.task('retrieveSingleSource') {
            description "Retrieves a single source into tgt items database"
            dependsOn 'initIntegration','buildTgtItems'
            onlyIf {
                sourceNames.size() == 1 && (action.equals(IntegrateAction.RETRIEVE) || action.equals(IntegrateAction.RETRIEVE_AND_LOAD))
            }

            doLast {
                Properties bioSourceProperties = integration.getBioSourceProperties(sourceNames.get(0))
                String sourceName = sourceNames.get(0)
                println "Retrieving " + sourceName + " in a tgt items database"
                integration.retrieveSingleSource(sourceName)

            }
        }

        project.task('buildTgtItems') {
            description "Builds tgt items database"
            dependsOn 'initIntegration'
            onlyIf { sourceNames.size() == 1 && !action.equals(IntegrateAction.LOAD)}

            doLast {
                Properties bioSourceProperties = integration.getBioSourceProperties(sourceNames.get(0))
                if (!bioSourceProperties.containsKey("have.file.custom.direct")) {
                    println "Building tgt items database"
                    dbUtils.createSchema("os." + COMMON_OS_PREFIX + "-tgt-items-std", "fulldata")
                    dbUtils.createTables("os." + COMMON_OS_PREFIX + "-tgt-items-std", "fulldata")
                    dbUtils.storeMetadata("os." + COMMON_OS_PREFIX + "-tgt-items-std", "fulldata")
                    dbUtils.analyse("os." + COMMON_OS_PREFIX + "-tgt-items-std", "fulldata")
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