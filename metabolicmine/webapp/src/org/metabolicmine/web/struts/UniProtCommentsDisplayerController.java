package org.metabolicmine.web.struts;

/*
 * Copyright (C) 2002-2010 metabolicMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Comment;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;

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
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
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

            // dealing with genes...
            if (object instanceof Gene) {
                // cast me Gene
                Gene gene = (Gene)object;
                result.put("gene", geneComments(gene));
            } else if (object instanceof Protein) {
                // cast me Protein
                Protein protein = (Protein)object;
                result.put("protein", proteinComments(protein));
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
     * Return a HashMap of comments retrieved for gene's proteins.
     * @author radek
     *
     * @param gene passed from a request
     * @return HashMap
     */
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
     *
     * @param gene passed from a request
     * @return HashMap
     */
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
