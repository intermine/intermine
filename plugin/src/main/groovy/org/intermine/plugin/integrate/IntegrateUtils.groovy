package org.intermine.plugin.integrate

import org.apache.tools.ant.util.StringUtils
import org.gradle.api.Project
import org.intermine.plugin.BioSourceProperties
import org.intermine.plugin.project.Source
import org.gradle.api.tasks.SourceSetContainer

class IntegrateUtils {
    String COMMON_OS_PREFIX = "common"
    Project gradleProject
    org.intermine.plugin.project.Project imProject
    String buildResourcesMainDir
    BioSourceProperties bioSourceProperties

    IntegrateUtils(Project gradleProject, org.intermine.plugin.project.Project imProject) {
        this.gradleProject = gradleProject
        this.imProject = imProject
        bioSourceProperties = new BioSourceProperties(imProject, gradleProject)

        SourceSetContainer sourceSets = (SourceSetContainer) gradleProject.getProperties().get("sourceSets")
        buildResourcesMainDir = sourceSets.getByName("main").getOutput().resourcesDir
    }

    protected preRetrieveSingleSource = { sourceName ->
        if (bioSourceProperties.preRetrieveExists(sourceName)) {
            println "Pre-retrieving " + sourceName
            def ant = new AntBuilder()

            ant.taskdef(name: 'preRetrieve', classname: bioSourceProperties.getPreRetrieveClassName(sourceName)) {
                classpath {
                    dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                    pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                    pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
                }
            }
            //prepare preRetrieve input parameters
            Properties antTaskProperties = bioSourceProperties.generatePreRetrieveAntInput(sourceName)
            ant.preRetrieve(antTaskProperties)
        }
    }

