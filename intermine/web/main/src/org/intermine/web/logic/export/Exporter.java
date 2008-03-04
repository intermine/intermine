package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Simple Exporter interface. Objects implementing this interface are created with
 * Exporter factories.   
 * @author Jakub Kulaviak
 **/
public interface Exporter
{

    /**
     * Do export.
     */
    public void export();
    
}
