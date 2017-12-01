package org.intermine.plugin.dbmodel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet
import org.intermine.plugin.TaskConstants
import org.intermine.plugin.VersionConfig

class DataBasePlugin implements Plugin<Project> {

    DBConfig config
    DBUtils dbUtils
    VersionConfig versionConfig
    String buildResourcesMainDir
    boolean regenerateModel = true

    void apply(Project project) {

        project.configurations {
            bioCore
            mergeSource
            commonResources
            api
        }

        versionConfig = project.extensions.create('versionConfig', VersionConfig)

        project.dependencies {
            bioCore group : "org.intermine", name: "bio-core", version: versionConfig.bioVersion, transitive: false
            commonResources group: "org.intermine", name: "intermine-resources", version: versionConfig.imVersion
            api group: "org.intermine", name: "intermine-api", version: versionConfig.imVersion, transitive: false
        }

        project.task('initConfig') {
            config = project.extensions.create('dbConfig', DBConfig)
            dbUtils = new DBUtils(project)
            SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
            buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
            if (new File(project.getBuildDir().getAbsolutePath() + File.separator + "gen").exists()) {
                regenerateModel = false
            }
        }

        project.task('copyDefaultInterMineProperties') {
            description "Copies default.intermine.integrate.properties file into resources output"
            dependsOn 'initConfig', 'processResources'
            doLast {
                dbUtils.copyDefaultPropertiesFile(config.defaultInterminePropertiesFile)
            }
        }

        project.task('copyGenomicModel') {
            dependsOn 'initConfig', 'processResources'
            onlyIf {regenerateModel}

            doLast {
                FileTree fileTree = project.zipTree(project.configurations.getByName("bioCore").singleFile)
                PatternSet patternSet = new PatternSet();
                patternSet.include("core.xml");
                File file = fileTree.matching(patternSet).singleFile
                String modelFilePath = buildResourcesMainDir + File.separator + config.modelName + "_model.xml"
                file.renameTo(modelFilePath)
                file.createNewFile()
            }
        }

        project.task('createSoModel') {
            description "Reads SO OBO files and writes so_additions.xml"
            dependsOn 'initConfig', 'processResources'
            onlyIf {regenerateModel}

            doLast {
                def ant = new AntBuilder()
                ant.taskdef(name: "createSoModel", classname: "org.intermine.bio.task.SOToModelTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("bioCore").asPath)
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.createSoModel(soTermListFile: config.soTermListFilePath, outputFile: config.soAdditionFilePath)
            }
        }

        project.task('generateModel') {
            description "Merges different source model files into an intermine XML model"
            dependsOn 'initConfig', 'mergeModels'
            onlyIf {regenerateModel}

            doLast {
                dbUtils.generateModel(config.modelName)
            }
        }
        project.getTasks().getByName("compileJava").dependsOn(project.getTasks().getByName("generateModel"))

        project.task('buildDB') {
            group TaskConstants.TASK_GROUP
            description "Build the database for the webapp"
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'jar'

            doLast {
                dbUtils.createSchema(config.objectStoreName, config.modelName)
                dbUtils.createTables(config.objectStoreName, config.modelName)
                dbUtils.storeMetadata(config.objectStoreName, config.modelName)
                dbUtils.createIndexes(config.objectStoreName, config.modelName)
                dbUtils.analyse(config.objectStoreName, config.modelName)
            }
        }

        project.task('buildUnitTestDB') {
            description "Build the database for the webapp"
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'jar'

            doLast {
                dbUtils.createSchema(config.objectStoreName, config.modelName)
                dbUtils.createTables(config.objectStoreName, config.modelName)
                dbUtils.storeMetadata(config.objectStoreName, config.modelName)
            }
        }

        project.task('copyUserProfileModel') {
            dependsOn 'initConfig', 'processResources'

            doLast {
                FileTree fileTree = project.zipTree(project.configurations.getByName("api").singleFile)
                PatternSet patternSet = new PatternSet();
                patternSet.include("userprofile_model.xml");
                File file = fileTree.matching(patternSet).singleFile
                String modelFilePath = buildResourcesMainDir + File.separator + config.userProfileModelName + "_model.xml"
                file.renameTo(modelFilePath)
                file.createNewFile()
            }
        }

        project.task('buildUserDB') {
            group TaskConstants.TASK_GROUP
            description "Build the user database for the webapp"
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'copyUserProfileModel', 'jar'

            doLast {
                dbUtils.createSchema(config.userProfileObjectStoreName, config.userProfileModelName)
                dbUtils.createTables(config.userProfileObjectStoreName, config.userProfileModelName)
                dbUtils.storeMetadata(config.userProfileObjectStoreName, config.userProfileModelName)
            }
        }
    }
}

