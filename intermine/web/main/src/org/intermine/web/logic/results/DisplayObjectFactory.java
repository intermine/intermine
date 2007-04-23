package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpSession;

import org.intermine.util.CacheMap;
import org.intermine.web.struts.ObjectDetailsController;
import org.intermine.model.InterMineObject;

/**
 * A factory for DisplayObjects.  If get() is called and the is no existing DisplayObject for the
 * argument InterMineObject, one is created, saved and returned.
 *
 * @author Kim Rutherford
 */

public class DisplayObjectFactory extends CacheMap
{
    private HttpSession session = null;

    /**
     * Create a new DisplayObjectCache for the given session.
     * @param session the HTTP session
     */
    public DisplayObjectFactory(HttpSession session) {
        this.session = session;
    }
    
    /**
     * Always returns true because get always returns an Object.
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return true;
    }

    /**
     * Get a DisplayObject for the given InterMineObject.  If there is no existing DisplayObject for
     * the argument InterMineObject, one is created, saved and returned.
     * {@inheritDoc}
     * @param object an InterMineObject to make a DisplayObject for
     * @return a DisplayObject
     */
    public synchronized Object get(Object object) {
        DisplayObject displayObject = (DisplayObject) super.get(object);

        if (displayObject == null) {
            InterMineObject interMineObject = (InterMineObject) object;
            DisplayObject newObject;
            
            try {
                newObject = ObjectDetailsController.makeDisplayObject(session, interMineObject);
            } catch (Exception e) {
                throw new RuntimeException("failed to make a DisplayObject", e);
            }

            put(interMineObject, newObject);
            return newObject;
        } else {
            return displayObject;
        }
    }
}
