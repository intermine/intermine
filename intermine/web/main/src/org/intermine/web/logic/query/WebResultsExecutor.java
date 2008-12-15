package org.intermine.web.logic.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.template.TemplateQuery;

// Very preliminary version of path query executor returning WebResults, because it 
// will be changed heavily checkstyle errors are ignored

public class WebResultsExecutor {
    private ObjectStore os;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagQueryConfig bagQueryConfig;
    private Profile profile;
    private List<TemplateQuery> conversionTemplates;
    private SearchRepository searchRepository;

    public WebResultsExecutor(ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig,
            Profile profile, List<TemplateQuery> conversionTemplates, 
            SearchRepository searchRepository) {
        this.os = os;
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.profile = profile;
        this.conversionTemplates = conversionTemplates;
        this.searchRepository = searchRepository;
    }

    public WebResults execute(PathQuery pq) throws ObjectStoreException {
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();

        Map<String, BagQueryResult> pathToBagQueryResult = new HashMap<String, BagQueryResult>();

        BagQueryRunner bqr = new BagQueryRunner(os, classKeys, bagQueryConfig,
                conversionTemplates);

        Map<String, InterMineBag> allBags = WebUtil.getAllBags(profile.getSavedBags(), 
                searchRepository);
        
        Query q = MainHelper.makeQuery(pq, allBags, pathToQueryNode, bqr, pathToBagQueryResult,
                false);

        Results results = os.execute(q);
        results.setBatchSize(Constants.BATCH_SIZE); 
        results.setNoPrefetch(); 

        WebResults webResults = new WebResults(pq, results, os.getModel(),
                pathToQueryNode, classKeys, pathToBagQueryResult);

        return webResults;
    }

    public ResultsInfo estimate(PathQuery pq) {
        return null;
    }

    public int count(PathQuery pq) {
        return 0;

    }
}
