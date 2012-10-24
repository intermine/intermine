package org.intermine.install.project.source;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Property;
import org.xml.sax.SAXException;

/**
 * A singleton class to load the data source metadata from the configuration files
 * in the code base.
 * <p>Source descriptors are resources distributed within this project's JAR file
 * in the package <code>org.intermine.install.project.sources</code>.</p>
 */
public class SourceInfoLoader
{
    /**
     * The name of the property in a mine's <code>dbmodel/project.properties</code>
     * file that lists the data source search path. 
     */
    public static final String SOURCE_PATH_PROPERTY = "source.location";
    
    /**
     * The name of a mine's project properties file.
     */
    public static final String SOURCE_PROPERTY_FILENAME = "project.properties";
    
    /**
     * The full resource path to the data source configuration directory.
     */
    protected static final String RESOURCE_PATH = "/org/intermine/install/project/sources/";
    
    /**
     * The single instance of this class.
     */
    private static SourceInfoLoader instance = new SourceInfoLoader();
    
    /**
     * Logger.
     */
    private Log logger = LogFactory.getLog(getClass());
    
    /**
     * Flag indicating that this loader has been initialised.
     */
    private boolean initialised;
    
    /**
     * The JAXB context for reading the data source descriptor files.
     */
    private JAXBContext jaxbContext;
    
    /**
     * The java.xml.validation schema to use to validate the descriptor files.
     */
    private Schema schema;
    
    /**
     * A list of the data source type names known to the system.
     */
    private List<String> knownTypes;
    
    /**
     * Data source information for the standard Intermine supplied data sources.
     */
    private Map<String, SourceInfo> knownSources = new HashMap<String, SourceInfo>();
    
    /**
     * Data source information for sources derived from the standard types.
     */
    private Map<String, SourceInfo> derivatedSources = new HashMap<String, SourceInfo>();
    
    
    /**
     * Get the singleton instance.
     * 
     * @return The instance of this class.
     */
    public static SourceInfoLoader getInstance() {
        return instance;
    }
    
    /**
     * Protected constructor.
     */
    protected SourceInfoLoader() {
    }
    
    /**
     * Get the list of known data source types.
     * @return A list of data source type names.
     */
    public List<String> getSourceTypes() {
        checkInitialised();
        return knownTypes;
    }
    
    /**
     * Get the data source information for the named type.
     * 
     * @param type The type name.
     * 
     * @return The data source information, or <code>null</code> if the type
     * name isn't recognised.
     */
    public SourceInfo getSourceInfo(String type) {
        checkInitialised();
        return knownSources.get(type);
    }
    
    /**
     * Check that this object has been initialised.
     * 
     * @throws IllegalStateException if not initialised.
     * 
     * @see #initialise()
     */
    protected void checkInitialised() {
        if (!initialised) {
            throw new IllegalStateException("SourceInfoLoader not initialised");
        }
    }
    
    /**
     * Initialise this object by creating the JAXB context and the XML validation
     * schema for loading source descriptors, then loading each available data source
     * metadata.
     * <p>This method should be called exactly once at application start up.</p>
     * 
     * @throws JAXBException if there is a problem preparing the JAXB system, or
     * reading any of the source descriptors.
     * @throws IOException if there is an I/O error while loading the configuration.
     * @throws IllegalStateException if this object has already been initialised.
     */
    public synchronized void initialise() throws JAXBException, IOException {
        if (initialised) {
            throw new IllegalStateException("SourceInfoLoader already initialised");
        }
        
        SchemaFactory sfact = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            schema = sfact.newSchema(
                    new StreamSource(getClass().getResourceAsStream("/xsd/sourcedescriptor.xsd")));
        } catch (SAXException e) {
            throw new JAXBException("Could not parse sourcedescriptor.xsd", e);
        }
        jaxbContext = JAXBContext.newInstance("org.intermine.install.project.source");

        // Initialise known types list.
        
        knownTypes = new ArrayList<String>();
        
