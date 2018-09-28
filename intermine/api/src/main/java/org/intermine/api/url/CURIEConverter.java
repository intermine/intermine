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
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

/**
 * This class converts an intermineID into a CURIE
 * or a CURIE into an intermineID.
 *
 * @author danielabutano
 */
public class CURIEConverter
{
    private static final Integer INTERMINE_ID_NOT_FOUND = -1;
    private PrefixRegistry prefixRegistry = null;
    private PrefixKeyMapper prefixKeyMapper = null;

    private static final Logger LOGGER = Logger.getLogger(CURIEConverter.class);

    /**
     * Constructor
     */
    public CURIEConverter() {
        prefixRegistry = PrefixRegistry.getRegistry();
        prefixKeyMapper = PrefixKeyMapper.getMapper();
    }

    /**
     * Given a curie (compact uri, e.g. uniprot:P27362) returns the internal intermine Id
     * @param curie the curie
     * @return internal intermine id
     * @throws ObjectStoreException if there are any objectstore issues
     */
    public Integer getIntermineID(CURIE curie) throws ObjectStoreException {
        if (curie != null) {
            List<String> classNames = prefixRegistry.getClassNames(curie.getPrefix());
            Integer intermineId;
            for (String className : classNames) {
                intermineId = getId(curie, className);
                if (intermineId != null) {
                    return intermineId;
                }
            }
            return INTERMINE_ID_NOT_FOUND;
        }
        throw new RuntimeException("CURIE is null");
    }

    /**
     * Returns the intermine id, given a curie and the class name
     * @param curie the curie prefix:lui
     * @param className the class name used to create the pathquery's view and the contraint
     * @return the pathquery
     */
    private Integer getId(CURIE curie, String className) throws ObjectStoreException {
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        String viewPath = className + ".id";
        pathQuery.addView(viewPath);
        String prefix = curie.getPrefix();
        String prefixKey = prefixKeyMapper.getKey(prefix);
        String contstraintPath = className + "." + prefixKey;
        String localUniqueId = curie.getLocalUniqueId();
        String constraintValue = prefixKeyMapper.getInterMineAdaptedLUI(curie);
        LOGGER.info("CURIEConverter: given the prefix " + prefix + " the constraintValue is: "
                + constraintValue);
        pathQuery.addConstraint(Constraints.eq(contstraintPath, constraintValue));
        if (!pathQuery.isValid()) {
            throw new RuntimeException("The PathQuery :" + pathQuery.toString() + " is not valid");
        }
        LOGGER.info("CURIEConverter: pathQuery to retrieve internal id: "
                + pathQuery.toString());
        PathQueryExecutor executor = new PathQueryExecutor(PathQueryAPI.getObjectStore(),
                PathQueryAPI.getProfile(), null, PathQueryAPI.getBagManager());
        ExportResultsIterator iterator = executor.execute(pathQuery);

        if (iterator.hasNext()) {
            ResultElement row = iterator.next().get(0);
            return row.getId();
        }
        return null;
    }
    /**
     * Generate the CURIE associated to the internal interMine ID
     * @param interMineID the interMineID
     * @return the CURIE
     */
    public CURIE getCURIE(Integer interMineID) throws ObjectStoreException {
        if (interMineID != null) {
            String type = getType(interMineID);
            List<String> possiblePrefixes = prefixRegistry.getPrefixes(type);
            if (possiblePrefixes == null) {
                //see Author type
                return null;
            }
            //find the possible data source associate to the prefix
            Map<String, List<String>> possibleDataSources = new HashMap<>();
            for (String prefix : possiblePrefixes) {
            }
            //todo harcoded -> read the project.xml instead
            possibleDataSources.put("uniprot", Arrays.asList("uniprot-malaria"));
            possibleDataSources.put("plasmodb", Arrays.asList("malaria-gff",
                    "malaria-chromosome-fasta"));
            possibleDataSources.put("go", Arrays.asList("go"));
            possibleDataSources.put("go-annotation", Arrays.asList("go-annotation"));
            possibleDataSources.put("pubmed", Arrays.asList("update-publications"));
            
            //starting from the first data source, check if in the tracker table there is an entry
            // fot that datasource and for the field which is the key for the prefix
            for (String prefix : possibleDataSources.keySet()) {
                String key = prefixKeyMapper.getKey(prefix);
                List<String> dataSources = possibleDataSources.get(prefix);
                if (dataSources != null) {
                    for (String dataSource : dataSources) {
                        if (checkTracker(interMineID, key, dataSource)) {
                            String keyValue = getKeyValue(key, type, interMineID);
                            LOGGER.info("CURIEConverter: prefix:" + prefix + " and keyValue: " + keyValue);
                            //key value is ome case mimight be different from the value stored by InterMine
                            //e.g. go uses 0000186 as LUI which is stored in intermine as GO:0000186
                            String originalLUI = prefixKeyMapper.getOriginalLUI(prefix, keyValue);
                            return new CURIE(prefix, originalLUI);
                        }
                    }
                }
            }
            //if we are here it's most likely because the entity doesn't have a value for the prymarykey
            //see some ontologyterm with empty primaryidentifier -> in that case there are no rows in the traker
            return null;
        }
        throw new RuntimeException("intermineID is null");
    }

