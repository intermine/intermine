package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * A base class for all list services that create lists.
 * @author Alexis Kalderimis
 *
 */
public abstract class ListMakerService extends AuthenticatedListService
{

    private static final String LIST_TYPE_KEY = "type";

    /**
     * Constructor.
     * @param api The InterMine settings bundle.
     */
    public ListMakerService(final InterMineAPI api) {
        super(api);
    }
    
    @Override
    protected void validateState() {
        super.validateState();
        if (!getPermission().isRW()) {
            throw new ServiceForbiddenException("This request has not been authenticated with " +
                    "RW permission");
        }
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(super.getHeaderAttributes());
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"" + LIST_SIZE_KEY + "\":");
        }
        return attributes;
    }

    /**
     * Initialise the accumulator that builds up the list of temporary lists to delete.
     * @param accumulator The accumulator we are using.
     * @param input The parsed parameter input.
     */
    protected void initialiseDelendumAccumulator(final Set<String> accumulator,
            final ListInput input) {
        accumulator.add(input.getTemporaryListName());
    }

    /**
     * Calculate the type of the new list.
     * @param input The parsed parameter input.
     * @return The type name.
     */
    protected abstract String getNewListType(ListInput input);

    @Override
    protected void execute() throws Exception {
        final Profile profile = getPermission().getProfile();
        final ListInput input = getInput(request);

        addOutputInfo(LIST_NAME_KEY, input.getListName());

        final String type = getNewListType(input);
        addOutputInfo(LIST_TYPE_KEY, type);

        final Set<String> rubbishbin = new HashSet<String>();
        initialiseDelendumAccumulator(rubbishbin, input);
        try {
            makeList(input, type, profile, rubbishbin);
        } finally {
            for (final String delendum: rubbishbin) {
                ListServiceUtils.ensureBagIsDeleted(profile, delendum);
            }
        }
    }

    /**
     * Make the list requested by the user.
     * @param input The parsed parameter input.
     * @param type The type of the new list.
     * @param profile The profile to save the list in.
     * @param temporaryBagNamesAccumulator The accumulator to store the list of bags to delete.
     * @throws Exception If something goes wrong.
     */
    protected abstract void makeList(ListInput input, String type, Profile profile,
        Set<String> temporaryBagNamesAccumulator) throws Exception;

}
