package org.intermine.plugin

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

    protected buildDB = { objectStoreName, modelName ->
        def ant = new AntBuilder()

        //create schema file
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

        //create db tables
        String tempDirectory = project.getBuildDir().getAbsolutePath() + File.separator + "tmp"
        ant.taskdef(name: "buildDB", classname: "org.intermine.task.BuildDbTask") {
            classpath {
                pathelement(path: project.buildDir.getAbsolutePath())//to read the schema
                dirset(dir: buildResourcesMainDir)//to read default.intermine.properties
                pathelement(path: project.configurations.getByName("compile").asPath)
            }
        }
        ant.buildDB(osname: objectStoreName, model: modelName,
                schemafile: schemaFile, tempDir: tempDirectory)

        //store metadata into db
        ant.taskdef(name: 'insertModel', classname: 'org.intermine.task.StoreMetadataTask') {
            classpath {
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("api").asPath) //userprofile_keyDefs.properties
                dirset(dir: buildResourcesMainDir) // intermine.properties
            }
        }
        ant.insertModel(osname: objectStoreName, modelName: modelName)

        // analyse database. makes postgres smarter and faster. autovacuum = FALSE
        // "Accurate statistics will help the planner to choose the most appropriate query plan,
        // and thereby improve the speed of query processing."
        ant.taskdef(name: 'analyse', classname: 'org.intermine.task.AnalyseDbTask') {
            classpath {
                pathelement(path: project.configurations.getByName("compile").asPath)
                dirset(dir: buildResourcesMainDir) // intermine.properties
            }
        }
        ant.analyse(osname: objectStoreName, model: modelName)

        ant.taskdef(name: 'createIndexes', classname: 'org.intermine.task.CreateIndexesTask') {
            classpath {
                pathelement(path: project.configurations.getByName("compile").asPath)
                dirset(dir: buildResourcesMainDir) // intermine.properties
            }
        }
        ant.createIndexes(alias: objectStoreName, attributeIndexes: true)
    }
}
