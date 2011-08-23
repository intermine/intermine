package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.Output;

/**
 * Returns a summary field information.
 *
 * @author Alexis Kalderimis
 */
public class SummaryServlet extends HttpServlet
{

    private static final Logger LOGGER = Logger.getLogger(SummaryServlet.class);

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        runService(request, response);
    }

    private void runService(HttpServletRequest request, HttpServletResponse response) {
        InterMineAPI im = SessionMethods.getInterMineAPI(request);
        SummaryService sum = new SummaryService(im);
        sum.service(request, response);
    }
        
}
