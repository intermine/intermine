package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.webservice.server.output.XMLFormatter;

public class UserDataFormatter extends XMLFormatter {

    protected String getRootElement() {
        return "Deregistration";
    }

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
