package org.intermine.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet

import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class WebAppPlugin implements Plugin<Project> {
    String imVersion = "2.0.0-SNAPSHOT"
    WebAppConfig config;
    public final static String TASK_GROUP = "InterMine"

    void apply(Project project) {
        project.configurations {
            commonResources
        }

        project.dependencies {
            commonResources group: "org.intermine", name: "intermine-resources", version: imVersion
        }

        project.task('initConfig') {
            config = project.extensions.create('webappConfig', WebAppConfig)
        }

        project.task('copyDefaultProperties') {
            description "Copies default.intermine.properties file into resources output"
            dependsOn 'initConfig', 'processResources'

            doLast {
                FileTree fileTree = project.zipTree(project.configurations.getByName("commonResources").singleFile)
                PatternSet patternSet = new PatternSet();
                patternSet.include(config.defaultInterminePropertiesFile);
                File file = fileTree.matching(patternSet).singleFile
                String defaultIMProperties = project.buildDir.absolutePath + File.separator + "resources" + File.separator + "main" + File.separator + "default.intermine.properties"
                file.renameTo(defaultIMProperties)
                file.createNewFile()
            }
        }

        project.task('mergeProperties') {
            description "Appendes intermine.properties to web.properties file"
            dependsOn 'initConfig', 'copyMineProperties'

            doLast {
                String webappDirPath = project.projectDir.absolutePath  + File.separator +  "src" + File.separator + "main" + File.separator + "webapp"
                String webPropertiesPath = webappDirPath + File.separator + "WEB-INF" + File.separator + "web.properties"
                if (!(new File(config.propsDir)).exists()) {
                    new File(config.propsDir).mkdir()
                }
                String webPropertiesBuiltPath = config.propsDir + File.separator + "web.properties"
                File webPropertiesBuilt = new File(webPropertiesBuiltPath)
                Files.copy((new File(webPropertiesPath)).toPath(), webPropertiesBuilt.toPath(), StandardCopyOption.REPLACE_EXISTING)
                String interMinePropertiesPath = project.buildDir.absolutePath + File.separator + "resources" + File.separator + "main" + File.separator + "intermine.properties"
                webPropertiesBuilt.append( (new File(interMinePropertiesPath)).getText())
            }
        }

        // this task requires a database to exist and be populated. However this task is run at compile time, not runtime.
        // We have no guarantee there will be a database. Hence the try/catch
        project.task('summariseObjectStore') {
            description "Summarise ObjectStore into objectstoresummary.properties file"
            dependsOn 'initConfig', 'copyDefaultProperties'

            doLast {
                try {
                    def ant = new AntBuilder()
                    ant.taskdef(name: "summarizeObjectStore", classname: "org.intermine.task.SummariseObjectStoreTask") {
                        classpath {
                            pathelement(path: project.configurations.getByName("compile").asPath)
                        }
                    }
                    ant.summarizeObjectStore(alias: config.objectStoreName, configFileName: "objectstoresummary.config.properties",
                            outputFile: config.propsDir + File.separator + "objectstoresummary.properties")
                } catch (Exception ex) {
                    println("Error: " + ex)
                }
            }
        }

        project.task('unwarBioWebApp') {
            description "Unwar bio-webapp under the build/explodedWebAppDir directory"
            dependsOn 'initConfig'

            doLast {
                String bioWebAppWar = project.configurations.getByName("bioWebApp").singleFile.absolutePath;
                String destination = project.buildDir.absolutePath + File.separator + "explodedWebApp"
                def ant = new AntBuilder()
                ant.unzip(src: bioWebAppWar, dest: destination, overwrite: "true" )
            }
        }

        project.task('loadDefaultTemplates') {
            group TASK_GROUP
            description "Loads default template queries from an XML file into a given user profile"

            doLast {
                def ant = new AntBuilder()
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;

                ant.taskdef(name: "loadTemplates", classname: "org.intermine.web.task.LoadDefaultTemplatesTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.loadTemplates(osAlias: config.userProfileObjectStoreName, userProfileAlias: config.userProfileObjectStoreWriterName,
                        templatesXml:buildResourcesMainDir + File.separator + "default-template-queries.xml",
                        username: "daniela@intermine.org", superuserPassword: "daniela")
            }
        }
    }
}

