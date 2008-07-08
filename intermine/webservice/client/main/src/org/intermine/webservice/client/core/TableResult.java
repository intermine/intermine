package org.intermine.webservice.client.core;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

/**
 * Interface representing data as a simple table. Data can be obtained directly or using iterator.
 *   
 * @author Jakub Kulaviak
 */
public interface TableResult 
{

    /**
     * @return iterator iterating over the result data
     */
    public Iterator<List<String>> getIterator ();

    /**
     * Returns all data at once. If you expect a lot of data then use <t>getIterator</t> method 
     * instead of getData method else OutOfMemoryException will be thrown.
     * @return all data at once 
     */
    public List<List<String>> getData ();

}

