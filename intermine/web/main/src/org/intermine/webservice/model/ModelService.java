package org.intermine.webservice.model;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;
import org.intermine.webservice.WebService;
import org.intermine.webservice.exceptions.InternalErrorException;

/**
 * Web service that returns xml representation of model. 
 * @author Jakub Kulaviak
 */
public class ModelService extends WebService 
{

    /**
     * {@inheritDoc}}
     */
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) {
        Model model = ((ObjectStore) request.getSession().getServletContext().
                getAttribute(Constants.OBJECTSTORE)).getModel();
        try {
            response.getWriter().append(model.toString());
        } catch (IOException e) {
            throw new InternalErrorException(e); 
        }
    }
}