    protected retrieveSingleSource = {sourceName ->
        Source source = imProject.sources.get(sourceName)
        Properties bioSourceProperties = bioSourceProperties.getBioSourceProperties(sourceName)
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
                dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
            }
        }
        def includes = BioSourceProperties.getUserProperty(source, "src.data.dir.includes")
        if (includes == null || includes == "") {
            includes = "*"
        }
        ant.convertFile(clsName: bioSourceProperties.getProperty("converter.class"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic") {
            fileset(dir: BioSourceProperties.getUserProperty(source, "src.data.dir"),
                    includes: includes,
                    excludes: BioSourceProperties.getUserProperty(source, "src.data.dir.excludes"))
        }
    }

    def retrieveTgtFromDB = {Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()
        source.userProperties.each { prop ->
            if (!"src.data.dir".equals(prop.name)) {
                ant.project.setProperty(prop.name, prop.value)
            }
        }
        ant.taskdef(name: "convertDB", classname: "org.intermine.task.DBConverterTask") {
            classpath {
                dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                dirset(dir: buildResourcesMainDir)//to read default.intermine.properties
                pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertDB(clsName: bioSourceProperties.getProperty("converter.class"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic",
                dbAlias: "db." + BioSourceProperties.getUserProperty(source, "source.db.name"))
    }

    def retrieveTgtFromCustomDir = {Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()
        //set dynamic properties
        source.userProperties.each { prop ->
            if (!"src.data.dir".equals(prop.name)) {
                ant.project.setProperty(prop.name, prop.value)
            }
        }
        ant.taskdef(name: "convertDir", classname: "org.intermine.task.DirectoryConverterTask") {
            classpath {
                dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertDir(clsName: bioSourceProperties.getProperty("converter.class"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic",
                dataDir: BioSourceProperties.getUserProperty(source, "src.data.dir"))
    }

    def retrieveTgtFromXMLFile = {Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()
        def includes = BioSourceProperties.getUserProperty(source, "src.data.dir.includes")
        if (includes == null || includes == "") {
            includes = "*.xml"
        }
        ant.taskdef(name: "insertXMLData", classname: "org.intermine.dataloader.XmlDataLoaderTask") {
            classpath {
                dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
            }
        }
        boolean hasFile = BioSourceProperties.getUserProperty(source, "src.data.file")
        if (hasFile) {
            ant.insertXMLData(integrationWriter: "integration.production",
                    sourceName: source.name,
                    sourceType: source.type,
                    file: BioSourceProperties.getUserProperty(source, "src.data.file"),
                    ignoreDuplicates: BioSourceProperties.getUserProperty(source, "ignoreDuplicates"))
        } else {
            ant.insertXMLData(integrationWriter: "integration.production",
                    sourceName: source.name,
                    sourceType: source.type,
                    //file: BioSourceProperties.getUserProperty(source, "src.data.file"),
                    ignoreDuplicates: BioSourceProperties.getUserProperty(source, "ignoreDuplicates")) {
                fileset(dir: BioSourceProperties.getUserProperty(source, "src.data.dir"),
                        includes: includes,
                        excludes: BioSourceProperties.getUserProperty(source, "src.data.dir.excludes"))
            }
        }
    }

    def retrieveTgtFromLargeXMLFile = {Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()
        def includes = BioSourceProperties.getUserProperty(source, "src.data.dir.includes")
        if (includes == null || includes == "") {
            includes = "*.xml"
        }
        ant.taskdef(name: "convertFullXMLFile", classname: "org.intermine.task.FullXmlConverterTask") {
            classpath {
                dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
            }
        }
        boolean hasFile = BioSourceProperties.getUserProperty(source, "src.data.file")
        if (hasFile) {
            ant.convertFullXMLFile(osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", sourceName: source.name,
                    file: BioSourceProperties.getUserProperty(source, "src.data.file"), modelName: "genomic")
        } else {
            ant.convertFullXMLFile(osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", sourceName: source.name,
                    modelName: "genomic")
                    {
                        fileset(dir: BioSourceProperties.getUserProperty(source, "src.data.dir"),
                                includes: includes,
                                excludes: BioSourceProperties.getUserProperty(source, "src.data.dir.excludes"))
                    }
        }
    }

    def retrieveFromGFF3 = {Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()
        //set dynamic properties
        source.userProperties.each { prop ->
            if (!"src.data.dir".equals(prop.name)) {
                ant.project.setProperty(prop.name, prop.value)
            }
        }
        String gff3SeqHandlerClassName = (bioSourceProperties.containsKey("gff3.seqHandlerClassName")) ?
                bioSourceProperties.getProperty("gff3.seqHandlerClassName") : ""
        String licence = (ant.project.getProperty("gff3.licence") != null) ?
                ant.project.getProperty("gff3.licence") : ""
        def includes = BioSourceProperties.getUserProperty(source, "src.data.dir.includes")
        if (includes == null || includes == "") {
            includes = "*.gff,*.gff3"
        }

        ant.taskdef(name: "convertGFF3File", classname: "org.intermine.bio.task.GFF3ConverterTask") {
            classpath {
                dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertGFF3File(converter: "org.intermine.bio.dataconversion.GFF3Converter",
                target: "osw." + COMMON_OS_PREFIX + "-tgt-items",
                seqClsName: BioSourceProperties.getUserProperty(source, "gff3.seqClsName"),
                orgTaxonId: BioSourceProperties.getUserProperty(source, "gff3.taxonId"),
                dataSourceName: BioSourceProperties.getUserProperty(source, "gff3.dataSourceName"),
                seqDataSourceName: BioSourceProperties.getUserProperty(source, "gff3.seqDataSourceName"),
                dataSetTitle: BioSourceProperties.getUserProperty(source, "gff3.dataSetTitle"),
                dontCreateLocations: BioSourceProperties.getUserProperty(source, "gff3.dontCreateLocations"),
                model: "genomic",
                handlerClassName: bioSourceProperties.getProperty("gff3.handlerClassName"),
                seqHandlerClassName: gff3SeqHandlerClassName,
                licence: licence) {
            fileset(dir: BioSourceProperties.getUserProperty(source, "src.data.dir"),
                    includes: includes)
        }
    }

    def retrieveFromOBO = {Source source, Properties bioSourceProperties ->
        def ant = new AntBuilder()

        //set dynamic properties
        source.userProperties.each { prop ->
            if (!"src.data.dir".equals(prop.name)) {
                ant.project.setProperty(prop.name, prop.value)
            }
        }

        String licence = (bioSourceProperties.getProperty("obo.ontology.licence") != null) ?
                bioSourceProperties.getProperty("obo.ontology.licence") : ""

        ant.taskdef(name: "convertOBO", classname: "org.intermine.bio.task.OboConverterTask") {
            classpath {
                dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
            }
        }
        ant.convertOBO(file: BioSourceProperties.getUserProperty(source, "src.data.file"),
                osName: "osw." + COMMON_OS_PREFIX + "-tgt-items", modelName: "genomic",
                ontologyName: bioSourceProperties.getProperty("obo.ontology.name"),
                url: bioSourceProperties.getProperty("obo.ontology.url"),
                termClass: bioSourceProperties.getProperty("obo.term.class"),
                licence: licence)
    }

    def loadSingleSource = { source ->
        //TODO manage duplicate
        Properties props = bioSourceProperties.getBioSourceProperties(source.name)
        String classname;
        def ant = new AntBuilder()
        if (props.containsKey("have.file.custom.direct")) {
            //set property change property name, fasta.idSuffix -> idSuffix)
            source.userProperties.each { prop ->
                if (!"src.data.dir".equals(prop.name)) {
                    String propName = StringUtils.removePrefix(prop.name, source.type + ".")
                    ant.project.setProperty(propName, prop.value)
                }
            }
            def fastaClassLoader = BioSourceProperties.getUserProperty(source, "fasta.loaderClassName")
            if (fastaClassLoader != null) {
                classname = fastaClassLoader
            } else {
                classname = props.getProperty("loader.class")
            }
        } else {
            classname = "org.intermine.dataloader.ObjectStoreDataLoaderTask"
        }
        ant.taskdef(name: "dataLoad", classname: classname) {
            classpath {
                dirset(dir: gradleProject.getBuildDir().getAbsolutePath())
                pathelement(path: gradleProject.configurations.getByName("compile").asPath)
                pathelement(path: gradleProject.configurations.getByName("integrateSource").asPath)
            }
        }
        if (props.containsKey("have.file.custom.direct")) {
            ant.dataLoad(integrationWriterAlias: "integration.production",
                    sourceName: source.name, sourceType: source.type){
                fileset(dir: BioSourceProperties.getUserProperty(source, "src.data.dir"),
                        includes: BioSourceProperties.getUserProperty(source, source.type + ".includes"))
            }
        } else {
            ant.dataLoad(integrationWriter: "integration.production",
                    source: "os." + COMMON_OS_PREFIX + "-translated",
                    sourceName: source.name, sourceType: source.type,
                    ignoreDuplicates: false,
                    allSources: "")
        }
    }

}