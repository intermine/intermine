package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.api.profile.InterMineBag;

/**
 * The common interface for formatters that know how to format lists.
 * @author Alex Kalderimis
 *
 */
public interface ListFormatter
{

    /**
     * Format a list into a list of string values.
     * @param list The list to format
     * @return A list of strings.
     */
    List<String> format(InterMineBag list);

    /**
     * set the size of the list.
     * @param size The size.
     **/
    void setSize(int size);


}
