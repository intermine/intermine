package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.InterMineException;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.CollectionUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.TypeConverter;

import javax.servlet.ServletContext;

/**
 * For a given list of input strings search for objects using default and configured queries for a
 * particular type.
 *
 * @author Richard Smith
 */
public class BagQueryRunner
{
    private ObjectStoreInterMineImpl os;

    private Model model;

    private Map classKeys;

    private BagQueryConfig bagQueryConfig;

    private ServletContext context;

    /**
     * Construct with configured bag queries and a map of type -> key fields.
     *
     * @param os
     *            the ObjectStore to run queries on
     * @param classKeys
     *            the class keys Map
     * @param bagQueryConfig
     *            the configuration for running queries
     * @param context
     *            the ServletContext used by type conversion
     */
    public BagQueryRunner(ObjectStore os, Map classKeys, BagQueryConfig bagQueryConfig,
                          ServletContext context) {
        this.os = (ObjectStoreInterMineImpl) os;
        this.context = context;
        this.model = os.getModel();
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
    }

    /**
     * Given an input list of string identifiers search for corresponding objects. First run a
     * default query then any queries configured for the speified type.
     *
     * @param type
     *            an unqualified class name to search for objects
     * @param input
     *            a list of strings to query
     * @param extraFieldValue
     *            the value used when adding an extra constraint to the bag query, configured in
     *            BagQueryConfig (eg. if connectField is "organism", the extraClassName is
     *            "Organism" and the constrainField is "name", the extraFieldValue might be
     *            "Drosophila melanogaster")
     * @return the matches, issues and unresolved input
     * @throws ClassNotFoundException
     * @throws ObjectStoreException
     * @throws InterMineException
     */
    public BagQueryResult searchForBag(String type, List input, String extraFieldValue)
        throws ClassNotFoundException, ObjectStoreException, InterMineException {

        Map lowerCaseInput = new HashMap();
        List cleanInput = new ArrayList();
        Iterator inputIter = input.iterator();
        while (inputIter.hasNext()) {
            String inputString = (String) inputIter.next();
            if (!(inputString == null) && !(inputString.equals(""))) {
                cleanInput.add(inputString);
                lowerCaseInput.put(inputString.toLowerCase(), inputString);
            }
        }

        // TODO tidy up using type String and Class

        // TODO BagQueryResult.getUnresolved() needs to return a map from input
        // to null (if not found) or a set of objects.
        // or just leave as a list of identifiers and objects of the qrong type
        // CollectionUtil.groupByClass will sort out the strings and types
        Class typeCls = Class.forName(model.getPackageName() + "." + type);
        List queries = getBagQueriesForType(bagQueryConfig.getBagQueries(), typeCls.getName(),
                                            cleanInput);
        Set unresolved = new LinkedHashSet(cleanInput);
        Iterator qIter = queries.iterator();
        BagQueryResult bqr = new BagQueryResult();
        while (qIter.hasNext() && !unresolved.isEmpty()) {
            BagQuery bq = (BagQuery) qIter.next();
            Map resMap = new HashMap();
            // run the next query on identifiers not yet resolved
            Query q = bq.getQuery(unresolved, extraFieldValue);
            // TODO this is hacky as default batch size is hard coded in the os
            boolean faster = false;
            if (unresolved.size() > 1000) {
                os.goFaster(q);
                faster = true;
            }
            Results res = os.execute(q);
            res.setNoPrefetch();
            Iterator resIter = res.iterator();
            while (resIter.hasNext()) {
                ResultsRow row = (ResultsRow) resIter.next();
                Integer id = (Integer) row.get(0);
                for (int i = 1; i < row.size(); i++) {
                    String field = (String) row.get(i);
                    if (field != null) {
                        String lowerField = field.toLowerCase();
                        if (lowerCaseInput.containsKey(lowerField)) {
                            Set ids = (Set) resMap.get(field);
                            if (ids == null) {
                                ids = new HashSet();
                                resMap.put(field, ids);
                            }
                            // obj is an Integer
                            ids.add(id);
                            // remove any identifiers that are now resolved
                            unresolved.remove(lowerCaseInput.get(lowerField));
                        }
                    }
                }
            }
            if (faster) {
                os.releaseGoFaster(q);
            }
            addResults(resMap, unresolved, bqr, bq, typeCls);
        }

        Map unresolvedMap = new HashMap();
        Iterator iter = unresolved.iterator();
        while (iter.hasNext()) {
            unresolvedMap.put(iter.next(), null);
        }
        bqr.getUnresolved().putAll(unresolvedMap);

        return bqr;
    }

