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

import java.util.Properties;

/**
 * Class to hold information about standard Intermine data sources.
 */
public class SourceInfo
{
    /**
     * The type name of the data source.
     */
    private String type;

    /**
     * The descriptor for the database source.
     */
    private SourceDescriptor source;
    
    /**
     * Default values for properties.
     */
    private Properties defaults;
    
    
    /**
     * Construct a new SourceInfo with the mandatory type name, descriptor
     * and default values.
     * 
     * @param type The source type name.
     * @param source The source descriptor.
     * @param defaults Default property values.
     */
    SourceInfo(String type, SourceDescriptor source, Properties defaults) {
        this.type = type;
        this.source = source;
        this.defaults = defaults;
    }

    /**
     * Get the source type name.
     * @return The type name.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the source descriptor.
     * @return The source descriptor.
     */
    public SourceDescriptor getSource() {
        return source;
    }

    /**
     * Get the default property values.
     * @return The default values.
     */
    public Properties getDefaults() {
        return defaults;
    }
}
