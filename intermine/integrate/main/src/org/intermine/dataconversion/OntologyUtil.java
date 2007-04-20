package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import org.intermine.metadata.Model;
import org.intermine.util.StringUtil;
import org.intermine.util.XmlUtil;

/**
 * General purpose ontology methods.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class OntologyUtil
{
    /**
     * Generate a package qualified class name within the specified model from a space separated
     * list of namespace qualified names
     *
     * @param classNames the list of namepace qualified names
     * @param model the relevant model
     * @return the package qualified names
     */
    public static String generateClassNames(String classNames, Model model) {
        if (classNames == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator i = StringUtil.tokenize(classNames).iterator(); i.hasNext();) {
            sb.append(model.getPackageName() + "."
                      + XmlUtil.getFragmentFromURI((String) i.next()) + " ");
        }
        return sb.toString().trim();
    }
}
