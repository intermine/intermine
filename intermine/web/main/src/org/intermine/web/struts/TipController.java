package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the tips page
 * @author Julie Sullivan
 */

public class TipController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Properties props = SessionMethods.getWebProperties(session.getServletContext());
        String tipCount = props.getProperty("tips.size");

        try {
            Integer i = new Integer(tipCount);
            Random generator = new Random();
            request.setAttribute("randomTip", getRandomTip(generator, i));
        } catch (NumberFormatException e) {
            // do something clever
        }

        request.setAttribute("tipCount", tipCount);
        request.setAttribute("showall", request.getParameter("showall"));

        return null;
    }

    private Integer getRandomTip(Random generator, Integer n) {
        int randomIndex = generator.nextInt(n.intValue());
        // add one because starting at zero is confusing
        randomIndex++;
        return new Integer(randomIndex);
    }
}
