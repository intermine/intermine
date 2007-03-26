package org.intermine.web.results;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for externalLinks.jsp
 * @author Kim Rutherford
 */
public class ExternalLinksController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(ExternalLinksController.class);

    private static final String PROPERTY_PREFIX = "externalLink";
    private static final Object URL_TAG = "urlPrefix";
    
    /**
     * Create a Map of external link type to URL by looking at the current object and by reading
     * the WEB_PROPERTIES.
     * @param context The current Tile context, containing Tile attributes.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Model model = ((ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE)).getModel();
        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        Map linkPrefixes = new LinkedHashMap();
        Enumeration enumeration = properties.keys();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            String urlPrefix = properties.getProperty(key);
            
            String[] bits = StringUtil.split(key, ".");
            
            if (bits[0].equals(PROPERTY_PREFIX)) {
                String linkType = bits[1];
                String className = bits[2];
                String fullClassName = model.getPackageName() + "." + className;
                String fieldName = bits[3];

                String propertyType = bits[4];
                if (!propertyType.equals(URL_TAG)) {
                    LOG.error("unknown property format: " + key + " no \"" + URL_TAG + "\" found");
                    continue;
                }

                DisplayObject displayObject = (DisplayObject) context.getAttribute("object");
                InterMineObject o = displayObject.getObject();
                ClassDescriptor cd = getMatchedType(model, o, fullClassName);
                if (cd == null) {
                    LOG.error("unknown class \"" + className + "\" in property: " + key);
                    continue;
                }
                FieldDescriptor fd = cd.getFieldDescriptorByName(fieldName);
                
                if (fd == null) {
                    LOG.error("unknown field \"" + fieldName + "\" in class \"" + className 
                              + "\" in property: " + key);
                    continue;
                }
                
                Object fieldValue = TypeUtil.getFieldValue(o, fieldName);
                
                String link = urlPrefix + fieldValue;
                
                linkPrefixes.put(linkType, link);
            }
        }
        
        request.setAttribute("externalLinkPrefixes", linkPrefixes);

        return null;
    }
    
    private static ClassDescriptor getMatchedType(Model model, InterMineObject o,
                                                  String className) {
        Set classDescriptors = model.getClassDescriptorsForClass(o.getClass());
        for (Iterator iter = classDescriptors.iterator(); iter.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            if (cld.getName().equals(className)) {
                return cld;
            }
        }
        return null;
    }


}
