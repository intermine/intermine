package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.UnauthorizedException;

/**
 * Change a user's password with the authority of a token previously emailed to them.
 * @author Daniela Butano
 *
 */
public class PswResetService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(PswResetService.class);

    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public PswResetService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String token = getRequiredParameter("pswResetToken");
        String newPassword = getRequiredParameter("newPassword");
        try {
            im.getProfileManager().changePasswordWithToken(token, newPassword);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }
}
