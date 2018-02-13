package org.intermine.plugin.dbmodel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet
import org.gradle.tooling.BuildException
import org.intermine.plugin.TaskConstants
import org.intermine.plugin.VersionConfig
import org.intermine.plugin.project.ProjectXmlBinding
import org.intermine.plugin.project.Source



class DBModelPlugin implements Plugin<Project> {

    DBModelConfig config
    DBModelUtils dbUtils
    VersionConfig versionConfig
    String buildResourcesMainDir
    boolean regenerateModel = true
    boolean generateKeys = true

    void apply(Project project) {

        project.configurations {
            bioCore
            commonResources
            api
            mergeSource
            plugin
        }

        versionConfig = project.extensions.create('versionConfig', VersionConfig)

        project.task('initConfig') {
            config = project.extensions.create('dbModelConfig', DBModelConfig)

            doLast {
                project.dependencies.add("bioCore", [group: "org.intermine", name: "bio-core", version: versionConfig.imVersion, transitive: false])
                project.dependencies.add("commonResources", [group: "org.intermine", name: "intermine-resources", version: versionConfig.imVersion])
                project.dependencies.add("api", [group: "org.intermine", name: "intermine-api", version: versionConfig.imVersion, transitive: false])

                dbUtils = new DBModelUtils(project)
                SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
                buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
                if (new File(project.getBuildDir().getAbsolutePath() + File.separator + "gen").exists()) {
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
                def projectXml = (new XmlParser()).parse(projectXmlFilePath)
                projectXml.sources.source.each { source ->
                    if (source.@type == "intermine-items-xml-file") {
                        dbUtils.addBioSourceDependency(source.'@name', versionConfig)
                    }

                    dbUtils.addBioSourceDependency(source.'@type', versionConfig)
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
                FileTree fileTree = project.zipTree(project.configurations.getByName("bioCore").singleFile)
                PatternSet patternSet = new PatternSet();
                patternSet.include("core.xml");
                File coreXml = fileTree.matching(patternSet).singleFile
                String modelFilePath = buildResourcesMainDir + File.separator + config.modelName + "_model.xml"
                coreXml.renameTo(modelFilePath)
                coreXml.createNewFile()
            }
        }

        project.task('generateKeys') {
            description "Append keys for each source in project.xml to generated genomic_keyDefs.properties file"
            dependsOn 'initConfig', 'addSourceDependencies', 'processResources'
            onlyIf {generateKeys}

            doLast {

                // parse project XML for each data source
                String projectXml = project.getParent().getProjectDir().getAbsolutePath() + File.separator + "project.xml"
                org.intermine.plugin.project.Project intermineProject = ProjectXmlBinding.unmarshall(new File(projectXml));
                List<String> sourceNames = new ArrayList<String>()
                intermineProject.sources.keySet().each { sourceName ->
                    sourceNames.add(sourceName)
                }

                Properties keysProperties = new Properties()

                // append each source keys file to make one big keys file
                sourceNames.each { sourceName ->

                    Source source = intermineProject.sources.get(sourceName)
                    String sourceType = source.getType()
                    FileTree dataSourceJar = null
                    File sourceKeysFile = null

                    // Prefix because actual value of the version string is 2.+ while the real version is 2.0.0
                    // Also versions might be strings, so can't use regular expressions (eg. RC or SNAPSHOT)
                    // have to include the version number at all because go-annotation will match go
                    String bioVersionPrefix = versionConfig.bioSourceVersion.substring(0, 1)

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

                    PatternSet patternSet = new PatternSet();
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
                            pathelement(path: project.configurations.getByName("bioCore").asPath)
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
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'jar', 'generateKeys'

            doLast {
                dbUtils.createSchema(config.objectStoreName, config.modelName)
                dbUtils.createTables(config.objectStoreName, config.modelName)
                dbUtils.storeMetadata(config.objectStoreName, config.modelName)
                dbUtils.createIndexes(config.objectStoreName, config.modelName, false)
                dbUtils.analyse(config.objectStoreName, config.modelName)
            }
        }

        project.task('buildUnitTestDB') {
            description "Build the database for the webapp"
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'jar', 'generateKeys'

            doLast {
                dbUtils.createSchema(config.objectStoreName, config.modelName)
                dbUtils.createTables(config.objectStoreName, config.modelName)
                dbUtils.storeMetadata(config.objectStoreName, config.modelName)
            }
        }

        project.task('copyUserProfileModel') {
            dependsOn 'initConfig', 'processResources'

            doLast {
                FileTree fileTree = project.zipTree(project.configurations.getByName("api").singleFile)
                PatternSet patternSet = new PatternSet();
                patternSet.include("userprofile_model.xml");
                File file = fileTree.matching(patternSet).singleFile
                String modelFilePath = buildResourcesMainDir + File.separator + config.userProfileModelName + "_model.xml"
                file.renameTo(modelFilePath)
                file.createNewFile()
            }
        }

        project.task('buildUserDB') {
            group TaskConstants.TASK_GROUP
            description "Build the user database for the webapp"
            dependsOn 'initConfig', 'copyDefaultInterMineProperties', 'copyUserProfileModel', 'jar'

            doLast {
                dbUtils.createSchema(config.userProfileObjectStoreName, config.userProfileModelName)
                dbUtils.createTables(config.userProfileObjectStoreName, config.userProfileModelName)
                dbUtils.storeMetadata(config.userProfileObjectStoreName, config.userProfileModelName)
            }
        }
    }
}

