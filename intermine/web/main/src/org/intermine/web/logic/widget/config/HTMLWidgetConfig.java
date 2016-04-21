package org.intermine.web.logic.widget.config;

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
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.widget.Widget;
import org.intermine.web.logic.widget.WidgetOptions;

/**
 * dummy widget to output html widget
 * @author julie
 */
public class HTMLWidgetConfig extends WidgetConfig
{

    private String content;

    @Override
    public Map<String, Collection<String>> getExtraAttributes(InterMineBag imBag, ObjectStore os) {
        return null;
    }

    @Override
    public Widget getWidget(InterMineBag imBag,
            InterMineBag populationBag,
            ObjectStore os,
            WidgetOptions options,
            String ids, String populationIds) {
        return null;
    }

/*    @Override
    public void setExternalLink(@SuppressWarnings("unused") String externalLink) {
        // dummy
    }

    @Override
    public void setExternalLinkLabel(@SuppressWarnings("unused") String externalLinkLabel) {
        // dummy
    }*/

    /**
     * Set the content of the widget.
     *
     * @param content content of widget
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Get the content of the widget.
     *
     * @return the content of the widget
     */
    public String getContent() {
        return content;
    }
}

