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

import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * This is a generic action to download file from any given directory.
 * Path needs to be setup in controller or jsp.
 *
 * @author Fengyuan Hu
 */
public class FileDownloadAction extends InterMineAction
{
    @Override
    public ActionForward execute(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {

//        String path = "WEB-INF/lib/";
//        String fileName = "intermine-webservice-client.jar";

        try {
            String path = request.getParameter("path");
            String fileName = request.getParameter("fileName");
            String mimeType = request.getParameter("mimeType");
            String mimeExtension = request.getParameter("mimeExtension");

//          String contextPath = getServlet().getServletContext().getRealPath("/");
//          String filePath = contextPath + path + fileName;
//          File file = new File(filePath);
//          FileOutputStream fos = new FileOutputStream(file);

            // Read the file into a input stream
            InputStream is = getServlet().getServletContext().getResourceAsStream(path + fileName);

            // MIME type
            if (fileName.endsWith(mimeExtension)) {
                response.setContentType(mimeType);
                response.setHeader("Content-disposition",
                        "attachment; filename=" + fileName);
            }

            ServletOutputStream sos = response.getOutputStream();

            byte[] buff = new byte[2048];
            int bytesRead;

            while (-1 != (bytesRead = is.read(buff, 0, buff.length))) {
                sos.write(buff, 0, bytesRead);
            }

            is.close();
            sos.close();
        } catch (Exception e) {
            e.printStackTrace();
            recordError(new ActionMessage("api.fileDownloadFailed"), request);
            return mapping.findForward("api");
        }

        return null;
    }

}
