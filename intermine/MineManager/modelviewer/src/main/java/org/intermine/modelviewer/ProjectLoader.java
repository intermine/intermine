package org.intermine.modelviewer;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.modelviewer.genomic.GenomicAddition;
import org.intermine.modelviewer.genomic.ModelBuilder;
import org.intermine.modelviewer.jaxb.ConfigParser;
import org.intermine.modelviewer.model.Model;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.PropertyNameComparator;
import org.intermine.modelviewer.project.Source;
import org.intermine.modelviewer.project.SourceNameComparator;
import org.xml.sax.SAXException;


/**
 * Class to load project files and assemble the combined model from the
 * data sources contained in the project.
 */
public class ProjectLoader
{
    /**
     * Logger.
     */
    private Log logger = LogFactory.getLog(ProjectLoader.class);
    
    /**
     * Parser class wrapping the JAXB interactions.
     */
    private ConfigParser configParser;
    
    /**
     * Constructs a new ProjectLoader with its own JAXB parser.
     * 
     * @throws JAXBException if initialising the parser fails.
     * @throws SAXException if initialising the parser fails.
     */
    public ProjectLoader() throws JAXBException, SAXException {
        configParser = new ConfigParser();
    }
    
    /**
     * Load the given project file.
     * 
     * @param projectFile The project file to load.
     * @return An assembled object model of the project.
     * 
     * @throws JAXBException if parsing the project file fails.
     * @throws SAXException if parsing the project file fails.
     * @throws ParserConfigurationException if there is an internal problem
     * with SAX.
     * @throws IOException if there is a problem reading the file.
     */
    public Project loadProject(File projectFile)
    throws JAXBException, SAXException, ParserConfigurationException, IOException {
        Project project = configParser.loadProjectFile(projectFile);
        return project;
    }
    
    /**
     * Assemble the genomic model from a project file.
     * <p>This method is a simple combination of {@link #loadProject(File)} followed
     * by {@link #loadModel(File, Project)}.
     * 
     * @param projectFile The project file.
     * 
     * @return The genomic model for the project.
     * 
     * @throws JAXBException if the parsing of the project or model files fails.
     * @throws SAXException if the parsing of the project or model files fails.
     * @throws ParserConfigurationException if there is an internal problem
     * with SAX.
     * @throws IOException if there is a problem reading any file.
     * 
     * @see #loadProject(File)
     * @see #loadModel(File, Project)
     */
    public Model loadModel(File projectFile)
    throws JAXBException, SAXException, ParserConfigurationException, IOException {
        Project project = configParser.loadProjectFile(projectFile);
        return loadModel(projectFile, project);
    }
        
