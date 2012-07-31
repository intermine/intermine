package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.Mine;
import org.intermine.api.profile.TagManager;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;


/**
 * Controller Action for buildBag.jsp
 *
 * @author Kim Rutherford
 */

public class BagBuildController extends TilesAction
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
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();

        ObjectStore os = im.getObjectStore();
        ObjectStoreSummary oss = im.getObjectStoreSummary();

        Collection<String> qualifiedTypes = model.getClassNames();

        ArrayList<String> typeList = new ArrayList();
        ArrayList<String> preferedTypeList = new ArrayList();

        TagManager tagManager = im.getTagManager();
        List<Tag> preferredBagTypeTags = tagManager.getTags("im:preferredBagType", null, "class",
                                                            im.getProfileManager().getSuperuser());
        for (Tag tag : preferredBagTypeTags) {
            preferedTypeList.add(TypeUtil.unqualifiedName(tag.getObjectIdentifier()));
        }
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        for (Iterator<String> iter = qualifiedTypes.iterator(); iter.hasNext();) {
            String className = iter.next();
            String unqualifiedName = TypeUtil.unqualifiedName(className);
            if (ClassKeyHelper.hasKeyFields(classKeys, unqualifiedName)
                && oss.getClassCount(className) > 0) {
                typeList.add(unqualifiedName);
            }
        }
        Collections.sort(preferedTypeList);
        Collections.sort(typeList);
        request.setAttribute("typeList", typeList);
        request.setAttribute("preferredTypeList", preferedTypeList);

        BagQueryConfig bagQueryConfig = im.getBagQueryConfig();
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();
        if (extraClassName != null) {
            request.setAttribute("extraBagQueryClass", TypeUtil.unqualifiedName(extraClassName));

            List extraClassFieldValues =
                getFieldValues(os, oss, extraClassName, bagQueryConfig.getConstrainField());
            request.setAttribute("extraClassFieldValues", extraClassFieldValues);

            // find the types in typeList that contain a field with the name given by
            // bagQueryConfig.getConnectField()
            List<String> typesWithConnectingField = new ArrayList<String>();
            Iterator<String> allTypesIterator =
                new IteratorChain(typeList.iterator(), preferedTypeList.iterator());
            while (allTypesIterator.hasNext()) {
                String connectFieldName = bagQueryConfig.getConnectField();
                String typeName = allTypesIterator.next();
                String qualifiedTypeName = model.getPackageName() + "." + typeName;
                ClassDescriptor cd = model.getClassDescriptorByName(qualifiedTypeName);
                FieldDescriptor fd = cd.getFieldDescriptorByName(connectFieldName);
                if (fd != null && fd instanceof ReferenceDescriptor) {
                    typesWithConnectingField.add(typeName);
                }
            }
            request.setAttribute("typesWithConnectingField", typesWithConnectingField);
            final String defaultValue = getDefaultValue(request, im);
            if (StringUtils.isNotEmpty(defaultValue)) {
                BuildBagForm bbf = (BuildBagForm) form;
                bbf.setExtraFieldValue(defaultValue);
            }
        }

        // get example bag values
        String bagExampleIdentifiersPropertiesKey = "bag.example.identifiers";
        ServletContext servletContext = session.getServletContext();
        Properties properties = SessionMethods.getWebProperties(servletContext);
        Properties bagExampleIdentifiers = PropertiesUtil.getPropertiesStartingWith(
                bagExampleIdentifiersPropertiesKey, properties);
        if (bagExampleIdentifiers.size() != 0) {
            Map<String, String> bagExampleIdentifiersMap = new HashMap<String, String>();
            Enumeration<?> e = bagExampleIdentifiers.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String value = bagExampleIdentifiers.getProperty(key);
                if (key.equals(bagExampleIdentifiersPropertiesKey)) {
                    bagExampleIdentifiersMap.put("default", value);
                } else {
                    bagExampleIdentifiersMap.put(key.replace(bagExampleIdentifiersPropertiesKey
                            + ".", ""), value);
                }
                bagExampleIdentifiers.getProperty(key);
            }
            request.setAttribute("bagExampleIdentifiers", bagExampleIdentifiersMap);
        }
        return null;
    }

    /**
     * Return a list of the possible field values for the given class/field name combination.
     * @param os the ObjectStore to query if the field values aren't available from the summary
     * @param oss the summary of the object store
     * @param extraClassName the class name
     * @param constrainField the field name
     * @return a List of the feild values
     */
    public static List<Object> getFieldValues(ObjectStore os, ObjectStoreSummary oss,
                                              String extraClassName, String constrainField) {
        List<Object> fieldValues = oss.getFieldValues(extraClassName, constrainField);
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
            fieldValues = new ArrayList<Object>();
            for (Iterator j = results.iterator(); j.hasNext();) {
                Object fieldValue = ((ResultsRow) j.next()).get(0);
                fieldValues.add(fieldValue == null ? null : fieldValue.toString());
            }
        }

        return fieldValues;
    }

    private String getDefaultValue(HttpServletRequest request, InterMineAPI im) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        final Properties webProperties = SessionMethods.getWebProperties(servletContext);
        final FriendlyMineManager linkManager = FriendlyMineManager.getInstance(im, webProperties);
        Mine mine = linkManager.getLocalMine();
        if (mine != null) {
            return mine.getDefaultValue();
        }
        return null;
    }

}
