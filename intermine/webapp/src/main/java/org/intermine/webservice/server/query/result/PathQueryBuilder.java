package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.core.Producer;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;


/**
 * PathQueryBuilder builds PathQuery object from xml and validates it.
 *
 * @author Jakub Kulaviak
 **/
public class PathQueryBuilder
{

    private PathQuery pathQuery;

    private static Logger logger = Logger.getLogger(PathQueryBuilder.class);

    /**
     * Constructor for testing.
     */
    PathQueryBuilder() {
        // empty constructor for testing
    }

    /**
     * PathQueryBuilder constructor.
     * @param xml xml string from which will be PathQuery constructed
     * @param schemaUrl url of XML Schema file, validation is performed according this file
     * @param bagSource previously saved bags
     */
    public PathQueryBuilder(
            String xml,
            String schemaUrl,
            Producer<Map<String, InterMineBag>> bagSource) {
        buildQuery(xml, schemaUrl, bagSource);
    }

    /**
     * Perform the build operation.
     * @param xml xml string from which will be PathQuery constructed
     * @param schemaUrl url of XML Schema file, validation is performed according this file
     * @param bagSource previously saved bags.
     */
    void buildQuery(
            String xml,
            String schemaUrl,
            Producer<Map<String, InterMineBag>> bagSource) {
        XMLValidator validator = new XMLValidator();
        validator.validate(xml, schemaUrl);
        if (validator.getErrorsAndWarnings().size() == 0) {
            try {
                pathQuery = PathQueryBinding.unmarshalPathQuery(new StringReader(xml),
                    PathQuery.USERPROFILE_VERSION);
            } catch (Exception e) {
                String message = String.format("XML is not well formatted. Got %s.", xml);
                throw new BadRequestException(message, e);
            }

            if (!pathQuery.isValid()) {
                throw new BadRequestException("XML is well formatted but query contains errors:\n"
                        + formatMessage(pathQuery.verifyQuery()));
            }

            // check bags used by this query exist and are current
            Set<String> missingBags = new HashSet<String>();
            Set<String> toUpgrade = new HashSet<String>();
            for (String bagName : pathQuery.getBagNames()) {
                // Use a producer so we only have to hit the userprofile if there
                // actually are any bags to query.
                Map<String, InterMineBag> savedBags = bagSource.produce();
                if (!savedBags.containsKey(bagName)) {
                    missingBags.add(bagName);
                } else {
                    InterMineBag bag = savedBags.get(bagName);
                    if (BagState.CURRENT != BagState.valueOf(bag.getState())) {
                        toUpgrade.add(bagName);
                    }
                }
            }
            if (!missingBags.isEmpty()) {
                throw new BadRequestException(
                        "The query XML is well formatted but you do not have access to the "
                        + "following mentioned lists:\n"
                        + formatMessage(missingBags));
            }
            if (!toUpgrade.isEmpty()) {
                throw new ServiceException(
                        "The query XML is well formatted, but the following lists"
                        + " are not 'current', and need to be manually upgraded:\n"
                        + formatMessage(toUpgrade));
            }
        } else {
            logger.debug("Received invalid xml: " + xml);
            throw new BadRequestException("Query does not pass XML validation. "
                    + formatMessage(validator.getErrorsAndWarnings()));
        }
    }

    private String formatMessage(Collection<String> msgs) {
        StringBuilder sb = new StringBuilder();
        for (String msg : msgs) {
            sb.append(msg);
            if (!msg.endsWith(".")) {
                sb.append(".");
            }
            sb.append("\n");
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
