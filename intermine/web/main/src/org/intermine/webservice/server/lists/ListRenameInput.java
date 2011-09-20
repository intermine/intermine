package org.intermine.webservice.server.lists;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.BagManager;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class ListRenameInput extends ListInput {

    private final String oldName;
    private final String newName;

    public static final String OLD_NAME_PARAM = "oldname";
    public final static String NEW_NAME_PARAM = "newname";

    public ListRenameInput(HttpServletRequest request, BagManager bagManager) {
        super(request, bagManager);
        this.oldName = request.getParameter(OLD_NAME_PARAM);
        this.newName = request.getParameter(NEW_NAME_PARAM);
        validateRenameParams();
    }

    public String getNewName() {
        return this.newName;
    }

    public String getOldName() {
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
