package org.flymine.web;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;


/**
 * Implementation of <strong>Action</strong> that runs a Query
 *
 * @author Andrew Varley
 */

public class QueryAction extends Action
{

    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
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

        // Extract attributes we will need
        MessageResources messages = getResources(request);
        HttpSession session = request.getSession();

        QueryForm queryform = (QueryForm) form;

        Connection con = null;
        try {
            Database db = DatabaseFactory.getDatabase("db.unittest");
            con = db.getConnection();
            Statement stmt = con.createStatement();
            ResultSet res = stmt.executeQuery(queryform.getQuerystring());
            List results = new ArrayList();
            while (res.next()) {
                List row = new ArrayList();
                for (int i = 1; i <= res.getMetaData().getColumnCount(); i++) {
                    row.add(res.getObject(i));
                }
                results.add(row);
            }
            request.setAttribute("results", results);
        } catch (SQLException e) {
            return (mapping.findForward("error"));
        } finally {
            if (con != null) {
                con.close();
            }
        }

        return (mapping.findForward("results"));

    }


}
