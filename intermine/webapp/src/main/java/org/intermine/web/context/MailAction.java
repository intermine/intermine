package org.intermine.web.context;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.util.Emailer;

/**
 * The type of objects that can send themselves with an Emailer.
 * @author Alex Kalderimis.
 *
 */
public interface MailAction
{

    /**
     * Inject an emailer, and do something.
     * @param emailer The emailer to use.
     * @throws Exception If something untoward happens.
     */
    void act(Emailer emailer) throws Exception;
}
