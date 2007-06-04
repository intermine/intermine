package org.intermine.web.struts;

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
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Action handles search.
 *
 * @author Thomas Riley
 */
public class SearchAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(SearchAction.class);

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
        SearchForm sf = (SearchForm) form;
        String queryString = sf.getQueryString();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        if (StringUtils.isNotEmpty(queryString)) {
            String type = sf.getType();
            LOG.info("Searching " + sf.getScope() + " for \""
                    + sf.getQueryString() + "\"    - type: " + type);
            long time = System.currentTimeMillis();
            SearchRepository globalSearchRepository =
                (SearchRepository) context.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
            Map<String, ? extends WebSearchable> globalWebSearchables =
                globalSearchRepository.getWebSearchableMap(type);
            Directory globalDirectory = globalSearchRepository.getDirectory(type);
            SearchRepository userSearchRepository = profile.getSearchRepository();
            Map<String, ? extends WebSearchable> userWebSearchables = 
                userSearchRepository.getWebSearchableMap(type);
            Directory userDirectory = userSearchRepository.getDirectory(type);
            IndexSearcher userIndexSearcher = new IndexSearcher(userDirectory);
            IndexSearcher globalIndexSearcher = new IndexSearcher(globalDirectory);
            Searchable[] searchables;
            if (sf.getScope().equals("user")) {
                searchables = new Searchable[]{userIndexSearcher};
            } else if (sf.getScope().equals("global")) {
                searchables = new Searchable[]{globalIndexSearcher};
            } else {
                searchables = new Searchable[]{userIndexSearcher, globalIndexSearcher};
            }
            MultiSearcher searcher = new MultiSearcher(searchables);
            
            Analyzer analyzer = new SnowballAnalyzer("English", StopAnalyzer.ENGLISH_STOP_WORDS);

            Query query;
            try {
                QueryParser queryParser = new QueryParser("content", analyzer);
                query = queryParser.parse(queryString);
            } catch (ParseException err) {
                recordError(new ActionMessage("errors.search.badinput", err.getMessage()),
                        request);
                return mapping.findForward("search");
            }
            
            // required to expand search terms
            query = query.rewrite(IndexReader.open(globalDirectory));
            Hits hits = searcher.search(query);
            
            time = System.currentTimeMillis() - time;
            Map hitMap = new LinkedHashMap();
            Map scopeMap = new LinkedHashMap();
            Map highlightedMap = new HashMap();
            
            LOG.info("Found " + hits.length() + " document(s) that matched query '"
                    + queryString + "' in " + time + " milliseconds:");
            
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            
            for (int i = 0; i < hits.length(); i++) {
                WebSearchable webSearchable = null;
                Document doc = hits.doc(i);
                String scope = doc.get("scope");
                String name = doc.get("name");
                
                webSearchable = userWebSearchables.get(name);
                if (webSearchable == null) {
                    webSearchable = globalWebSearchables.get(name);
                }
                if (webSearchable == null) {
                    throw new RuntimeException("unknown WebSearchable: " + name);
                }
                
                hitMap.put(webSearchable, new Float(hits.score(i)));
                scopeMap.put(webSearchable, scope);
                
                String highlightString = 
                    webSearchable.getTitle() + "  - " + webSearchable.getDescription();
                TokenStream tokenStream
                    = analyzer.tokenStream("", new StringReader(highlightString));
                highlighter.setTextFragmenter(new NullFragmenter());
                highlightedMap.put(webSearchable, 
                                   highlighter.getBestFragment(tokenStream, highlightString));
            }
            
            request.setAttribute("results", hitMap);
            request.setAttribute("resultScopes", scopeMap);
            request.setAttribute("highlighted", highlightedMap);
            request.setAttribute("querySeconds", new Float(time / 1000f));
            request.setAttribute("queryString", queryString);
            request.setAttribute("resultCount", new Integer(hitMap.size()));
        }
        
        return mapping.findForward("search");
    }

    
}
