package org.intermine.webservice.server.output;

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
import java.util.Map;

/**
 * Simplest possible formatting. Output rows are just joined with commas.
 * @author Alex Kalderimis
 *
 */
public class PlainFormatter extends Formatter
{
    /**
     * The string that begins an error line.
     */
    public static final String ERROR_INTRO = "[ERROR] ";

    @Override
    public String formatHeader(Map<String, Object> attributes) {
        return "";
    }

    @Override
    public String formatResult(List<String> resultRow) {
        StringBuilder sb = new StringBuilder();
        boolean needsComma = false;
        for (String item: resultRow) {
            if (needsComma) {
                sb.append(",");
            }
            sb.append(item);
            needsComma = true;
        }
        return sb.toString();
    }

    @Override
    public String formatFooter(String errorMessage, int errorCode) {
        StringBuilder sb = new StringBuilder();
        if (errorCode != Output.SC_OK) {
            sb.append(ERROR_INTRO).append(errorCode).append(" " + errorMessage);
        }
        return sb.toString();
    }


}
