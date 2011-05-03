package org.intermine.webservice.server.lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.Query;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.BadRequestException;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;

public class ListUnionService extends WebService {

    public static final String USAGE =
      "\nList Union Service\n"
      + "===================\n"
      + "Combine lists to a new one\n"
      + "Parameters:\n"
      + "lists: a list of list names - separated by semi-cola (';')\n"
      + "name: the name of the new list resulting from the union\n\n"
      + "Content: text/plain - list of ids\n";

    public static final String LIST_SEPARATOR = ";";
    public static final String TEMP_SUFFIX = "_temp";
    protected final BagManager bagManager;
    protected final Model model;

    public ListUnionService(InterMineAPI im) {
        super(im);
        bagManager = im.getBagManager();
        model = im.getObjectStore().getModel();
    }

    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, getCallback());
        }
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"newListSize\":");
        }
        return attributes;
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        if (!this.isAuthenticated()) {
            throw new BadRequestException("Not authenticated." + USAGE);
        }
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);
        Properties webProperties
            = (Properties) session.getServletContext().getAttribute(Constants.WEB_PROPERTIES);;

        String lists = request.getParameter("lists");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        boolean replace = Boolean.parseBoolean("replaceExisting");

        if (StringUtils.isEmpty(lists) || StringUtils.isEmpty(name)) {
            throw new BadRequestException("Name or lists is blank." + USAGE);
        }

        Map<String, Object> attributes = getHeaderAttributes();
        Map<String, String> kvPairs = new HashMap<String, String>();
        kvPairs.put("newListName", name);
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        output.setHeaderAttributes(attributes);

        String[] listNames = StringUtils.split(lists, LIST_SEPARATOR);

        Set<ClassDescriptor> classes = new HashSet<ClassDescriptor>();
        List<InterMineBag> listsToJoin = new ArrayList<InterMineBag>();
        for (int i = 0; i < listNames.length; i++) {
            String listName = listNames[i];
            InterMineBag bag = bagManager.getUserOrGlobalBag(profile, listName);
            if (bag == null) {
                throw new BadRequestException(listName + " is not a list you have access to");
            }

            ClassDescriptor cd = model.getClassDescriptorByName(bag.getType());
            classes.add(cd);
            listsToJoin.add(bag);
        }
        String type = ListServiceUtils.findCommonSuperTypeOf(classes);

        String tempName = name + TEMP_SUFFIX;
        try {
            InterMineBag newList = profile.createBag(tempName, type, description);
            for (InterMineBag bag: listsToJoin) {
                Query q = new Query();
                q.addToSelect(bag.getOsb());
                newList.addToBagFromQuery(q);
            }
            output.addResultItem(Arrays.asList("" + newList.size()));
            if (replace) {
                ListServiceUtils.ensureBagIsDeleted(profile, name);
            }
            profile.renameBag(tempName, name);
        } finally {
            ListServiceUtils.ensureBagIsDeleted(profile, tempName);
        }
    }
}
