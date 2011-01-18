package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.CacheMap;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;

/**
 * A factory for DisplayObjects.  If get() is called and the is no existing DisplayObject for the
 * argument InterMineObject, one is created, saved and returned.
 *
 * @author Kim Rutherford
 */

public class DisplayObjectFactory extends CacheMap<InterMineObject, DisplayObject>
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
    @Override public synchronized DisplayObject get(Object object) {
        InterMineObject imObj = (InterMineObject) object;
        DisplayObject displayObject = super.get(imObj);

        if (displayObject == null) {
            try {
                final InterMineAPI im = SessionMethods.getInterMineAPI(session);
                Model model = im.getModel();
                Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
                ServletContext servletContext = session.getServletContext();
                WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
                Properties webProperties = SessionMethods.getWebProperties(servletContext);
                displayObject = new DisplayObject(imObj, model, webConfig, webProperties,
                        classKeys);
            } catch (Exception e) {
                throw new RuntimeException("failed to make a DisplayObject", e);
            }

            super.put(imObj, displayObject);
        }
        return displayObject;
    }

    /**
     * Disable this method.
     *
     * @param key Do not use
     * @param value Do not use
     * @return never
     */
    public DisplayObject put(InterMineObject key, DisplayObject value) {
        throw new UnsupportedOperationException("Put called on DisplayObjectFactory");
    }
}
