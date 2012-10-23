package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Timestamp;

import org.intermine.api.tracker.track.TemplateTrack;

import junit.framework.TestCase;

/**
 * test the TemplateTrack class
 * @author dbutano
 *
 */
public class TemplateTrackTest extends TestCase
{

    public void testValidate() {
        TemplateTrack tt1 = new TemplateTrack("templateName", "username", "sessionIdentifier");
        assertEquals("templateName", tt1.getTemplateName());
        assertEquals("username", tt1.getUserName());
        assertEquals("sessionIdentifier", tt1.getSessionIdentifier());
        assertNull(tt1.getTimestamp());

        long currenTime = System.currentTimeMillis();
        TemplateTrack tt2 = new TemplateTrack("name", "username", "sessionIdentifier", new Timestamp(currenTime));
        assertEquals(currenTime, tt2.getTimestamp().getTime());

        assertEquals(true, new TemplateTrack("templateName", "username", "sessionIdentifier")
                                            .validate());
        assertEquals(false, new TemplateTrack("", "username", "sessionIdentifier").validate());
        assertEquals(true, new TemplateTrack("templateName", "", "sessionIdentifier").validate());
        assertEquals(false, new TemplateTrack("templateName", "username", "").validate());
    }
}
