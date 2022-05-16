package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2022 FlyMine
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.rdf.Namespaces;
import org.intermine.api.rdf.RDFHelper;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.PermanentURIHelper;
import org.intermine.web.uri.InterMineLUI;
import org.intermine.web.uri.InterMineLUIConverter;
import org.intermine.webservice.server.core.ResultProcessor;
import org.intermine.webservice.server.exceptions.BadRequestException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

/**
 * A class that defines the basic methods for processing RDF results.
 * @author Daniela Butano
 *
 */
public class RDFProcessor extends ResultProcessor
{
    private String uri;
    private InterMineLUIConverter luiConverter;

    private final InterMineAPI im;

    /**
     * Constructor
     * @param request the http request
     * @param im the intermine api class
     * @param  profile the user profile
     *
     */
    public RDFProcessor(HttpServletRequest request, InterMineAPI im, Profile profile) {
        this.im = im;
        uri = (new PermanentURIHelper(request)).getPermanentBaseURI();
        luiConverter = new InterMineLUIConverter(profile);
    }

    @Override
    public void write(Iterator<List<ResultElement>> resultIt, Output output) {
        Model model = ModelFactory.createDefaultModel();
        setKnownPrefixes(model);

        while (resultIt.hasNext())  {
            Resource resource = null;
            ClassDescriptor classDescriptor = null;
            List<ResultElement> row = resultIt.next();
            Integer id;
            ClassDescriptor currentClassDesc = null;
            Map<String, Resource> resources = new HashMap<>();

            for (ResultElement item : row) {
                Path path = item.getPath();
                classDescriptor = path.getLastClassDescriptor();

                if (currentClassDesc == null || currentClassDesc != classDescriptor) {
                    currentClassDesc = classDescriptor;
                    id = item.getId();
                    InterMineLUI lui = luiConverter.getInterMineLUI(id);

                    String resourceURI = (lui != null) ? uri.concat(lui.toString())
                            : uri.concat(id.toString());
                    if (classDescriptor.getOntologyTerm() != null) {
                        String[] terms = classDescriptor.getOntologyTerm().split(",");
                        for (int index = 0; index < terms.length; index++) {
                            resource = model.createResource(resourceURI,
                                    model.createResource(terms[index]));
                        }
                    } else {
                        resource = model.createResource(resourceURI,
                                RDFHelper.createIMTypeResource(classDescriptor));
                    }
                    resources.put(currentClassDesc.getName(), resource);
                    if (!resources.isEmpty()) {
                        String stringPath = path.toString();
                        stringPath = stringPath.substring(0, stringPath.lastIndexOf("."));
                        Path partialPath = null;

                        try {
                            partialPath = new Path(im.getModel(), stringPath);
                            if (partialPath.endIsReference() || partialPath.endIsCollection()) {
                                FieldDescriptor rd = partialPath.getEndFieldDescriptor();
                                String parentToLink =
                                        partialPath.getSecondLastClassDescriptor().getName();
                                Resource parentResource = resources.get(parentToLink);
                                parentResource.addProperty(
                                        RDFHelper.createIMProperty(rd), resource);
                            }
                        } catch (PathException pe) {
                            throw new BadRequestException(stringPath + " is not a valid path");
                        }
                    }
                }
                FieldDescriptor fd = path.getEndFieldDescriptor();
                if (fd.isAttribute() && item.getField() != null) {
                    resource.addProperty(RDFHelper.createProperty((AttributeDescriptor) fd),
                            item.getField().toString());
                }
            }

        }
        ((RDFOutput) output).addResultItem(model);
    }

    private void setKnownPrefixes(Model model) {
        Map<String, String> namespaces = Namespaces.getNamespaces();
        for (String prefix : namespaces.keySet()) {
            model.setNsPrefix(prefix, namespaces.get(prefix));
        }
    }
}
