package org.intermine.plugin.dbmodel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

class BioCoreDBModelPlugin implements Plugin<Project>{
    boolean regenerateModel = true

    @Override
    void apply(Project project) {
        project.task('checkRegenerateModel') {
                if (new File(project.getBuildDir().getAbsolutePath() + File.separator + "gen").exists()
                        || project.name.equals("bio-core")) {
                    regenerateModel = false
                }
            }

        project.task('mergeModels') {
            description "Merges only genomic_additions.xml and so_additions.xml into an intermine XML model"
            dependsOn 'initConfig', 'checkRegenerateModel', 'copyGenomicModel'
            onlyIf {regenerateModel}

            doLast {
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
                String inputModelFilePath = buildResourcesMainDir + File.separator + "genomic_model.xml"
                def ant = new AntBuilder()

                ant.taskdef(name: "mergeBioCoreModels", classname: "org.intermine.task.ModelMergerTask") {
                    classpath {
                        pathelement(path: project.configurations.getByName("compile").asPath)
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                    }
                }

                ant.mergeBioCoreModels(inputModelFile: inputModelFilePath, additionsFile: "genomic_additions.xml",
                        outputFile: inputModelFilePath)
            }
        }
    }
}
