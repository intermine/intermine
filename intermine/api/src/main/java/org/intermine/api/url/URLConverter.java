package org.intermine.api.url;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.api.query.PathQueryAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;

import java.util.List;

/**
 * This class converts an intermineID into a permanentURL
 * or a permanentURL into an intermineID.
 *
 * @author danielabutano
 */
public class URLConverter
{
    private Integer intermineID = null;
    private PermanentURL permanentURL = null;
    private static final Integer INTERMINE_ID_NOT_FOUND = -1;

    private static final Logger LOGGER = Logger.getLogger(URLConverter.class);

    public URLConverter() {
    }

    public Integer getIntermineID(PermanentURL permanentURL) throws ObjectStoreException {
        if (permanentURL != null) {
            List<String> classNames = PrefixRegistry.getRegistry()
                    .getClassNames(permanentURL.getPrefix());
            Integer intermineId = INTERMINE_ID_NOT_FOUND;

            for (String className : classNames) {
                PathQuery pathQuery = buildPathQuery(permanentURL.getPrefix(),
                        className, permanentURL.getExternalLocalId());
                LOGGER.info("URLConverter: pathQuery to retrieve internal id: " + pathQuery.toString());
                //PathQueryExecutor executor = PathQueryAPI.getPathQueryExecutor();
                PathQueryExecutor executor = new PathQueryExecutor(PathQueryAPI.getObjectStore(),
                        PathQueryAPI.getProfile(), null, PathQueryAPI.getBagManager());
                ExportResultsIterator iterator = executor.execute(pathQuery);

                if (iterator.hasNext()) {
                    ResultElement row = iterator.next().get(0);
                    intermineId = row.getId();
                    return intermineId;
                }
            }
            return intermineId;

        }
        throw new RuntimeException("PermanentURL is null");
    }

    private PathQuery buildPathQuery(String prefix, String className, String externalLocalId) {
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        String viewPath = className + ".id";
        pathQuery.addView(viewPath);
        PrefixKeysProperties prefixKeys = PrefixKeysProperties.getProperties();
        String prefixKey = prefixKeys.getPrefixKey(prefix);
        LOGGER.info("URLConverter: given the prefix " + prefix + " the key is: " + prefixKey);
        String contstraintPath = className + "." + prefixKey;
        String constraintValue = prefixKeys.getInterMineExternalIdValue(prefix, externalLocalId);
        LOGGER.info("URLConverter: given the prefix " + prefix + " the constraintValue is: " + constraintValue);
        pathQuery.addConstraint(Constraints.eq(contstraintPath, constraintValue));
        if (!pathQuery.isValid()) {
            throw new RuntimeException("The PathQuery :" + pathQuery.toString() + " is not valid");
        }
        return pathQuery;
    }

    public PermanentURL getPermanentURL(Integer intermineID) {
        if (intermineID != null) {
            //given an intermine ID retrieve te prefix ans the value of the externalLocalId
            //TODO
            return permanentURL;
        }
        throw new RuntimeException("intermineID is null");
    }
}
