package org.intermine.plugin.dbmodel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

class BioCoreDataBasePlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        project.task('mergeModels') {
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'initConfig', 'copyGenomicModel', 'copyMineProperties', 'createSoModel'

            doLast {
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
                String inputModelFilePath = buildResourcesMainDir + File.separator + "genomic_model.xml"
                def ant = new AntBuilder()

                ant.taskdef(name: "mergeBioCoreModels", classname: "org.intermine.task.ModelMergerTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("bioCore").asPath)
                        pathelement(path: project.configurations.getByName("compile").asPath)
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                    }
                }

                ant.mergeBioCoreModels(inputModelFile: inputModelFilePath, additionsFile: "genomic_additions.xml",
                        outputFile: inputModelFilePath)
                ant.mergeBioCoreModels(inputModelFile: inputModelFilePath, additionsFile: "so_additions.xml",
                        outputFile: inputModelFilePath)
            }
        }
    }
}
