package org.intermine.webservice.server.displayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.output.JSONFormatter;

public class DisplayerService extends JSONService {

    public DisplayerService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        // Get the displayer's name
        String name = request.getParameter("name");
        output.addResultItem(Arrays.asList("function() {}"));
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(super.getHeaderAttributes());
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"displayer\":");
        }
        return attributes;
    }


}
