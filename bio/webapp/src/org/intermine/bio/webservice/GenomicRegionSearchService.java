package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.bio.web.logic.GenomicRegionSearchQueryRunner;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.webservice.GenomicRegionSearchListInput.GenomicRegionSearchInfo;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.lists.ListInput;
import org.intermine.webservice.server.lists.ListMakerService;
import org.intermine.webservice.server.lists.ListServiceUtils;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONException;

/**
 * A web service resource to expose the Region Search functionality.
 * @author Alex Kalderimis
 */
public class GenomicRegionSearchService extends ListMakerService
{

    /**
     * Constructor.
     * @param im The InterMine state and settings object.
     */
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

        InterMineBag tempBag;
        try {
            tempBag = doListCreation(searchInput, profile, type);
        } catch (UnknownBagTypeException e) {
            throw new BadRequestException(e.getMessage(), e);
        }

        addOutputInfo(LIST_SIZE_KEY, tempBag.getSize() + "");

        List<String> row = new ArrayList<String>(searchInput.getSearchInfo().getInvalidSpans());
        output.addResultItem(row);
        if (!input.getTags().isEmpty()) {
            im.getBagManager().addTagsToBag(input.getTags(), tempBag, profile);
        }
        profile.renameBag(input.getTemporaryListName(), input.getListName());
    }

    /**
     * Create the list specified by the region search input.
     * @param input The input object, containing the values specified by the user.
     * @param profile The user's profile
     * @param type The unqualified name of the class of object in the new list.
     * @return A new list
     * @throws ObjectStoreException if there is an error running the queries.
     * @throws UnknownBagTypeException
     */
    protected InterMineBag doListCreation(GenomicRegionSearchListInput input, Profile profile,
        String type) throws ObjectStoreException, UnknownBagTypeException {
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
            return new GenomicRegionSearchListInput(req, bagManager, getPermission().getProfile(), im);
        } catch (JSONException e) {
            String msg = e.getMessage();
            if (msg == null) {
                throw new BadRequestException("Error parsing region search input");
            }
            throw new BadRequestException("Error parsing region search input: " + e.getMessage());
        }
    }

    /**
     * Create the queries used to run the genomic region search.
     * @param info The options input object.
     * @return A map from a region to the query needed to find objects in that region.
     */
    protected Map<GenomicRegion, Query> createQueries(GenomicRegionSearchInfo info) {
        return GenomicRegionSearchUtil.createRegionListQueries(
                info.getGenomicRegions(),
                info.getExtension(),
                GenomicRegionSearchQueryRunner.getChromosomeInfo(im, SessionMethods.getProfile(request.getSession())).get(info.getOrganism()),
                info.getOrganism(),
                info.getFeatureClasses());
    }
}
