package org.intermine.install.project.postprocessing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import au.com.bytecode.opencsv.CSVReader;


/**
 * A singleton class to load the post processor information from the configuration file
 * in the code base.
 */
public class PostProcessorLoader
{
    /**
     * The full resource path to the post processor configuration file.
     */
    protected static final String RESOURCE_PATH =
        "/org/intermine/install/project/postprocessing/postprocessors.csv";
    
    /**
     * The single instance of this class.
     */
    private static PostProcessorLoader instance = new PostProcessorLoader();
    
    /**
     * Logger.
     */
    @SuppressWarnings("unused")
    private Log logger = LogFactory.getLog(getClass());
    
    /**
     * Flag indicating that this loader has been initialised.
     */
    private boolean initialised;
    
    /**
     * The list of post processor information.
     */
    private List<PostProcessorInfo> info;
    
    
    /**
     * Get the singleton instance.
     * 
     * @return The instance of this class.
     */
    public static PostProcessorLoader getInstance() {
        return instance;
    }
    
    /**
     * Protected constructor.
     */
    protected PostProcessorLoader() {
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
            throw new IllegalStateException("PostProcessorLoader not initialised");
        }
    }
    
    /**
     * Initialise this object by loading the post processor configuration from the
     * resource file and instantiating the list of post processor information.
     * <p>This method should be called exactly once at application start up.</p>
     *  
     * @throws IOException if there is an I/O error while loading the configuration.
     * @throws IllegalStateException if this object has already been initialised.
     */
    public synchronized void initialise() throws IOException {
        if (initialised) {
            throw new IllegalStateException("PostProcessorLoader already initialised");
        }
        
        InputStream in = getClass().getResourceAsStream(RESOURCE_PATH);
        if (in == null) {
            throw new FileNotFoundException(RESOURCE_PATH);
        }
        CSVReader reader = new CSVReader(new InputStreamReader(in));
        try {
            info = new ArrayList<PostProcessorInfo>();
            String[] next;
            while ((next = reader.readNext()) != null) {
                if (next.length == 2) {
                    info.add(new PostProcessorInfo(next[0], Boolean.parseBoolean(next[1])));
                }
            }
        } finally {
            reader.close();
        }
        
        Collections.sort(info, new PostProcessInfoSorter());
        info = Collections.unmodifiableList(info);
        
        initialised = true;
    }
    
    /**
     * Get the list of post processor information.
     * 
     * @return The PostProcessorInfo list.
     */
    public List<PostProcessorInfo> getPostProcessorInfo() {
        return info;
    }
    
    /**
     * Comparator to sort the post processors by recommended status and name.
     * Recommended post processors appear before others.
     */
    private static class PostProcessInfoSorter implements Comparator<PostProcessorInfo>
    {
        /**
         * Locale specific comparator for name strings.
         * @see Collator
         */
        private Comparator<Object> collator = Collator.getInstance();
        
        /**
         * Compare two PostProcessorInfo objects for ordering.
         * 
         * @param o1 PostProcessorInfo one.
         * @param o2 PostProcessorInfo two.
         * 
         * @return A negative integer, zero, or a positive integer as the
         *         first argument is less than, equal to, or greater than the
         *         second.
         */
        @Override
        public int compare(PostProcessorInfo o1, PostProcessorInfo o2) {
            int rec1 = o1.isRecommended() ? 1 : 0;
            int rec2 = o2.isRecommended() ? 1 : 0;
            
            int result = rec2 - rec1;
            if (result == 0) {
                result = collator.compare(o1.getName(), o2.getName());
            }
            
            return result;
        }
    }
}
