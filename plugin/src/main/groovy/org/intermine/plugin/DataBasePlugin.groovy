package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet

class DataBasePlugin implements Plugin<Project> {
    // TODO pass these into plugin
    public final static String bioVersion = "2.0.0-SNAPSHOT"
    public final static String antVersion = "2.0.0-SNAPSHOT"
    public final static String imVersion = "2.0.0-SNAPSHOT"
    public final static String TASK_GROUP = "InterMine"

    DBConfig config;
    DBUtils dbUtils
    String buildResourcesMainDir
    boolean regenerateModel = true

    void apply(Project project) {

        project.configurations {
            bioCore
            mergeSource
            commonResources
            api
        }

        project.dependencies {
            bioCore group : "org.intermine", name: "bio-core", version: bioVersion, transitive: false
            mergeSource group : "org.intermine", name: "ant-tasks", version: antVersion
            commonResources group: "org.intermine", name: "intermine-resources", version: imVersion
            api group: "org.intermine", name: "intermine-api", version: imVersion, transitive: false
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
            group TASK_GROUP
            description "Build the database for the webapp"
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'jar'

            doLast {
                dbUtils.buildDB(config.objectStoreName, config.modelName)
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
            group TASK_GROUP
            description "Build the user database for the webapp"
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'copyUserProfileModel', 'copyMineProperties', 'jar'

            doLast {
                dbUtils.buildDB(config.userProfileObjectStoreName, config.userProfileModelName)
            }
        }
    }
}

