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

import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.model.userprofile.Tag;
import org.intermine.util.XmlUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.tagging.TagBinding;

/**
 * Export tags.
 * 
 * @author Thomas Riley
 */
public class ExportTagsAction extends InterMineAction
{

    /**
     * Export user's tags.
     * 
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
        
        writer.writeStartElement("tags");
        List tags = profile.getProfileManager().getTags(null, null, null,
                                                        profile.getUsername());
        for (Iterator i = tags.iterator(); i.hasNext();) {
            Tag tag = (Tag) i.next();
            TagBinding.marshal(tag, writer);
        }
        writer.writeEndElement();
        writer.close();
        
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=tags.xml");
        response.getWriter().print(XmlUtil.indentXmlSimple(sw.getBuffer().toString()));
        return null;
    }
    
}
