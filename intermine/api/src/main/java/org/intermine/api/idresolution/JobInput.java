package org.intermine.api.idresolution;

/*
 * Copyright (C) 2002-2021 FlyMine
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

    /**
    * There is config that allows mines to configure match behaviour. e.g. matchOnFirst let's
    * you select the first / best match. This parameter overrides that setting. see #1494
    *
    * Basically we want to use this NOT in LOOKUPs but only in the list upload. However there
    * is only one bag query runner and it's used on both so we have to set this param for each
    * request.
    *
    * @return TRUE if you should ignore config
    */
    Boolean getIgnoreConfig();
}
