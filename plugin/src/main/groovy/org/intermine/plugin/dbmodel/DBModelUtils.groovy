package org.intermine.plugin.dbmodel

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.util.PatternSet

class DBModelUtils {
    Project project
    String buildResourcesMainDir

    DBModelUtils(Project project) {
        this.project = project
        SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets")
        buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir
    }

    protected copyDefaultPropertiesFile = { String defaultPropertiesFile ->
        FileTree fileTree = project.zipTree(project.configurations.getByName("commonResources").singleFile)
        PatternSet patternSet = new PatternSet()
        patternSet.include(defaultPropertiesFile)
        File file = fileTree.matching(patternSet).singleFile
        String defaultIMProperties = buildResourcesMainDir + File.separator + "default.intermine.properties"
        file.createNewFile()
        file.renameTo(defaultIMProperties)
    }

    protected addBioSourceDependency = { sourcePostfix, sourceVersion ->
        DependencyHandler dh = project.getDependencies()
        Dependency dep = dh.create(
                [group: "org.intermine", name: "bio-source-${sourcePostfix}", version: "${sourceVersion}"])

        // This can prove useful for debugging but may be a bit too noisy in practice
        // System.out.println("Adding mergeSource configuration dependency ${dep}")
        dh.add("mergeSource", dep)
    }

    protected generateModel = { modelName ->
        def ant = new AntBuilder()
        String destination = project.getBuildDir().getAbsolutePath() + File.separator + "gen"
        File destinationPath = new File(destination);
        if (! destinationPath.exists()){
            // we might have done a clean right before. compileJava comes after this step
            destinationPath.mkdirs();
        }
        ant.taskdef(name: "modelOutputTask", classname: "org.intermine.task.ModelOutputTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("bioModel").asPath)
            }
        }
        ant.modelOutputTask(model: modelName, destDir: destination, type: "java")
    }

    protected createSchema = { objectStoreName ->
        String schemaFile = objectStoreName + "-schema.xml"
        String destination = project.getBuildDir().getAbsolutePath() + File.separator + schemaFile
        System.out.println("Creating schema in objectstore ${objectStoreName} for ${destination}")

        def ant = new AntBuilder()
        ant.taskdef(name: "torque", classname: "org.intermine.objectstore.intermine.TorqueModelOutputTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("bioModel").asPath)
                pathelement(path: project.configurations.getByName("api").asPath) //userprofile classes
            }
        }
        ant.torque(osname: objectStoreName, destFile: destination)
    }

    protected createTables = { objectStoreName, modelName ->
        String schemaFile = objectStoreName + "-schema.xml"
        String tempDirectory = project.getBuildDir().getAbsolutePath() + File.separator + "tmp"
        System.out.println("Creating tables in objectstore ${objectStoreName} with schema ${schemaFile}")

        def ant = new AntBuilder()
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
        System.out.println("Storing metadata in objectstore ${objectStoreName} for model ${modelName}")

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
        System.out.println("Postgres analyzing objectstore ${objectStoreName} for ${modelName} to optimize performance")

        def ant = new AntBuilder()
        ant.taskdef(name: 'analyse', classname: 'org.intermine.task.AnalyseDbTask') {
            classpath {
                dirset(dir: buildResourcesMainDir) // intermine.properties
                pathelement(path: project.configurations.getByName("compile").asPath)
            }
        }
        ant.analyse(osname: objectStoreName, model: modelName)
    }

    protected createIndexes = { objectStoreName, attributeIndexes ->
        System.out.println("Creating search indexes for objectstore ${objectStoreName}")

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