    /**
     * Return the type of the entity with the id given in input
     * @param interMineID the interMineID
     * @return the type, e.g. Protein
     */
    private String getType(Integer interMineID) {
        ObjectStore os = PathQueryAPI.getObjectStore();
        InterMineObject imObj = null;
        String type = null;
        try {
            Class shadowClass = os.getObjectById(interMineID).getClass();
            try {
                Field field = shadowClass.getField("shadowOf");
                Class clazz = (Class) field.get(null);
                type = clazz.getSimpleName();
                LOGGER.info("CURIEConverter: the entity with id " + interMineID
                        + " has type: " + type);
            } catch (NoSuchFieldException e) {
                LOGGER.error(e);
            }  catch (IllegalAccessException e) {
                LOGGER.error(e);
            }
        } catch (ObjectStoreException e) {
            LOGGER.error("Failed to find the object with id: " + interMineID, e);
        }
        return type;
    }

    /**
     * Method to heck if there is an entry, in the tracker table,
     * matching the id,fieldname and datasource given in input
     * @param interMineId the interMineID
     * @param field the fieldname
     * @param sourceName the sourceName
     * @return true if the entry exists
     */
    private boolean checkTracker(Integer interMineId, String field, String sourceName) {
        Connection conn = null;
        try {
            conn = ((ObjectStoreInterMineImpl) PathQueryAPI.getObjectStore())
                    .getDatabase().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT * FROM tracker WHERE objectid = ? AND fieldname = ? AND sourcename = ?");
            pstmt.setInt(1, interMineId);
            pstmt.setString(2, field);
            pstmt.setString(3, sourceName);

            ResultSet result = pstmt.executeQuery();
            if (result.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException ex) {
            LOGGER.error("Error querying the tracker for id: " + interMineId
                    + " fieldname: " + field + " and sourcename: " + sourceName);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.error("Error closing db connection");
                    return false;
                }
            }
        }
    }

    /**
     * Return the value of the field given in input for the entity specified
     * by the interMineId given in input
     * @param keyField the keyField, for example primaryAccession
     * @param type the type of the entity,e.g. Protein
     * @param interMineId the interMineId which identifies the entity
     * @return the value of field geven in input
     */
    private String getKeyValue(String keyField, String type, Integer interMineId)
        throws ObjectStoreException {
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        String viewPath = type + "." + keyField;
        pathQuery.addView(viewPath);
        String contstraintPath = type + ".id";
        pathQuery.addConstraint(Constraints.eq(contstraintPath, Integer.toString(interMineId)));
        if (!pathQuery.isValid()) {
            throw new RuntimeException("The PathQuery :" + pathQuery.toString() + " is not valid");
        }
        PathQueryExecutor executor = new PathQueryExecutor(PathQueryAPI.getObjectStore(),
                PathQueryAPI.getProfile(), null, PathQueryAPI.getBagManager());
        ExportResultsIterator iterator = executor.execute(pathQuery);

        if (iterator.hasNext()) {
            ResultElement cell = iterator.next().get(0);
            return (String) cell.getField();
        }
        return null;
    }

}
