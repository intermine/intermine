package org.intermine.bio.webservice;

import org.directwebremoting.util.Logger;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.lists.ListServiceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.bio.web.logic.GenomicRegionSearchQueryRunner;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.webservice.GenomicRegionSearchListInput.GenomicRegionSearchInfo;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.lists.ListInput;
import org.intermine.webservice.server.lists.ListMakerService;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONException;

public class GenomicRegionSearchService extends ListMakerService {
    
    private static final Logger LOG = Logger.getLogger(GenomicRegionSearchService.class);

    public GenomicRegionSearchService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getNewListType(ListInput input) {
        GenomicRegionSearchListInput searchInput = (GenomicRegionSearchListInput) input;
        return ListServiceUtils.findCommonSuperTypeOf(searchInput.getSearchInfo().getFeatureCds());
    }

    @Override
    protected void makeList(ListInput input, String type, Profile profile,
            Set<String> temporaryBagNamesAccumulator) throws Exception {

        if (input.doReplace()) {
            ListServiceUtils.ensureBagIsDeleted(profile, input.getListName());
        }
        if (profile.getCurrentSavedBags().containsKey(input.getListName())) {
            throw new BadRequestException("Attempt to overwrite an existing bag - name:'"
                    + input.getListName() + "'");
        }
        GenomicRegionSearchListInput searchInput = (GenomicRegionSearchListInput) input;

        InterMineBag tempBag = doListCreation(searchInput, profile, type);

        addOutputInfo(LIST_SIZE_KEY, tempBag.getSize() + "");

        final List<String> row = new ArrayList<String>(searchInput.getSearchInfo().getInvalidSpans());
        output.addResultItem(row);
        if (!input.getTags().isEmpty()) {
            im.getBagManager().addTagsToBag(input.getTags(), tempBag, profile);
        }
        profile.renameBag(input.getTemporaryListName(), input.getListName());
    }
    
    protected InterMineBag doListCreation(GenomicRegionSearchListInput input, Profile profile, String type) throws ObjectStoreException {
        final InterMineBag tempBag = profile.createBag(
                input.getTemporaryListName(), type, input.getDescription(), im.getClassKeys());
        Map<GenomicRegion, Query> queries = createQueries(input.getSearchInfo());
        for (Entry<GenomicRegion, Query> e : queries.entrySet()) {
            Query q = e.getValue();
            tempBag.addToBagFromQuery(q);
        }
        return tempBag;
    }
    
    /**
     * Gets the header attributes on the output object.
     * @return A map of header attributes for JSON output.
     */
    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = super.getHeaderAttributes();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"invalidSpans\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
            attributes.put(JSONFormatter.KEY_QUOTE, Boolean.TRUE);
        }
        return attributes;
    }
    
    @Override
    protected ListInput getInput(final HttpServletRequest req) {
        try {
            return new GenomicRegionSearchListInput(req, bagManager, im);
        } catch (JSONException e) {
            String msg = e.getMessage();
            if (msg == null) {
                throw new BadRequestException("Error parsing region search input");
            }
            throw new BadRequestException("Error parsing region search input: " + e.getMessage());
        }
    }

    private Map<GenomicRegion, Query> createQueries(GenomicRegionSearchInfo info) {
        return GenomicRegionSearchUtil.createRegionListQueries(
                info.getGenomicRegions(), 
                info.getExtension(), 
                GenomicRegionSearchQueryRunner.getChromosomeInfo(im, SessionMethods.getProfile(request.getSession())).get(info.getOrganism()), 
                info.getOrganism(), 
                info.getFeatureClasses());
    }
}
