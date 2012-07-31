package org.intermine.modelviewer.jaxb;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.xml.sax.ContentHandler;

/**
 * An internal interface to embellish <code>ContentHandler</code> with a generic
 * means of fetching the result from an implementation's parsing.
 */
interface BackupContentHandler extends ContentHandler {

    /**
     * Get the result of a parse.
     * 
     * @return The Object created by parsing a document.
     */
    Object getResult();
}
