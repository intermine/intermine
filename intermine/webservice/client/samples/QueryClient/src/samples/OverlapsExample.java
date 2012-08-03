package samples;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraintMultitype;
import org.intermine.pathquery.PathConstraintRange;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;

public class OverlapsExample {
    
    private static final String PROG_NAME = "SequenceFeaturesFinder-v2.0";
    private static final String ROOT_URL = "http://localhost/squirrelmine/service";
    private static final ServiceFactory FACTORY = new ServiceFactory(ROOT_URL);

    public static void main(String[] args)  {
        for (String arg: args) {
            System.out.println(arg);
        }
        if (args.length == 0) {
            System.out.println("Please supply some intervals");
            
        }
        
        QueryService service = FACTORY.getQueryService();

        // Create a query
        PathQuery query = new PathQuery(FACTORY.getModel());

        query.addViews("SequenceFeature.primaryIdentifier", "SequenceFeature.chromosomeLocation.locatedOn.primaryIdentifier",
                "SequenceFeature.chromosomeLocation.start", "SequenceFeature.chromosomeLocation.end");

        //query.addConstraint(Constraints.eq("SequenceFeature.organism.taxonId", "7227"));
        query.addConstraint(new PathConstraintRange("SequenceFeature.chromosomeLocation", ConstraintOp.OVERLAPS, Arrays.asList(args)));
        query.addConstraint(new PathConstraintMultitype("SequenceFeature", ConstraintOp.ISA, Arrays.asList("Gene", "Exon", "Intron")));

        // Run the query
        Iterator<List<Object>> result = service.getRowListIterator(query);
        
        // Print the results
        while (result.hasNext()) {
            System.out.printf("%s %s:%d-%d\n", result.next().toArray());
        }
    }
}
