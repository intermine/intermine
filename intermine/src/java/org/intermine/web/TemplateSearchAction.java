package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action handles template search.
 *
 * @author Thomas Riley
 */
public class TemplateSearchAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(TemplateSearchAction.class);
    
    /** 
     * Method called when user has submitted search form.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext context = session.getServletContext();
        TemplateSearchForm sf = (TemplateSearchForm) form;
        String queryString = sf.getQueryString();
        Map globalTemplates = (Map) context.getAttribute(Constants.GLOBAL_TEMPLATE_QUERIES);
        
        if (StringUtils.isNotEmpty(queryString)) {
            LOG.info("Searching " + sf.getType() + " templates for \""
                    + sf.getQueryString() + "\"");
            long time = System.currentTimeMillis();
            Directory dir = (Directory) context.getAttribute(Constants.TEMPLATE_INDEX_DIR);
            IndexSearcher is = new IndexSearcher(dir);
    
            Query query = QueryParser.parse(queryString, "description", new StandardAnalyzer());
            Hits hits = is.search(query);
            
            time = System.currentTimeMillis() - time;
            Map hitMap = new LinkedHashMap();
            
            LOG.info("Found " + hits.length() + " document(s) that matched query '"
                    + queryString + "' in " + time + " milliseconds:");
            for (int i = 0; i < hits.length(); i++) {
                Document doc = hits.doc(i);
                LOG.info("" + globalTemplates.get(doc.get("name")));
                hitMap.put(globalTemplates.get(doc.get("name")), new Float(hits.score(i)));
            }
            
            request.setAttribute("results", hitMap);
            request.setAttribute("querySeconds", new Float(time / 1000f));
            request.setAttribute("queryString", queryString);
            request.setAttribute("resultCount", new Integer(hitMap.size()));
        }
        
        return mapping.findForward("templateSearch");
    }

}
