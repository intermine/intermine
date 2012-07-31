package org.intermine.modelviewer.genomic;

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
 * Simple class recording a set of additions against a tag indicating the
 * source of the additions.
 */
public class GenomicAddition
{
    /**
     * The tag for these additions.
     */
    private String tag;
    
    /**
     * The addition objects.
     */
    private Classes additions;
    
    
    /**
     * Create a new GenomicAddition object with the given tag and additions.
     * 
     * @param tag The tag.
     * @param additions The additions.
     */
    public GenomicAddition(String tag, Classes additions) {
        this.tag = tag;
        this.additions = additions;
    }

    /**
     * Get the tag.
     * @return The tag.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Get the genomic additions.
     * @return The additions.
     */
    public Classes getAdditions() {
        return additions;
    }
}
