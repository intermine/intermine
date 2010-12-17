package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.model.bio.CellLine;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.GeneExpressionScore;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.logic.ModMineUtil;

/**
 * Class that generates heatMap data for a list of genes.
 */
public class HeatMapController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(SubListGBrowseTrackController.class);
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        InterMineBag bag = (InterMineBag) request.getAttribute("bag");

        ObjectStore os = im.getObjectStore();
        HttpSession session = request.getSession();

        /*
        class CellLineScore
        {
            // the cell line name
            private String cellLine;
            // the object id of the stored Item
            private Double score;
            
            public CellLineScore(String x, Double y) {
                cellLine = x;
                score = y;
            }  
            
            public Double getScore() {
                return score;
            }
            
            public String getCellLine() {
                return cellLine;
            }
        }

    */

        Map<String, List<CellLineScore>> geneCellLines =
            new HashMap<String, List<CellLineScore>>();

        Class c = null;
        try {
            // or use -
            // Class.forName(im.getObjectStore().getModel().getPackageName() + "." + bag.getType());
            // Class is: interface org.intermine.model.bio.Submission
            c = Class.forName(bag.getQualifiedType());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        if (!"Gene".equals(bag.getType())) { return null; }

        // Logic 1: query all the DccId for the list of submission in the bag, refer to
        //          OrthologueLinkController and BioUtil

        Set<Gene> genes = ModMineUtil.getGenes(im.getObjectStore(), bag);

        Set<String> primaryIds = new LinkedHashSet<String>();
        for (Gene gene : genes) {
            primaryIds.add(gene.getPrimaryIdentifier());
        }

        // Logic 2:
        // do query with list of genes, build map and pass it to jsp

        // create the query
        Query q = new Query();
        QueryClass qcGScore = new QueryClass(GeneExpressionScore.class);
        QueryField qfScore = new QueryField(qcGScore, "score");
        q.addFrom(qcGScore);

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryField qfGene = new QueryField(qcGene, "primaryIdentifier");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        q.addFrom(qcGene);

        QueryClass qcCellLine = new QueryClass(CellLine.class);
        QueryField qfCellLine = new QueryField(qcCellLine, "name");
        q.addFrom(qcCellLine);

        q.addToSelect(qfGene);
        q.addToSelect(qfCellLine);
        q.addToSelect(qfScore);

//        QueryClass qcDevStage = new QueryClass(DevelopmentalStage.class);
//        QueryField qfDevStage = new QueryField(qcDevStage, "name");
//        q.addFrom(qcDevStage);
//        q.addToSelect(qfDevStage);

        // join the tables
        QueryObjectReference rgn =
            new QueryObjectReference(qcGScore, "gene");
        ContainsConstraint cgn = new ContainsConstraint(rgn, ConstraintOp.CONTAINS,
                qcGene);
        QueryObjectReference rcl =
            new QueryObjectReference(qcGScore, "cellLine");
        ContainsConstraint ccl = new ContainsConstraint(rcl, ConstraintOp.CONTAINS,
                qcCellLine);
//        QueryObjectReference rdv =
//            new QueryObjectReference(qcGScore, "developmentalStage");
//        ContainsConstraint cdv = new ContainsConstraint(rdv, ConstraintOp.CONTAINS,
//                qcDevStage);

        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cgn);
        cs.addConstraint(ccl);
//        cs.addConstraint(cdv);
        cs.addConstraint(new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getContentsAsIds()));
        
        q.setConstraint(cs);

        q.addToOrderBy(qfGene, "ASC");
        q.addToOrderBy(qfCellLine, "ASC");
        
        Results results = os.execute(q);

        @SuppressWarnings("unchecked") Iterator<ResultsRow> iter =
            (Iterator) results.iterator();

        
        Profile profile = SessionMethods.getProfile(session);

        
        String prevGene = null;
        List<CellLineScore> aGeneScores = null;
        aGeneScores = new LinkedList<CellLineScore>();

        while (iter.hasNext()) {
            ResultsRow<?> row = (ResultsRow<?>) iter.next();
            
            String gene = (String) row.get(0);
            String line = (String) row.get(1);
            Double score = (Double) row.get(2);

            if (prevGene != null && !gene.equalsIgnoreCase(prevGene)) {
                geneCellLines.put(prevGene, aGeneScores);
                aGeneScores = new LinkedList<CellLineScore>();
            } 
            CellLineScore aScore = new CellLineScore(line, score);
            LOG.info("XZZc " + line + ": " +  score);
            aGeneScores.add(aScore);
            prevGene = gene;
        }
        // store the last item
        geneCellLines.put(prevGene, aGeneScores);
        
        LOG.debug("XZZ2 " + geneCellLines.size());

        request.setAttribute("geneCellLines", geneCellLines);
        return null;
    }
}
