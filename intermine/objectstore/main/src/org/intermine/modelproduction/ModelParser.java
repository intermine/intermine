package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
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
    Model process(Reader reader) throws ModelParserException;

    /**
     * Read source information and construct a list of InterMine ClassDescriptors
     * @param fileReader
     * @param packageName
     * @return
     */
	Set<ClassDescriptor> generateClassDescriptors(Reader fileReader,
			String packageName) throws ModelParserException;
}
