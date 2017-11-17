package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet

class MineDataBasePlugin implements Plugin<Project> {
    //TODO investigate where to put common config?
    String bioVersion = "2.0.0-SNAPSHOT"

    void apply(Project project) {
        String projectXmlFilePath = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"

        project.task('parseProjectXml') {
            description "Parse the project XML file and add associated datasource dependencies"
            doLast {
                def projectXml = (new XmlParser()).parse(projectXmlFilePath)
                projectXml.sources.source.each { source ->
                    project.dependencies.add("mergeSource", [group: "org.intermine", name: "${source.'@type'}", version: bioVersion])
                }
            }
        }

        project.task('mergeModels') {
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'initConfig', 'copyGenomicModel', 'copyModelProperties', 'createSoModel', 'parseProjectXml'
            MineDBConfig config = project.extensions.create('mineDBConfig', MineDBConfig)

            doLast {
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
                def ant = new AntBuilder()


                String modelFilePath = buildResourcesMainDir + File.separator + config.modelName + "_model.xml"
                ant.taskdef(name: "mergeSourceModels", classname: "org.intermine.task.MergeSourceModelsTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("mergeSource").asPath)
                        pathelement(path: project.configurations.getByName("compile").asPath)
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                    }
                }
                ant.mergeSourceModels(projectXmlPath: projectXmlFilePath,
                        modelFilePath: modelFilePath,
                        extraModelsStart: config.extraModelsStart,
                        extraModelsEnd: config.extraModelsEnd)
            }
        }

    }


}

