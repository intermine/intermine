package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class DataBasePlugin implements Plugin<Project> {
    final static String TASK_GROUP = "InterMine"

    void apply(Project project) {
        project.task('createSoModel') {
            def config = project.extensions.create('buildDBConfig', BuildDBConfig)
            group TASK_GROUP
            description "Reads SO OBO files and writes so_additions.xml"
            dependsOn 'processResources'
            doLast {
                def ant = new AntBuilder()
                ant.taskdef(name: "createSoModel", classname: "org.intermine.bio.task.SOToModelTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("so").asPath)
                    }
                }
                ant.createSoModel(soTermListFile: config.soTermListFilePath, outputFile: config.soAdditionFilePath)
            }
        }

        project.task('mergeModelsNew') {
            group TASK_GROUP
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'createSoModel'
            doLast {
                println("mergeModelsNew")
                /*
                def ant = new AntBuilder()
                ant.taskdef(name: "mergeSourceModels", classname: "org.intermine.task.MergeSourceModelsTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("mergeSource").asPath)
                        dirset("build/")
                    }
                }
                ant.mergeSourceModels(projectXmlPath: "$rootDir/project.xml", modelFilePath: "build/resources/main/${modelName}_model.xml", extraModelsStart: "$extraModelsStart",  extraModelsEnd: "$extraModelsEnd")
                */
            }
        }
    }
}

