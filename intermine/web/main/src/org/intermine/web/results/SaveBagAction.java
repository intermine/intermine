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

import org.intermine.InterMineException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
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

        String bagName;
        if (request.getParameter("saveNewBag") != null) {
            bagName = crf.getNewBagName();
        } else {
            bagName = crf.getExistingBagName();
        }

        // TODO make more generic when type thing is ready by creating an InterMineIdBag
        InterMineBag bag = new InterMineBag(profile.getUserId(), bagName, null, userProfileOs, os,
                                                     Collections.EMPTY_SET);

        WebColumnTable allRows = pt.getAllRows();
        ArrayList objectTypes = new ArrayList();
        
        // the number of complete columns to save in the bag
        int wholeColumnsToSave = 0;

        for (int i = 0; i < crf.getSelectedObjects().length; i++) {
            String selectedObjectString = crf.getSelectedObjects()[i];
            int indexOfFirstComma = selectedObjectString.indexOf(",");
            if (indexOfFirstComma == selectedObjectString.lastIndexOf(",")) {
                // there's just one comma eg. "1,Gene"
                wholeColumnsToSave++;
                String columnIndexString = selectedObjectString.substring(0, indexOfFirstComma);
                int columnIndex = Integer.parseInt(columnIndexString);
                Path columnPath = ((Column) allRows.getColumns().get(columnIndex)).getPath();
                String columnType = columnPath.getLastClassDescriptor().getName();
                objectTypes.add(TypeUtil.unqualifiedName(columnType));
            }
        }

        if (allRows instanceof WebResults) {
            try {
                ((WebResults) allRows).goFaster();
            } catch (ObjectStoreException e) {
                // ignore and try to save slowly
            }
        }
        
        try {
            int rowCount = allRows.size();

            try {
                /*
                 * don't do for now because if the column contains duplicates then
                 * wholeColumnsToSave * rowCount doesn't tell us the true final bag size 
                 * 
                ActionForward forward =
                   checkBagSize(mapping, request, wholeColumnsToSave * rowCount);
                
                if (forward != null) {
                    return forward;
                }
                 */
                
                // make sure we can get the first batch - catch any RunTimeExceptions and report
                // them immediately
                try {
                    allRows.get(0);
                } catch (IndexOutOfBoundsException e) {
                    // Ignore - that means there are NO rows in this results object.
                }
            } catch (RuntimeException e) {
                if (e.getCause() instanceof ObjectStoreException) {
                    recordError(new ActionMessage("errors.query.objectstoreerror"),
                                request, (ObjectStoreException) e.getCause(), LOG);
                    return mapping.findForward("results");
                }
                throw e;
            }

            // as we add objects from the selected column to the bag, add the indexes
            // (ie. "2,3" - row 2, column 3) of the objects so we know not to add them twice
            Set seenObjects = new HashSet();

            // save selected columns first
            if (wholeColumnsToSave > 0) {
                for (int i = 0; i < rowCount; i++) {
                    List thisRow = allRows.getResultElements(i);
                    List rowToSave = new ArrayList();

                    // go through the selected items (checkboxes) and add to the bag-to-save
                    for (Iterator itemIterator = Arrays.asList(crf.getSelectedObjects()).iterator();
                         itemIterator.hasNext();) {
                        String selectedObjectID = (String) itemIterator.next();

                        // renove the type information
                        selectedObjectID = 
                            selectedObjectID.substring(0, selectedObjectID.lastIndexOf(","));
                        // selectedObject is now of the form "column,row" or "column"
                        int commaIndex = selectedObjectID.indexOf(",");
                        if (commaIndex == -1) {
                            int column = Integer.parseInt(selectedObjectID);

                            if (column < thisRow.size()) {
                                ResultElement resCell = (ResultElement) thisRow.get(column);
                                BagElement bagElement = new BagElement(resCell);
                                rowToSave.add(bagElement);
                                seenObjects.add(i + "," + column);
                            }
                        }
                    }
                    bag.addAll(rowToSave);

                    ActionForward forward = checkBagSize(mapping, request, bag.size());
                    
                    if (forward != null) {
                        return forward;
                    }
                }
            }

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

                    ActionForward forward = checkBagSize(mapping, request, bag.size());
                    
                    if (forward != null) {
                        return forward;
                    }
                }
            }

            if (objectTypes.size() > 1) {
                ActionMessage actionMessage = new ActionMessage("bag.moreThanOneType");
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
            
            ActionForward forward = checkBagSize(mapping, request, bag.size());
            if (forward != null) {
                return forward;
            }
            profile.saveBag(bagName, bag); 
            recordMessage(new ActionMessage("bag.saved", bagName), request);

        } catch (InterMineException e) {
            ActionMessage actionMessage =
                new ActionMessage("An error occured while save the bag");
            recordError(actionMessage, request);
            return mapping.findForward("results");
        } finally {
            if (allRows instanceof WebResults) {
                try {
                    ((WebResults) allRows).releaseGoFaster();
                } catch (ObjectStoreException e) {
                    // ignore
                }
            }
        }

        return mapping.findForward("results");
    }

    private ActionForward checkBagSize(ActionMapping mapping, HttpServletRequest request, 
                                       int bagSize) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        int defaultMax = 10000;
        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", defaultMax);

        if (bagSize > maxBagSize) {
            ActionMessage actionMessage =
                new ActionMessage("bag.tooBig", new Integer(maxBagSize));
            recordError(actionMessage, request);

            return mapping.findForward("results");
        }
        int maxNotLoggedSize = 
            WebUtil.getIntSessionProperty(session, "max.bag.size.notloggedin",
                                          Constants.MAX_NOT_LOGGED_BAG_SIZE);
        if (profile.getUsername() == null
            && bagSize > maxNotLoggedSize) {
            recordError(new ActionMessage("bag.bigNotLoggedIn", String.valueOf(maxNotLoggedSize)),
                        request);
            return mapping.findForward("results");
        }
        return null;
    }
}
