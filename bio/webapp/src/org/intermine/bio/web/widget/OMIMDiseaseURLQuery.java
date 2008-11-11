package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.path.Path;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.WidgetURLQuery;

/**
 * Builds a pathquery.  Used when a user clicks on a results record in an enrichment widget.
 * @author Dominik Grimm
 */
public class OMIMDiseaseURLQuery implements WidgetURLQuery
{

    private InterMineBag bag;
    private String key;
    private ObjectStore os;

    /**
     * @param key value selected by user to display
     * @param bag bag included in query
     * @param os object store
     */
    public OMIMDiseaseURLQuery(ObjectStore os, InterMineBag bag, String key) {
        this.bag = bag;
        this.key = key;
        this.os = os;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery generatePathQuery() {

        Model model = os.getModel();
        PathQuery q = new PathQuery(model);

        List<Path> view = new ArrayList<Path>();

        Path genePrimaryIdentifier = PathQuery.makePath(model, q,
                                                        "Gene.primaryIdentifier");
        Path geneSymbol = PathQuery.makePath(model, q, "Gene.symbol");
        Path omimId = PathQuery.makePath(model, q, "Gene.omimDiseases.omimId");
        Path omimTitle = PathQuery.makePath(model, q, "Gene.omimDiseases.title");
        Path omimDes = PathQuery.makePath(model, q, "Gene.omimDiseases.description");

        view.add(genePrimaryIdentifier);
        view.add(geneSymbol);
        view.add(omimId);
        view.add(omimTitle);
        view.add(omimDes);

        q.setViewPaths(view);

        String bagType = bag.getType();

        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = q.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        q.addNode(bagType).getConstraints().add(c);

        constraintOp = ConstraintOp.LOOKUP;
        code = q.getUnusedConstraintCode();
        PathNode node = q.addNode("Gene.omimDiseases");
        c = new Constraint(constraintOp, key, false, label, code, id, null);
        node.getConstraints().add(c);

        q.setConstraintLogic("A and B");
        q.syncLogicExpression("and");

        Map<Path, String>  sortOrder = new LinkedHashMap<Path, String>();
        sortOrder.put(geneSymbol, PathQuery.ASCENDING);

        q.setSortOrder(sortOrder);

        return q;
    }
}