        String resourceName = RESOURCE_PATH + "sourcetypes.txt";
        InputStream in = getClass().getResourceAsStream(resourceName);
        if (in == null) {
            throw new FileNotFoundException(resourceName);
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                knownTypes.add(line.trim());
            }
        } finally {
            in.close();
        }
        
        knownTypes = Collections.unmodifiableList(knownTypes);
        
        
        // Initialise information for these known types. 
        
        for (String type : knownTypes) {
            String descriptorFile = RESOURCE_PATH + type + ".xml";
            InputStream descriptorStream = getClass().getResourceAsStream(descriptorFile);
            if (descriptorStream == null) {
                logger.warn("There is no source descriptor file for the type " + type);
                continue;
            }
            
            // Load the source descriptor.
            SourceDescriptor source;
            try {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                unmarshaller.setSchema(schema);
                JAXBElement<?> result = (JAXBElement<?>) unmarshaller.unmarshal(descriptorStream);
                source = (SourceDescriptor) result.getValue();
            } finally {
                descriptorStream.close();
            }
            
            // Load default property values, if any are present.
            Properties defaults = new Properties();
            String defaultsFile = RESOURCE_PATH + type + "_defaults.properties";
            InputStream defaultsStream = getClass().getResourceAsStream(defaultsFile);
            if (defaultsStream != null) {
                try {
                    defaults.load(defaultsStream);
                } finally {
                    defaultsStream.close();
                }
            }
        
            SourceInfo info = new SourceInfo(type, source, defaults);
            knownSources.put(type, info);
            
            if (info.getSource().getDerivation() != null) {
                derivatedSources.put(info.getSource().getDerivation().getAntTask(), info);
            }
        }
        
        initialised = true;
    }
    
    /**
     * Searches the Intermine data source path for derived data sources with the
     * given name.
     * 
     * @param type The name of the data source type to find.
     * @param project The Project object, which provides the data source path.
     * @param projectHome The Intermine home directory.
     * 
     * @return The SourceInfo for the found data source's parent standard source if
     * the data source is found on the path, or <code>null</code> if no such source
     * can be located.
     * 
     * @throws IOException if there is an I/O problem while locating the source.
     */
    public SourceInfo findDerivedSourceInfo(String type, Project project, File projectHome)
    throws IOException {
        checkInitialised();
        
        FileFilter sourceDirFilter = new SourceDirectoryFilter(type);
        for (Property p : project.getProperty()) {
            if (SOURCE_PATH_PROPERTY.equals(p.getName())) {
                File dir = new File(projectHome, p.getLocation());
                if (dir.exists()) {
                    File[] candidates = dir.listFiles(sourceDirFilter);
                    if (candidates.length > 0) {
                        SourceInfo info = determineSourceType(candidates[0]);
                        if (info != null) {
                            return info;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * For a candidate data source directory, check whether its
     * <code>project.properties</code> file indicates what type of standard source
     * it was derived from and return that parent source if possible.
     * 
     * @param sourceDir The data source directory.
     * 
     * @return The SourceInfo for the data source's parent standard source if
     * possible, or <code>null</code> if no such source can be determined.
     * 
     * @throws IOException if there is an I/O problem while determining the
     * parent source.
     */
    protected SourceInfo determineSourceType(File sourceDir) throws IOException {
        Properties props = new Properties();
        File propsFile = new File(sourceDir, SOURCE_PROPERTY_FILENAME);
        Reader reader = new FileReader(propsFile);
        try {
            props.load(reader);
        } finally {
            reader.close();
        }
        
        for (String antTaskName : derivatedSources.keySet()) {
            boolean found =
                Boolean.parseBoolean(props.getProperty(antTaskName, Boolean.FALSE.toString()));
            if (found) {
                return derivatedSources.get(antTaskName);
            }
        }
        return null;
    }
    
    /**
     * Filter for returning data source directories. Valid directories have the given
     * name and contain the file <code>project.properties</code> at their top level.
     */
    private static class SourceDirectoryFilter implements FileFilter
    {
        /**
         * The data source name we are searching for.
         */
        private String sourceName;
        
        /**
         * Create a new filter looking for the given directory name.
         * 
         * @param name The data source name we are searching for.
         */
        public SourceDirectoryFilter(String name) {
            sourceName = name;
        }

        /**
         * Test whether the given file is a data source directory.
         * 
         * @param path The file to check.
         * 
         * @return <code>true</code> if the directory is an Intermine data
         * source, <code>false</code> if not.
         */
        @Override
        public boolean accept(File path) {
            boolean ok = path.isDirectory() && path.getName().equals(sourceName);
            if (ok) {
                File propsFile = new File(path, SOURCE_PROPERTY_FILENAME);
                ok = ok && propsFile.exists();
            }
            return ok;
        }
    }
}
