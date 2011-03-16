package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.Util;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Builds datastructure from go parent id to go term id.  Includes evidence codes.
 * @author julie
 */
public class GeneOntologyDisplayer extends CustomDisplayer
{

    private static final Set<String> ONTOLOGIES = new HashSet<String>();
    Map<String, Map<String, Set<String>>> goTermsByOntology = new HashMap<String, Map<String,
        Set<String>>>();

    /**
     * @param config config
     * @param im API
     */
    public GeneOntologyDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    static {
        ONTOLOGIES.add("GO:0008150");
        ONTOLOGIES.add("GO:0003674");
        ONTOLOGIES.add("GO:0005575");
    }

    @Override
    public void display(HttpServletRequest request, DisplayObject displayObject) {
        Model model = im.getModel();
        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);

        InterMineObject object = (InterMineObject) request.getAttribute("object");
        String primaryIdentifier = null;
        try {
            primaryIdentifier = (String) object.getFieldValue("primaryIdentifier");
        } catch (IllegalAccessException e) {
            return;
        }
        if (StringUtils.isEmpty(primaryIdentifier)) {
            return;
        }

        PathQuery query = buildQuery(model, primaryIdentifier);
        ExportResultsIterator result = executor.execute(query);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();
            String parentTerm = (String) row.get(0).getField();
            Map<String, Set<String>> termToEvidence = getChildTerms(parentTerm);
            String term = (String) row.get(1).getField();
            String code = (String) row.get(2).getField();
            Util.addToSetMap(termToEvidence, term, code);
        }

        request.setAttribute("goTerms", goTermsByOntology);
    }

    private Map<String, Set<String>> getChildTerms(String parentTerm) {
        Map<String, Set<String>> termToEvidence = goTermsByOntology.get(parentTerm);
        if (termToEvidence == null) {
            termToEvidence = new HashMap<String, Set<String>>();
            goTermsByOntology.put(parentTerm, termToEvidence);
        }
        return termToEvidence;
    }

    private PathQuery buildQuery(Model model, String primaryIdentifier) {
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.goAnnotation.ontologyTerm.parents.name",
                "Gene.goAnnotation.ontologyTerm.name",
                "Gene.goAnnotation.evidence.code.code");
        q.addOrderBy("Gene.goAnnotation.ontologyTerm.parents.name", OrderDirection.ASC);
        q.addOrderBy("Gene.goAnnotation.ontologyTerm.name", OrderDirection.ASC);

        // parents have to be main ontology
        q.addConstraint(Constraints.oneOfValues("Gene.goAnnotation.ontologyTerm.parents.identifier",
                ONTOLOGIES));

        // not a NOT relationship
        q.addConstraint(Constraints.isNull("Gene.goAnnotation.qualifier"));

        // gene from report page
        q.addConstraint(Constraints.eq("Gene.primaryIdentifier", primaryIdentifier));

        return q;
    }



}
