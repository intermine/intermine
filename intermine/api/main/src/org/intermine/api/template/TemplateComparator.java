package org.intermine.api.template;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;

/**
 * Comparator used for ordering templates by title
 * @author kmr
 */
public class TemplateComparator implements Comparator
{
    /**
     * Compare two TemplateQuery objects by title.
     * {@inheritDoc}
     */
    public int compare(Object arg0, Object arg1) {

        TemplateQuery template0 = (TemplateQuery) arg0;
        TemplateQuery template1 = (TemplateQuery) arg1;

        if (template0.getTitle().equals(template1.getTitle())) {
            return template0.getName().compareTo(template1.getName());
        } else {
            return template0.getTitle().compareTo(template1.getTitle());
        }
    }
}
