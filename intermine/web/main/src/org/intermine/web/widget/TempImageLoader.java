package org.intermine.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.InterMineException;
import org.intermine.web.InterMineAction;

/**
 * @author Xavier Watkins
 *
 */
public class TempImageLoader extends InterMineAction
{
    /**
     * Action to load an image from a file and send it as a binary
     * through the HttpServletResponse
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
        OutputStream out = response.getOutputStream();
        try {
            String fileName = (String) request.getParameter("fileName");
            File file = new File(System.getProperty("java.io.tmpdir"), fileName);
            if (!file.exists()) {
                        throw new InterMineException("File '" + file.getAbsolutePath() 
                                 + "' does not exist");
            }

            FileInputStream is = new FileInputStream(file);
//          Create the byte array to hold the data
            byte[] bytes = new byte[(int) file.length()];

//             Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                   && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            response.setContentType("image/png");
            out.write(bytes);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
