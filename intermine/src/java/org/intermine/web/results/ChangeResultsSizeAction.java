package org.flymine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.struts.actions.LookupDispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.web.Constants;

/**
 * Implementation of <strong>LookupDispatchAction</strong>. Changes the
 * size of the results displayed.
 *
 * @author Andrew Varley
 */
public class ChangeResultsSizeAction extends LookupDispatchAction
{
    /**
     * Change the page size of the DisplayableResults
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward changePageSize(ActionMapping mapping, ActionForm form,
                                        HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();

        DisplayableResults dr = (DisplayableResults) session.getAttribute(Constants.RESULTS_TABLE);
        ChangeResultsForm changeResultsForm = (ChangeResultsForm) form;

        dr.setPageSize(Integer.parseInt(changeResultsForm.getPageSize()));

        // Need to set the start so that we are on the page containing the current start item
        dr.setStart((dr.getStart() / dr.getPageSize()) * dr.getPageSize());

        return mapping.findForward("results");
    }

    /**
     * Save a new bag of objects
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward saveNewBag(ActionMapping mapping, ActionForm form,
                                    HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        ChangeResultsForm changeResultsForm = (ChangeResultsForm) form;

        saveBag(changeResultsForm.getNewBagName(), mapping, form, request, response);
        return mapping.findForward("results");
    }

    /**
     * Add to existing bag of objects
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward addToExistingBag(ActionMapping mapping, ActionForm form,
                                          HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        ChangeResultsForm changeResultsForm = (ChangeResultsForm) form;

        saveBag(changeResultsForm.getBagName(), mapping, form, request, response);
        return mapping.findForward("results");
    }

    /**
     * Save the selected objects to a bag on the session
     *
     * @param bagName the bag to save to
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     *
     * @exception ServletException if a servlet error occurs
     */
    public void saveBag(String bagName, ActionMapping mapping, ActionForm form,
                        HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        ChangeResultsForm changeResultsForm = (ChangeResultsForm) form;

        HttpSession session = request.getSession();

        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
        Map savedBagsInverse =
            (Map) session.getAttribute(Constants.SAVED_BAGS_INVERSE);
        Results results = (Results) session.getAttribute("results");
        String[] selectedObjects = changeResultsForm.getSelectedObjects();

        Collection bag = (Collection) savedBags.get(bagName);

        if (bag == null) {
            bag = new LinkedHashSet();
        }

        // Go through the selected items and add to the set
        Iterator iter = Arrays.asList(selectedObjects).iterator();
        while (iter.hasNext()) {
            String selectedObject = (String) iter.next();
            // selectedObject is of the form column,row - we use those
            // to pick out the object from the underlying results
            int commaIndex = selectedObject.indexOf(",");
            int column = Integer.parseInt(selectedObject.substring(0, commaIndex));
            int row = Integer.parseInt(selectedObject.substring(commaIndex + 1));

            bag.add(((ResultsRow) results.get(row)).get(column));
        }

        // handle the case where queryName already exists - remove it from
        // both maps
        if (savedBags.get(bagName) != null) {
            Collection savedCollection = (Collection) savedBags.get(bagName);
            savedBagsInverse.remove(savedCollection);
            savedBags.remove(bagName);
        }

        // Save the altered bag in the savedBags map
        savedBags.put(bagName, bag);
        savedBagsInverse.put(bag, bagName);
    }

    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("button.change", "changePageSize");
        map.put("bag.new", "saveNewBag");
        map.put("bag.existing", "addToExistingBag");
        return map;
    }


}
