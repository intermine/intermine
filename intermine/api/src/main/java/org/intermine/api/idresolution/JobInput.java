package org.intermine.api.idresolution;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

/**
 *
 * @author Alex
 *
 */
public interface JobInput
{
    /**
     *
     * @return IDs
     */
    Collection<String> getIds();

    /**
     *
     * @return extra value
     */
    String getExtraValue();

    /**
     *
     * @return type
     */
    String getType();

    /**
     *
     * @return true if case sensitive
     */
    Boolean getCaseSensitive();

    /**
     *
     * @return wildcards
     */
    Boolean getWildCards();

}
