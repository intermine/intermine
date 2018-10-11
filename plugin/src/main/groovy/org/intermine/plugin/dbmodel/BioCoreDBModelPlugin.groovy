package org.intermine.plugin.dbmodel

import org.gradle.api.Plugin
import org.gradle.api.Project

class BioCoreDBModelPlugin implements Plugin<Project>{
    @Override
    void apply(Project project) {
        project.task('mergeModels') {
            description "bio-core gets its model from bio-model. Skip this step. MERGE NOTHING!"
            dependsOn 'initConfig', 'copyGenomicModel'
            // do nothing

            // used by biocore and bio-postprocess. packages that do NOT generate their own
            // model but instead use ones already provided. 'mergeModels' is required however
            // so that's why you are reading this empty method.
        }
    }
}
