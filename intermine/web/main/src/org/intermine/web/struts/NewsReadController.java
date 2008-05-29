package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * @author "Xavier Watkins"
 *
 */
public class NewsReadController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
                                 throws Exception {
        String rssURI = (String) context.getAttribute("rss");
        URL feedUrl = new URL(rssURI);
        SyndFeedInput input = new SyndFeedInput();
        XmlReader reader;
        try {
            reader = new XmlReader(feedUrl);
        } catch (Throwable e) {
            // xml document at this url doesn't exist or is invalid, so the news cannot be read
            return null;
        }
        SyndFeed feed = input.build(reader);
        List<SyndEntry> entries = feed.getEntries();
        
        request.setAttribute("rssMap", entries);
        return null;
    }

}
