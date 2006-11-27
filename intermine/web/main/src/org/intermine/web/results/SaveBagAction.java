package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.objectstore.query.Results;

import org.intermine.InterMineException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.Constants;
import org.intermine.web.InterMineAction;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.SessionMethods;
import org.intermine.web.WebUtil;
import org.intermine.web.bag.BagElement;
import org.intermine.web.bag.InterMineBag;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * Saves selected items in a new bag or combines with existing bag.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 * @author Kim Rutherford
 */
public class SaveBagAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(SaveBagAction.class);

    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        if (request.getParameter("saveNewBag") != null) {
            return saveBag(mapping, form, request, response);
        } else if (request.getParameter("addToExistingBag") != null) {
            return saveBag(mapping, form, request, response);
        } else {
            // the form was submitted without pressing a submit button, eg. using submit() from
            // Javascript
        }

        return null;
    }

    /**
     * The batch size to use when we need to iterate through the whole result set.
     */
    public static final int BIG_BATCH_SIZE = 10000;

    /**
     * Save the selected objects to a bag on the session
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    public ActionForward saveBag(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        SaveBagForm crf = (SaveBagForm) form;
        ObjectStore userProfileOs = ((ProfileManager) servletContext.getAttribute(Constants
                    .PROFILE_MANAGER)).getUserProfileObjectStore();

        int defaultMax = 10000;
        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", defaultMax);

        // Create the right kind of bag
        String selected = crf.getSelectedObjects()[0];
        int index = selected.indexOf(",");
        int col = Integer.parseInt(index == -1 ? selected : selected.substring(0, index));
        Object type = ((Column) pt.getColumns().get(col)).getType();

        String bagName;
        if (request.getParameter("saveNewBag") != null) {
            bagName = crf.getNewBagName();
        } else {
            bagName = crf.getExistingBagName();
        }

        // TODO make more generic when type thing is ready by creating an InterMineIdBag
        InterMineBag bag = new InterMineBag(profile.getUserId(), bagName, null, userProfileOs, os,
                                                     Collections.EMPTY_SET);

        // the number of complete columns to save in the bag
        int wholeColumnsToSave = 0;

        for (int i = 0; i < crf.getSelectedObjects().length; i++) {
            String selectedObjectString = crf.getSelectedObjects()[i];
            if (selectedObjectString.indexOf(",") == -1) {
                wholeColumnsToSave++;
            }
        }

        List allRows = pt.getAllRows();

        if (allRows instanceof Results) {
            Results results = (Results) allRows;

            if (wholeColumnsToSave * results.size() > maxBagSize) {
                ActionMessage actionMessage =
                    new ActionMessage("bag.tooBig", new Integer(maxBagSize));
                recordError(actionMessage, request);

                return mapping.findForward("results");
            }

            try {
                // make a copy of the Results object with a larger batch size so the object
                // store doesn't need to do lots of queries
                // we copy because setBatchSize() throws an exception if size() has already
                // been called
                Results newResults;

                if (maxBagSize < BIG_BATCH_SIZE) {
                    newResults = WebUtil.changeResultBatchSize(results, maxBagSize);
                } else {
                    newResults = WebUtil.changeResultBatchSize(results, BIG_BATCH_SIZE);
                }

                // make sure we can get the first batch
                try {
                    newResults.get(0);
                } catch (IndexOutOfBoundsException e) {
                    // Ignore - that means there are NO rows in this results object.
                }

                allRows = newResults;
            } catch (RuntimeException e) {
                if (e.getCause() instanceof ObjectStoreException) {
                    recordError(new ActionMessage("errors.query.objectstoreerror"),
                                request, (ObjectStoreException) e.getCause(), LOG);
                    return mapping.findForward("results");
                }
                throw e;
            } catch (ObjectStoreException e) {
                recordError(new ActionMessage("errors.query.objectstoreerror"),
                            request, e, LOG);
                return mapping.findForward("results");
            }
        }

        // as we add objects from the selected column to the bag, add the indexes
        // (ie. "2,3" - row 2, column 3) of the objects so we know not to add them twice
        Set seenObjects = new HashSet();
        ArrayList objectTypes = new ArrayList();

        // save selected columns first
        // TODO check if this really is obsolete now with the new bag system
//        if (wholeColumnsToSave > 0) {
//            int rowCount = 0;
//            for (Iterator rowIterator = allRows.iterator(); rowIterator.hasNext();) {
//                List thisRow = (List) rowIterator.next();
//
//                List rowToSave = new ArrayList();
//                
//                // go through the selected items (checkboxes) and add to the bag-to-save
//                for (Iterator itemIterator = Arrays.asList(crf.getSelectedObjects()).iterator();
//                     itemIterator.hasNext();) {
//                    String selectedObject = (String) itemIterator.next();
//                    // selectedObject is of the form "column,row" or "column"
//                    int commaIndex = selectedObject.indexOf(",");
//                    if (commaIndex == -1) {
//                        int column = Integer.parseInt(selectedObject);
//                        Object columnValue = null;
//                        if (column < thisRow.size()) {
//                            columnValue = thisRow.get(column);
//                        }
//                        rowToSave.add(columnValue);
//                        seenObjects.add(rowCount + "," + column);
//                    }
//                }
//                bag.add(rowToSave);
//                if (bag.size() > maxBagSize) {
//                    ActionMessage actionMessage =
//                        new ActionMessage("bag.tooBig", new Integer(maxBagSize));
//                    recordError(actionMessage, request);
//
//                    return mapping.findForward("results");
//                }
//                rowCount++;
//            }
//        }

        // now save individually selected items
        for (Iterator itemIterator = Arrays.asList(crf.getSelectedObjects()).iterator();
             itemIterator.hasNext();) {
            String selectedObject = (String) itemIterator.next();
            // renove the type information
            selectedObject = selectedObject.substring(0, selectedObject.lastIndexOf(","));
            // selectedObject is of the form "column,row" or "column"
            int commaIndex = selectedObject.indexOf(",");
            if (commaIndex != -1) {
                // use the column,row to pick out the object from PagedTable
                int column = Integer.parseInt(selectedObject.substring(0, commaIndex));
                int row = Integer.parseInt(selectedObject.substring(commaIndex + 1));
                if (seenObjects.contains(row + "," + column)) {
                    continue;
                }
                Object value = ((List) pt.getRows().get(row)).get(column);
                ResultElement resCell = (ResultElement) value;
                BagElement bagElement = new BagElement(resCell);
                
                if (!objectTypes.contains(resCell.getType())) {
                    objectTypes.add(resCell.getType());
                }
                
                bag.add(bagElement);
                if (bag.size() > maxBagSize) {
                    ActionMessage actionMessage =
                        new ActionMessage("bag.tooBig", new Integer(maxBagSize));
                    recordError(actionMessage, request);

                    return mapping.findForward("results");
                }
            }
        }
        
        if (objectTypes.size() > 1 || objectTypes == null) {
            ActionMessage actionMessage =
                new ActionMessage("The bag contains more than one different type");
            recordError(actionMessage, request);
            return mapping.findForward("results");
        }

        bag.setType(objectTypes.get(0).toString());
        
        InterMineBag existingBag = (InterMineBag) profile.getSavedBags().get(bagName);
        if (existingBag != null) {
            if (!existingBag.getType().equals(bag.getType())) {
                recordError(new ActionMessage("bag.typesDontMatch"), request);
                return mapping.findForward("results");
            }
            bag.addAll(existingBag);
            bag.setSavedBagId(existingBag.getSavedBagId());
            SessionMethods.invalidateBagTable(session, bagName);
        }
        if (bag.size() > maxBagSize) {
            ActionMessage actionMessage =
                new ActionMessage("bag.tooBig", new Integer(maxBagSize));
            recordError(actionMessage, request);

            return mapping.findForward("results");
        }
        int maxNotLoggedSize = WebUtil.getIntSessionProperty(session, "max.bag.size.notloggedin",
                                                             Constants.MAX_NOT_LOGGED_BAG_SIZE);
        try {
            profile.saveBag(bagName, bag, maxNotLoggedSize);
        } catch (InterMineException e) {
            recordError(new ActionMessage(e.getMessage(), String.valueOf(maxNotLoggedSize)),
                        request);
            return mapping.findForward("results");
        }

        recordMessage(new ActionMessage("bag.saved", bagName), request);

        return mapping.findForward("results");
    }
}
