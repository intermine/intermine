package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;

import org.intermine.metadata.Model;

/**
 * Common interface for parsing source models into InterMine model format.
 *
 * @author Richard Smith
 */

public interface ModelParser
{

    /**
     * Read source model information and construct a InterMine Model object.
     * @param reader the source model to parse
     * @return the InterMine Model created
     * @throws Exception if Model not created successfully
     */
    public Model process(Reader reader) throws Exception;

}
