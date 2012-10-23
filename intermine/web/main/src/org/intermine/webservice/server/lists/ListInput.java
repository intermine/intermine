package org.intermine.webservice.server.lists;

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

    public static final String SUBTRACT_PARAM = "subtract";
    public final static String NAME_PARAMETER = "name";
    public final static String LISTS_PARAMETER = "lists";
    public final static String REFERENCE_PARAMETER = "references";
    public final static String TAGS_PARAMETER = "tags";
    public final static String TYPE_PARAMETER = "type";
    public final static String DESCRIPTION_PARAMETER = "description";
    public final static String REPLACE_PARAMETER = "replaceExisting";
    public final static String TEMP_SUFFIX = "_temp";
    public final static String EXTRA_PARAMETER = "extraValue";
    private static final String DEFAULT_LIST_NAME = "new_list";

    /**
     * Parse the values from the request, and validate them.
     * @param request
     * @param bagManager
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
    
    protected String produceName() {
        // Give the user a default name if none is provided.
        String nameParam = request.getParameter(NAME_PARAMETER);
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
    
    public String getListName() {
        return this.listName;
    }

    public String getExtraValue() {
        return this.extraValue;
    }

    public String getTemporaryListName() {
        return this.listName + TEMP_SUFFIX;
    }

    public String getDescription() {
        return this.description;
    }

    public String getType() {
        return this.type;
    }

    public Set<InterMineBag> getLists() {
        return new HashSet<InterMineBag>(this.lists.values());
    }

    public Set<InterMineBag> getReferenceLists() {
        return new HashSet<InterMineBag>(this.referencelists.values());
    }

    public Set<InterMineBag> getSubtractLists() {
        return new HashSet<InterMineBag>(this.toSubtract.values());
    }

    public Set<String> getTags()  {
        return new HashSet<String>(this.tags);
    }

    public boolean doReplace() {
        return this.replaceExisting;
    }

    protected void init() {
        populateListMap(lists, LISTS_PARAMETER);
        populateListMap(referencelists, REFERENCE_PARAMETER);
        populateList(tags, TAGS_PARAMETER);
        populateListMap(toSubtract, SUBTRACT_PARAM);
    }

    protected void populateList(List<String> list, String param) {
        String[] paramValues = request.getParameterValues(param);
        if (paramValues != null) {
            for (String value: paramValues) {
                String[] subvalues = StringUtils.split(value, ";");
                for (String subvalue: subvalues) {
                    list.add(subvalue);
                }
            }
        }
    }

    protected void populateListMap(Map<String, InterMineBag> map, String param) {
        String[] paramValues = request.getParameterValues(param);
        if (paramValues != null) {
            for (String value: paramValues) {
                String[] names = StringUtils.split(value, ";");
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
