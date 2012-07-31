package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Interface for web service code generation.
 *
 * @author Fengyuan Hu
 *
 */
public interface WebserviceCodeGenerator
{

    /**
     * This method will generate web service source code from a path query or template query.
     *
     * @param wsCodeGeninfo a WebserviceCodeGenInfo object
     * @return web service source code in a string
     */
    String generate(WebserviceCodeGenInfo wsCodeGeninfo);

}
