package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Export protein interaction information as SIF file suitable for use with
 * CytoScape if there are ProteinInteraction objects on the select list of
 * the query.
 * 
 * @author Florian Reisinger
 * @author Richard Smith
 */
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.flymine.model.genomic.ProteinInteraction;
import org.intermine.bio.networkview.FlyNetworkCreator;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.TableExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.results.WebTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * An implementation of TableExporter that exports protein interactions 
 * in cytoscape SIF format
 *
 * @author Florian Reisinger
 */
public class ProteinInteractionExporter implements TableExporter
{
    static final boolean DEBUG = false;

    /**
     * Method called to export a PagedTable object using the BioJava 
     * sequence and feature writers.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward export(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "attachment; filename=interaction"
                + StringUtil.uniqueString() + ".sif"); //flo

        OutputStream outputStream = null;

        PagedTable pt = SessionMethods.getResultsTable(session, request
                .getParameter("table"));


        int realFeatureIndex = ExportHelper.getFirstColumnForClass(pt, ProteinInteraction.class);

        int writtenInteractionsCount = 0; //flo
        Set exported = new HashSet();
        PrintWriter printWriter = null;
        
        try {
            WebTable rowList = pt.getAllRows(); // get all the data in rows
            
            // loop over the rows
            for (int rowIndex = 0; rowIndex < rowList.size()
                    && rowIndex <= pt.getMaxRetrievableIndex(); rowIndex++) {
                List<ResultElement> row;
                try {
                    row = rowList.getResultElements(rowIndex); // einzelne reihe des resultset
                } catch (RuntimeException e) {
                    // re-throw as a more specific exception
                    if (e.getCause() instanceof ObjectStoreException) {
                        throw (ObjectStoreException) e.getCause();
                    } else {
                        throw e;
                    }
                }
                // das objekt aus der gueltigen spalte der aktuellen reihe holen
                // get object of interest - ProteinInteraction
                InterMineObject object = (InterMineObject) row.get(realFeatureIndex).getInterMineObject();
                
                if (!exported.contains(object.getId())) {
                    // cast to ProteinInteraction
                    ProteinInteraction feature = (ProteinInteraction) object;
                    
                    if (outputStream == null) {
                        // try to avoid opening the OutputStream until we know that the query is
                        // going to work - this avoids some problems that occur when
                        // getOutputStream() is called twice (once by this method and again to
                        // write the error)
                        outputStream = response.getOutputStream();
                        printWriter = new PrintWriter(outputStream, true);
                    }

                    Set<ProteinInteraction> interactions = 
                        (Set<ProteinInteraction>) Collections.singleton(feature);
                    printWriter.write(getSifLines(interactions));
                    printWriter.flush();

                    writtenInteractionsCount++; //flo
                    exported.add(object.getId());
                }
            }
            
            if (printWriter != null) {
                printWriter.flush();
                printWriter.close();
            }
            
            if (outputStream != null) {
                outputStream.close();
            }

            if (writtenInteractionsCount == 0) {
                ActionErrors messages = new ActionErrors();
                ActionError error = new ActionError("errors.export.nothingtoexport");
                messages.add(ActionErrors.GLOBAL_ERROR, error);
                request.setAttribute(Globals.ERROR_KEY, messages);

                return mapping.findForward("results");
            }
        //} catch (ObjectStoreException e) {
        } catch (Exception e) {
            ActionErrors messages = new ActionErrors();
            ActionError error = new ActionError("errors.query.objectstoreerror");
            messages.add(ActionErrors.GLOBAL_ERROR, error);
            request.setAttribute(Globals.ERROR_KEY, messages);
        }
        return null;
    }

    /**
     * @param pt the PagedTable containing the results 
     * @return true if exportable results were found
     * @see org.intermine.web.logic.export.TableExporter#canExport
     * @see org.intermine.bio.web.PIUtil#canExport
     */
    public boolean canExport(PagedTable pt) {
        return ExportHelper.canExport(pt, ProteinInteraction.class);
    }
    
    /**
     * create lines in sif format
     * @param interactors list of interactors
     * @return String respresenting the network in sif format
     */
    public static String getSifLines(Collection<ProteinInteraction> interactions) {
        FlyNetwork fn = FlyNetworkCreator.createFlyNetwork(interactions);
        return fn.toSIF();
    }
        
        /**
     * create lines in sif format
     * @param interactors list of interactors
     * @return String respresenting the network in sif format
     */
//    public static String getSifLinesOld(Collection interactors) {
//        StringBuffer sif = new StringBuffer();
//        if (DEBUG) {
//            if (interactors.size() > 0) {
//                sif.append("found interactors - ");
//            }
//        } // flo debug
//        // lists of proteins
//        ArrayList baits = new ArrayList();
//        ArrayList preys = new ArrayList();
//        ArrayList others = new ArrayList();
//        ProteinInteractor pInt = null;
//        for (Iterator i = interactors.iterator(); i.hasNext();) {
//            pInt = (ProteinInteractor) i.next();
//            if (DEBUG) {
//                if (pInt != null) {
//                    sif.append("found protIntractr - ");
//                }
//            } // flo debug
//            if (pInt.getRole().equalsIgnoreCase("bait")) {
//                if (DEBUG) {
//                    sif.append("found bait - ");
//                } // flo debug
//                baits.add(pInt.getProtein());
//            } else if (pInt.getRole().equalsIgnoreCase("prey")) {
//                if (DEBUG) {
//                    sif.append("found prey - ");
//                } // flo debug
//                preys.add(pInt.getProtein());
//            } else {
//                if (DEBUG) {
//                    sif.append("found other - ");
//                }
//                others.add(pInt.getProtein());
//            }
//        }
//
//        Protein protein = null;
//        if (baits.size() == 1 && preys.size() > 0 && others.size() == 0) {
//            if (DEBUG) {
//                sif.append("adding protein name");
//            } // flo debug
//            protein = (Protein) baits.get(0);
//            // adding bait and Interaction type
//            sif.append(protein.getPrimaryAccession() + "\t" + "pp");
//            for (Iterator i = preys.iterator(); i.hasNext();) {
//                protein = (Protein) i.next();
//                // adding prey(s)
//                sif.append("\t" + protein.getPrimaryAccession());
//            }
//            sif.append("\n");
//        } else {
//            // link all proteins to each other
//            // TODO: test this
//            others.addAll(preys);
//            others.addAll(baits);
//            Protein[] p = new Protein[1];
//            p = (Protein[]) others.toArray(p);
//            for (int i = 0; i < others.size(); i++) {
//                for (int j = i + 1; j < others.size(); j++) {
//                    sif.append(p[i].getPrimaryAccession() + "\tcp\t"
//                            + p[j].getPrimaryAccession() + "\n");
//                }
//            }
//        }
//        return sif.toString();
//    }

}
