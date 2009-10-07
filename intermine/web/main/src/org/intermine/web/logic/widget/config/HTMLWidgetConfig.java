package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.Widget;

/**
 * dummy widget to output html widget
 * @author julie
 */
public class HTMLWidgetConfig extends WidgetConfig
{

    private String content;
    
    @Override
    public String getExternalLink() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getExternalLinkLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Collection<String>> getExtraAttributes(InterMineBag imBag, ObjectStore os)
                    throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Widget getWidget(InterMineBag imBag, ObjectStore os, List<String> attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setExternalLink(String externalLink) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setExternalLinkLabel(String externalLinkLabel) {
        // TODO Auto-generated method stub
        
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
    

}

