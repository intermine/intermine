package org.intermine.webservice.server.core;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;

public abstract class JSONService extends WebService {

    protected final BagManager bagManager;
    protected final Model model;

    private final Map<String, String> kvPairs = new HashMap<String, String>();

    public JSONService(InterMineAPI im) {
        super(im);
        bagManager = im.getBagManager();
        model = im.getObjectStore().getModel();
    }

    @Override
    protected void initState() {
        output.setHeaderAttributes(getHeaderAttributes());
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

    @Override
    protected int getDefaultFormat() {
        if (hasCallback()) {
            return WebService.JSONP_FORMAT;
        } else {
            return WebService.JSON_FORMAT;
        }
    }
}
