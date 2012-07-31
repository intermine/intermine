package org.intermine.dataloader;

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
 * Represents a data source, by name and skeleton status.
 *
 * @author Matthew Wakeling
 */
public class Source
{
    private final String name;
    private final String type;
    private final boolean skeleton;

    /**
     * Create a new source object.
     * @param name the source name
     * @param type the source type
     * @param skeleton true if this soruce is a skeleton
     */
    public Source(String name, String type, boolean skeleton) {
        this.name = name;
        this.type = type;
        this.skeleton = skeleton;
    }

    /**
     * Create a new source object that isn't a skeleton.
     * @param name the source name
     * @param type the source type
     */
    public Source(String name, String type) {
        this.name = name;
        this.type = type;
        this.skeleton = false;
    }

    /**
     * Create a new source object with no type information.
     * @param name the source name
     * @param skeleton true if this soruce is a skeleton
     */
    public Source(String name, boolean skeleton) {
        this.name = name;
        this.type = null;
        this.skeleton = skeleton;
    }

    /**
     * Create a new source object with no type information that isn't a skeleton
     * @param name the source name
     */
    public Source(String name) {
        this.name = name;
        this.type = null;
        this.skeleton = false;
    }

    /**
     * Getter for name.
     *
     * @return a String
     */
    public String getName() {
        return name;
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "<Source: name=\"" + getName() + "\", type=\"" + getType() + "\", skeleton="
            + getSkeleton() + ">";
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }
}
