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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * Web service that returns xml representation of model.
 * @author Jakub Kulaviak
 */
public class ModelService extends WebService
{
    private static final String DEFAULT_CALLBACK = "parseModel";

    private static final String FILE_BASE_NAME = "model";

    public ModelService(InterMineAPI im) {
        super(im);
    }

    /**
     * {@inheritDoc}}
     */
    protected void execute(HttpServletRequest request, HttpServletResponse response) {
        Model model = this.im.getModel();
        try {
            if (formatIsJSON()) {
                ResponseUtil.setJSONHeader(response, FILE_BASE_NAME + ".json");
                Map<String, String> attributes = new HashMap<String, String>();
                if (formatIsJSONP()) {
                    String callback = getCallback();
                    if (callback == null || "".equals(callback)) {
                        callback = DEFAULT_CALLBACK;
                    }
                    attributes.put(JSONFormatter.KEY_CALLBACK, callback);
                }
                output.setHeaderAttributes(attributes);
                output.addResultItem(Arrays.asList(model.toJSONString()));
            } else {
                ResponseUtil.setXMLHeader(response, FILE_BASE_NAME + ".xml");
                response.getWriter().append(model.toString());
            }
        } catch (IOException e) {
            throw new InternalErrorException(e);
        }
    }
}
