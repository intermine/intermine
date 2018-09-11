package org.intermine.web.url;

import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermanentURL {

    //permamanent ULR pattern: domain/context/prefix:external_local_id
    //e.g. humanmine.org/humanmine/uniprot:P31946
    public static final String LOCAL_ID_SEPARATOR = ":";

    public static boolean isPermanentURIPattern(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String context = request.getContextPath();

        List<String> prefixes = new ArrayList<>();
        //load prefixes
        prefixes.add("biosample");
        prefixes.add("ensembl");
        prefixes.add("uniprot");
        for (String prefix : prefixes) {
            String interminePattern = context + "/" + prefix + LOCAL_ID_SEPARATOR;
            if (requestURI.startsWith(interminePattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given a request as humanmine.org/humanmine/uniprot:P31946 returns the intermine internal id
     * @param
     * @return
     * @throws ObjectStoreException
     */
    public static Integer getInterMineId(HttpServletRequest permanentURLRequest) throws ObjectStoreException {
        if (isPermanentURIPattern(permanentURLRequest)) {
            String externalLocalId = permanentURLRequest.getRequestURI().split(LOCAL_ID_SEPARATOR)[1];
            PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
            pathQuery.addView("Protein.id");
            pathQuery.addConstraint(Constraints.lookup("Protein", externalLocalId, null));

            Map<String, BagQueryResult> returnBagQueryResults = new HashMap<String, BagQueryResult>();
            HttpSession session = permanentURLRequest.getSession();
            Profile profile = SessionMethods.getProfile(session);
            PathQueryExecutor executor = SessionMethods.getInterMineAPI(session).getPathQueryExecutor(profile);
            ExportResultsIterator iterator = executor.execute(pathQuery);
            Integer imId = null;
            if (iterator.hasNext()) {
                ResultElement row = iterator.next().get(0);
                imId = row.getId();
            } else {
                imId = -1;
            }
            return imId;
        }
        return null;
    }

    public static String generatePermanentURL() {
        return "";
    }

}
