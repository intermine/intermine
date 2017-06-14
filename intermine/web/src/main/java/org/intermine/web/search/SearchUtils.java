package org.intermine.web.search;

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
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.LinkRedirectManager;
import org.intermine.api.lucene.KeywordSearchHit;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.WebConfig;

/**
 * The bits of the search infrastructure that belong in the web-package because
 * they have view dependent logic.
 * @author nils
 * @author Alex Kalderimis
 *
 */
public final class SearchUtils
{

    private static final Logger LOG = Logger.getLogger(SearchUtils.class);

    private SearchUtils() {
        // Hidden constructor.
    }

    /**
     * Parse the results into a human friendly form
     * @param im The InterMine state object.
     * @param webconfig The web configuration.
     * @param searchHits The actual search hits.
     * @return A collection of results
     */
    public static Collection<KeywordSearchResult> parseResults(
            InterMineAPI im,
            WebConfig webconfig,
            Collection<KeywordSearchHit> searchHits) {
        long time = System.currentTimeMillis();
        Model model = im.getModel();
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        Vector<KeywordSearchResult> searchResultsParsed = new Vector<KeywordSearchResult>();
        LinkRedirectManager redirector = im.getLinkRedirector();
        for (KeywordSearchHit keywordSearchHit : searchHits) {
            Class<?> objectClass = DynamicUtil.getSimpleClass(keywordSearchHit.getObject()
                    .getClass());
            ClassDescriptor classDescriptor = model.getClassDescriptorByName(objectClass.getName());
            InterMineObject o = keywordSearchHit.getObject();
            String linkRedirect = null;
            if (redirector != null) {
                linkRedirect = redirector.generateLink(im, o);
            }
            KeywordSearchResult ksr = new KeywordSearchResult(webconfig, o, classKeys,
                    classDescriptor, keywordSearchHit.getScore(), null, linkRedirect);
            searchResultsParsed.add(ksr);
        }
        LOG.debug("Parsing search hits took " + (System.currentTimeMillis() - time)  + " ms");
        return searchResultsParsed;
    }
}
