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

import java.io.PrintStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;

/**
 * Provide an attribute value as a file
 * @author Mark Woodbridge
 */
public class GetAttributeAsFileAction extends Action
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
        Integer objectId = new Integer(request.getParameter("object"));
        String fieldName = request.getParameter("field");
        InterMineObject object = os.getObjectById(objectId);
        Object fieldValue = TypeUtil.getFieldValue(object, fieldName);
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=" + fieldName + ".txt");
        PrintStream out = new PrintStream(response.getOutputStream());
        out.print(fieldValue);
        out.flush();
        return null;
    }
}
