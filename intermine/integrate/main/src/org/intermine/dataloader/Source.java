package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Represents a data source, by name and skeleton status.
 *
 * @author Matthew Wakeling
 */
public class Source
{
    private String name;
    private boolean skeleton;

    /**
     * Getter for name.
     *
     * @return a String
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for name.
     *
     * @param name a String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for skeleton.
     *
     * @return a boolean
     */
    public boolean getSkeleton() {
        return skeleton;
    }

    /**
     * Setter for skeleton.
     *
     * @param skeleton a boolean
     */
    public void setSkeleton(boolean skeleton) {
        this.skeleton = skeleton;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "<Source: name=\"" + getName() + "\", skeleton=" + getSkeleton() + ">";
    }
}
