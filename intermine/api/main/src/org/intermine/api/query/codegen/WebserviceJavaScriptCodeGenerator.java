package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringEscapeUtils.escapeJavaScript;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.util.TypeUtil;

/**
 * A Class for generating JavaScript that would run a given query.
 * @author Alexis Kalderimis
 *
 */
public class WebserviceJavaScriptCodeGenerator implements WebserviceCodeGenerator
{

    protected static final String INVALID_QUERY = "Invalid query. No fields selected for output.";
    protected static final String NULL_QUERY    = "Invalid query. Query can not be null.";

    protected static final String INDENT        = "    ";
    protected static final String INDENT2       = INDENT + INDENT;
    protected static final String SPACE         = " ";
    protected static final String ENDL          = System.getProperty("line.separator");

    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
                                                + "constraint, which is currently not supported.";
    protected static final String LOOP_CONSTRAINT         = "Loop path constraint is not supported "
                                                              + "at the moment...";
    protected static final String SCRIPT_IMPORTS          =
          "<!-- You need to import the IMBedding client library - this is hosted at intermine.org for your convenience: -->" + ENDL
          + "<script src=\"http://www.intermine.org/lib/imbedding/0.2/imbedding.js\" type=\"text/javascript\"></script>" + ENDL + ENDL
          + "<!-- We also need to import a stylesheet - you can choose from light, dark or bold-->" + ENDL
          + "<link rel=\"stylesheet\" type=\"text/css\" title=\"light\" href=\"http://intermine.org/lib/imbedding/0.2/style/light.css\">" + ENDL
          + ENDL;

    protected static final String PRELUDE =
        "<html>" + ENDL
        + "<!-- This is an automatically generated code snippet to run your query" + ENDL
        + " using the intermine JavaScript client library. It is assumed that you" + ENDL
        + " will be wanting to run this query from a webpage, and so the code is" + ENDL
        + " formatted such that you can just cut and paste it into any webpage -->" + ENDL + ENDL;

    protected static final String PLACEHOLDER =
        "<!-- You need to set a place holder element in your page to hold the resultant table - this can also hold apology text/content -->" + ENDL
        + "<div id=\"queryplaceholder\"><p class=\"apology\">Please be patient while the results of your query are retrieved.</p></div>" + ENDL + ENDL;

    protected static final String BOILERPLATE = "<script type=\"text/javascript\">" + ENDL;
    protected static final String QUERY_METHOD = "IMBedding.loadQuery(query, ";
    protected static final String TEMPLATE_METHOD = "IMBedding.loadTemplate(";

    private static int counter = 0;

    private String getImports(WebserviceCodeGenInfo wsCodeGenInfo) {
        StringBuffer sb = new StringBuffer();
        
        sb.append("<link href=\"http://cdn.intermine.org/css/bootstrap/2.0.3-prefixed/css/bootstrap.min.css\" rel=\"stylesheet\">");
        sb.append("<script src=\"http://cdn.intermine.org/js/jquery/1.7/jquery.min.js\"></scr"+"ipt>");
        sb.append("<script src=\"http://cdn.intermine.org/js/underscore.js/1.3.3/underscore-min.js\"></scr"+"ipt>");
        sb.append("<script src=\"http://cdn.intermine.org/js/backbone.js/0.9.2/backbone-min.js\"></scr"+"ipt>");
        sb.append("<script src=\"http://cdn.intermine.org/js/intermine/imjs/latest/imjs.js\"></scr"+"ipt>");
        sb.append("<script src=\"http://cdn.intermine.org/js/intermine/im-tables/latest/deps.js\"></scr"+"ipt>");
        sb.append("<script src=\"http://cdn.intermine.org/js/intermine/im-tables/latest/imtables.js\"></scr"+"ipt>");
        sb.append("<link href=\"http://cdn.intermine.org/js/intermine/im-tables/latest/tables.css\" rel=\"stylesheet\">");
        sb.append("<link href=\"http://cdn.intermine.org/css/jquery-ui/1.8.19/jquery-ui-1.8.19.custom.css\" rel=\"stylesheet\">");
        sb.append("<link href=\"http://cdn.intermine.org/css/google-code-prettify/latest/prettify.css\" rel=\"stylesheet\">");

        return sb.toString();
    }

    /**
     * This method will generate code that will run using the python webservice
     * client library.
     *
     * @param wsCodeGenInfo a WebserviceCodeGenInfo object
     * @return the code as a string
     */
    @Override
    public String generate(WebserviceCodeGenInfo wsCodeGenInfo) {

        PathQuery query = wsCodeGenInfo.getQuery();
        String serviceBaseURL = wsCodeGenInfo.getServiceBaseURL();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }
        if (query.getView().isEmpty()) {
            return INVALID_QUERY;
        }

        String title = "query";
        if (query.getTitle() != null) {
            title = query.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }

        StringBuffer sb = new StringBuffer().append(PRELUDE);
        sb.append("<head>" + ENDL);
        sb.append(getImports(wsCodeGenInfo));
        sb.append("<script type=\"text/javascript\">\n");
        sb.append("(function($) {\n");
        sb.append("    var query = " + query.getJson() + ";\n");
        sb.append("    var service = new intermine.Service({\n");
        sb.append("        root: \"" + wsCodeGenInfo.getBaseUrl() + "\",\n");
        sb.append("     // token: \"YOUR-TOKEN-HERE\"\n");
        sb.append("    });\n");
        sb.append("\n");
        sb.append("    $(function() {\n");
        sb.append("      var view = new intermine.query.results.CompactView(service, query);\n");
        sb.append("      view.$el.appendTo('#" + title + "-table-container-" + counter + "');\n");
        sb.append("      view.render();\n");
        sb.append("    });\n");
        sb.append("})(jQuery);\n");
        sb.append("</script>\n");
        sb.append("</head>" + ENDL);
        sb.append("<body>" + ENDL);
        sb.append("  <div id=\"" + title + "-table-container-" + counter + "\"></div>\n");
        sb.append("</body>" + ENDL);
        sb.append("</html>");

        counter++;

        return sb.toString();
                                            
    }
}