    /**
     * Assemble the genomic model from a Project object model.
     * <p>The model is built by reading the project's <code>dbmodel/project.properties</code>
     * file to assemble a data source directory path. From this the Intermine core directory
     * is located, and from this the core objects are read (<code>core.xml</code>, plus
     * standard additions). Then each source in the project is examined and its additions to
     * the model are read. Finally, all this information is passed to a {@link ModelBuilder}
     * to create the final model that is returned.
     * 
     * @param projectFile The project file from which <code>project</code> was loaded.
     * @param project The project object model.
     * 
     * @return The genomic model for the project.
     * 
     * @throws JAXBException if the parsing of the model files fails.
     * @throws SAXException if the parsing of the model files fails.
     * @throws ParserConfigurationException if there is an internal problem
     * with SAX.
     * @throws IOException if there is a problem reading any file.
     * 
     * @see ModelBuilder
     */
    public Model loadModel(File projectFile, Project project)
    throws JAXBException, SAXException, ParserConfigurationException, IOException {
        
        File projectDir = projectFile.getParentFile();
        File dbmodelDir = new File(projectDir, "dbmodel");
        File propertiesFile = new File(dbmodelDir, "project.properties");
        
        List<File> sourcePaths = new ArrayList<File>();
        for (org.intermine.modelviewer.project.Property prop : project.getProperty()) {
            if ("source.location".equals(prop.getName())) {
                String value = prop.getLocation();
                if (value == null) {
                    value = prop.getValue();
                }
                File dir = new File(projectDir, value).getCanonicalFile();
                logger.debug("Source parent dir " + sourcePaths.size() + " = " + dir);
                sourcePaths.add(dir);
            }
        }
        
        File intermineDir = projectDir.getParentFile().getAbsoluteFile();
        logger.debug("Intermine dir = " + intermineDir);
        
        Properties projectProps = new Properties();
        Reader propsReader = new FileReader(propertiesFile);
        try {
            projectProps.load(propsReader);
        } finally {
            propsReader.close();
        }
        
        String corePath = projectProps.getProperty("core.model.path", "bio/core");
        File coreDir = new File(intermineDir, corePath).getCanonicalFile();
        
        File coreFile = new File(coreDir, ModelBuilder.CORE_TAG);
        logger.debug("Core file = " + coreFile);
        
        List<GenomicAddition> genomicClasses = new ArrayList<GenomicAddition>();
        
        org.intermine.modelviewer.genomic.Model xmlModel = configParser.loadCoreFile(coreFile);
        additionalPaths(projectProps.getProperty("extra.model.paths.start"),
                        genomicClasses, intermineDir);
        additionalPaths(projectProps.getProperty("extra.model.paths.end"),
                        genomicClasses, intermineDir);
        
        for (org.intermine.modelviewer.project.Source source : project.getSources().getSource()) {
            String sourceType = source.getType();
            boolean found = false;
            for (File sourcePath : sourcePaths) {
                File sourceDir = new File(sourcePath, sourceType);
                if (sourceDir.exists() && sourceDir.isDirectory()) {
                    found = true;
                    File sourceAdditionsFile = new File(sourceDir, sourceType + "_additions.xml");
                    if (sourceAdditionsFile.exists()) {
                        logger.debug("Loading additions from "
                                + sourceAdditionsFile.getAbsolutePath());
                        genomicClasses.add(
                                new GenomicAddition(sourceAdditionsFile.getName(),
                                        configParser.loadGenomicFile(sourceAdditionsFile)));
                        break;
                    }
                }
            }
            if (!found) {
                logger.warn("Cannot locate directory for source " + sourceType);
            }
        }
        
        return new ModelBuilder().buildHierarchy(xmlModel, genomicClasses);
    }
    
    /**
     * Helper to <code>loadModel</code>, this method reads model additions from files listed
     * in a given property value (comma-separated path) and adds them to the list of additions
     * for loading.
     * 
     * @param prop The property value containing the path of additions files.
     * @param additions The list of GenomicAddition records the loading is building up.
     * @param intermineDir The intermine home directory.
     * 
     * @throws JAXBException if the parsing of the model files fails.
     * @throws SAXException if the parsing of the model files fails.
     * @throws ParserConfigurationException if there is an internal problem
     * with SAX.
     * @throws IOException if there is a problem reading any file.
     */
    private void additionalPaths(String prop, List<GenomicAddition> additions, File intermineDir)
    throws JAXBException, SAXException, ParserConfigurationException, IOException {
        if (prop != null) {
            String[] paths = prop.split("\\s+");
            for (String path : paths) {
                File file = new File(intermineDir, path).getCanonicalFile();
                if (!file.exists()) {
                    logger.error("Cannot locate additional model information file "
                            + file.getAbsolutePath());
                } else {
                    logger.debug("Loading additions from " + file.getAbsolutePath());
                    GenomicAddition a =
                        new GenomicAddition(file.getName(), configParser.loadGenomicFile(file));
                    additions.add(a);
                }
            }
        }
    }
    
    /**
     * Writes out a Project object model to the given file as XML.
     * <p>This is simply JAXB marshalling.</p>
     * 
     * @param project The project to save.
     * @param saveFile The file to write the project to.
     * 
     * @throws JAXBException if there is a problem marshalling the object.
     * @throws XMLStreamException if there is a problem with XML streaming.
     * @throws IOException if there is a lower level I/O problem.
     */
    public void saveProject(Project project, File saveFile)
    throws JAXBException, XMLStreamException, IOException {
        Writer out = new OutputStreamWriter(new FileOutputStream(saveFile), "UTF-8");
        try {
            configParser.writeProjectFile(project, out);
        } finally {
            out.close();
        }
    }
}
