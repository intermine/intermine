package org.intermine.api.uri;

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
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;

import java.lang.reflect.Field;

/**
 * This class converts an intermineID into a InterMineLUI
 * or a InterMineLUI into an intermineID.
 *
 * @author danielabutano
 */
public class InterMineLUIConverter
{
    private static final Integer INTERMINE_ID_NOT_FOUND = -1;
    private static final String DEFAULT_IDENTIFIER = "primaryIdentifier";
    private ClassNameURIIdentifierMapper classNameIdentifierMapper = null;

    private static final Logger LOGGER = Logger.getLogger(InterMineLUIConverter.class);

    /**
     * Constructor
     */
    public InterMineLUIConverter() {
        classNameIdentifierMapper = ClassNameURIIdentifierMapper.getMapper();
    }

    /**
     * Given a InterMineLUI (compact uri, e.g. protein:P27362) returns the internal intermine Id
     * @param interMineLUI the interMineLUI
     * @return internal intermine id
     * @throws ObjectStoreException if there are any objectstore issues
     */
    public Integer getInterMineID(InterMineLUI interMineLUI) throws ObjectStoreException {
        if (interMineLUI == null) {
            throw new RuntimeException("InterMineLUI is null");
        }
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        String className = interMineLUI.getClassName();
        String viewPath =  className + ".id";
        pathQuery.addView(viewPath);
        String identifier = classNameIdentifierMapper.getIdentifier(className);
        if (identifier == null) {
            identifier = DEFAULT_IDENTIFIER;
        }
        String contstraintPath = className + "." + identifier;
        pathQuery.addConstraint(Constraints.eq(contstraintPath, interMineLUI.getIdentifier()));
        if (!pathQuery.isValid()) {
            LOGGER.info("The PathQuery :" + pathQuery.toString() + " is not valid");
            return null;
        }
        LOGGER.info("InterMineLUIConverter: pathQuery to retrieve internal id: "
                + pathQuery.toString());
        PathQueryExecutor executor = new PathQueryExecutor(PathQueryAPI.getObjectStore(),
                PathQueryAPI.getProfile(), null, PathQueryAPI.getBagManager());
        ExportResultsIterator iterator = executor.execute(pathQuery);

        if (iterator.hasNext()) {
            ResultElement row = iterator.next().get(0);
            return row.getId();
        } else {
            LOGGER.info("InterMineLUIConverter: there are not " + className
                    + " with " + contstraintPath + "=" + interMineLUI.getIdentifier());
            return INTERMINE_ID_NOT_FOUND;
        }
    }

    /**
     * Generate the InterMineLUI associated to the internal interMine ID
     * @param interMineID the interMineID
     * @return the InterMineLUI
     * @throws ObjectStoreException if something goes wrong when retrieving the identifier
     */
    public InterMineLUI getInterMineLUI(Integer interMineID) throws ObjectStoreException {
        if (interMineID == null) {
            LOGGER.error("intermineID is null");
        }
        String type = getType(interMineID);
        if (type == null) {
            LOGGER.error("The entity " + interMineID + " has no class");
        }
        String identifier = getIdentifier(type, interMineID);
        if (identifier == null) {
            LOGGER.info("The identifier's value for the entity " + interMineID + " is null");
            return null;
        }
        return new InterMineLUI(type, identifier);
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
                LOGGER.info("InterMineLUIConverter: the entity with id " + interMineID
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
     * Return the identifier's value of the entity specified by the interMineId given in input
     * @param type the type of the entity,e.g. Protein
     * @param interMineId the interMineId which identifies the entity
     * @return the identifier's value
     * @throws ObjectStoreException if something goes wrong with query retrieving identifier
     */
    private String getIdentifier(String type, Integer interMineId)
        throws ObjectStoreException {
        String identifier = classNameIdentifierMapper.getIdentifier(type);
        if (identifier == null) {
            identifier = DEFAULT_IDENTIFIER;
        }
        PathQuery pathQuery = new PathQuery(Model.getInstanceByName("genomic"));
        String viewPath = type + "." + identifier;
        pathQuery.addView(viewPath);
        String contstraintPath = type + ".id";
        pathQuery.addConstraint(Constraints.eq(contstraintPath, Integer.toString(interMineId)));
        if (!pathQuery.isValid()) {
            LOGGER.info("The PathQuery :" + pathQuery.toString() + " is not valid");
            return null;
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
