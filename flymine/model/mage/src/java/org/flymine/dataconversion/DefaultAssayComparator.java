package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;

import org.intermine.xml.full.Item;

/**
 * Return -1 if first assay (o1) identifier is in natural order before the
 * second assay (o2) identifier, 1 if after, 0 if equal.
 * @author Richard Smith
 */
public class DefaultAssayComparator implements Comparator
{
    MageDataTranslator translator;
    String tgtNs;

    /**
     * Construct with a MageDataTranslator as access may be needed to ItemReader
     * or maps.
     * @param translator the translator this method is being used in
     */
    public DefaultAssayComparator(MageDataTranslator translator) {
        this.translator = translator;
        this.tgtNs = translator.getTgtNamespace();
    }

    /**
     * Check that both arguments are items and have MicroArrayAssay as class name.
     * @param o1 first object to compare
     * @param o2 second object to compare
     */
    protected void checkArguments(Object o1, Object o2) {
        if (!(o1 instanceof Item
              && ((Item) o1).getClassName().equals(tgtNs + "MicroArrayAssay")
              && o2 instanceof Item
              && ((Item) o2).getClassName().equals(tgtNs + "MicroArrayAssay"))) {
            throw new IllegalArgumentException("Objects to compare are no both "
                                               + "MicroArrayAssays");
        }
    }

    /**
     * Compare natural order of MicroArrayAssay.name.
     * @param o1 first object to compare
     * @param o2 second object to compare
     * @return comparason
     */
    public int compare(Object o1, Object o2) {
        checkArguments(o1, o2);

        String assay1 = ((Item) o1).getAttribute("name").getValue();
        String assay2 = ((Item) o2).getAttribute("name").getValue();

        return assay1.compareTo(assay2);
    }
}
