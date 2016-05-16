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
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.BadRequestException;
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

    private enum Filter { PREFIX, SUFFIX, CONTAINS, EXACT };

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

    private Filter getFilterType(String term) {
        if (term == null) {
            throw new IllegalArgumentException("term must not be null");
        } else if (term.startsWith("*") && term.endsWith("*")) {
            return Filter.CONTAINS;
        } else if (term.startsWith("*")) {
            return Filter.SUFFIX;
        } else if (term.endsWith("*")) {
            return Filter.PREFIX;
        } else {
            return Filter.EXACT;
        }
    }

    /**
     * Get the lists that match the current filter.
     * @param listManager The list manager which has all the lists.
     * @param nameFilter Filter over the names.
     * @return The lists that match the filter.
     */
    protected Collection<InterMineBag> getListsMatching(
            ListManager listManager,
            String nameFilter) {
        if (nameFilter == null) {
            throw new IllegalArgumentException("nameFilter must not be null");
        }

        nameFilter = nameFilter.trim();
        final Filter type = getFilterType(nameFilter);
        final String term = StringUtils.strip(nameFilter, "*");

        Set<InterMineBag> ret = new LinkedHashSet<InterMineBag>();
        for (InterMineBag bag: listManager.getLists()) {
            boolean suitable = false;
            if (bag != null) {
                String bagName = StringUtils.defaultString(bag.getName(), "");
                switch (type) {
                    case EXACT:
                        suitable = term.equals(bagName);
                        break;
                    case PREFIX:
                        suitable = bagName.startsWith(term);
                        break;
                    case SUFFIX:
                        suitable = bagName.endsWith(term);
                        break;
                    case CONTAINS:
                        suitable = bagName.contains(term);
                        break;
                    default:
                        throw new IllegalStateException("someone has gone and expanded this enum");
                }
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
        return format == Format.JSON
                || format == Format.HTML
                || format == Format.TEXT
                || Format.FLAT_FILES.contains(format);
    }

    private Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"lists\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, this.getCallback());
        } else if (getFormat() == Format.HTML) {
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
