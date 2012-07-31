package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.Organism;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Builds datastructure from go parent id to go term id.  Includes evidence codes.
 * @author julie
 */
public class GeneOntologyDisplayer extends ReportDisplayer
{
    /**
     * The names of ontology root terms.
     */
    public static final Set<String> ONTOLOGIES = new HashSet<String>();
    private static final Map<String, String> EVIDENCE_CODES = new HashMap<String, String>();
    private Map<String, Boolean> organismCache = new HashMap<String, Boolean>();

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public GeneOntologyDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    static {
        ONTOLOGIES.add("biological_process");
        ONTOLOGIES.add("molecular_function");
        ONTOLOGIES.add("cellular_component");

        EVIDENCE_CODES.put("EXP", "Inferred from Experiment");
        EVIDENCE_CODES.put("IDA", "Inferred from Direct Assay");
        EVIDENCE_CODES.put("IPI", "Inferred from Physical Interaction");
        EVIDENCE_CODES.put("IMP", "Inferred from Mutant Phenotype");
        EVIDENCE_CODES.put("IGI", "Inferred from Genetic Interaction");
        EVIDENCE_CODES.put("IEP", "Inferred from Expression Pattern");
        EVIDENCE_CODES.put("ISS", "Inferred from Sequence or Structural Similarity");
        EVIDENCE_CODES.put("ISO", "Inferred from Sequence Orthology");
        EVIDENCE_CODES.put("ISA", "Inferred from Sequence Alignment");
        EVIDENCE_CODES.put("ISM", "Inferred from Sequence Model");
        EVIDENCE_CODES.put("IGC", "Inferred from Genomic Context");
        EVIDENCE_CODES.put("RCA", "Inferred from Reviewed Computational Analysis");
        EVIDENCE_CODES.put("TAS", "Traceable Author Statement");
        EVIDENCE_CODES.put("NAS", "Non-traceable Author Statement");
        EVIDENCE_CODES.put("IC", "Inferred by Curator");
        EVIDENCE_CODES.put("ND", "No biological Data available");
        EVIDENCE_CODES.put("IEA", "Inferred from Electronic Annotation");
        EVIDENCE_CODES.put("NR", "Not Recorded ");
    }


    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        Profile profile = SessionMethods.getProfile(request.getSession());

        // noGoMessage
        boolean goLoadedForOrganism = true;

        // check whether GO annotation is loaded for this organism
        // if we can't work out organism just proceed with display
        String organismName = getOrganismName(reportObject);

        if (organismName != null) {
            goLoadedForOrganism = isGoLoadedForOrganism(organismName, profile);
        }

        if (!goLoadedForOrganism) {
            String noGoMessage = "No Gene Ontology annotation loaded for " + organismName;
            request.setAttribute("noGoMessage", noGoMessage);
        } else {
            Model model = im.getModel();
            PathQueryExecutor executor = im.getPathQueryExecutor(profile);

            InterMineObject object = (InterMineObject) reportObject.getObject();
            String primaryIdentifier = null;
            try {
                primaryIdentifier = (String) object.getFieldValue("primaryIdentifier");
            } catch (IllegalAccessException e) {
                return;
            }
            if (StringUtils.isEmpty(primaryIdentifier)) {
                return;
            }

            PathQuery query = buildQuery(model, new Integer(reportObject.getId()));
            ExportResultsIterator result = executor.execute(query);

            Map<String, Map<OntologyTerm, Set<String>>> goTermsByOntology =
                new HashMap<String, Map<OntologyTerm, Set<String>>>();

            while (result.hasNext()) {
                List<ResultElement> row = result.next();
                String parentTerm = (String) row.get(0).getField();
                parentTerm = parentTerm.replaceAll("_", " ");
                OntologyTerm term = (OntologyTerm) row.get(1).getObject();
                String code = (String) row.get(2).getField();
                addToOntologyMap(goTermsByOntology, parentTerm, term, code);
            }

            // If no terms in a particular category add the parent term only to put heading in JSP
            for (String ontology : ONTOLOGIES) {
                String parentTerm = ontology.replaceAll("_", " ");
                if (!goTermsByOntology.containsKey(parentTerm)) {
                    goTermsByOntology.put(parentTerm, null);
                }
            }
            request.setAttribute("goTerms", goTermsByOntology);
            request.setAttribute("codes", EVIDENCE_CODES);
        }
    }

    private void addToOntologyMap(Map<String, Map<OntologyTerm, Set<String>>> goTermsByOntology,
            String namespace, OntologyTerm term, String evidenceCode) {
        Map<OntologyTerm, Set<String>> termToEvidence = goTermsByOntology.get(namespace);
        if (termToEvidence == null) {
            termToEvidence = new HashMap<OntologyTerm, Set<String>>();
            goTermsByOntology.put(namespace, termToEvidence);
        }
        Set<String> codes = termToEvidence.get(term);
        if (codes == null) {
            codes = new HashSet<String>();
            termToEvidence.put(term, codes);
        }
        codes.add(evidenceCode);
    }

    private PathQuery buildQuery(Model model, Integer geneId) {
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.goAnnotation.ontologyTerm.parents.name",
                "Gene.goAnnotation.ontologyTerm.name",
                "Gene.goAnnotation.evidence.code.code");
        q.addOrderBy("Gene.goAnnotation.ontologyTerm.parents.name", OrderDirection.ASC);
        q.addOrderBy("Gene.goAnnotation.ontologyTerm.name", OrderDirection.ASC);

        // parents have to be main ontology
        q.addConstraint(Constraints.oneOfValues("Gene.goAnnotation.ontologyTerm.parents.name",
                ONTOLOGIES));

        // not a NOT relationship
        q.addConstraint(Constraints.isNull("Gene.goAnnotation.qualifier"));

        // gene from report page
        q.addConstraint(Constraints.eq("Gene.id", "" + geneId));

        return q;
    }

    private String getOrganismName(ReportObject reportObject) {
        Organism organism = ((BioEntity) reportObject.getObject()).getOrganism();
        if (organism != null) {
            if (!StringUtils.isBlank(organism.getName())) {
                return organism.getName();
            } else if (organism.getTaxonId() != null) {
                return "" + organism.getTaxonId();
            }
        }
        return null;
    }

    private boolean isGoLoadedForOrganism(String organismField, Profile profile) {
        if (!organismCache.containsKey(organismField)) {
            PathQuery q = new PathQuery(im.getModel());
            q.addViews("Gene.goAnnotation.ontologyTerm.name");
            if (StringUtils.isNumeric(organismField)) {
                q.addConstraint(Constraints.eq("Gene.organism.taxonId", organismField));
            } else {
                q.addConstraint(Constraints.eq("Gene.organism.name", organismField));
            }
            PathQueryExecutor executor = im.getPathQueryExecutor(profile);
            ExportResultsIterator result = executor.execute(q, 0, 1);
            organismCache.put(organismField, Boolean.valueOf(result.hasNext()));
        }
        return organismCache.get(organismField).booleanValue();
    }
}
