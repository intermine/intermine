package org.intermine.plugin.webapp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.intermine.plugin.TaskConstants
import org.intermine.plugin.VersionConfig
import org.intermine.plugin.dbmodel.DBModelUtils

import java.nio.file.Files
import java.nio.file.StandardCopyOption

class WebAppPlugin implements Plugin<Project> {
    WebAppConfig config;
    DBModelUtils dbUtils
    VersionConfig versionConfig

    void apply(Project project) {
        project.configurations {
            commonResources
            bioWebApp
        }

        versionConfig = project.extensions.create('webappVersionConfig', VersionConfig)

        project.task('initConfig') {
            config = project.extensions.create('webappConfig', WebAppConfig)
            dbUtils = new DBModelUtils(project)

            doLast {
                project.dependencies.add("commonResources", [group: "org.intermine", name: "intermine-resources", version: versionConfig.imVersion])
                project.dependencies.add("bioWebApp", [group: "org.intermine", name: "bio-webapp", version: versionConfig.imVersion, transitive: false])
            }
        }

        project.task('copyDefaultInterMineProperties') {
            description "Copies default.intermine.properties file into resources output"
            dependsOn 'initConfig', 'processResources'

            doLast {
                dbUtils.copyDefaultPropertiesFile(config.defaultInterminePropertiesFile)
            }
        }

        project.task('mergeProperties') {
            description "Append intermine.properties to web.properties file"
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

        // this plugin requires a database to exist and be populated. However this plugin is run at compile time, not runtime.
        // We have no guarantee there will be a database. Hence the try/catch
        project.task('summariseObjectStore') {
            description "Summarise ObjectStore into objectstoresummary.properties file"
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'copyMineProperties'

            doLast {
                try {
                    def ant = new AntBuilder()
                    ant.taskdef(name: "summarizeObjectStore", classname: "org.intermine.task.SummariseObjectStoreTask") {
                        classpath {
                            dirset(dir: project.getBuildDir().getAbsolutePath())
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
            group TaskConstants.TASK_GROUP
            description "Loads default template queries from an XML file into a given user profile"
            dependsOn 'copyMineProperties', 'copyDefaultInterMineProperties', 'jar'
            //jar dependency has been added in order to generate the dbmodel.jar (in case a clean plugin has been called)
            //to allow to read class_keys.properties file

            doLast {
                def ant = new AntBuilder()

                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
                Properties intermineProperties = new Properties();
                intermineProperties.load(new FileInputStream(buildResourcesMainDir + File.separator + "intermine.properties"));
                String superUser = intermineProperties.getProperty("superuser.account")
                String superUserPsw = intermineProperties.getProperty("superuser.initialPassword")


                ant.taskdef(name: "loadTemplates", classname: "org.intermine.web.task.LoadDefaultTemplatesTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.loadTemplates(osAlias: config.userProfileObjectStoreName, userProfileAlias: config.userProfileObjectStoreWriterName,
                        templatesXml:buildResourcesMainDir + File.separator + "default-template-queries.xml",
                        username: superUser,
                        superuserPassword: superUserPsw)
            }
        }
    }
}

