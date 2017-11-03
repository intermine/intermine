package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class IntegratePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.task('load') {
            //group "${taskGroup}"
            description "Load items into production database"
            doLast {
                println 'Load from the org.intermine.plugin.IntegratePlugin'
                project.ant {}
            }
        }
    }
}