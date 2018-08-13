package org.intermine.plugin.webapp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.intermine.plugin.TaskConstants
import org.intermine.plugin.dbmodel.DBModelUtils

import java.nio.file.Files
import java.nio.file.StandardCopyOption

class WebAppPlugin implements Plugin<Project> {
    WebAppConfig config;
    DBModelUtils dbUtils

    void apply(Project project) {
        project.configurations {
            commonResources
            bioWebApp
        }

        project.task('initConfig') {
            config = project.extensions.create('webappConfig', WebAppConfig)
            dbUtils = new DBModelUtils(project)

            doLast {
                project.dependencies.add("commonResources", [group: "org.intermine", name: "intermine-resources", version: System.getProperty("imVersion")])
                project.dependencies.add("bioWebApp", [group: "org.intermine", name: "bio-webapp", version: System.getProperty("bioVersion"), transitive: false])
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

        project.task('addStrutsConfig') {
            description "Append the struts config modifications to the webapp"
            dependsOn 'processResources','unwarBioWebApp'

            doLast {
                String buildResDir = project.buildDir.absolutePath + File.separator + "resources" + File.separator + "main" + File.separator
                File strutsConfigModelPath = new File(buildResDir + "struts-config-model.xml")
                File strutsConfigFormPath = new File(buildResDir + "struts-config-form-model.xml")
                File strutsConfigPath = new File(project.buildDir.absolutePath + "/explodedWebApp/WEB-INF/struts-config.xml")

                //struts-config.xml
                String content = strutsConfigPath.text
                strutsConfigPath.withWriter { w ->
                    w << content.replace("<!--@MODEL_INCLUDE@-->", strutsConfigModelPath.text)
                }
                content = strutsConfigPath.text
                strutsConfigPath.withWriter { w ->
                    w << content.replace("<!--@MODEL_FORM_INCLUDE@-->", strutsConfigFormPath.text)
                }

                //tiles-defs.xml
                File tilesDefsModel = new File(buildResDir + "tiles-defs-model.xml")
                File tilesDefs = new File(project.buildDir.absolutePath + "/explodedWebApp/WEB-INF/tiles-defs.xml")
                content = tilesDefs.text
                tilesDefs.withWriter { w ->
                    w << content.replace("<!--@MODEL_INCLUDE@-->", tilesDefsModel.text)
                }

                // web.xml
                File webXml = new File(project.buildDir.absolutePath + "/explodedWebApp/WEB-INF/web.xml")
                File webBio = new File(project.buildDir.absolutePath + "/explodedWebApp/WEB-INF/web-bio.xml")
                content = webXml.text
                webXml.withWriter { w ->
                    w << content.replace("<!--@MODEL_INCLUDE@-->", webBio.text)
                }

                //internationalisation
                File modelProperties = new File(buildResDir + "model.properties")
                File intermineWebAppProperties = new File(project.buildDir.absolutePath + "/explodedWebApp/WEB-INF/classes/InterMineWebApp.properties")
                intermineWebAppProperties.append(modelProperties.text)
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
            dependsOn 'copyMineProperties', 'copyDefaultInterMineProperties', 'jar', ':dbmodel:createUserDB'
            mustRunAfter ':dbmodel:createUserDB'
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

        project.task('dropPrecomputedTables') {
            group TaskConstants.TASK_GROUP
            description "Drops all precomputed tables from the database"
            dependsOn 'initConfig', 'copyMineProperties', 'copyDefaultInterMineProperties', 'jar'

            SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets")
            String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir

            doLast {
                def ant = new AntBuilder()
                ant.taskdef(name: "dropPrecomputedTables", classname: "org.intermine.task.DropPrecomputedTablesTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.dropPrecomputedTables(alias: config.objectStoreName)
            }
        }

        project.task('precomputeQueries') {
            group TaskConstants.TASK_GROUP
            description "Creates temporary tables to make querying faster -- uses 'genomic_precompute.properties'"
            dependsOn 'initConfig', 'copyMineProperties', 'copyDefaultInterMineProperties', 'jar'

            SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets")
            String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir

            doLast {
                def ant = new AntBuilder()
                ant.taskdef(name: "precomputeQueries", classname: "org.intermine.task.PrecomputeTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.precomputeQueries(objectStoreAlias: config.objectStoreName, minRows:0,
                        precomputePropertiesPath: "genomic_precompute.properties")
            }
        }

        project.task('precomputeTemplates') {
            group TaskConstants.TASK_GROUP
            description "Creates temporary tables to make querying faster -- uses the results of your template queries"
            dependsOn 'initConfig', 'copyMineProperties', 'copyDefaultInterMineProperties', 'jar'

            doLast {
                def ant = new AntBuilder()

                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir
                Properties intermineProperties = new Properties()
                intermineProperties.load(new FileInputStream(buildResourcesMainDir + File.separator + "intermine.properties"));
                String superUser = intermineProperties.getProperty("superuser.account")

                ant.taskdef(name: "precomputeTemplates", classname: "org.intermine.web.task.PrecomputeTemplatesTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.precomputeTemplates(alias: config.objectStoreName,
                        userProfileAlias: config.userProfileObjectStoreWriterName, minRows:0,
                        username: superUser)
            }
        }
    }
}

