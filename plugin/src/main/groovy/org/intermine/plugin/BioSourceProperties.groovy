package org.intermine.plugin

import org.apache.tools.ant.util.StringUtils
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.util.PatternSet
import org.intermine.plugin.project.Project
import org.intermine.plugin.project.Source

import java.util.regex.Matcher
import java.util.regex.Pattern

class BioSourceProperties {
    private Project imProject
    private org.gradle.api.Project gradleProject
    public static final String PRE_RETRIEVER_CLASSNAME = "classname";
    public static final String POSTPROCESSOR_CLASS = "postprocessor.class";

    BioSourceProperties(Project imProject, org.gradle.api.Project gradleProject) {
        this.imProject = imProject
        this.gradleProject = gradleProject
    }

    Properties getBioSourceProperties(String sourceName) {
        String sourceType = imProject.sources.get(sourceName).type
        String propsFileName = sourceType + ".properties"
        return getProperties(sourceType, propsFileName)
    }

    String getPostProcesserClassName(String processName) {
        String propsFileName = processName + ".properties"
        return getPostProcessProperties(processName, propsFileName).getProperty(POSTPROCESSOR_CLASS)
    }

    Properties getBioSourcePreRetrieveProperties(String sourceName) {
        String sourceType = imProject.sources.get(sourceName).type
        String propsFileName = sourceType + "-pre-retrieve.properties"
        return getProperties(sourceType, propsFileName)
    }

    private Properties getProperties(String sourceType, String propsFileName) {
        Properties properties = new Properties()
        Configuration config = gradleProject.configurations.getByName("integrateSource")
        config.files.each {file ->
            if (file.name.contains(sourceType)) {
                FileTree fileTree = gradleProject.zipTree(file)
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

    private Properties getPostProcessProperties(String sourceType, String propsFileName) {
        Properties properties = new Properties()
        Configuration config = gradleProject.configurations.getByName("postProcesses")
        config.files.each {file ->
            if (file.name.contains(sourceType)) {
                FileTree fileTree = gradleProject.zipTree(file)
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

    boolean preRetrieveExists(String sourceName) {
        Properties preRetrieveProps = getBioSourcePreRetrieveProperties(sourceName)
        if (preRetrieveProps != null && !preRetrieveProps.isEmpty()) {
            return true
        }
        return false
    }

    Properties generatePreRetrieveAntInput(String sourceName) {
        Source source = imProject.sources.get(sourceName)
        Properties preRetrieveProps = getBioSourcePreRetrieveProperties(sourceName)
        Properties antTaskProperties = new Properties()
        preRetrieveProps.each {prop ->
            if (prop.key != PRE_RETRIEVER_CLASSNAME) {
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
        return antTaskProperties
    }

    String getPreRetrieveClassName(String sourceName) {
        Source source = imProject.sources.get(sourceName)
        Properties preRetrieveProps = getBioSourcePreRetrieveProperties(sourceName)
        return preRetrieveProps.getProperty(PRE_RETRIEVER_CLASSNAME)
    }

    boolean postprocessorExists(String sourceName) {
        String postprocessorClassname = getBioSourceProperties(sourceName).getProperty(POSTPROCESSOR_CLASS)
        if (postprocessorClassname != null && postprocessorClassname != "") {
            return true
        }
        return false
    }

    String getPostprocessorClassname(String sourceName) {
        return getBioSourceProperties(sourceName).getProperty(POSTPROCESSOR_CLASS)
    }

    static String getUserProperty(Source source, String key) {
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
