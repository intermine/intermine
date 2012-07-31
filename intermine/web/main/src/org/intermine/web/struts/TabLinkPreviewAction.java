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

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.util.HttpClient;
import org.intermine.web.util.URLUtil;


/**
 * Class that renders page with web service results and displays
 * message when there are no results.
 * @author Jakub Kulaviak
 **/
public class TabLinkPreviewAction extends InterMineAction
{
    /**
     * {@inheritDoc}
     * @param mapping not used
     * @param form not used
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter writer = response.getWriter();
        String link = request.getParameter("link");
        if (link != null && !"".equals(link)) {
            String content;
            try {
                String url = prepareURL(link);
                HttpClient client = new HttpClient();
                byte[] data = client.download(url);
                writer.println("<html>");

                if (data.length == 0) {
                    content = "There are no results for this query.<br>"
                            + "Please notice, that this message is displayed only for preview.<br>"
                            + "Empty output is returned in case of downloading data with script.<br>";
                } else {
                    content = new String(data);
                }
                printPage(content, writer);
            } catch (Exception e) {
                e.printStackTrace();
                writer.println("<html>");
                content =
                    "Please examine your template, there might be some invalid characters, e.g. \"%\".";
                printPage(content, writer);
            }
        }
        String content = request.getParameter("content");
        if (content != null && !"".equals(content)) {
            printPage(content, writer);
        }
        return null;
    }

    private String prepareURL(String link) {
        String url = link.replaceAll("qwertyui", "&");
        url = URLUtil.encodeURL(url);
        return url;
    }

    private void printPage(String content, PrintWriter writer) {
        writer.println("<html><head><title>Web service results preview</title></head><body><pre>");
        writer.println(content);
        writer.println("</pre></body></html>");
        writer.flush();
    }
}
