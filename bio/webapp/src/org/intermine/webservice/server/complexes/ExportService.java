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


import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
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
    private static final String EBI = "intact";
    private static final Logger LOG = Logger.getLogger(ExportService.class);

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

        LOG.error("parsing Complex " + identifier);

        // get complex from InterMine database, transform to JAMI complex
        DefaultComplex complex = getComplex(identifier);

        // initialise all existing json writers
        InteractionViewerJson.initialiseAllMIJsonWriters();

        LOG.error("InteractionViewerJson.initialiseAllMIJsonWriters(); ");

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

    private DefaultComplex getComplex(String identifier) throws ObjectStoreException {

        LOG.error("QUERYING");

        Query q = getQuery(identifier);

        LOG.error("QUERYING " + q);

        Results results = im.getObjectStore().execute(q);

        DefaultComplex complex = new DefaultComplex(identifier);

        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            ResultsRow<?> row = (ResultsRow<?>) iter.next();

            LOG.error(" *** loopdy");

            String name = (String) row.get(0);

            LOG.error(" *** " + name);
            String systematicName = (String) row.get(1);
            String properties = (String) row.get(2);
            String function = (String) row.get(3);
            String primaryIdentifier = (String) row.get(4);
            String stoichiometry = (String) row.get(5);
            Integer taxonId = (Integer) row.get(6);
            String biologicalRole = (String) row.get(7);

            complex.setFullName(name);
            complex.setSystematicName(systematicName);
            complex.setPhysicalProperties(properties);

            CvTerm type = new DefaultCvTerm("protein");
            DefaultOrganism organism = new DefaultOrganism(taxonId);

            // java.lang.String name, CvTerm type, Organism organism, Xref uniqueId
            DefaultInteractor interactor = new DefaultInteractor(primaryIdentifier, type, organism);

            DefaultCvTerm stoichTerm = new DefaultCvTerm(stoichiometry);
            DefaultModelledParticipant participant
                = new DefaultModelledParticipant(interactor, stoichTerm);

            participant.setBiologicalRole(new DefaultCvTerm(biologicalRole));

            complex.addParticipant(participant);
        }
        return complex;
    }

    private Query getQuery(String identifier) throws ObjectStoreException {
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
        return MainHelper.makeQuery(query, new HashMap(), new HashMap(), null, new HashMap());
    }
}
