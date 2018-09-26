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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.PathQueryAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;

import java.lang.reflect.Field;
import java.util.List;

/**
 * This class converts an intermineID into a CURIE
 * or a CURIE into an intermineID.
 *
 * @author danielabutano
 */
public class CURIEConverter
{
    //private Integer intermineID = null;
    //private CURIE curie = null;
    private static final Integer INTERMINE_ID_NOT_FOUND = -1;

    private static final Logger LOGGER = Logger.getLogger(CURIEConverter.class);

    /**
     * Constructor
     */
    public CURIEConverter() {
    }

    /**
     * Given a curie (compact uri, e.g. uniprot:P27362) returns the internal intermine Id
     * @param curie the curie
     * @return internal intermine id
     * @throws ObjectStoreException if there are any objectstore issues
     */
    public Integer getIntermineID(CURIE curie) throws ObjectStoreException {
        if (curie != null) {
            List<String> classNames = PrefixRegistry.getRegistry()
                    .getClassNames(curie.getPrefix());
            Integer intermineId = INTERMINE_ID_NOT_FOUND;

            for (String className : classNames) {
                PathQuery pathQuery = buildPathQuery(curie, className);
                LOGGER.info("CURIEConverter: pathQuery to retrieve internal id: "
                        + pathQuery.toString());
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
        throw new RuntimeException("CURIE is null");
    }

    /**
     * Create a path query to retrieve the intermine id given a curie and the class name
     * @param curie
     * @param className the class name used to create the view an dthe contstrints
     * @return the pathquery
     */
    private PathQuery buildPathQuery(CURIE curie, String className) {
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        String viewPath = className + ".id";
        pathQuery.addView(viewPath);
        PrefixKeysProperties prefixKeys = PrefixKeysProperties.getProperties();
        String prefix = curie.getPrefix();
        String prefixKey = prefixKeys.getPrefixKey(prefix);
        LOGGER.info("CURIEConverter: given the prefix " + prefix + " the key is: " + prefixKey);
        String contstraintPath = className + "." + prefixKey;
        String localUniqueId = curie.getLocalUniqueId();
        String constraintValue = prefixKeys.getInterMineValue(curie);
        LOGGER.info("CURIEConverter: given the prefix " + prefix + " the constraintValue is: "
                + constraintValue);
        pathQuery.addConstraint(Constraints.eq(contstraintPath, constraintValue));
        if (!pathQuery.isValid()) {
            throw new RuntimeException("The PathQuery :" + pathQuery.toString() + " is not valid");
        }
        return pathQuery;
    }

    /**
     * Generate the CURIE associated to the internal interMine ID
     * @param interMineID the interMineID
     * @return the CURIE
     */
    public CURIE getCURIE(Integer interMineID) {
        if (interMineID != null) {
            //given an intermine ID retrieve te prefix and the value of the localUniqueId
            //TODO
            String type = getType(interMineID);
            return null;
        }
        throw new RuntimeException("intermineID is null");
    }

    private String getType(Integer interMineID) {
        ObjectStore os = PathQueryAPI.getObjectStore();
        InterMineObject imObj = null;
        try {
            imObj = os.getObjectById(interMineID);
            Class shadowClass = imObj.getClass();

            try {
                LOGGER.info("CURIEConverter");
                Field field = shadowClass.getField("shadowOf");
                Class clazz1 = field.getType();
                LOGGER.info("CURIEConverter: after getType");
                LOGGER.info("CURIEConverter: field.getType().getSimpleName():" + clazz1.getSimpleName());
                Class clazz = (Class) field.get(null);
                LOGGER.info("CURIEConverter: " + clazz.getSimpleName());
            } catch (NoSuchFieldException e) {
                LOGGER.error(e);
            } catch (IllegalAccessException e) {
                LOGGER.error("CURIEConverter", e);
            }

            LOGGER.info("CURIEConverter: given the interMineID " + interMineID + " the canonicalname is:" + imObj.getClass().getCanonicalName());
            LOGGER.info("CURIEConverter: given the interMineID " + interMineID + " the simplename is:" + imObj.getClass().getSimpleName());
            LOGGER.info("CURIEConverter: given the interMineID " + interMineID + " the typename is:" + imObj.getClass().getTypeName());
        } catch (ObjectStoreException e) {
            LOGGER.error("Failed to find the object with id: " + interMineID, e);
        }
        return imObj.getClass().getName();
    }
}
