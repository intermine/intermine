package org.intermine.web.logic.results;

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

import org.intermine.api.profile.InterMineBag;

/**
 * Wrapper, mainly to provide size
 * @author radek
 *
 */
public class ReportInList
{

    private Collection<InterMineBag> bagsWithId = null;

    /**
     * Setup
     * @param bagsWithId collection
     */
    public ReportInList(Collection<InterMineBag> bagsWithId) {
        this.bagsWithId = bagsWithId;
    }

    /**
     *
     * @return the collection
     */
    public Collection<InterMineBag> getCollection() {
        return bagsWithId;
    }

    /**
     *
     * @return size of the collection
     */
    public Integer getCount() {
        Integer count = 0;
        for (InterMineBag b : bagsWithId) {
            if (b != null) {
                count++;
            }
        }
        return count;
    }

}
