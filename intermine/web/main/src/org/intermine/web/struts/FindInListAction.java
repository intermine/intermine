package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to search a list for an identifier and then highlight it on the list details page.
 * @author Kim Rutherford
 */
public class FindInListAction extends InterMineAction
{
    /**
     * Method called when user has submitted the search form.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        FindInListForm qsf = (FindInListForm) form;
        String textToFind = qsf.getTextToFind().trim();
        String bagName = qsf.getBagName();
        Profile profile = SessionMethods.getProfile(session);
        BagManager bagManager = im.getBagManager();
        InterMineBag bag = bagManager.getBag(profile, bagName);

        ForwardParameters forwardParameters =
            new ForwardParameters(mapping.findForward("bagDetails"));
        forwardParameters.addParameter("name", bagName);

        if (bag != null) {
            Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
            ObjectStore os = im.getObjectStore();

            String bagQualifiedType = bag.getQualifiedType();
            Collection<String> keyFields =
                ClassKeyHelper.getKeyFieldNames(classKeys, bagQualifiedType);
            int foundId = -1;
            if (keyFields.size() > 0) {
                Query q = makeQuery(textToFind, bag, keyFields, os.getModel());
                foundId = findFirst(os, q);
            }
            if (foundId == -1) {
                // no class key fields match so try all keys
                List<String> allStringFields = getStringFields(os.getModel(), bagQualifiedType);
                Query q = makeQuery(textToFind, bag, allStringFields, os.getModel());
                foundId = findFirst(os, q);
            }

            if (foundId == -1) {
                SessionMethods.recordMessage("Cannot find \"" + textToFind + "\" in " + bagName,
                                             session);
                // radek: so we can apply a style based on failed search results
                forwardParameters.addParameter("foundItem", "false");
            } else {
                forwardParameters.addParameter("highlightId", foundId + "");
                forwardParameters.addParameter("gotoHighlighted", "true");
            }
        }

        return forwardParameters.forward();
    }

    private List<String> getStringFields(Model model, String bagQualifiedType) {
        List<String> retList = new ArrayList<String>();
        ClassDescriptor cd = model.getClassDescriptorByName(bagQualifiedType);
        for (AttributeDescriptor ad: cd.getAllAttributeDescriptors()) {
            if (ad.getType().equals(String.class.getName())) {
                retList.add(ad.getName());
            }
        }
        return retList;
    }

    private Query makeQuery(String searchTerm, InterMineBag bag,
                            Collection<String> identifierFieldNames, Model model) {
        String bagClassName = bag.getQualifiedType();

        ClassDescriptor cd = model.getClassDescriptorByName(bagClassName);

        Query q = new Query();
        QueryClass qc;
        try {
            qc = new QueryClass(Class.forName(bagClassName));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class not found", e);
        }
        QueryField idQF = new QueryField(qc, "id");
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.addFrom(qc);
        q.addToSelect(idQF);

        BagConstraint bagConstraint = new BagConstraint(idQF, ConstraintOp.IN, bag.getOsb());
        cs.addConstraint(bagConstraint);

        ConstraintSet fieldCS = new ConstraintSet(ConstraintOp.OR);

        for (String fieldName: identifierFieldNames) {
            QueryField qf = new QueryField(qc, fieldName);
//     For case insensitive
//            QueryExpression lowerQF = new QueryExpression(QueryExpression.LOWER, qf);
//            SimpleConstraint sc =
//              new SimpleConstraint(lowerQF, ConstraintOp.EQUALS, new QueryValue(lowerSearchTerm));

            QueryValue queryValue;

            AttributeDescriptor attDesc = cd.getAttributeDescriptorByName(fieldName, true);

            String attType = attDesc.getType();

            if ("java.lang.Integer".equals(attType)) {
                try {
                    Integer intSearchTerm = Integer.valueOf(searchTerm);
                    queryValue = new QueryValue(intSearchTerm);
                } catch (NumberFormatException e) {
                    // not a number so don't constrain this field
                    continue;
                }
            } else {
                if ("java.lang.Long".equals(attType)) {

                    try {
                        Long longSearchTerm = Long.valueOf(searchTerm);
                        queryValue = new QueryValue(longSearchTerm);
                    } catch (NumberFormatException e) {
                        // not a number so don't constrain this field
                        continue;
                    }
                } else {
                    if ("java.lang.String".equals(attType)) {
                        queryValue = new QueryValue(searchTerm);
                    } else {
                        continue;
                    }
                }
            }

            SimpleConstraint sc =
                new SimpleConstraint(qf, ConstraintOp.EQUALS, queryValue);
            fieldCS.addConstraint(sc);
        }

        cs.addConstraint(fieldCS);

        q.setConstraint(cs);

        return q;
    }

    /**
     * Return the id of the first object in the output, or -1 if there aren't any rows.
     */
    @SuppressWarnings("rawtypes")
    private int findFirst(ObjectStore os, Query q) {
        Results res = os.execute(q);
        try {
            return ((Integer) ((ResultsRow) res.get(0)).get(0)).intValue();
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }
    }
}
