package org.intermine.webservice.server.idresolution;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import org.intermine.api.idresolution.Job;

/** @author Alex Kalderimis **/
public interface BagResultFormatter
{

    /**
     * Turn a job into a serialisable structure.
     * @param job The job to serialise.
     * @return A simple serialisable data structure.
     */
    Map<String, Object> format(Job job);

}
