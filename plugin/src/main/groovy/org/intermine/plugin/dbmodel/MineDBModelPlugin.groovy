package org.intermine.plugin.dbmodel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

class MineDBModelPlugin implements Plugin<Project> {

    void apply(Project project) {
        String projectXmlFilePath = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"

        project.task('mergeModels') {
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'initConfig', 'copyGenomicModel', 'createSoModel', 'addSourceDependencies'
            onlyIf {!new File(project.getBuildDir().getAbsolutePath() + File.separator + "gen").exists()}

            MineDBModelConfig config = project.extensions.create('mineDBModelConfig', MineDBModelConfig)

            doLast {
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
                def ant = new AntBuilder()
                //added this dependecy otherwise ant doesn't find MergeSourceModelsTask at runtime
                project.dependencies.add("plugin", [group: "org.intermine", name: "plugin", version: System.getProperty("imVersion")])
                String modelFilePath = buildResourcesMainDir + File.separator + config.modelName + "_model.xml"
                ant.taskdef(name: "mergeSourceModels", classname: "org.intermine.plugin.ant.MergeSourceModelsTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("plugin").asPath)
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

