package org.intermine.template;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * @author Kim Rutherford
 */
public class TemplateComparator implements Comparator<TemplateQuery>
{
    /**
     * Compare two TemplateQuery objects by title, falling back to name if the titles are
     * identical.
     * {@inheritDoc}
     */
    public int compare(TemplateQuery t0, TemplateQuery t1) {

        if (t0.getTitle().equals(t1.getTitle())) {
            return t0.getName().compareTo(t1.getName());
        } else {
            return t0.getTitle().compareTo(t1.getTitle());
        }
    }
}
