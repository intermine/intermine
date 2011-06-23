package org.intermine.webservice.server.lists;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.output.JSONFormatter;

public abstract class ListMakerService extends AuthenticatedListService {

    public ListMakerService(InterMineAPI api) {
        super(api);
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(super.getHeaderAttributes());
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\""+ LIST_SIZE_KEY + "\":");
        }
        return attributes;
    }

    protected void initialiseDelendumAccumulator(Set<String> accumulator, ListInput input) {
        accumulator.add(input.getTemporaryListName());
    }

    protected abstract String getNewListType(ListInput input);

    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        Profile profile = SessionMethods.getProfile(request.getSession());
        ListInput input = getInput(request);

        addOutputInfo(LIST_NAME_KEY, input.getListName());

        String type = getNewListType(input);

        Set<String> rubbishbin = new HashSet<String>();
        initialiseDelendumAccumulator(rubbishbin, input);
        try {
            makeList(input, type, profile, rubbishbin);
        } finally {
            for (String delendum: rubbishbin) {
                ListServiceUtils.ensureBagIsDeleted(profile, delendum);
            }
        }
    }

    protected abstract void makeList(ListInput input, String type, Profile profile,
        Set<String> temporaryBagNamesAccumulator) throws Exception;



}
