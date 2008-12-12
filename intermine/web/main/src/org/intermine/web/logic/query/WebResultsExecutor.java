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
import org.intermine.web.logic.bag.BagConversionHelper;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.TableHelper;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.template.TemplateQuery;

// Very preliminary version of path query executor returning WebResults, because it 
// will be changed heavily checkstyle errors are ignored

public class WebResultsExecutor {
    private ObjectStore os;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagQueryConfig bagQueryConfig;
    private Profile superUserProfile;
    private Profile profile; 
    
    public WebResultsExecutor(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys, 
            BagQueryConfig bagQueryConfig, Profile superUserProfile, Profile profile) {
        this.os = os;
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.superUserProfile = superUserProfile;
        this.profile = profile;
    }
    
//    public WebResults execute(PathQuery pq) throws ObjectStoreException {
//        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
//        
//        Map<String, BagQueryResult> pathToBagQueryResult = new HashMap<String, BagQueryResult>();
//        
//        List<TemplateQuery> conversionTemplates = BagConversionHelper.getConversionTemplates(superUserProfile);
//        
//        BagQueryRunner bqr = new BagQueryRunner(os, classKeys, bagQueryConfig, conversionTemplates);
//                
//        Query q = MainHelper.makeQuery(pq, profile.getSavedBags(), pathToQueryNode, bqr,
//                pathToBagQueryResult, false);
//                
//        Results results = os.execute(q);
//        results.setBatchSize(TableHelper.BATCH_SIZE);  // define locally or elsewhere
//        results.setNoPrefetch();  // is this a global setting already?
//
//        WebResults webResults = new WebResults(pq, results, os.getModel(), pathToQueryNode, 
//                classKeys, pathToBagQueryResult);
//        
//        return webResults;
//    }
   
    

    //public WebResults execute(PathQuery pq, Profile profile, Map returnBagQueryResults) {    
    //    return null;
    //}
    
   public ResultsInfo estimate(PathQuery pq) {
       return null;
   }
   
   public int count(PathQuery pq) {
       return 0;
       
   }
}
