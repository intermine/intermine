package org.intermine.plugin.dbmodel

import org.gradle.api.Plugin
import org.gradle.api.Project

class BioCoreDBModelPlugin implements Plugin<Project>{
    @Override
    void apply(Project project) {
        project.task('mergeModels') {
            description "Merges only genomic_additions.xml and so_additions.xml into an intermine XML model"
            // do nothing

            // used by biocore and bio-postprocess. packages that do NOT generate their own
            // model but instead use ones already provided. 'mergeModels' is required however
            // so that's why you are reading this empty method.
        }
    }
}
