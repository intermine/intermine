package org.intermine.webservice.server.displayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.output.JSONFormatter;

public class DisplayerService extends WebService {

    public DisplayerService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.TEXT;
    }

    @Override
    protected void execute() throws Exception {
        // Get the displayer's name
        String name = getOptionalParameter("name", "World");
        output.addResultItem(Arrays.asList(String.format("Hello %s!", name)));
    }

}
