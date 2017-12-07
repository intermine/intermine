package org.intermine.plugin.integrate

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.util.PatternSet
import org.intermine.plugin.project.Source
import org.gradle.api.tasks.SourceSetContainer

class IntegrateUtils {
    String COMMON_OS_PREFIX = "common"
    Project project
    org.intermine.plugin.project.Project imProject
    String buildResourcesMainDir

    IntegrateUtils(Project project, org.intermine.plugin.project.Project imProject) {
        this.project = project
        this.imProject = imProject

        SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir;
    }

    Properties getBioSourceProperties(String sourceName) {
        String sourceType = imProject.sources.get(sourceName).type
        FileTree fileTree = project.zipTree(project.configurations.getByName("integrateSource").singleFile)
        PatternSet patternSet = new PatternSet();
        patternSet.include(sourceType + ".properties");
        File file = fileTree.matching(patternSet).singleFile
        Properties bioSourceProperties = new Properties()
        file.withInputStream { bioSourceProperties.load(it) }
        return bioSourceProperties
    }

    protected retrieveSingleSource = {sourceName ->
        Source source = imProject.sources.get(sourceName)
        Properties bioSourceProperties = getBioSourceProperties(sourceName)
        if (bioSourceProperties.containsKey("have.file.custom.tgt")) {
            retrieveTgtFromCustomFile(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.db.tgt")) {
            retrieveTgtFromDB(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.dir.custom.tgt")) {
            retrieveTgtFromCustomDir(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.file.xml.tgt")) {
            retrieveTgtFromXMLFile(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.large.file.xml.tgt")) {
            retrieveTgtFromLargeXMLFile(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.file.gff3")) {
            retrieveFromGFF3(source, bioSourceProperties)
        } else if (bioSourceProperties.containsKey("have.file.obo")) {
            retrieveFromOBO(source, bioSourceProperties)
        }
        // TODO throw exception here if we haven't found a valid type?

    }

    protected retrieveTgtFromCustomFile = {Source source, Properties bioSourceProperties  ->
        def ant = new AntBuilder()
        //set dynamic properties
        source.userProperties.each { prop ->
            if (!"src.data.dir".equals(prop.name)) {
                ant.project.setProperty(prop.name, prop.value)
            }
        }
        ant.taskdef(name: "convertFile", classname: "org.intermine.task.FileConverterTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        def includes = getUserProperty(source, "src.data.dir.includes")
        if (includes == null || includes == "") {
            includes = "*"
        }
        ant.convertFile(clsName: bioSourceProperties.getProperty("converter.class"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic") {
            fileset(dir: getUserProperty(source, "src.data.dir"),
                    includes: includes,
                    excludes: getUserProperty(source, "src.data.dir.excludes"))
        }
    }

    def retrieveTgtFromDB = { Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()
        //set dynamic properties
        source.userProperties.each { prop ->
            if (!"src.data.dir".equals(prop.name)) {
                ant.project.setProperty(prop.name, prop.value)
            }
        }
        ant.taskdef(name: "convertDB", classname: "org.intermine.task.DBConverterTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                dirset(dir: buildResourcesMainDir)//to read default.intermine.properties
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertDB(clsName: bioSourceProperties.getProperty("converter.class"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic",
                dbAlias: getUserProperty(source, "src.db.name"))
    }

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

    def retrieveTgtFromXMLFile = {Source source, Properties bioSourceProperties  ->
        def ant = new AntBuilder()
        ant.taskdef(name: "insertXMLData", classname: "org.intermine.dataloader.XmlDataLoaderTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.insertXMLData(integrationWriter: "integration.production",
                sourceName: source.name,
                sourceType: source.type,
                file: getUserProperty(source, "src.data.file"),
                ignoreDuplicates: getUserProperty(source, "ignoreDuplicates"),

        ) {
            fileset(dir: getUserProperty(source, "src.data.dir"),
                    includes: "*.xml",
                    excludes: getUserProperty(source, "src.data.dir.excludes"))
        }
    }

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

    def retrieveFromOBO = {Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()
        ant.taskdef(name: "convertOBO", classname: "org.intermine.bio.task.OboConverterTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertOBO(file: getUserProperty(source, "src.data.file"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic",
                ontologyName: bioSourceProperties.getProperty("obo.ontology.name"),
                url: bioSourceProperties.getProperty("obo.ontology.url"),
                termClass: bioSourceProperties.getProperty("obo.term.class"))
    }

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
                if ("src.data.dir".equals(prop.name) || "src.data.file".equals(prop.name)) {
                    return prop.location
                } else {
                    return prop.value
                }
            }
        }
    }
}
