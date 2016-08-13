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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * Class representing input to a list service request.
 * @author Alex Kalderimis
 *
 */
public class ListInput
{

    protected final HttpServletRequest request;
    protected final BagManager bagManager;
    private final String listName;
    private final String description;
    private final String type;
    private final String extraValue;
    private final Map<String, InterMineBag> lists = new HashMap<String, InterMineBag>();
    private final Map<String, InterMineBag> referencelists = new HashMap<String, InterMineBag>();
    private final List<String> tags = new ArrayList<String>();
    protected final Profile profile;
    private final boolean replaceExisting;
    private final Map<String, InterMineBag> toSubtract
        = new HashMap<String, InterMineBag>();

    /** Things to subtract **/
    public static final String SUBTRACT_PARAM = "subtract";
    /** The name of the new list **/
    public static final String NAME_PARAMETER = "name";
    /** The name of the new list (synonym) **/
    public static final String ALT_NAME_PARAM = "listName";
    /** The lists to operate on **/
    public static final String LISTS_PARAMETER = "lists";
    /** The lists to subtract from **/
    public static final String REFERENCE_PARAMETER = "references";
    /** Tags to apply **/
    public static final String TAGS_PARAMETER = "tags";
    /** The type of the list **/
    public static final String TYPE_PARAMETER = "type";
    /** The description **/
    public static final String DESCRIPTION_PARAMETER = "description";
    /** Whether to overwrite existing lists of the same name **/
    public static final String REPLACE_PARAMETER = "replaceExisting";
    /** A suffix to attach to temporary lists **/
    public static final String TEMP_SUFFIX = "_temp";
    /** extra value parameter **/
    public static final String EXTRA_PARAMETER = "extraValue";
    /** The default list name if none is provided **/
    private static final String DEFAULT_LIST_NAME = "new_list";
    /** The separator used between lists when passed joined together. **/
    public static final String LIST_SEPARATOR = ";";

    /**
     * Parse the values from the request, and validate them.
     * @param request The request.
     * @param bagManager A service for fetching bags.
     * @param profile The profile of the current user.
     */
    public ListInput(HttpServletRequest request, BagManager bagManager, Profile profile) {
        this.request = request;
        this.bagManager = bagManager;
        this.profile = profile;

        this.listName = produceName();
        this.description = request.getParameter(DESCRIPTION_PARAMETER);
        this.replaceExisting = Boolean.parseBoolean(REPLACE_PARAMETER);
        this.type = request.getParameter(TYPE_PARAMETER);
        this.extraValue = request.getParameter(EXTRA_PARAMETER);

        init();
        validate();
    }

    /** @return a name for the new list **/
    protected String produceName() {
        // Give the user a default name if none is provided.
        String nameParam = request.getParameter(NAME_PARAMETER);
        nameParam = StringUtils.isBlank(nameParam)
                ? request.getParameter(ALT_NAME_PARAM)
                : nameParam;
        String name;
        if (StringUtils.isBlank(nameParam)) {
            nameParam = DEFAULT_LIST_NAME;
            name = nameParam;
            Set<String> listNames = bagManager.getBags(profile).keySet();
            int counter = 2;

            while (listNames.contains(name)) {
                name = nameParam + "_" + counter;
                counter++;
            }
        } else {
            name = nameParam;
        }
        return name;
    }

    /** @return the list name **/
    public String getListName() {
        return this.listName;
    }

    /** @return the extra value **/
    public String getExtraValue() {
        return this.extraValue;
    }

    /** @return the temporary list name **/
    public String getTemporaryListName() {
        return this.listName + TEMP_SUFFIX;
    }

    /** @return the description **/
    public String getDescription() {
        return this.description;
    }

    /** @return the type of the list. **/
    public String getType() {
        return this.type;
    }

    /** @return the lists to operate on **/
    public Set<InterMineBag> getLists() {
        return new HashSet<InterMineBag>(this.lists.values());
    }

    /** @return the reference set of lists **/
    public Set<InterMineBag> getReferenceLists() {
        return new HashSet<InterMineBag>(this.referencelists.values());
    }

    /** @return the lists to subtract **/
    public Set<InterMineBag> getSubtractLists() {
        return new HashSet<InterMineBag>(this.toSubtract.values());
    }

    /** @return the tags to apply **/
    public Set<String> getTags()  {
        return new HashSet<String>(this.tags);
    }

    /** @return whether or not we should overwrite existing lists. **/
    public boolean doReplace() {
        return this.replaceExisting;
    }

    /** Initialise this structure **/
    protected void init() {
        populateListMap(lists, LISTS_PARAMETER);
        populateListMap(referencelists, REFERENCE_PARAMETER);
        populateList(tags, TAGS_PARAMETER);
        populateListMap(toSubtract, SUBTRACT_PARAM);
    }

    /**
     * read the parameters into a list.
     * @param list The list to fill with values.
     * @param param The parameter to read from.
     */
    protected void populateList(List<String> list, String param) {
        populateList(list, param, false);
    }

    /**
     * Read parameters into a list, normalising the values.
     * @param list The list to fill.
     * @param param The parameter to read from.
     */
    protected void populateNormedList(List<String> list, String param) {
        populateList(list, param, true);
    }

    private void populateList(List<String> list, String param, boolean lowerCase) {
        String[] paramValues = request.getParameterValues(param);
        if (paramValues != null) {
            for (String value: paramValues) {
                String[] subvalues = StringUtils.split(value, ";");
                for (String s: subvalues) {
                    if (lowerCase) {
                        s = s.toLowerCase();
                    }
                    list.add(s);
                }
            }
        }
    }

    /**
     * Fill a map with intermine bags from the names in a parameter.
     * @param map The map to fill.
     * @param param The parameter to read names from.
     */
    protected void populateListMap(Map<String, InterMineBag> map, String param) {
        String[] paramValues = request.getParameterValues(param);
        if (paramValues != null) {
            for (String value: paramValues) {
                String[] names = StringUtils.split(value, LIST_SEPARATOR);
                for (String name: names) {
                    InterMineBag list = bagManager.getBag(profile, name);
                    map.put(name, list);
                }
            }
        }
    }

    private void validate() {
        validateRequiredParams();
        validateBags();
    }

    /** Check that the required parameters are there **/
    protected void validateRequiredParams() {
        List<String> errors = new ArrayList<String>();
        if (StringUtils.isEmpty(listName)) {
            errors.add("Required parameter " + NAME_PARAMETER + " is missing.");
        }
        if (!errors.isEmpty()) {
            String message = StringUtils.join(errors, ", ");
            throw new BadRequestException(message);
        }
    }

    /**
     * Make sure that any lists names exist.
     * @param map The lists we have available to us.
     * @param errors A collector for accumulating errors.
     */
    protected void checkLists(Map<String, InterMineBag> map, List<String> errors) {
        for (Entry<String, InterMineBag> pair: lists.entrySet()) {
            if (pair.getValue() == null) {
                errors.add("You do not have access to " + pair.getKey());
            }
        }

    }

    private void validateBags() {
        List<String> errors = new ArrayList<String>();
        checkLists(lists, errors);
        checkLists(referencelists, errors);
        checkLists(toSubtract, errors);
        if (!errors.isEmpty()) {
            String message = StringUtils.join(errors, ", ");
            throw new ServiceForbiddenException(message);
        }

    }
}
