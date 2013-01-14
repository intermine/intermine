package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.exceptions.BadRequestException;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.HTMLTableFormatter;
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

    private enum Filter{ PREFIX, SUFFIX, CONTAINS, EXACT };

    /**
     * Get the lists for this request.
     * @return The lists that are available.
     */
    protected Collection<InterMineBag> getLists() {
        ListManager listManager = new ListManager(im, getPermission().getProfile());
        String nameFilter = getOptionalParameter("name");
        if (nameFilter == null) {
            return listManager.getLists();
        } else {
            return getListsMatching(listManager, nameFilter);
        }
    }

    protected Collection<InterMineBag> getListsMatching(ListManager listManager, String nameFilter) {
        nameFilter = nameFilter.trim();
        final Filter type;
        if (nameFilter.startsWith("*") && nameFilter.endsWith("*")) {
            type = Filter.CONTAINS;
        } else if (nameFilter.startsWith("*")) {
            type = Filter.SUFFIX;
        } else if (nameFilter.endsWith("*")) {
            type = Filter.PREFIX;
        } else {
            type = Filter.EXACT;
        }
        String term = StringUtils.strip(nameFilter, "*");

        Set<InterMineBag> ret = new LinkedHashSet<InterMineBag>();
        for (InterMineBag bag: listManager.getLists()) {
            boolean suitable = false;
            switch (type) {
            case EXACT:
                suitable = term.equals(bag.getName());
                break;
            case PREFIX:
                suitable = bag.getName().startsWith(term);
                break;
            case SUFFIX:
                suitable = bag.getName().endsWith(term);
                break;
            case CONTAINS:
                suitable = bag.getName().contains(term);
                break;
            }
            if (suitable) {
                ret.add(bag);
            }
        }
        if (ret.isEmpty()) {
            throw new ResourceNotFoundException("No lists matched " + nameFilter);
        }
        return ret;
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.JSON;
    }

    @Override
    protected boolean canServe(Format format) {
        return format == Format.JSON || format == Format.HTML || format == Format.TEXT || Format.FLAT_FILES.contains(format);
    }

    private Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"lists\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, this.getCallback());
        } if (getFormat() == Format.HTML) {
            attributes.put(HTMLTableFormatter.KEY_COLUMN_HEADERS,
                Arrays.asList("Name", "Type", "Description", "Size"));
        }
        return attributes;
    }

    private ListFormatter getFormatter() {
        boolean jsDates = Boolean.parseBoolean(request.getParameter("jsDates"));
        if (formatIsJSON()) { // Most common - test this first.
            Profile profile = getPermission().getProfile();
            return new JSONListFormatter(im, profile, jsDates);
        }
        if (formatIsFlatFile() || Format.TEXT == getFormat()) {
            return new FlatListFormatter(); // One name per line, so tsv and csv is the same
        }
        if (Format.HTML == getFormat()) {
            return new HtmlListFormatter();
        }
        throw new BadRequestException("Unknown request format");
    }

}
