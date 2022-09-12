package org.intermine.webservice.server.entity;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.httpclient.HttpStatus;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.results.RDFObject;
import org.intermine.web.uri.InterMineLUI;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

import java.io.PrintWriter;


/**
 * Service to write an entity using RDF/XML format
 * @author Daniela Butano
 *
 */
public class EntityRepresentationService extends JSONService
{

    /**
     * Constructor
     *
     * @param im The InterMine configuration object.
     */
    public EntityRepresentationService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String luiInput = getRequiredParameter("lui");
        InterMineLUI lui = new InterMineLUI("/" + luiInput);
        String type = getRequiredParameter("format");
        if (!type.equalsIgnoreCase(WebServiceRequestParser.FORMAT_PARAMETER_RDF)) {
            throw new BadRequestException("Only rdf format has been implemented");
        }
        RDFObject rdfObject = new RDFObject(lui, im, request);
        if (!rdfObject.isValid()) {
            output = makeJSONOutput(out, getLineBreak());
            String filename = getRequestFileName() + ".json";
            ResponseUtil.setJSONHeader(response, filename, formatIsJSONP());
            throw new ResourceNotFoundException("The lui doesn't exist");
        } else {
            response.setStatus(HttpStatus.SC_OK);
            ResponseUtil.setRDFXMLContentType(response);
            PrintWriter out = new PrintWriter(response.getOutputStream());
            rdfObject.serializeAsRDF(out);
            response.flushBuffer();
        }
    }

    @Override
    protected boolean canServe(Format format) {
        //for now only rdf is supported
        return (format == Format.RDF);
    }
}
