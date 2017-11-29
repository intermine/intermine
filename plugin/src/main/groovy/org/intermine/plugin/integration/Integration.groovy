package org.intermine.plugin.integration

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.util.PatternSet
import org.intermine.project.Source

class Integration {
    String COMMON_OS_PREFIX = "common"
    Project project
    org.intermine.project.Project imProjet

    Integration(Project project, org.intermine.project.Project imProject) {
        this.project = project
        this.imProjet = imProject
    }

    Properties getBioSourceProperties(String sourceName) {
        String sourceType = imProjet.sources.get(sourceName).type
        FileTree fileTree = project.zipTree(project.configurations.getByName("integrateSource").singleFile)
        PatternSet patternSet = new PatternSet();
        patternSet.include(sourceType + ".properties");
        File file = fileTree.matching(patternSet).singleFile
        Properties bioSourceProperties = new Properties()
        file.withInputStream { bioSourceProperties.load(it) }
        return bioSourceProperties
    }

    protected retrieveSingleSource = {sourceName ->
        Source source = imProjet.sources.get(sourceName)
        Properties bioSourceProperties = getBioSourceProperties(sourceName)
        if (bioSourceProperties.containsKey("have.file.custom.tgt")) {
            retrieveTgtFromCustomFile(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.db.tgt")) {
            retrieveTgtFromDB()
        } else if (bioSourceProperties.containsKey("have.dir.custom.tgt")) {
            retrieveTgtFromCustomDir(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.file.xml.tgt")) {
            retrieveTgtFromXMLFile()
        } else if (bioSourceProperties.containsKey("have.large.file.xml.tgt")) {
            retrieveTgtFromLargeXMLFile()
        } else if (bioSourceProperties.containsKey("have.file.gff3")) {
            retrieveFromGFF3()
        } else if (bioSourceProperties.containsKey("have.file.obo")) {
            retrieveFromOBO()
        }

    }

    protected retrieveTgtFromCustomFile = {source, bioSourceProperties ->
        String location, includes, excludes
        source.userProperties.each { prop ->
            if ("src.data.dir".equals(prop.name)) {
                location = prop.location
            }
            if ("src.data.dir.includes".equals(prop.name)) {
                includes = prop.value
            }
            if ("src.data.dir.excludes".equals(prop.name)) {
                excludes = prop.value
            }
        }

        def ant = new AntBuilder()
        ant.taskdef(name: "convertFile", classname: "org.intermine.task.FileConverterTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertFile(clsName: bioSourceProperties.getProperty("converter.class"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic") {
            fileset(dir: location, includes: includes, excludes: excludes)
        }
    }
    def retrieveTgtFromDB = {}

    def retrieveTgtFromCustomDir = {source, bioSourceProperties ->
        def ant = new AntBuilder()
        String location
        source.userProperties.each { prop ->
            if ("src.data.dir".equals(prop.name)) {
                location = prop.location
            } else {
                ant.project.setProperty(prop.name, prop.value)
            }

        }
        //ant.project.setProperty("uniprotOrganisms", "36329")
        ant.taskdef(name: "convertDir", classname: "org.intermine.task.DirectoryConverterTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertDir(clsName: bioSourceProperties.getProperty("converter.class"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic",
                dataDir: location)
    }

    def retrieveTgtFromXMLFile = {}

    def retrieveTgtFromLargeXMLFile = {}

    def retrieveFromGFF3 = {}

    def retrieveFromOBO = {}

    def loadSingleSource = { source ->
        //String allSources = String.join(" ", intermineProject.getSources().keySet())
        def ant = new AntBuilder()
        ant.taskdef(name: "dataLoad", classname: "org.intermine.dataloader.ObjectStoreDataLoaderTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.dataLoad(integrationWriter: "integration.production",
                source: "os." + COMMON_OS_PREFIX + "-translated",
                sourceName: source.name, sourceType: source.type,
                ignoreDuplicates: false,
                allSources: "")
    }
}
