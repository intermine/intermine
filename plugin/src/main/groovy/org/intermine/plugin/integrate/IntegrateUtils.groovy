package org.intermine.plugin.integrate

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.util.PatternSet
import org.intermine.plugin.project.Source
import org.gradle.api.tasks.SourceSetContainer

import java.util.regex.Matcher
import java.util.regex.Pattern

class IntegrateUtils {
    String COMMON_OS_PREFIX = "common"
    Project project
    org.intermine.plugin.project.Project imProject
    String buildResourcesMainDir

    IntegrateUtils(Project project, org.intermine.plugin.project.Project imProject) {
        this.project = project
        this.imProject = imProject

        SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets")
        buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir
    }

    Properties getBioSourceProperties(String sourceName) {
        String sourceType = imProject.sources.get(sourceName).type
        String propsFileName = sourceType + ".properties"
        return getProperties(sourceName, propsFileName)
    }

    Properties getBioSourcePreRetrieveProperties(String sourceName) {
        String sourceType = imProject.sources.get(sourceName).type
        String propsFileName = sourceType + "-pre-retrieve.properties"
        return getProperties(sourceName, propsFileName)
    }

    Properties getProperties(String sourceName, String propsFileName) {
        String sourceType = imProject.sources.get(sourceName).type
        Properties properties = new Properties()
        Configuration config = project.configurations.getByName("integrateSource")
        config.files.each {file ->
            if (file.name.contains(sourceType)) {
                FileTree fileTree = project.zipTree(file)
                PatternSet patternSet = new PatternSet();
                patternSet.include(propsFileName);
                if (fileTree.matching(patternSet).find()) {//-pre-retrieve.properties might not exist
                    File props = fileTree.matching(patternSet).singleFile
                    props.withInputStream { properties.load(it) }
                }
                return properties
            }
        }
        return properties
    }

    protected preRetrieveSingleSource = { sourceName ->
        Properties preRetrieveProps = getBioSourcePreRetrieveProperties(sourceName)
        if (preRetrieveProps != null && !preRetrieveProps.isEmpty()) {
            println "Pre-retrieving " + sourceName
            def ant = new AntBuilder()

            ant.taskdef(name: 'preRetrieve', classname: preRetrieveProps.getProperty("classname")) {
                classpath {
                    dirset(dir: project.getBuildDir().getAbsolutePath())
                    pathelement(path: project.configurations.getByName("compile").asPath)
                    pathelement(path: project.configurations.getByName("integrateSource").asPath)
                }
            }
            //prepare preRetrieve input parameters
            Source source = imProject.sources.get(sourceName)
            Properties antTaskProperties = new Properties()
            preRetrieveProps.each {prop ->
                if (prop.key != "classname") {
                    String value = prop.value
                    Pattern p = Pattern.compile("[\$\\{\\}]")
                    Matcher m = p.matcher(value)
                    if (m.find()) {
                        value = getUserProperty(source, m.replaceAll(""))
                    }
                    if (value != null) {
                        antTaskProperties[prop.key] = value
                    }
                }
            }
            ant.preRetrieve(antTaskProperties)
        }
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
                dbAlias: "db." + getUserProperty(source, "source.db.name"))
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

    def retrieveTgtFromLargeXMLFile = {Source source, Properties bioSourceProperties  ->
        def ant = new AntBuilder()
        ant.taskdef(name: "convertFullXMLFile", classname: "org.intermine.task.FullXmlConverterTask") {
            classpath {
                dirset(dir: project.getBuildDir().getAbsolutePath())
                pathelement(path: project.configurations.getByName("compile").asPath)
                pathelement(path: project.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertFullXMLFile(osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", sourceName: source.name,
                file: getUserProperty(source, "src.data.file"), modelName: "genomic")
        {
            fileset(dir: getUserProperty(source, "src.data.dir"),
                    includes: getUserProperty(source, "src.data.dir.includes"),
                    excludes: getUserProperty(source, "src.data.dir.excludes"))
        }
    }

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
