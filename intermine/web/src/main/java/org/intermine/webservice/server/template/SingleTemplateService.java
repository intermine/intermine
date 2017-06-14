package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;

/**
 * Service that responds with a single template.
 * @author Alex Kalderimis
 */
public class SingleTemplateService extends JSONService
{

    /** @param im The InterMine state object **/
    public SingleTemplateService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected boolean canServe(Format format) {
        switch (format) {
            case XML:
            case JSON:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "template.xml");
        return new StreamedOutput(out, new PlainFormatter(), separator);
    }

    @Override
    protected void execute() {
        String name = StringUtils.defaultString(request.getPathInfo(), "");
        name = name.replaceAll("^/", "");
        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("No name provided");
        }
        Profile p = getPermission().getProfile();
        TemplateManager tm = im.getTemplateManager();
        TemplateQuery t = tm.getUserOrGlobalTemplate(p, name);
        if (t == null) {
            throw new ResourceNotFoundException("No template found called " + name);
        }
        if (Format.JSON == getFormat()) {
            output.addResultItem(Arrays.asList(t.toJSON()));
        } else {
            ResponseUtil.setXMLHeader(response, name + ".xml");
            output.addResultItem(Arrays.asList(t.toXml()));
        }
    }

    @Override
    public String getResultsKey() {
        return "template";
    }

}
