package org.intermine.plugin.integrate

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.util.PatternSet
import org.intermine.project.Source

class IntegrateUtils {
    String COMMON_OS_PREFIX = "common"
    Project project
    org.intermine.project.Project imProjet

    IntegrateUtils(Project project, org.intermine.project.Project imProject) {
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
            retrieveFromGFF3(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.file.obo")) {
            retrieveFromOBO()
        }

    }

    protected retrieveTgtFromCustomFile = {Source source, Properties bioSourceProperties  ->
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
            fileset(dir: getUserProperty(source, "src.data.dir"),
                    includes: getUserProperty(source, "src.data.dir.includes"),
                    excludes: getUserProperty(source, "src.data.dir.excludes"))
        }
    }
    def retrieveTgtFromDB = {}

    def retrieveTgtFromCustomDir = {Source source, Properties bioSourceProperties  ->
        def ant = new AntBuilder()
        //set dynamic properties
        source.userProperties.each { prop ->
            if (!"src.data.dir".equals(prop.name)) {
                ant.project.setProperty(prop.name, prop.value)
            }

        }
        ant.taskdef(name: "convertDir", classname: "org.intermine.task.DirectoryConverterTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertDir(clsName: bioSourceProperties.getProperty("converter.class"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic",
                dataDir: getUserProperty(source, "src.data.dir"))
    }

    def retrieveTgtFromXMLFile = {}

    def retrieveTgtFromLargeXMLFile = {}

    def retrieveFromGFF3 = {Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()
        String gff3SeqHandlerClassName = (bioSourceProperties.containsKey("gff3.seqHandlerClassName")) ?
                bioSourceProperties.getProperty("gff3.seqHandlerClassName") : ""
        ant.taskdef(name: "convertGFF3File", classname: "org.intermine.bio.task.GFF3ConverterTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertGFF3File(converter: "org.intermine.bio.dataconversion.GFF3Converter",
                target: "osw." + COMMON_OS_PREFIX + "-tgt-items",
                seqClsName: getUserProperty(source, "gff3.seqClsName"),
                orgTaxonId: getUserProperty(source, "gff3.taxonId"),
                dataSourceName: getUserProperty(source, "gff3.dataSourceName"),
                seqDataSourceName: getUserProperty(source, "gff3.seqDataSourceName"),
                dataSetTitle: getUserProperty(source, "gff3.dataSetTitle"),
                dontCreateLocations: getUserProperty(source, "gff3.dontCreateLocations"),
                model: "genomic",
                handlerClassName: bioSourceProperties.getProperty("gff3.handlerClassName"),
                seqHandlerClassName: gff3SeqHandlerClassName) {
            fileset(dir: getUserProperty(source, "src.data.dir"),
                    includes: "*.gff,*.gff3")
        }
    }

    def retrieveFromOBO = {}

    def loadSingleSource = { source ->
        //TODO manage duplicate and AllSources
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

    String getUserProperty(Source source, String key) {
        boolean found = false
        for (prop in source.userProperties) {
            if (key.equals(prop.name)) {
                if ("src.data.dir".equals(prop.name)) {
                    return prop.location
                } else {
                    return prop.value
                }
            }
        }
    }
}
