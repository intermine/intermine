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

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.web.Constants;
import org.intermine.web.InterMineAction;
import org.intermine.web.SessionMethods;
import org.intermine.web.config.WebConfig;

/**
 * Implementation of <strong>Action</strong> that assembles data for viewing an object.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class ObjectDetailsController extends InterMineAction
{
    /**
     * @see Action#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Map displayObjects = SessionMethods.getDisplayObjects(session);

        String idString = (String) request.getParameter("id");
        if (idString != null && !idString.equals("")
            && (session.getAttribute("object") == null
                || ((DisplayObject) session.getAttribute("object")).getId()
                != Integer.parseInt(idString))) {
            // Move to a different object
            Integer key;
            try {
                key = new Integer(idString);
            } catch (NumberFormatException e) {
                session.removeAttribute("object");
                return null;
            }
            InterMineObject object = os.getObjectById(key);
            if (object == null) {
                // no such object
                session.removeAttribute("object");
                return null;
            }
            String field = request.getParameter("field");
            if (field != null) {
                object = (InterMineObject) TypeUtil.getFieldValue(object, field);
            }
            DisplayObject dobj = (DisplayObject) displayObjects.get(key);
            if (dobj == null) {
                dobj = makeDisplayObject(session, object);
                displayObjects.put(key, dobj);
            }
            request.setAttribute("object", dobj);

            if (session.getAttribute(Constants.PORTAL_QUERY_FLAG) != null) {
                session.removeAttribute(Constants.PORTAL_QUERY_FLAG);
                setVerboseCollections(session, dobj);
            }

       }

        return null;
    }

    /**
     * The prefix to use before properties the specify which collection fields are open when coming
     * from the portal page.  eg. portal.verbose.fields.Gene = proteins,chromosome
     */
    public static final String PORTAL_VERBOSE_FIELDS_PREFIX = "portal.verbose.fields.";

    /**
     * Read the port.verbose.fields.* properties from WEB_PROPERTIES and call
     * DisplayObject.setVerbosity(true) on the field in the property value.
     */
    private static void setVerboseCollections(HttpSession session, DisplayObject dobj) {
        ServletContext servletContext = session.getServletContext();
        Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);

        Set clds = dobj.getClds();

        Iterator iter = clds.iterator();

        while (iter.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) iter.next();

            String propName = PORTAL_VERBOSE_FIELDS_PREFIX + TypeUtil.unqualifiedName(cd.getName());
            String fieldNamesString = (String) webProperties.get(propName);

            if (fieldNamesString != null) {
                String[] fieldNames = fieldNamesString.split("\\s*,\\s*");
                for (int i = 0; i < fieldNames.length; i++) {
                    String fieldName = fieldNames[i];
                    dobj.setVerbosity(fieldName, true);
                }
            }
        }
    }

    /**
     * Make a new DisplayObject from the given object.
     * @param session used to get WEB_PROPERTIES and DISPLAYERS Maps
     * @param object the InterMineObject
     * @return the new DisplayObject
     * @throws Exception if an error occurs
     */
    public static DisplayObject makeDisplayObject(HttpSession session, InterMineObject object)
        throws Exception {
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Map webPropertiesMap = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        return new DisplayObject(object, os.getModel(), webConfig, webPropertiesMap);
    }
}
