package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet

class DataBasePlugin implements Plugin<Project> {
    // TODO pass these into plugin
    String bioVersion = "2.0.0-SNAPSHOT"
    String antVersion = "2.0.0-SNAPSHOT"
    DBConfig config;
    String buildResourcesMainDir
    SourceSetContainer sourceSets
    public final static String TASK_GROUP = "InterMine"

    void apply(Project project) {

        project.configurations {
            bioCore
            mergeSource
        }

        project.dependencies {
            bioCore group : "org.intermine", name: "bio-core", version: bioVersion, transitive: false
            mergeSource group : "org.intermine", name: "ant-tasks", version: antVersion
        }

        project.task('initConfig') {
            config = project.extensions.create('dbConfig', DBConfig)
            sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
            buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
        }

        project.task('copyDefaultProperties') {
            description "Copies default.intermine.integrate.properties file into resources output"
            dependsOn 'initConfig', 'processResources'

            doLast {
                FileTree fileTree = project.zipTree(project.configurations.getByName("commonResources").singleFile)
                PatternSet patternSet = new PatternSet();
                patternSet.include(config.defaultInterminePropertiesFile);
                File file = fileTree.matching(patternSet).singleFile
                String defaultIMProperties = buildResourcesMainDir + File.separator + "default.intermine.properties"
                file.renameTo(defaultIMProperties)
                file.createNewFile()
            }
        }

        project.task('copyGenomicModel') {
            dependsOn 'initConfig', 'processResources'

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
/*
        project.task('mergeTestModels') {
            group TASK_GROUP
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'initConfig', 'copyGenomicModel', 'copyModelProperties', 'createSoModel'

            doLast {
                def ant = new AntBuilder()
                String inputModelFilePath = buildResourcesMainDir + File.separator + config.modelName + "_model.xml"

                String[] modelNames = config.allModelNames.split(" ")
                for (String modelName: modelNames) {
                    if (!"genomic".equalsIgnoreCase(modelName)) {
                        project.dependencies.add("mergeSource", [group: "org.intermine", name: "bio-source-" + modelName, version: bioVersion])
                    }
                }

                ant.taskdef(name: "mergeAllSourceModels", classname: "org.intermine.task.ModelMergerTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("mergeSource").asPath)
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                    }
                }
                for (String modelFileName: modelFileNames) {
                    ant.mergeAllSourceModels(
                            inputModelFile: inputModelFilePath,
                            additionsFile: modelFileName + "_addition.xml",
                            outputFile: inputModelFilePath)
                }

            }
        }
*/
        project.task('generateModel') {
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'initConfig', 'mergeModels'

            doLast {
                def ant = new AntBuilder()

                //String destination = project.projectDir.absolutePath + "/src/main/java"
                String destination = project.getBuildDir().getAbsolutePath() + File.separator + "gen"
                ant.taskdef(name: "modelOutputTask", classname: "org.intermine.task.ModelOutputTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("compile").asPath)
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                    }
                }
                ant.modelOutputTask(model: config.modelName, destDir: destination, type: "java")
            }
        }
        project.getTasks().getByName("compileJava").dependsOn(project.getTasks().getByName("generateModel"))

        project.task('buildDB') {
            group TASK_GROUP
            description "Build the database for the webapp"
            dependsOn 'initConfig', 'copyDefaultProperties', 'jar'

            doLast {
                def ant = new AntBuilder()

                //create schema file
                String schemaFile = config.objectStoreName + "-schema.xml"
                String destination = project.getBuildDir().getAbsolutePath() + File.separator + schemaFile
                ant.taskdef(name: "torque", classname: "org.intermine.objectstore.intermine.TorqueModelOutputTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.torque(osname: config.objectStoreName, destFile:destination)

                //create db tables
                String tempDirectory = project.getBuildDir().getAbsolutePath() + File.separator + "tmp"
                ant.taskdef(name: "buildDB", classname: "org.intermine.task.BuildDbTask") {
                    classpath {
                        pathelement(path: project.buildDir.getAbsolutePath())//to read the schema
                        dirset(dir: buildResourcesMainDir)//to read default.intermine.properties
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.buildDB(osname: config.objectStoreName, model: config.modelName,
                        schemafile: schemaFile, tempDir: tempDirectory)

                //store metadata into db
                ant.taskdef(name: 'insertModel', classname: 'org.intermine.task.StoreMetadataTask') {
                    classpath {
                        pathelement(path: project.configurations.getByName("compile").asPath)
                        dirset(dir: buildResourcesMainDir) // intermine.properties
                    }
                }
                ant.insertModel(osname: config.objectStoreName, modelName: config.modelName)
            }
        }
    }
}

