package org.intermine.bio.web.displayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.bio.web.logic.CytoscapeNetworkDBQueryRunner;
import org.intermine.bio.web.logic.CytoscapeNetworkUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;

public class CytoscapeNetworkDisplayer extends CustomDisplayer {

    public CytoscapeNetworkDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, DisplayObject displayObject) {

        ObjectStore os = im.getObjectStore(); // Get OS
        Model model = im.getModel(); // Get Model
        Profile profile = SessionMethods.getProfile(request.getSession()); // Get Profile
        PathQueryExecutor executor = im.getPathQueryExecutor(profile); // Get PathQueryExecutor

        Set<Integer> startingFeatureSet = new HashSet<Integer>(); // feature: gene or protein
        String featureType = "";

        //=== Get Interaction information ===
        Map<String, Set<String>> interactionInfoMap = CytoscapeNetworkUtil
                .getInteractionInfo(model, executor);

        //=== Handle object ===
        // From gene report page
        InterMineObject object = (InterMineObject) request.getAttribute("object");
        // From list analysis page
        InterMineBag bag = (InterMineBag) request.getAttribute("bag"); // OrthologueLinkController

        if (bag != null) {
            startingFeatureSet.addAll(bag.getContentsAsIds());
            if ("Gene".equals(bag.getType())) {
                featureType = "Gene";
            } else if ("Protein".equals(bag.getType())) {
                featureType = "Protein";
            }
        } else {
            startingFeatureSet.add(object.getId());
            if (object instanceof Gene) {
                featureType = "Gene";
            } else if (object instanceof Protein) {
                featureType = "Protein";
            }
        }

        //=== Query a full set of interacting genes ===
        CytoscapeNetworkDBQueryRunner queryRunner = new CytoscapeNetworkDBQueryRunner();
        Set<Integer> fullInteractingGeneSet = queryRunner.getInteractingGenes(
                featureType, startingFeatureSet, model, executor);
        request.setAttribute("fullInteractingGeneSet",
                StringUtil.join(fullInteractingGeneSet, ","));

        //=== Validation ===
        if (interactionInfoMap == null) {
            String dataNotIncludedMessage = "Interaction data is not integrated.";
            request.setAttribute("dataNotIncludedMessage", dataNotIncludedMessage);
        }

        // Check if interaction data available for the organism
        Gene aTestGene;
		try {
			aTestGene = (Gene) os.getObjectById((Integer) fullInteractingGeneSet.toArray()[0]);
	        String orgName = aTestGene.getOrganism().getName();
	        if (!interactionInfoMap.containsKey(orgName)) {
	            String orgWithNoDataMessage = "No interaction data found for "
	                    + orgName + " genes";
	            request.setAttribute("orgWithNoDataMessage", orgWithNoDataMessage);
	        }
		} catch (ObjectStoreException e) {
			request.setAttribute("exception", "An exception occured");
			e.printStackTrace();
		}
    }
}