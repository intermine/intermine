package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.exceptions.BadRequestException;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * A service to report what lists a user has access to, and some details of
 * those lists.
 * @author Alexis Kalderimis.
 *
 */
public class AvailableListsService extends WebService
{

    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public AvailableListsService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Collection<InterMineBag> lists = getLists();
        ListFormatter formatter = getFormatter();
        formatter.setSize(lists.size());
        output.setHeaderAttributes(getHeaderAttributes());
        for (InterMineBag list: lists) {
            if (list == null) {
                continue;
            }
            output.addResultItem(formatter.format(list));
        }
    }

    /**
     * Get the lists for this request.
     * @return The lists that are available.
     */
    protected Collection<InterMineBag> getLists() {
        ListManager listManager = new ListManager(im, getPermission().getProfile());
        return listManager.getLists();
    }

    @Override
    protected int getDefaultFormat() {
        if (hasCallback()) {
            return JSONP_FORMAT;
        } else {
            return JSON_FORMAT;
        }
    }

    private Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"lists\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, this.getCallback());
        }
        return attributes;
    }

    private ListFormatter getFormatter() {
        int format = getFormat();
        boolean jsDates = Boolean.parseBoolean(request.getParameter("jsDates"));
        switch(format) {
            case (WebService.TSV_FORMAT): {
                return new FlatListFormatter();
            }
            case (WebService.JSON_FORMAT): {
                Profile profile = getPermission().getProfile();
                return new JSONListFormatter(im, profile, jsDates);
            }
            case (WebService.JSONP_FORMAT): {
                Profile profile = getPermission().getProfile();
                return new JSONListFormatter(im, profile, jsDates);
            }
            default: {
                throw new BadRequestException("Unknown request format");
            }
        }
    }

}
