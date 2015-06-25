package org.intermine.webservice.server.complexes;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.ols.CachedOlsOntologyTermFetcher;
import psidev.psi.mi.jami.datasource.InteractionWriter;
import psidev.psi.mi.jami.factory.InteractionWriterFactory;
import psidev.psi.mi.jami.json.InteractionViewerJson;
import psidev.psi.mi.jami.json.MIJsonOptionFactory;
import psidev.psi.mi.jami.json.MIJsonType;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.InteractionCategory;
import psidev.psi.mi.jami.model.impl.DefaultComplex;
import psidev.psi.mi.jami.model.impl.DefaultCvTerm;
import psidev.psi.mi.jami.model.impl.DefaultInteractor;
import psidev.psi.mi.jami.model.impl.DefaultModelledParticipant;
import psidev.psi.mi.jami.model.impl.DefaultOrganism;

/**
 * Web service that produces JSON required by the complex viewer.
 *
 * @author julie
 */
public class ExportService extends JSONService
{
    private static final String FORMAT_PARAMETER = "format";
    private static final String DEFAULT_FORMAT = "JSON";

    /**
     * Default constructor.
     * @param im The InterMine state object.
     */
    public ExportService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {

        // format is optional
        String format = request.getParameter(FORMAT_PARAMETER);
        if (StringUtils.isEmpty(format)) {
            format = DEFAULT_FORMAT;
        }

        // EBI complex identifier
        String identifier = getComplexIdentifier();

        // get complex from InterMine database, transform to JAMI complex
        DefaultComplex complex = getComplex(identifier);

        // initialise all existing json writers
        InteractionViewerJson.initialiseAllMIJsonWriters();

        // get jsob option factory
        MIJsonOptionFactory optionFactory = MIJsonOptionFactory.getInstance();

        // get json writer for complexes from factory
        InteractionWriterFactory writerFactory = InteractionWriterFactory.getInstance();
        InteractionWriter writer = null;

        try {
            // you will try to use a cachedOlsClient if available, otherwise, you don't sort
            // your features using ols
            writer = writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(
                    getRawOutput(), InteractionCategory.modelled, null, MIJsonType.n_ary_only,
                    new CachedOlsOntologyTermFetcher(), null));
        } catch (BridgeFailedException e) {
            writer = writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(
                    getRawOutput(), InteractionCategory.modelled, null, MIJsonType.n_ary_only,
                    null, null));
        }

        try {
            writer.start();
            writer.write(complex);
            writer.end();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private String getComplexIdentifier() {
        String identifier = StringUtils.substring(request.getPathInfo(), 1);
        if (StringUtils.isBlank(identifier)) {
            throw new BadRequestException("No identifier provided");
        }
        return identifier;
    }

    /**
     * @param identifier complex identifier, e.g. EBI-123
     * @return complex of interest
     * @throws ObjectStoreException if something goes wrong
     */
    protected DefaultComplex getComplex(String identifier) throws ObjectStoreException {

        // construct query
        PathQuery q = getQuery(identifier);

        // execute query
        ExportResultsIterator results = im.getPathQueryExecutor().execute(q);

        // create the complex
        DefaultComplex complex = new DefaultComplex(identifier);

        // loop through query results
        // each row will be a different protein
        while (results.hasNext()) {
            List<ResultElement> row = results.next();

            String name = (String) row.get(0).getField();
            String systematicName = (String) row.get(1).getField();
            String properties = (String) row.get(2).getField();
            String function = (String) row.get(3).getField();
            String primaryIdentifier = (String) row.get(4).getField();
            Integer stoichiometry = (Integer) row.get(5).getField();
            Integer taxonId = (Integer) row.get(6).getField();
            String biologicalRole = (String) row.get(7).getField();

            // set complex attributes
            complex.setFullName(name);
            complex.setSystematicName(systematicName);
            complex.setPhysicalProperties(properties);

            // interactor type
            CvTerm type = new DefaultCvTerm("protein");

            // organism
            DefaultOrganism organism = new DefaultOrganism(taxonId);

            // interactor
            DefaultInteractor interactor = new DefaultInteractor(primaryIdentifier, type, organism);

            // stoichiometry
            DefaultCvTerm stoichTerm = new DefaultCvTerm(stoichiometry.toString());

            // participant
            DefaultModelledParticipant participant
                = new DefaultModelledParticipant(interactor, stoichTerm);

            // biological role
            participant.setBiologicalRole(new DefaultCvTerm(biologicalRole));

            // set relationship to complex
            complex.addParticipant(participant);
        }
        return complex;
    }

    private PathQuery getQuery(String identifier) throws ObjectStoreException {
        PathQuery query = new PathQuery(model);
        query.addViews("Complex.name",
                "Complex.systematicName",
                "Complex.properties",
                "Complex.function",
                "Complex.allInteractors.participant.primaryIdentifier",
                "Complex.allInteractors.stoichiometry",
                "Complex.allInteractors.participant.organism.taxonId",
                "Complex.allInteractors.biologicalRole");
        query.addConstraint(Constraints.eq("Complex.identifier", identifier));
        return query;
    }
}
