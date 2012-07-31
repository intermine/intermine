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

/**
 * Class to hold information about Intermine post processors.
 */
public class PostProcessorInfo
{
    /**
     * The post processor's name.
     */
    private String name;
    
    /**
     * Whether this post processor is recommended.
     */
    private boolean recommended;
    
    
    /**
     * Construct a new PostProcessorInfo with the mandatory name and recommended
     * flag.
     * 
     * @param name The post processor's name.
     * @param recommended Whether the post processor is recommended.
     */
    PostProcessorInfo(String name, boolean recommended) {
        this.name = name;
        this.recommended = recommended;
    }

    /**
     * Get the post processor's name.
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Whether the post processor is recommended.
     * @return The recommended status.
     */
    public boolean isRecommended() {
        return recommended;
    }
}
