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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller Action for buildBag.jsp
 *
 * @author Kim Rutherford
 */

public class BagController extends TilesAction
{
    /**
     * Set up environment for the buildBag page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Model model = ((ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE)).getModel();
        ProfileManager pm = SessionMethods.getProfileManager(servletContext);


        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        ObjectStoreSummary oss =
            (ObjectStoreSummary) servletContext.getAttribute(Constants.OBJECT_STORE_SUMMARY);
        Collection qualifiedTypes = os.getModel().getClassNames();

        ArrayList typeList = new ArrayList();
        ArrayList preferedTypeList = new ArrayList();
        String superUserName = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);

        List preferredBagTypeTags = pm.getTags("im:preferredBagType", null, "class", superUserName);
        for (Iterator iter = preferredBagTypeTags.iterator(); iter.hasNext();) {
            Tag tag = (Tag) iter.next();
            preferedTypeList.add(TypeUtil.unqualifiedName(tag.getObjectIdentifier()));
        }
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        for (Iterator iter = qualifiedTypes.iterator(); iter.hasNext();) {
            String className = (String) iter.next();
            String unqualifiedName = TypeUtil.unqualifiedName(className);
            if (ClassKeyHelper.hasKeyFields(classKeys, unqualifiedName)
                && oss.getClassCount(className) > 0
                && !preferedTypeList.contains(unqualifiedName)) {
                typeList.add(unqualifiedName);
            }
        }
        Collections.sort(preferedTypeList);
        Collections.sort(typeList);
        request.setAttribute("typeList", typeList);
        request.setAttribute("preferredTypeList", preferedTypeList);

        BagQueryConfig bagQueryConfig =
            (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG);
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();
        if (extraClassName != null) {
            request.setAttribute("extraBagQueryClass", TypeUtil.unqualifiedName(extraClassName));

            List extraClassFieldValues =
                getFieldValues(os, oss, extraClassName, bagQueryConfig.getConstrainField());
            request.setAttribute("extraClassFieldValues", extraClassFieldValues);

            // find the types in typeList that contain a field with the name given by
            // bagQueryConfig.getConnectField()
            List typesWithConnectingField = new ArrayList();
            Iterator allTypesIterator =
                new IteratorChain(typeList.iterator(), preferedTypeList.iterator());
            while (allTypesIterator.hasNext()) {
                String connectFieldName = bagQueryConfig.getConnectField();
                String typeName = (String) allTypesIterator.next();
                String qualifiedTypeName = model.getPackageName() + "." + typeName;
                ClassDescriptor cd = model.getClassDescriptorByName(qualifiedTypeName);
                FieldDescriptor fd = cd.getFieldDescriptorByName(connectFieldName);
                if (fd != null && fd instanceof ReferenceDescriptor) {
                    typesWithConnectingField.add(typeName);
                }
            }
            request.setAttribute("typesWithConnectingField", typesWithConnectingField);
        }

        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        request.setAttribute("ageClasses", getBagAgeClasses(profile.getSavedBags()));

        return null;
    }

    private List getFieldValues(ObjectStore os, ObjectStoreSummary oss, String extraClassName,
                                String constrainField) {
        List fieldValues = oss.getFieldValues(extraClassName, constrainField);
        if (fieldValues == null) {
            Query q = new Query();
            q.setDistinct(true);
            QueryClass qc;
            try {
                qc = new QueryClass(Class.forName(extraClassName));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Can't find class for: " + extraClassName);
            }
            q.addToSelect(new QueryField(qc, constrainField));
            q.addFrom(qc);
            Results results = os.execute(q);
            fieldValues = new ArrayList();
            for (Iterator j = results.iterator(); j.hasNext();) {
                Object fieldValue = ((ResultsRow) j.next()).get(0);
                fieldValues.add(fieldValue == null ? null : fieldValue.toString());
            }
        }

        return fieldValues;
    }
    
    /**
     * Return a Map from bag name to css class to use on the div/tr/td displaying the bag.
     * @param bags a Map from bag name to InterMineBag
     * @return a Map from query name to CSS class
     */
    static Map<String, String> getBagAgeClasses(Map<String, InterMineBag> bags) {
        Map<String, String> ageClassMap = new HashMap<String, String>();
        for (Map.Entry<String, InterMineBag> entry: bags.entrySet()) {
            String bagName = entry.getKey();
            ageClassMap.put(bagName, 
                            MyMineController.getCSSClassForAge(entry.getValue().getDateCreated()));
        }
        return ageClassMap;
    }
}
