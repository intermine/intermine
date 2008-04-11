package org.intermine.webservice.template.result;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.template.TemplateQuery;


/**
 * Class that implements generating links of TemplateResultService web service.
 * @author Jakub Kulaviak
 **/
public class TemplateResultLinkGenerator
{

    /**
     * Generates TemplateResultService web service link.
     * @param baseUrl base url that doesn't terminate with '/' , 
     * e.g. http://www.flymine.org/release-12.0
     * @param template template for which the link generate
     * @return generated link
     */
    public String generateServiceLink(String baseUrl, TemplateQuery template) {
        if (template.getBagNames().size() > 0) {
            return "This template contains list(s). The service for this "
                + "special template is not implemented yet.";
        }
        String ret = baseUrl;
        ret += "/data/template/results?name=" + template.getName() + "&";
        List<Constraint> constraints = template.getAllEditableConstraints();
        for (int i = 0; i < constraints.size(); i++) {
            Constraint cs = constraints.get(i);
            int code = i + 1;
            ret += "op" + code + "=" + TemplateResultLinkGenerator.encode(cs.getOp()) + "&";
            ret += "value" + code + "=" + TemplateResultLinkGenerator.encode(cs.getValue()) + "&";
            ret += "extraValue" + code + "=" 
                + TemplateResultLinkGenerator.encode(cs.getExtraValue());
        }
        return ret;
    }

    /**
     *  Encodes object string value to be able to be part of url.
     * @param o encoded object
     * @return encoded string
     */
    private static String encode(Object o) {
        if (o == null) {
            return "";
        } else {
            try {
                return URLEncoder.encode(o.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding string failed", e);
            }            
        }
    }

}
