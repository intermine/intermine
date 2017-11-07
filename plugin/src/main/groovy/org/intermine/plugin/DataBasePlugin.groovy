package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.util.PatternSet

class DataBasePlugin implements Plugin<Project> {
    DBConfig dbConfig;
    public final static String TASK_GROUP = "InterMine"

    void apply(Project project) {
        project.task('initConfig') {
            def config = project.extensions.create('dbConfig', DBConfig)
            dbConfig = config
        }

        project.task('copyGenomicModel') {
            dependsOn 'initConfig', 'processResources'

            doLast {
                FileTree fileTree = project.zipTree(project.configurations.getByName("bioCore").singleFile)
                PatternSet patternSet = new PatternSet();
                patternSet.include("core.xml");
                File file = fileTree.matching(patternSet).singleFile
                String modelFilePath = project.buildDir.absolutePath + File.separator + "resources" + File.separator + "main" + File.separator + dbConfig.modelName + "_model.xml"
                file.renameTo(modelFilePath)
                file.createNewFile()
            }
        }

        project.task('createSoModel') {
            group TASK_GROUP
            description "Reads SO OBO files and writes so_additions.xml"
            dependsOn 'initConfig', 'processResources'

            doLast {
                def ant = new AntBuilder()
                ant.taskdef(name: "createSoModel", classname: "org.intermine.bio.task.SOToModelTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("so").asPath)
                    }
                }
                ant.createSoModel(soTermListFile: dbConfig.soTermListFilePath, outputFile: dbConfig.soAdditionFilePath)
            }
        }

        project.task('mergeModels') {
            group TASK_GROUP
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'initConfig', 'copyGenomicModel', 'createSoModel'

            doLast {
                def ant = new AntBuilder()
                String projectXmlFilePath = project.getParent().getProjectDir().getAbsolutePath() + File.separator +  "project.xml"
                String modelFilePath = project.buildDir.absolutePath + File.separator + "resources" + File.separator + "main" + File.separator + dbConfig.modelName + "_model.xml"
                ant.taskdef(name: "mergeSourceModels", classname: "org.intermine.task.MergeSourceModelsTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("mergeSource").asPath)
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                    }
                }
                ant.mergeSourceModels(projectXmlPath: projectXmlFilePath,
                        modelFilePath: modelFilePath,
                        extraModelsStart: dbConfig.extraModelsStart,
                        extraModelsEnd: dbConfig.extraModelsEnd)

            }
        }

        project.task('generateModel') {
            group TASK_GROUP
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'initConfig', 'mergeModels'

            doLast {
                def ant = new AntBuilder()
                String destination = project.getBuildDir().getAbsolutePath() + File.separator + "gen"
                ant.taskdef(name: "modelOutputTask", classname: "org.intermine.task.ModelOutputTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("compile").asPath)
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                    }
                }
                ant.modelOutputTask(model: dbConfig.modelName, destDir: destination, type: "java")
            }
        }

        project.task('buildDB') {
            group TASK_GROUP
            description "Build the database for the webapp"
            dependsOn 'initConfig', 'jar'

            doLast {
                def ant = new AntBuilder()
                String schemaFile = dbConfig.objectStoreName + "-schema.xml"
                String destination = project.getBuildDir().getAbsolutePath() + File.separator + schemaFile
                ant.taskdef(name: "torque", classname: "org.intermine.objectstore.intermine.TorqueModelOutputTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.torque(osname: dbConfig.objectStoreName, destFile:destination)

                String tempDirectory = project.getBuildDir().getAbsolutePath() + File.separator + "tmp"
                ant.taskdef(name: "buildDB", classname: "org.intermine.task.BuildDbTask") {
                    classpath {
                        pathelement(path: project.buildDir.getAbsolutePath())//to read the schema
                        pathelement(path: project.getBuildDir().getAbsolutePath() + File.separator + "libs" + File.separator + "dbmodel.jar")
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.buildDB(osname: dbConfig.objectStoreName, model: dbConfig.modelName,
                        schemafile: schemaFile, tempDir: tempDirectory)
            }
        }
    }
}

