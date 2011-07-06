package org.intermine.webservice.server.lists;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;

public abstract class AbstractListService extends WebService {

    public static final String LIST_SEPARATOR = ";";
    public static final String TEMP_SUFFIX = "_temp";
    protected static final String LIST_NAME_KEY = "listName";
    protected static final String LIST_SIZE_KEY = "listSize";

    protected final BagManager bagManager;
    protected final Model model;

    private final Map<String, String> kvPairs = new HashMap<String, String>();

    public AbstractListService(InterMineAPI im) {
        super(im);
        bagManager = im.getBagManager();
        model = im.getObjectStore().getModel();
    }

    @Override
    protected int getDefaultFormat() {
        if (hasCallback()) {
            return WebService.JSONP_FORMAT;
        } else {
            return WebService.JSON_FORMAT;
        }
    }

    @Override
    protected void initState() {
        output.setHeaderAttributes(getHeaderAttributes());
    }

    protected Set<ClassDescriptor> getClassesForBags(Collection<InterMineBag> bags) {
        Set<ClassDescriptor> classes = new HashSet<ClassDescriptor>();
        for (InterMineBag bag: bags) {
            ClassDescriptor cd = model.getClassDescriptorByName(bag.getType());
            classes.add(cd);
        }
        return classes;
    }

    protected ListInput getInput(HttpServletRequest request) {
        return new ListInput(request, bagManager);
    }

    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, getCallback());
        }
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        return attributes;
    }

    protected void addOutputInfo(String key, String value) {
        kvPairs.put(key, value);
    }
}
