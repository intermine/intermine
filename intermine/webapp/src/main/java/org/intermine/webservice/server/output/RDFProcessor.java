package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.PermanentURIHelper;
import org.intermine.web.uri.InterMineLUIConverter;
import org.intermine.webservice.server.core.ResultProcessor;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.List;

public class RDFProcessor extends ResultProcessor {
    private HttpServletRequest request;
    private Profile profile;
    private String uri;
    private InterMineLUIConverter lui;

    public RDFProcessor(HttpServletRequest request, Profile profile) {
        this.request = request;
        this.profile = profile;
        uri = (new PermanentURIHelper(request)).getPermanentBaseURI();
        uri = (!uri.endsWith("/")) ? uri.concat("/") : uri;
        lui = new InterMineLUIConverter(profile);
    }

    @Override
    public void write(Iterator<List<ResultElement>> resultIt, Output output) {
        Model model = ModelFactory.createDefaultModel();
        while (resultIt.hasNext())  {
            List<ResultElement> row = resultIt.next();
            Integer id = -1;
            for (ResultElement item : row) {
                id = item.getId();
                String resourceURI = uri.concat(lui.getInterMineLUI(id).toString());
                Resource resource = model.createResource(resourceURI);
                Path path = item.getPath();
                FieldDescriptor fd = path.getEndFieldDescriptor();
                if (fd.isAttribute() && item.getField() != null) {
                    String ontologyTerm = ((AttributeDescriptor) fd).getOntologyTerm();
                    resource.addProperty(model.createProperty(ontologyTerm), item.getField().toString());
                }
            }
            ((RDFOutput) output).addResultItem(model);
        }
    }
}
