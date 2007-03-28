package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;

/**
 * Comparator used for ordering templates by description length.
 * @author kmr
 */
public class TemplateComparator implements Comparator
{
    /**
     * Compare two TemplateQuery objects by length of description.
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object arg0, Object arg1) {
        TemplateQuery template0 = (TemplateQuery) arg0;
        TemplateQuery template1 = (TemplateQuery) arg1;
        
        if (template0.title.length() < template1.title.length()) {
            return -1;
        } else {
            if (template0.title.length() > template1.title.length()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
