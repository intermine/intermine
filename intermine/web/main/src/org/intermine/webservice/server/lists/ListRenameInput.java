package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * Class for encapsulating the input to a list rename request.
 * @author Alex Kalderimis.
 *
 */
public class ListRenameInput extends ListInput
{

    private final String oldName;
    private final String newName;

    /** The request parameter for the old name **/
    public static final String OLD_NAME_PARAM = "oldname";

    /** The request parameter for the new name **/
    public static final String NEW_NAME_PARAM = "newname";

    /**
     * Constructor.
     * @param request The web service request.
     * @param bagManager A bag manager.
     * @param profile The current user's profile.
     */
    public ListRenameInput(HttpServletRequest request,
            BagManager bagManager, Profile profile) {
        super(request, bagManager, profile);
        this.oldName = request.getParameter(OLD_NAME_PARAM);
        this.newName = request.getParameter(NEW_NAME_PARAM);
        validateRenameParams();
        validateBagAccess();
    }

    /**
     * Get the new name.
     * @return A name.
     */
    String getNewName() {
        return this.newName;
    }

    /**
     * Get the old name.
     * @return A name.
     */
    String getOldName() {
        if (!StringUtils.isEmpty(oldName)) {
            return this.oldName;
        } else {
            return getListName();
        }
    }

    @Override
    protected String produceName() {
        // Don't provide default names - makes no sense in this context
        return request.getParameter(NAME_PARAMETER);
    }

    @Override
    protected void validateRequiredParams() {
        // Disable
    }

    private void validateRenameParams() {
        List<String> errors = new ArrayList<String>();
        if (StringUtils.isEmpty(oldName) && StringUtils.isEmpty(getListName())) {
            errors.add(
                "Both '" + NAME_PARAMETER + "' and '" + OLD_NAME_PARAM + "' "
                + "are missing - at least one is required");
        }
        if (!StringUtils.isEmpty(oldName) && !StringUtils.isEmpty(getListName())) {
            errors.add(
                "Values have been supplied for '"
                + OLD_NAME_PARAM + "' (" + oldName + ") and '"
                + NAME_PARAMETER + "' (" + getListName()
                + "), but at most one value is expected");
        }
        if (StringUtils.isEmpty(newName)) {
            errors.add("Required parameter '" + NEW_NAME_PARAM + "' is missing");
        }
        if (!errors.isEmpty()) {
            String message = StringUtils.join(errors, ", ");
            throw new BadRequestException(message + " - PARAMS: "
                    + request.getQueryString());
        }
    }

    private void validateBagAccess() {
        if (!profile.getSavedBags().containsKey(this.getOldName())) {
            throw new ServiceForbiddenException("You do not have access to a list"
                + " called '" + getOldName() + "'.");

        }
    }

}
