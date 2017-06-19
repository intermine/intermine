package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.webservice.server.output.XMLFormatter;

/**
 * A class that processes information about a user into XML. Used when
 * exporting a profile following deregistration.
 * @author Alex Kalderimis
 *
 */
public class UserDataFormatter extends XMLFormatter
{

    @Override
    protected String getRootElement() {
        return "Deregistration";
    }

    @Override
    protected String getRowElement() {
        return "UserData";
    }

    @Override
    public String formatResult(List<String> userData) {
        StringBuilder sb = new StringBuilder();
        String elem = getRowElement();
        sb.append("<" + elem + ">");
        pushTag(elem);

        for (String s : userData) {
            sb.append(s); // Only called once, we can append directly.
        }

        sb.append("</" + elem + ">");
        popTag();
        return sb.toString();
    }

}
