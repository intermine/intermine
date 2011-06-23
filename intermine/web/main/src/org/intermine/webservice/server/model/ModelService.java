package org.intermine.webservice.server.model;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;

/**
 * Web service that returns a serialised representation of the data model. The currently
 * supported formats are JSON and XML.
 *
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 */
public class ModelService extends WebService
{
    private static final String DEFAULT_CALLBACK = "parseModel";

    private static final String FILE_BASE_NAME = "model";

    /**
     * Constructor.
     * @param im The API settings bundle
     */
    public ModelService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out) {
        ResponseUtil.setXMLHeader(response, FILE_BASE_NAME + ".xml");
        return new StreamedOutput(out, new PlainFormatter());
    }

    @Override
    protected int getDefaultFormat() {
        return XML_FORMAT;
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response) {
        Model model = this.im.getModel();
        if (formatIsJSON()) {
            ResponseUtil.setJSONHeader(response, FILE_BASE_NAME + ".json");
            Map<String, Object> attributes = new HashMap<String, Object>();
            if (formatIsJSONP()) {
                String callback = getCallback();
                if (callback == null || "".equals(callback)) {
                    callback = DEFAULT_CALLBACK;
                }
                attributes.put(JSONFormatter.KEY_CALLBACK, callback);
            }
            attributes.put(JSONFormatter.KEY_INTRO, "\"model\":{");
            attributes.put(JSONFormatter.KEY_OUTRO, "}");
            output.setHeaderAttributes(attributes);
            output.addResultItem(Arrays.asList(model.toJSONString()));
        } else {
            output.addResultItem(Arrays.asList(model.toString()));
        }
    }
}
