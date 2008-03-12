package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;

/*
 * TODO   
 * - clean exporthelper
 * - commit to svn
 * - enables set position from which export should start 
 * - solve error handling in export action
 * - solve excel too many results
 * - rewrite  web service for new exporters
 * - what to do if more columns with LocatedSequenceFeature ?
 * - do goFaster?
 */

/**
 * Implementation of <strong>Action</strong> that allows the user to export a 
 * PagedTable to a file.
 *
 * @author Kim Rutherford
 * @author Jakub Kulaviak 
 */
public class TableExportAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(TableExportAction.class);

    //private static final ActionForward forward = null; 
    
    /**
     * Method called to export a PagedTable object.  Uses the type request parameter to choose the
     * export method.
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
        String type = request.getParameter("type");
        try {
            TableExporterFactory factory = new TableExporterFactory(request);
            TableHttpExporter exporter = factory.getExporter(type);
            exporter.export(mapping, form, request, response);
            return null;
        } catch (RuntimeException ex) {
            ActionMessages messages = new ActionMessages();
            ActionMessage error = new ActionMessage("errors.export.exportfailed");
            messages.add(ActionMessages.GLOBAL_MESSAGE, error);
            request.setAttribute(Globals.ERROR_KEY, messages);
            LOG.error("Export failed.", ex);
            return mapping.findForward("error");
        } 
    }
}
