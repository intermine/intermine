package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.store.Directory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * Action handles template search.
 *
 * @author Thomas Riley
 */
public class TemplateSearchAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(TemplateSearchAction.class);

    private Formatter formatter = new Formatter() {
        public String highlightTerm(String term, TokenGroup group) {
            if (group.getTotalScore() > 0) {
                return "<span style=\"background: yellow\">" + term + "</span>";
            }
            return term;
        }
    };
    
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
        Map globalTemplates = SessionMethods.getSuperUserProfile(context).getSavedTemplates();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        if (StringUtils.isNotEmpty(queryString)) {
            LOG.info("Searching " + sf.getType() + " templates for \""
                    + sf.getQueryString() + "\"");
            long time = System.currentTimeMillis();
            Directory dir = (Directory) context.getAttribute(Constants.TEMPLATE_INDEX_DIR);
            IndexSearcher is = new IndexSearcher(profile.getUserTemplatesIndex());
            IndexSearcher is2 = new IndexSearcher(dir);
            Searchable[] searchables;
            if (sf.getType().equals("user")) {
                searchables = new Searchable[]{is};
            } else if (sf.getType().equals("global")) {
                searchables = new Searchable[]{is2};
            } else {
                searchables = new Searchable[]{is, is2};
            }
            MultiSearcher searcher = new MultiSearcher(searchables);
            
            Analyzer analyzer = new SnowballAnalyzer("English", StopAnalyzer.ENGLISH_STOP_WORDS);

            Query query;
            try {
                QueryParser queryParser = new QueryParser("content", analyzer);
                query = queryParser.parse(queryString);
            } catch (ParseException err) {
                recordError(new ActionMessage("errors.templatesearch.badinput", err.getMessage()),
                        request);
                return mapping.findForward("templateSearch");
            }
            query = query.rewrite(IndexReader.open(dir)); // required to expand search terms
            Hits hits = searcher.search(query);
            
            time = System.currentTimeMillis() - time;
            Map hitMap = new LinkedHashMap();
            Map typeMap = new LinkedHashMap();
            Map highlightedMap = new HashMap();
            
            LOG.info("Found " + hits.length() + " document(s) that matched query '"
                    + queryString + "' in " + time + " milliseconds:");
            
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            
            for (int i = 0; i < hits.length(); i++) {
                TemplateQuery template = null;
                Document doc = hits.doc(i);
                String type = doc.get("type");
                String name = doc.get("name");
                
                if (type.equals("user")) {
                    template = (TemplateQuery) profile.getSavedTemplates().get(name);
                } else if (type.equals("global")) {
                    template = (TemplateQuery) globalTemplates.get(name);
                }
                
                hitMap.put(template, new Float(hits.score(i)));
                typeMap.put(template, type);
                
                TokenStream tokenStream
                    = analyzer.tokenStream("", new StringReader(template.getTitle()));
                highlighter.setTextFragmenter(new NullFragmenter());
                highlightedMap.put(template,
                        highlighter.getBestFragment(tokenStream, template.getTitle()));
            }
            
            request.setAttribute("results", hitMap);
            request.setAttribute("templateTypes", typeMap);
            request.setAttribute("highlighted", highlightedMap);
            request.setAttribute("querySeconds", new Float(time / 1000f));
            request.setAttribute("queryString", queryString);
            request.setAttribute("resultCount", new Integer(hitMap.size()));
        }
        
        return mapping.findForward("templateSearch");
    }

    
}
