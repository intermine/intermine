package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.results.WebTable;
import org.intermine.web.logic.session.SessionMethods;

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
    @Override
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
                                 @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        SaveBagForm crf = (SaveBagForm) form;
        ObjectStoreWriter uosw = ((ProfileManager) servletContext.getAttribute(Constants
                    .PROFILE_MANAGER)).getUserProfileObjectStore();

        String bagName;
        if (request.getParameter("saveNewBag") != null) {
            bagName = crf.getNewBagName();
        } else {
            bagName = crf.getExistingBagName();
        }

        InterMineBag bag = profile.getSavedBags().get(bagName);

        WebTable allRows = pt.getAllRows();
        Set<String> objectTypes = new HashSet<String>();
        if (bag != null) {
            objectTypes.add(bag.getType());
        }
        
        // First pass through, just to check types are compatible.
        for (int i = 0; i < crf.getSelectedObjects().length; i++) {
            String selectedObjectString = crf.getSelectedObjects()[i];
            int indexOfFirstComma = selectedObjectString.indexOf(",");
            String columnIndexString = selectedObjectString.substring(0, indexOfFirstComma);
            int columnIndex = Integer.parseInt(columnIndexString);
            Path columnPath = allRows.getColumns().get(columnIndex).getPath();
            String columnType = columnPath.getLastClassDescriptor().getName();
            objectTypes.add(TypeUtil.unqualifiedName(columnType));
        }

        if (objectTypes.size() > 1) {
            ActionMessage actionMessage = new ActionMessage("bag.moreThanOneType");
            recordError(actionMessage, request);
            return mapping.findForward("results");
        }
        
        ObjectStoreWriter osw = null;
        try {
            if (bag == null) {
                bag = new InterMineBag(bagName, objectTypes.iterator().next(), null, new Date(), os,
                        profile.getUserId(), uosw);
                profile.saveBag(bagName, bag);
            }

            osw = new ObjectStoreWriterInterMineImpl(os);
            // Second pass through, to actually copy the data.
            for (int i = 0; i < crf.getSelectedObjects().length; i++) {
                String selectedObjectString = crf.getSelectedObjects()[i];
                LOG.error("SelectedObjectString: " + selectedObjectString);
                int indexOfFirstComma = selectedObjectString.indexOf(',');
                String columnIndexString = selectedObjectString.substring(0, indexOfFirstComma);
                int columnIndex = Integer.parseInt(columnIndexString);
                Path columnPath = allRows.getColumns().get(columnIndex).getPath();
                int indexOfLastComma = selectedObjectString.lastIndexOf(',');
                if (indexOfFirstComma == indexOfLastComma) {
                    // there's just one comma eg. "1,Gene", so save the whole column
                    LOG.error("Column index: " + columnIndex);
                    if (allRows instanceof WebResults) {
                        Query q = QueryCloner.cloneQuery(((WebResults) allRows)
                                .getInterMineResults().getQuery());
                        QuerySelectable qs = (QuerySelectable) ((WebResults) allRows)
                            .getPathToQueryNode().get(columnPath.toStringNoConstraints());
                        if (qs instanceof QueryField) {
                            qs = (QueryClass) ((QueryField) qs).getFromElement();
                        }
                        q.clearSelect();
                        q.addToSelect(qs);
                        LOG.error("Creating bag from query: " + q);
                        osw.addToBagFromQuery(bag.getOsb(), q);
                        LOG.error("Created bag from query: " + q);
                    } else {
                        if (allRows instanceof WebPathCollection) {
                            List allRowsList = ((WebPathCollection) allRows).getList();
                            if (allRowsList instanceof Results) {
                                osw.addAllToBag(bag.getOsb(), new SingleColumnResults(allRowsList));
                            }
                        } else {
                            throw new ClassCastException("Unhandled table type: "
                                                         + allRows.getClass());
                        }
                    }
                } else {
                    // It's an individual object.
                    int row = Integer.parseInt(selectedObjectString.substring(indexOfFirstComma + 1,
                                indexOfLastComma));
                    ResultElement value = pt.getResultElementRows().get(row).get(columnIndex);
                    osw.addToBag(bag.getOsb(), value.getId());
                }
            }

            recordMessage(new ActionMessage("bag.saved", bagName), request);
            SessionMethods.invalidateBagTable(session, bagName);
        } catch (ObjectStoreException e) {
            LOG.error(e);
            ActionMessage actionMessage =
                new ActionMessage("An error occured while save the bag");
            recordError(actionMessage, request);
            return mapping.findForward("results");
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
            } catch (ObjectStoreException e) {
                // empty
            }
        }
        if (request.getParameter("saveNewBag") != null) {
            return new ForwardParameters(mapping.findForward("bag")).addParameter("bagName",
                bag.getName()).forward();
        } else {
            return mapping.findForward("results");
        }
    }

    /**
     * A class the wraps a list of lists and acts like a list that just contains the elements
     * in the first column.
     * @author Kim Rutherford
     */
    @SuppressWarnings("unchecked")
    private static class SingleColumnResults extends AbstractList
    {
        private List list;

        /** 
         * Create a new SingleColumnResults object
         * @param the list of lists to act on
         */
        public SingleColumnResults(List list) {
            this.list = list;
        }

        /* (non-Javadoc)
         * @see java.util.AbstractList#get(int)
         */
        @Override
        public Object get(int index) {
            Object row = list.get(index);
            if (row instanceof List) {
                Object obj = ((List) row).get(0);
                if (obj instanceof InterMineObject) {
                    return ((InterMineObject) obj).getId();
                } else {
                    return obj;
                }
            } else {
                if (row instanceof InterMineObject) {
                    return ((InterMineObject) row).getId();
                } else {
                    return row;
                }
            }
        }

        /* (non-Javadoc)
         * @see java.util.AbstractCollection#size()
         */
        @Override
        public int size() {
            return list.size();
        }
    }
}
