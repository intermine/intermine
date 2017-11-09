package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.util.PatternSet

import java.nio.file.Files

class WebAppPlugin implements Plugin<Project> {
    WebAppConfig config;
    public final static String TASK_GROUP = "InterMine"

    void apply(Project project) {
        project.task('initConfig') {
            config = project.extensions.create('webappConfig', WebAppConfig)
        }

        project.task('copyDefaultProperties') {
            description "Copies default.intermine.properties file into resources output"
            dependsOn 'initConfig', 'processResources'

            doLast {
                FileTree fileTree = project.zipTree(project.configurations.getByName("commonResources").singleFile)
                PatternSet patternSet = new PatternSet();
                patternSet.include("default.intermine.properties");
                File file = fileTree.matching(patternSet).singleFile
                String defaultIMProperties = project.buildDir.absolutePath + File.separator + "resources" + File.separator + "main" + File.separator + "default.intermine.properties"
                file.renameTo(defaultIMProperties)
                file.createNewFile()
            }
        }

        //TODO store biotestmine.properties file (with place holder) in resources and uses that one to build the webapp
        project.task('copyMineProperties') {
            description "Copies mine specific intermine.properties file (from .intermine directory) into resources output to be included in the war"
            dependsOn 'initConfig', 'processResources'

            doLast {
                String mineProperties = config.mineName + ".properties"
                String minePropertiesPath = System.getenv("HOME") + File.separator + ".intermine" + File.separator + mineProperties
                String interminePropertiesPath = project.buildDir.absolutePath + File.separator + "resources" + File.separator + "main" + File.separator + "intermine.properties"
                Files.copy((new File(minePropertiesPath)).toPath(), new File(interminePropertiesPath).toPath())
            }
        }

        project.task('mergeProperties') {
            group TASK_GROUP
            description "Appendes intermine.properties to web.properties file"
            dependsOn 'initConfig'

            doLast {
                String webappDirPath = project.projectDir.absolutePath  + File.separator +  "src" + File.separator + "main" + File.separator + "webapp"
                String webPropertiesPath = webappDirPath + File.separator + "WEB-INF" + File.separator + "web.properties"
                String webPropertiesBuiltPath = project.buildDir.absolutePath + File.separator + "props" + File.separator + "web.properties"
                File webPropertiesBuilt = new File(webPropertiesBuiltPath)
                Files.copy((new File(webPropertiesPath)).toPath(), webPropertiesBuilt.toPath())
                String interMinePropertiesPath = project.buildDir.absolutePath + File.separator + "resources" + File.separator + "main" + File.separator + "intermine.properties"
                webPropertiesBuilt.append( (new File(interMinePropertiesPath)).getText())
            }
        }

        // this task requires a database to exist and be populated. However this task is run at compile time, not runtime.
        // We have no guarantee there will be a database. Hence the try/catch
        project.task('summariseObjectStore') {
            group TASK_GROUP
            description "Summarise ObjectStore into objectstoresummary.properties file"
            dependsOn 'initConfig'

            doLast {
                try {
                    def ant = new AntBuilder()
                    ant.taskdef(name: "summarizeObjectStore", classname: "org.intermine.task.SummariseObjectStoreTask") {
                        classpath {
                            pathelement(path: project.configurations.getByName("compile").asPath)
                        }
                    }
                    ant.summarizeObjectStore(alias: config.objectStoreName, configFileName: "objectstoresummary.config.properties",
                            outputFile: "build/props/objectstoresummary.properties")
                } catch (Exception ex) {
                    println("Error: " + ex)
                }
            }
        }

        project.task('unwarBioWebApp') {
            group TASK_GROUP
            description "Unwar bio-webapp under the build/explodedWebAppDir directory"
            dependsOn 'initConfig'

            doLast {
                String bioWebAppWar = project.configurations.getByName("bioWebApp").singleFile.absolutePath;
                String destination = project.buildDir.absolutePath + File.separator + "explodedWebApp"
                def ant = new AntBuilder()
                ant.unzip(src: bioWebAppWar, dest: destination, overwrite: "true" )
            }
        }


    }
}

