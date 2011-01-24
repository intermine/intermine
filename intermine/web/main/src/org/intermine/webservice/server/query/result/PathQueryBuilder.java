package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.profile.InterMineBag;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.exceptions.BadRequestException;


/**
 * PathQueryBuilder builds PathQuery object from xml and validates it.
 *
 * @author Jakub Kulaviak
 **/
public class PathQueryBuilder
{

    private PathQuery pathQuery;

    protected PathQueryBuilder() {
    	// empty constructor for testing
    }

    /**
     * PathQueryBuilder constructor.
     * @param xml xml string from which will be PathQuery constructed
     * @param schemaUrl url of XML Schema file, validation is performed according this file
     * @param savedBags previously saved bags
     */
    public PathQueryBuilder(String xml, String schemaUrl, Map<String, InterMineBag> savedBags) {
        buildQuery(xml, schemaUrl, savedBags);
    }

    protected void buildQuery(String xml, String schemaUrl,
            Map<String, InterMineBag> savedBags) {
        XMLValidator validator = new XMLValidator();
        validator.validate(xml, schemaUrl);
        if (validator.getErrorsAndWarnings().size() == 0) {
            pathQuery = PathQueryBinding.unmarshalPathQuery(new StringReader(xml),
                    PathQuery.USERPROFILE_VERSION);

            if (!pathQuery.isValid()) {
                throw new BadRequestException("XML is well formatted but query contains errors: "
                        + formatMessage(pathQuery.verifyQuery()));
            }

            // check bags used by this query exist
            Set<String> missingBags = new HashSet<String>();
            for (String bagName : pathQuery.getBagNames()) {
                if (!savedBags.containsKey(bagName)) {
                    missingBags.add(bagName);
                }
            }
            if (!missingBags.isEmpty()) {
                throw new BadRequestException("XML is well formatted but saved Lists (bags) used "
                        + "by this query don't exist: " + missingBags + " query: " + xml);
            }
        } else {
            throw new BadRequestException(formatMessage(validator.getErrorsAndWarnings()));
        }
    }

    private String formatMessage(List<String> msgs) {
        StringBuilder sb = new StringBuilder();
        for (String msg : msgs) {
            sb.append(msg);
            if (!msg.endsWith(".")) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    /**
     * Returns parsed PathQuery.
     * @return parsed PathQuery
     */
    public PathQuery getQuery() {
        return pathQuery;
    }
}
