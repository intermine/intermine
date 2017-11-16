package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet

class MineDataBasePlugin implements Plugin<Project> {
    //TODO investigate where t put common config?
    String bioVersion = "2.0.0-SNAPSHOT"

    void apply(Project project) {
        //temporary
        project.dependencies.add("mergeSource", [group: "org.intermine", name: "bio-source-uniprot", version: bioVersion])
        project.dependencies.add("mergeSource", [group: "org.intermine", name: "bio-source-fasta", version: bioVersion])
        //project.dependencies.add("mergeSource", [group: "org.intermine", name: "bio-source-go-annotation", version: bioVersion])

        project.task('mergeModels') {
            description "Merges defferent source model files into an intermine XML model"
            dependsOn 'initConfig', 'copyGenomicModel', 'copyModelProperties', 'createSoModel'
            MineDBConfig config = project.extensions.create('mineDBConfig', MineDBConfig)

            doLast {
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
                def ant = new AntBuilder()

                String projectXmlFilePath = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"
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

                //def obj = new org.intermine.task.project.ProjectXmlBinding()
            }

            //obj.doSomething()

            //def projectXmlBinding = new org.intermine.task.project.ProjectXmlBinding()
//            Project imProject = ProjectXmlBinding().unmarshall(projectXmlFilePath);
//
//            Collection<Source> sources = imProject.getSources().values();

//            for (Source source: sources) {
//                  project.dependencies.add("mergeSource", [group: "org.intermine", name: source.getType(), version: bioVersion])
//            }

//            project.dependencies.add("mergeSource", [group: "org.intermine", name: "uniprot", version: bioVersion])
//            project.dependencies.add("mergeSource", [group: "org.intermine", name: "fasta", version: bioVersion])
//            project.dependencies.add("mergeSource", [group: "org.intermine", name: "go-annotation", version: bioVersion])
        }
    }
}

