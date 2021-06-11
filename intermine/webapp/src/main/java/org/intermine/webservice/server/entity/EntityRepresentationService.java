package org.intermine.webservice.server.entity;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.Constants;
import org.intermine.webservice.server.core.JSONService;


/**
 *
 * @author Daniela Butano
 *
 */
public class EntityRepresentationService extends JSONService
{

    /**
     * Constructor
     * @param im The InterMine configuration object.
     */
    public EntityRepresentationService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String entity = request.getPathInfo();
        System.out.println (entity);
    }

}
