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

import static org.intermine.install.project.source.SourceInfoLoader.SOURCE_PROPERTY_FILENAME;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

/**
 * A class to create new derived data sources from Intermine standard sources.
 */
public class SourceTypeCreator
{
    /**
     * The Intermine home directory.
     */
    private File intermineHome;
    
    /**
     * The Intermine data sources directory to create the new source in.
     */
    private File sourcesDir;
    
    
    /**
     * Initialise with no directories known.
     */
    public SourceTypeCreator() {
    }
    
    /**
     * Initialise with the given Intermine home directory.
     * 
     * @param intermineHome The Intermine home directory.
     */
    public SourceTypeCreator(File intermineHome) {
        setIntermineHome(intermineHome);
    }

    /**
     * Get the Intermine home directory.
     * 
     * @return The Intermine home directory.
     */
    public File getIntermineHome() {
        return intermineHome;
    }

    /**
     * Set the Intermine home directory. Also sets the data sources parent
     * directory in the standard location under the Intermine home.
     * 
     * @param intermineHome The Intermine home directory.
     */
    public void setIntermineHome(File intermineHome) {
        this.intermineHome = intermineHome;
        File bioDir = new File(intermineHome, "bio");
        sourcesDir = new File(bioDir, "sources");
    }
    
    /**
     * Create and return a File object for a data source with the given name.
     * 
     * @param sourceName The name of the data source.
     * 
     * @return A File object for the data source directory.
     */
    public File getSourceDirectory(String sourceName) {
        return new File(sourcesDir, sourceName);
    }
    
    /**
     * Check whether the named data source exists.
     * 
     * @param sourceName The name of the data source.
     * 
     * @return <code>true</code> if the named data source directory exists and contains
     * the data source descriptor <code>project.properties</code> file.
     */
    public boolean doesSourceExist(String sourceName) {
        File sourceDir = new File(sourcesDir, sourceName);
        if (sourceDir.exists()) {
            File propertiesFile = new File(sourceDir, SOURCE_PROPERTY_FILENAME);
            return propertiesFile.exists();
        }
        return false;
    }
    
    /**
     * Check whether the named data source is derived from the given standard type.
     * 
     * @param sourceName The name of the data source.
     * @param info The expected parent source type of the named source.
     * 
     * @return <code>true</code> if the data source exists and is derived from the
     * given type, <code>false</code> otherwise.
     */
    public boolean isSourceCorrectType(String sourceName, SourceInfo info) {
        File sourceDir = new File(sourcesDir, sourceName);
        File propertiesFile = new File(sourceDir, SOURCE_PROPERTY_FILENAME);
        
        boolean correct = false;
        
        SourceTypeDerivation derivation = info.getSource().getDerivation();
        if (derivation != null) {
            
            try {
                Properties projectProps = new Properties();
                Reader reader = new FileReader(propertiesFile);
                try {
                    projectProps.load(reader);
                } finally {
                    reader.close();
                }
                
                correct = projectProps.containsKey(derivation.getAntTask());
            } catch (IOException e) {
                // Ignore - just return false.
            }
        }
        
        return correct;
    }
}
