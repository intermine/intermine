package org.intermine.web.displayer;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.api.beans.PartnerLink;
import org.intermine.api.mines.Mine;
import org.intermine.api.mines.ObjectRequest;

/**
 * Helper class for intermine links generated on report and list pages
 *
 * @author Julie Sullivan
 */
public interface InterMineLinkGenerator
{
    /**
     * Query other intermines for this object
     *
     * @param thisMine The mine instance representing this application.
     * @param thatMine The mine object where we want to get the data from.
     * @param request The information about the things we want to get.
     * @return map of mines to objects to link to
     */
    Collection<PartnerLink> getLinks(Mine thisMine, Mine thatMine, ObjectRequest request);
}
