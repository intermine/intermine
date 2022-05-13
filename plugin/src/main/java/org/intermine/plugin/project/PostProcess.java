package org.intermine.plugin.project;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A representation of a post-processing production step.
 * @author Kim Rutherford
 */
public class PostProcess extends Action
{
    private String version;

    /**
     * Set the version of this Source, e.g. 2.0.0
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the version of this object.
     * @return the version
     */
    public String getVersion() {
        return version;
    }
}
