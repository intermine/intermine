package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.Path;


/**
 * Simple exporter interface. Objects implementing this interface are
 * able to make export.
 * @author Jakub Kulaviak
 **/
public interface Exporter
{
    /** Windows line separator  CR+LF **/
    String WINDOWS_SEPARATOR = "\r\n";

    /** Unix line separator  only LF **/
    String UNIX_SEPARATOR = "\n";

    /**
     * Do export.
     * @param it iterator over stuff to be exported
     * @param unionPathCollection a collection of Path combining old and new views
     * @param newPathCollection a collection of Path to export from the results
     */
    void export(Iterator<? extends List<ResultElement>> it,
            Collection<Path> unionPathCollection,
            Collection<Path> newPathCollection);


    /**
     * Perform the export with the exporter's defaults in the absence of
     * path collection changes.
     * @param resultIt iterator over stuff to be exported.
     */
    void export(Iterator<? extends List<ResultElement>> resultIt);

    /**
     * This method finds out if result row composed from instances of these
     * classes can be exported with actual implementation of exporter.
     * @param clazzes classes in row
     * @return true if result row can be exported or false
     */
    boolean canExport(List<Class<?>> clazzes);

    /**
     * @return count of written results
     */
    int getWrittenResultsCount();

}