    /**
     * Add results from resMap to a a BagQueryResults object.
     *
     * @throws InterMineException
     */
    private void addResults(Map resMap, Set unresolved, BagQueryResult bqr, BagQuery bq, Class type)
    throws InterMineException {
        Map objsOfWrongType = new HashMap();
        Iterator mapIter = resMap.entrySet().iterator();
        while (mapIter.hasNext()) {
            Map.Entry entry = (Map.Entry) mapIter.next();
            String input = (String) entry.getKey();
            Set ids = (Set) entry.getValue();
            boolean resolved = true;

            if (!bq.matchesAreIssues()) {

                // if matches are not issues then each entry will be a match or a duplicate
                if (ids.size() == 1) {
                    bqr.addMatch(input, (Integer) ids.iterator().next());
                } else {
                    List objs = new ArrayList();
                    Iterator objIter;
                    try {
                        objIter = os.getObjectsByIds(ids).iterator();
                    } catch (ObjectStoreException e) {
                        throw new InterMineException("can't fetch: " + ids, e);
                    }
                    while (objIter.hasNext()) {
                        objs.add(((List) objIter.next()).get(0));
                    }
                    bqr.addIssue(BagQueryResult.DUPLICATE, bq.getMessage(),
                                 (String) entry.getKey(), objs);
                }
            } else {
                List objs = new ArrayList();
                Set localObjsOfWrongType = new HashSet();
                Iterator objIter;
                try {
                    objIter = os.getObjectsByIds(ids).iterator();
                } catch (ObjectStoreException e) {
                    throw new InterMineException("can't fetch: " + ids, e);
                }

                // we have a list of objects that result from some query, divide into any that
                // match the type of the bag to be created and candidates for conversion
                while (objIter.hasNext()) {
                    Object obj = ((List) objIter.next()).get(0);

                    // TODO this won't cope with dynamic classes
                    Class c = (Class) DynamicUtil.decomposeClass(obj.getClass()).iterator().next();
                    if (type.isAssignableFrom(c)) {
                        objs.add(obj);
                    } else {
                        localObjsOfWrongType.add(obj);
                    }
                }

                if (!objs.isEmpty()) {
                    // we have a list of objects, if any match the type then add to bqr as an issue
                    // discard objects that matched a different type
                    if (objs.size() == 1) {
                        bqr.addIssue(BagQueryResult.OTHER, bq.getMessage(), input, objs);
                    } else {
                        bqr.addIssue(BagQueryResult.DUPLICATE, bq.getMessage(), input, objs);
                    }
                } else {
                    // all wrong, allow conversion attempts
                    resolved = false;
                    objsOfWrongType.put(input, localObjsOfWrongType);
                }
            }
            if (resolved) {
                unresolved.remove(input);
            }
        }

        // now objsOfWrongType contains all wrong types found for this query, try converting
        convertObjects(bqr, bq, type, objsOfWrongType);

        bqr.getUnresolved().putAll(objsOfWrongType);
    }

    /**
     * Find any objects in the objsOfWrongType Map that can be converted to the destination type,
     * add them to bqr as TYPE_CONVERTED issues and remove them from objsOfWrongType.
     */
    void convertObjects(BagQueryResult bqr, BagQuery bq, Class type, Map objsOfWrongType)
        throws InterMineException {
        if (!objsOfWrongType.isEmpty()) {
            // group objects by class
            Map objectToInput = new HashMap();
            Iterator iter = objsOfWrongType.entrySet().iterator();
            while (iter.hasNext()) {
                Entry entry = (Entry) iter.next();
                String input = (String) entry.getKey();
                Set set = (Set) entry.getValue();
                Iterator objIter = set.iterator();
                while (objIter.hasNext()) {
                    InterMineObject imo = (InterMineObject) objIter.next();
                    Set inputSet = (Set) objectToInput.get(imo);
                    if (inputSet == null) {
                        inputSet = new HashSet();
                        objectToInput.put(imo, inputSet);
                    }
                    inputSet.add(input);
                }
            }

            Map objTypes = CollectionUtil.groupByClass(objectToInput.keySet(), true);

            Iterator objTypeIter = objTypes.keySet().iterator();
            while (objTypeIter.hasNext()) {
                Class fromClass = (Class) objTypeIter.next();
                List candidateObjs = (List) objTypes.get(fromClass);

                // we may have already converted some of these types, remove any that have been.
                List objs = new ArrayList();
                Iterator candidateIter = candidateObjs.iterator();
                while (candidateIter.hasNext()) {
                    Object candidate = (Object) candidateIter.next();
                    if (objectToInput.containsKey(candidate)) {
                        objs.add(candidate);
                    }
                }

                // try to convert objects to target type
                Map convertedObjsMap = TypeConverter.convertObjects(context, fromClass, type, objs);
                if (convertedObjsMap == null) {
                    // no conversion found
                    continue;
                }
                // loop over the old objects
                Iterator origObjIter = convertedObjsMap.keySet().iterator();
                while (origObjIter.hasNext()) {
                    InterMineObject origObj = (InterMineObject) origObjIter.next();
                    List converterObjList = (List) convertedObjsMap.get(origObj);
                    Iterator convertedObjListIter = converterObjList.iterator();

                    // then for each new object ...
                    while (convertedObjListIter.hasNext()) {
                        InterMineObject convertedObj = (InterMineObject) convertedObjListIter
                        .next();
                        ConvertedObjectPair convertedPair = new ConvertedObjectPair(origObj,
                                                                                    convertedObj);
                        List objPairList = new ArrayList();
                        Set origInputStringSet = (Set) objectToInput.get(origObj);
                        objPairList.add(convertedPair);
                        // remove this object so we don't try to convert it again
                        objectToInput.remove(origObj);
                        // make an issue for each input identifier that matched the objects in
                        // this old/new pair
                        Iterator inputStringIter = origInputStringSet.iterator();
                        while (inputStringIter.hasNext()) {
                            String origInputString = (String) inputStringIter.next();
                            bqr.addIssue(BagQueryResult.TYPE_CONVERTED,
                                         bq.getMessage() + " found by converting from x",
                                         origInputString, objPairList);
                            objsOfWrongType.remove(origInputString);
                        }
                    }
                }
            }
        }
    }

    // temporary method - will be replaced by BagQueryHelper method
    private List getBagQueriesForType(Map bagQueries, String type, List input)
    throws ClassNotFoundException {
        List queries = new ArrayList();
        // create the default query and put it first in the list
        BagQuery defaultQuery = BagQueryHelper.createDefaultBagQuery(type, bagQueryConfig, model,
                                                                     classKeys, input);
        if (defaultQuery != null) {
            queries.add(defaultQuery);
        }

        // add any queries that are configured for this type
        List bqs = (List) bagQueries.get(TypeUtil.unqualifiedName(type));
        if (bqs != null) {
            queries.addAll(bqs);
        }
        return queries;
    }
}
