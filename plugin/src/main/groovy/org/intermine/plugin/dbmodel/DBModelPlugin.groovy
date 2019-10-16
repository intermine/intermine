package org.intermine.plugin.dbmodel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet
import org.intermine.plugin.TaskConstants
import org.intermine.plugin.project.ProjectXmlBinding
import org.intermine.plugin.project.Source
import org.intermine.plugin.webapp.WebAppPlugin

class DBModelPlugin implements Plugin<Project> {

    DBModelConfig config
    DBModelUtils dbUtils
    WebAppPlugin webAppPlugin
    String buildResourcesMainDir
    boolean regenerateModel = true
    boolean generateKeys = true

    void apply(Project project) {

        project.configurations {
            bioModel
            bioModelForSOOnly
            commonResources
            api
            mergeSource
            plugin
            antlr
        }

        project.task('initConfig') {
            config = project.extensions.create('dbModelConfig', DBModelConfig)

            doLast {
                project.dependencies.add("commonResources", [group: "org.intermine", name: "intermine-resources", version: System.getProperty("imVersion")])
                project.dependencies.add("api", [group: "org.intermine", name: "intermine-api", version: System.getProperty("imVersion"), transitive: false])

                dbUtils = new DBModelUtils(project)
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets")
                buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir
                if (new File(project.getBuildDir().getAbsolutePath() + File.separator + "gen").exists()) {
                    // don't generate the model if it's already there
                    regenerateModel = false
                }
                if (!(new File(project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml").exists())) {
                    generateKeys = false
                }
            }
        }

        project.task('addSourceDependencies') {
            description "Add associated datasource dependencies"
            //FIXME: I'm sure this bit is important, but it currently blocks getting my (justincc) biotestmine working
            onlyIf {generateKeys} // && !new File(project.getBuildDir().getAbsolutePath() + File.separator + "gen").exists()}

            doLast {
                String projectXmlFilePath = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"
                def parser = new XmlParser()
                parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
                def projectXml = parser.parse(projectXmlFilePath)
                projectXml.sources.source.each { source ->
                   String version = System.getProperty("bioVersion")
                    if (source.'@version' != null) {
                        version = source.'@version'
                    }
                    if (source.@type == "intermine-items-xml-file") {
                        dbUtils.addBioSourceDependency(source.'@name', version)
                    }
                    dbUtils.addBioSourceDependency(source.'@type', version)
                }
            }
        }

        project.task('copyDefaultInterMineProperties') {
            description "Copies default.intermine.integrate.properties file into resources output"
            dependsOn 'initConfig', 'processResources'
            doLast {
                dbUtils.copyDefaultPropertiesFile(config.defaultInterminePropertiesFile)
            }
        }

        project.task('copyGenomicModel') {
            dependsOn 'initConfig', 'processResources'
            onlyIf {regenerateModel}

            doLast {
                FileTree fileTree
                String genomicModelName = "genomic_model.xml"

                try {
                    project.configurations.getByName("testModel")
                    fileTree = project.zipTree(project.configurations.getByName("testModel").singleFile)
                } catch (UnknownConfigurationException ex) {
                    fileTree = project.zipTree(project.configurations.getByName("bioModel").singleFile)
                }
                PatternSet patternSet = new PatternSet()
                patternSet.include(genomicModelName)
                File coreXml = fileTree.matching(patternSet).singleFile
                String modelFilePath = buildResourcesMainDir + File.separator + config.modelName + "_model.xml"

                coreXml.createNewFile()
                coreXml.renameTo(modelFilePath)
            }
        }

        project.task('copyGenomicKeys') {
            description "Copies default keys for bio in the case of unit tests."
            dependsOn 'initConfig', 'processResources'

            doLast {
                FileTree fileTree
                String genomicModelName = "genomic_keyDefs.properties"

                try {
                    project.configurations.getByName("testModel")
                    fileTree = project.zipTree(project.configurations.getByName("testModel").singleFile)
                } catch (UnknownConfigurationException ex) {
                    fileTree = project.zipTree(project.configurations.getByName("bioModel").singleFile)
                }
                PatternSet patternSet = new PatternSet()
                patternSet.include(genomicModelName)
                File propertiesFile = fileTree.matching(patternSet).singleFile
                String keysFilePath = buildResourcesMainDir + File.separator + config.modelName + "_keyDefs.properties"

                propertiesFile.createNewFile()
                propertiesFile.renameTo(keysFilePath)
            }
        }

        project.task('generateKeys') {
            description "Append keys for each source in project.xml to generated genomic_keyDefs.properties file"
            dependsOn 'initConfig', 'addSourceDependencies', 'processResources'
            onlyIf {generateKeys}

            doLast {

                // parse project XML for each data source
                String projectXml = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"
                org.intermine.plugin.project.Project intermineProject = ProjectXmlBinding.unmarshall(new File(projectXml))
                List<String> sourceNames = new ArrayList<String>()
                intermineProject.sources.keySet().each { sourceName ->
                    sourceNames.add(sourceName)
                }

                Properties keysProperties = new Properties()

                // append each source keys file to make one big keys file
                sourceNames.each { sourceName ->

                    Source source = intermineProject.sources.get(sourceName)
                    String sourceVersion = source.getVersion()
                    String sourceType = source.getType()
                    FileTree dataSourceJar = null
                    File sourceKeysFile = null

                    // Prefix because actual value of the version string is 2.+ while the real version is 2.0.0
                    // Also versions might be strings, so can't use regular expressions (eg. RC or SNAPSHOT)
                    // have to include the version number at all because go-annotation will match go
                    String bioVersionPrefix = System.getProperty("bioVersion").substring(0, 1)
                    if (sourceVersion != null && sourceVersion != "") {
                        bioVersionPrefix = sourceVersion
                    }


                    project.configurations.getByName("mergeSource").asFileTree.each {
                        if (it.name.startsWith("bio-source-$sourceName-$bioVersionPrefix")) {
                             dataSourceJar = project.zipTree(it)
                        }
                    }

                    if (dataSourceJar == null) {
                        project.configurations.getByName("mergeSource").asFileTree.each {
                            if (it.name.startsWith("bio-source-$sourceType-$bioVersionPrefix")) {
                                dataSourceJar = project.zipTree(it)
                            }
                        }
                    }

                    if (dataSourceJar == null) {
                        throw new RuntimeException("Failed to find JAR: 'bio-source-" + sourceType + "-"
                                + bioVersionPrefix + "*.jar' OR 'bio-source-" + sourceName + "-"
                                + bioVersionPrefix + "*.jar'")
                    }

                    System.out.println("Processing ${sourceName}_keys.properties")

                    PatternSet patternSet = new PatternSet()
                    patternSet.include("${sourceName}_keys.properties")
                    if (!dataSourceJar.matching(patternSet).empty) {
                        sourceKeysFile = dataSourceJar.matching(patternSet).singleFile
                    }
                    if (sourceKeysFile == null) {
                        System.out.println("Processing ${sourceType}_keys.properties now, " +
                                "as didn't find ${sourceName}_keys.properties in 'bio-source-" + sourceType
                                + "-" + bioVersionPrefix + "*.jar'")
                        patternSet.include("${sourceType}_keys.properties")
                        if (dataSourceJar.matching(patternSet).empty) {
                            throw new RuntimeException("No keys file found for " + sourceName + " (or "
                                    + sourceType + ") in 'bio-source-" + sourceType + "-" + bioVersionPrefix
                                    + "*.jar'. Please add this file and try your build again.")
                        }
                        sourceKeysFile = dataSourceJar.matching(patternSet).singleFile
                    }

                    Properties sourceProperties = new Properties()
                    sourceProperties.load(sourceKeysFile.newDataInputStream())
                    keysProperties.putAll(sourceProperties)
                }
                String keysPath = buildResourcesMainDir + File.separator + config.modelName + "_keyDefs.properties"
                keysProperties.store(new File(keysPath).newWriter(), null)
            }
        }

        project.task('createSoModel') {
            description "Reads SO OBO files and writes so_additions.xml"
            dependsOn 'initConfig', 'processResources'
            onlyIf {regenerateModel}

            doLast {
                // we need bio-model for the SO files. It may be on the classpath already
                // but we can't be sure.
                project.dependencies.add("bioModelForSOOnly", [group: "org.intermine", name: "bio-model", version: System.getProperty("bioVersion"), transitive: false])

                def ant = new AntBuilder()
                //when we execute bio-model build, bio-core might be not installed yet
                if (project.name.equals("bio-model")) {
                    ant.taskdef(name: "createSoModel", classname: "org.intermine.bio.task.SOToModelTask") {
                        classpath {
                            dirset(dir: project.buildDir.absolutePath)
                            pathelement(path: project.configurations.getByName("compile").asPath)
                        }
                    }
                } else {
                    ant.taskdef(name: "createSoModel", classname: "org.intermine.bio.task.SOToModelTask") {
                        classpath {
                            pathelement(path: project.configurations.getByName("bioModelForSOOnly").asPath)
                            pathelement(path: project.configurations.getByName("compile").asPath)
                        }
                    }
                }
                ant.createSoModel(soTermListFile: config.soTermListFilePath, outputFile: config.soAdditionFilePath)
            }
        }

        project.task('generateModel') {
            description "Merges different source model files into an intermine XML model"
            dependsOn 'initConfig', 'mergeModels'
            onlyIf {regenerateModel}

            doLast {
                dbUtils.generateModel(config.modelName)
            }
        }

        project.tasks.compileJava.dependsOn project.tasks.generateModel

        project.task('buildDB') {
            group TaskConstants.TASK_GROUP
            description "Build the database for the webapp"
            dependsOn 'initConfig', 'copyMineProperties', 'copyDefaultInterMineProperties', 'jar', 'generateKeys'

            doLast {
                dbUtils.createSchema(config.objectStoreName)
                dbUtils.createTables(config.objectStoreName, config.modelName)
                dbUtils.storeMetadata(config.objectStoreName, config.modelName)
                dbUtils.createIndexes(config.objectStoreName, false)
                dbUtils.analyse(config.objectStoreName, config.modelName)

                println "************************************************************************************************************************"
                println "IF YOU CHANGE YOUR DATA MODEL BE SURE TO RUN THE clean TASK BEFORE buildDB ELSE YOU WILL NOT UPDATE THE MINE'S MODEL! :)"
                println "************************************************************************************************************************"
            }
        }

        project.task('buildUnitTestDB') {
            description "Build the database for the webapp"
            dependsOn 'initConfig', 'copyMineProperties', 'copyDefaultInterMineProperties', 'copyGenomicModel', 'jar', 'copyGenomicKeys'

            doLast {
                dbUtils.createSchema(config.objectStoreName)
                dbUtils.createTables(config.objectStoreName, config.modelName)
                dbUtils.storeMetadata(config.objectStoreName, config.modelName)
            }
        }

        project.task('copyUserProfileModel') {
            dependsOn 'initConfig', 'processResources'

            doLast {
                FileTree fileTree = project.zipTree(project.configurations.getByName("api").singleFile)
                PatternSet patternSet = new PatternSet()
                patternSet.include("userprofile_model.xml")
                File file = fileTree.matching(patternSet).singleFile
                String modelFilePath = buildResourcesMainDir + File.separator + config.userProfileModelName + "_model.xml"
                file.createNewFile()
                file.renameTo(modelFilePath)
            }
        }

        project.task('createUserDB') {
            group TaskConstants.TASK_GROUP
            description "Creates empty tables in the userprofile database."
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'copyMineProperties', 'copyUserProfileModel', 'jar'

            doFirst {
                dbUtils.createSchema(config.userProfileObjectStoreName)
                dbUtils.createTables(config.userProfileObjectStoreName, config.userProfileModelName)
                dbUtils.storeMetadata(config.userProfileObjectStoreName, config.userProfileModelName)
            }
        }

        project.task('buildUserDB') {
            group TaskConstants.TASK_GROUP
            description "Creates empty tables in the userprofile database, then loads the superuser and default templates."
            dependsOn 'createUserDB', ':webapp:loadDefaultTemplates'
            // do nothing
        }

        project.task('runAcceptanceTests') {
            group TaskConstants.TASK_GROUP
            description "Run the configured acceptance tests against the production database, uses <MINE_NAME>_acceptance_test.conf"
            dependsOn 'initConfig', 'copyMineProperties', 'copyDefaultInterMineProperties', 'jar'

            doLast {
                def ant = new AntBuilder()

                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets")
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir
                String outputFilePath = buildResourcesMainDir + File.separator + "acceptance_test.html"
                String acceptanceTestFilePath = buildResourcesMainDir + File.separator + config.mineName + "_acceptance_test.conf"

                ant.taskdef(name: "acceptanceTest", classname: "org.intermine.task.AcceptanceTestTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.acceptanceTest(database: "db.production", configFile: acceptanceTestFilePath,
                    outputFile: outputFilePath)
            }
        }

        project.task('runIQLQuery') {
            group TaskConstants.TASK_GROUP
            description "Run an IQL query against the database, e.g. -Pquery=SELECT DISTINCT a2_.primaryIdentifier AS a1_ FROM org.intermine.model.bio.Gene AS a2_"
            dependsOn 'initConfig', 'copyMineProperties', 'copyDefaultInterMineProperties', 'generateModel', 'jar'

            doLast {
                project.dependencies.add("antlr", [group: "org.antlr", name: "antlr-complete", version: "3.5.2", transitive: false])
                def ant = new AntBuilder()
                if (project.hasProperty('query')) {
                    def query = project.properties['query']
                    ant.taskdef(name: "runIQLQuery", classname: "org.intermine.task.RunIqlQueryTask") {
                        classpath {
                            dirset(dir: project.getBuildDir().getAbsolutePath())
                            pathelement(path: project.configurations.getByName("compile").asPath)
                            pathelement(path: project.configurations.getByName("antlr").asPath)
                        }
                    }
                    ant.runIQLQuery(alias: config.objectStoreName, query: query)
                }
            }
        }

        project.task('generateUpdateTriggers') {
            group TaskConstants.TASK_GROUP
            description "Generates SQL files for manual CRUD operations on InterMine database. THIS IS AN EXPERIMENTAL FEATURE, USE AT YOUR OWN RISK."
            dependsOn 'initConfig', 'copyMineProperties', 'copyDefaultInterMineProperties', 'generateModel', 'jar'

            doLast {
                def ant = new AntBuilder()
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets")
                String buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir
                ant.taskdef(name: "generateUpdateTriggers", classname: "org.intermine.task.GenerateUpdateTriggersTask") {
                    classpath {
                        dirset(dir: project.getBuildDir().getAbsolutePath())
                        pathelement(path: project.configurations.getByName("compile").asPath)
                    }
                }
                ant.generateUpdateTriggers(osName: config.objectStoreName, destDir: buildResourcesMainDir)
            }
        }
    }
}

