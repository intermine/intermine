package org.intermine.plugin.dbmodel

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet

class DBUtils {
    Project project
    String buildResourcesMainDir

    DBUtils(Project project) {
        this.project = project
        SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
    }

    protected copyDefaultPropertiesFile = { defaultPropertiesFile ->
        FileTree fileTree = project.zipTree(project.configurations.getByName("commonResources").singleFile)
        PatternSet patternSet = new PatternSet();
        patternSet.include(defaultPropertiesFile);
        File file = fileTree.matching(patternSet).singleFile
        String defaultIMProperties = buildResourcesMainDir + File.separator + "default.intermine.properties"
        file.renameTo(defaultIMProperties)
        file.createNewFile()
    }

    protected generateModel = { modelName ->
        def ant = new AntBuilder()
        String destination = project.getBuildDir().getAbsolutePath() + File.separator + "gen"
        ant.taskdef(name: "modelOutputTask", classname: "org.intermine.task.ModelOutputTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
            }
        }
        ant.modelOutputTask(model: modelName, destDir: destination, type: "java")
    }

    protected createSchema = { objectStoreName, modelName ->
        def ant = new AntBuilder()
        String schemaFile = objectStoreName + "-schema.xml"
        String destination = project.getBuildDir().getAbsolutePath() + File.separator + schemaFile
        ant.taskdef(name: "torque", classname: "org.intermine.objectstore.intermine.TorqueModelOutputTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("api").asPath) //userprofile classes
            }
        }
        ant.torque(osname: objectStoreName, destFile: destination)
    }

    protected createTables = { objectStoreName, modelName ->
        def ant = new AntBuilder()
        String schemaFile = objectStoreName + "-schema.xml"
        String tempDirectory = project.getBuildDir().getAbsolutePath() + File.separator + "tmp"
        ant.taskdef(name: "buildDB", classname: "org.intermine.task.BuildDbTask") {
            classpath {
                dirset(dir: buildResourcesMainDir)//to read default.intermine.properties
                pathelement(path: project.buildDir.getAbsolutePath())//to read the schema
                pathelement(path: project.configurations.getByName("compile").asPath)
            }
        }
        ant.buildDB(osname: objectStoreName, model: modelName, schemafile: schemaFile, tempDir: tempDirectory)
    }

    protected storeMetadata = { objectStoreName, modelName ->
        def ant = new AntBuilder()
        ant.taskdef(name: 'insertModel', classname: 'org.intermine.task.StoreMetadataTask') {
            classpath {
                dirset(dir: buildResourcesMainDir) // intermine.properties
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("api").asPath) //userprofile_keyDefs.properties
            }
        }
        ant.insertModel(osname: objectStoreName, modelName: modelName)
    }

    protected analyse = { objectStoreName, modelName ->
        def ant = new AntBuilder()

        ant.taskdef(name: 'analyse', classname: 'org.intermine.task.AnalyseDbTask') {
            classpath {
                dirset(dir: buildResourcesMainDir) // intermine.properties
                pathelement(path: project.configurations.getByName("compile").asPath)
            }
        }
        ant.analyse(osname: objectStoreName, model: modelName)
    }

    protected createIndexes = { objectStoreName, modelName, attributeIndexes ->
        def ant = new AntBuilder()

        ant.taskdef(name: 'createIndexes', classname: 'org.intermine.task.CreateIndexesTask') {
            classpath {
                dirset(dir: buildResourcesMainDir) // intermine.properties
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
            }
        }
        ant.createIndexes(alias: objectStoreName, attributeIndexes: attributeIndexes)
    }

}
