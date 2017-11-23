package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class IntegratePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.task('loadSource') {
            //group "${taskGroup}"
            description "Load items into production database"
            doLast {
                def ant = new AntBuilder()
                ant.taskdef(name: "dataLoad", classname: "org.intermine.dataloader.ObjectStoreDataLoaderTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.dataLoad(integrationWriter: "integration.production",
                        source: "os.${common.os.prefix}-translated",
                        sourceName: "", sourceType: "",
                        ignoreDuplicates: "",
                        allSources: "")
            }
        }
    }
}