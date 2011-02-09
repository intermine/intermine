package org.metabolicmine.web.struts;

/*
 * Copyright (C) 2002-2011 metabolicMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Comment;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

public class UniProtCommentsDisplayerController extends TilesAction {

    // allowed comment types
    private String allowedCommentTypes[] = new String []{
            "similarity",
            "function",
            "tissue specificity",
            "subcellular location",
            "catalytic activity",
            "disease",
            "developmental stage",
            "pathway",
            "pharmaceutical"
            };

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        try {
            // get the gene/protein in question from the request
            InterMineObject object = (InterMineObject) request.getAttribute("object");
            // wrapper for the result so we can say what type it is
            HashMap<String, Object> result = new HashMap();

            // API connection
            HttpSession session = request.getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Model model = im.getModel();
            PathQuery query = new PathQuery(model);

            // dealing with genes...
            if (object instanceof Gene) {
                // cast me Gene
                Gene gene = (Gene)object;
                String geneID = String.valueOf(gene.getId());
                query = geneCommentsQuery(geneID, query);

                Profile profile = SessionMethods.getProfile(session);
                PathQueryExecutor executor = im.getPathQueryExecutor(profile);
                ExportResultsIterator values = executor.execute(query);

                result.put("gene", geneComments2(values));

                //result.put("gene", geneComments(gene));
            } else if (object instanceof Protein) {
                // cast me Protein
                Protein protein = (Protein)object;
                String proteinID = String.valueOf(protein.getId());
                query = proteinCommentsQuery(proteinID, query);

                Profile profile = SessionMethods.getProfile(session);
                PathQueryExecutor executor = im.getPathQueryExecutor(profile);
                ExportResultsIterator values = executor.execute(query);

                result.put("protein", proteinComments2(values));

                //result.put("protein", proteinComments(protein));
            } else {
                // big fat fail
            }

            request.setAttribute("response", result);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

    /**
     * Create a HashMap of comments (for all Gene Proteins).
     * @author radek
     *
     * @param values
     * @return
     */
    private Object geneComments2(ExportResultsIterator values) {
        // returned result columns positions
        int posCommentText = 0, posProteinID = 1, posObjectID = 2, posCommentType = 3;

        // resulting HashMap <comment.text, the rest...>
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();

        listing:
            while (values.hasNext()) {
                List<ResultElement> row = values.next();
                // XXX: no guarantee on order! Expect the unexpected...
                HashMap<String, Object> columns = new HashMap<String, Object>();

                // Gene.proteins.comments.text
                String commentText = ((ResultElement) row.get(posCommentText)).getField().toString();
                // comment is already there...
                if (result.containsKey(commentText)) {
                    // ... add the protein ID to an existing "row"
                    columns = (HashMap<String, Object>) result.get(commentText);
                    // fetch the values already there...
                    HashMap<String, String> proteins = (HashMap<String, String>)columns.get("proteins");
                    proteins.put(row.get(posObjectID).getField().toString(), row.get(posProteinID).getField().toString());
                    // add the new protein
                    columns.put("proteins", proteins);

                    // skip...
                    continue listing;
                } else {
                    // save new entry under "proteins"
                    HashMap<String, String> proteins = new HashMap<String, String>();
                    proteins.put(row.get(posObjectID).getField().toString(), row.get(posProteinID).getField().toString());
                    columns.put("proteins", proteins);

                    // save the comment text (again...)
                    columns.put("text", commentText);

                    // save the comment type
                    columns.put("type", row.get(posCommentType).getField().toString());

                    // save back to results
                    result.put(commentText, columns);
                }
            }

        return result;
    }

    /**
     * Create a HashMap of comments.
     * @author radek
     *
     * @param values
     * @return
     */
    private Object proteinComments2(ExportResultsIterator values) {
        // returned result columns positions
        int posCommentText = 0, posCommentType = 1;

        // resulting HashMap <comment.text, the rest...>
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();

        while (values.hasNext()) {
            List<ResultElement> row = values.next();

            // save back to results
            result.put(row.get(posCommentText).getField().toString(), row.get(posCommentType).getField().toString());
        }

        return result;
    }

    /**
     * Return an API query fetching all Comments for Proteins for Genes, ordered by type
     * @author radek
     *
     * @param geneID
     * @param query
     * @return
     */
    private PathQuery geneCommentsQuery(String geneID, PathQuery query) {
        query.addViews(
                "Gene.proteins.comments.text",
                "Gene.proteins.primaryIdentifier",
                "Gene.proteins.id",
                "Gene.proteins.comments.type",
                "Gene.primaryIdentifier"
                );
        query.addOrderBy("Gene.proteins.comments.type", OrderDirection.ASC);
        query.addConstraint(Constraints.eq("Gene.id", geneID));
        query.addConstraint(Constraints.oneOfValues("Gene.proteins.comments.type", Arrays.asList(allowedCommentTypes)));

        return query;
    }

    /**
     * Return an API query fetching all Comments for Genes, ordered by type
     * @author radek
     *
     * @param proteinID
     * @param query
     * @return
     */
    private PathQuery proteinCommentsQuery(String proteinID, PathQuery query) {
        query.addViews(
                "Protein.comments.text",
                "Protein.comments.type",
                "Protein.primaryIdentifier"
                );
        query.addOrderBy("Protein.comments.type", OrderDirection.ASC);
        query.addConstraint(Constraints.eq("Protein.id", proteinID));
        query.addConstraint(Constraints.oneOfValues("Protein.comments.type", Arrays.asList(allowedCommentTypes)));

        return query;
    }

    /**
     * Return a HashMap of comments retrieved for gene's proteins.
     * @author radek
     * @deprecated
     *
     * @param gene passed from a request
     * @return HashMap
     */
    @SuppressWarnings("unused")
    private HashMap<String, HashMap> geneComments(Gene gene) {
        // resulting HashMap
        HashMap<String, HashMap> result = new HashMap();

        // traverse proteins in a gene
        for (Protein protein : gene.getProteins()) {
            // traverse comments in a protein
            for (Comment comment : protein.getComments()) {
                // is the comment allowed?
                String commentType = comment.getType();
                if (Arrays.asList(allowedCommentTypes).contains(commentType)) {
                    // is it in the map already?
                    String commentText = comment.getText();
                    if (result.containsKey(commentText)) {
                        // ...add to the list of proteins
                        HashMap<String, Object> values = (HashMap)result.get(commentText);
                        // ...fetch the proteins
                        HashMap<String, Integer> proteins = (HashMap)values.get("proteins");
                        // add the new protein under primary identifier => id
                        proteins.put(protein.getPrimaryIdentifier(), protein.getId());
                        // save the proteins
                        values.put("proteins", proteins);
                        // save the new values
                        result.put(commentText, values);
                    } else {
                        // new map for all values
                        HashMap<String, Object> values = new HashMap<String, Object>();
                        // ...add new key and list as a value
                        HashMap<String, Integer> proteins = new HashMap<String, Integer>();
                        // add the new protein under primary identifier => id
                        proteins.put(protein.getPrimaryIdentifier(), protein.getId());
                        // save the new values
                        values.put("proteins", proteins);
                        // save the type
                        values.put("type", commentType);
                        // save the shebang
                        result.put(commentText, values);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Return a HashMap of comments retrieved for protein.
     * @author radek
     * @deprecated
     *
     * @param gene passed from a request
     * @return HashMap
     */
    @SuppressWarnings("unused")
    private Object proteinComments(Protein protein) {
            // resulting HashMap: text => type
        HashMap<String, String> result = new HashMap();

        // traverse comments in a protein
        for (Comment comment : protein.getComments()) {
            // is the comment allowed?
            String commentType = comment.getType();
            if (Arrays.asList(allowedCommentTypes).contains(commentType)) {
                result.put(comment.getText(), commentType);
            }
        }

        return result;
    }

}
