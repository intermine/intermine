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
import org.intermine.model.bio.Comment;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;

public class UniProtCommentsDisplayerController extends TilesAction {
	
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
        	// get the gene in question from the request
        	Gene gene = (Gene) request.getAttribute("object");
        	
        	// allowed comment types
      	  	String sArray[] = new String []{"similarity", "function", "tissue specificity", "subcellular location", "catalytic activity",
      	  			"disease", "developmental stage", "pathway", "pharmaceutical"};
      	  	//convert array to list
      	  	List allowedCommentTypes = Arrays.asList(sArray);

      	  	// resulting HashMap
        	HashMap<String, HashMap> result = new HashMap();
        	
        	// traverse proteins in a gene
        	for (Protein protein : gene.getProteins()) {
        		// traverse comments in a protein
        		for (Comment comment : protein.getComments()) {
        			// is the comment allowed?
        			if (allowedCommentTypes.contains(comment.getType())) {
        				// is it in the map already?
        				String commentText = comment.getText();
        				if (result.containsKey(commentText)) {
        					// ...add to the list of proteins
        					HashMap<String, Object> values = (HashMap)result.get(commentText);
        					// ...fetch the proteins
        					ArrayList<String> proteins = (ArrayList)values.get("proteins");
        					// add the new protein primary identifier
        					proteins.add(protein.getPrimaryIdentifier());
        					// save the proteins
        					values.put("proteins", proteins);
        					// save the new values
        					result.put(commentText, values);
        				} else {
        					// new map for all values
        					HashMap<String, Object> values = new HashMap<String, Object>();
        					// ...add new key and list as a value
        					ArrayList<String> proteins = new ArrayList<String>();
        					// save add protein to the list
        					proteins.add(protein.getPrimaryIdentifier());
        					// save the new values
        					values.put("proteins", proteins);
        					// save the type
        					values.put("type", comment.getType());
        					// save the shebang
        					result.put(commentText, values);
        				}
        			}
        		}
        	}
        	request.setAttribute("comments", result);
        } catch (Exception err) {
            err.printStackTrace();
        }

        return null;
    }
}
