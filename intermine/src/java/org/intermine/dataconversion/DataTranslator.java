package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import org.flymine.xml.full.Item;
import org.flymine.xml.full.Field;
import org.flymine.xml.full.ReferenceList;
import org.flymine.ontology.OntologyUtil;


/**
 * Convert data in FlyMine Full XML format conforming to a source OWL definition
 * to FlyMine Full XML conforming to FLyMine OWL definition.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class DataTranslator
{

    /**
     * Convert a list of Items in source format to a list of items conforming
     * to target OWL where mapping between OWL model is described by equivalence
     * in the given OntModel.
     * @param srcItems items to convert
     * @param model OWL model specifying mapping between source and target models
     * @return list of converted items
     */
    public static Collection translate(Collection srcItems, OntModel model) {
        Map equivMap = buildEquivalenceMap(model);

        Set tgtItems = new LinkedHashSet();
        Iterator iter = srcItems.iterator();
        while (iter.hasNext()) {
            tgtItems.add(translateItem((Item) iter.next(), equivMap));
        }
        return tgtItems;
    }

    /**
     * Convert an Item in source format to a list of items conforming
     * to target OWL where mapping between OWL model is described by equivalence
     * map.
     * @param srcItem item to convert
     * @param equivMap map of equivalent resources in source and target namespaces
     * @return converted item
     */
    protected static Item translateItem(Item srcItem, Map equivMap) {
        String ns = OntologyUtil.getNamespaceFromURI(srcItem.getClassName());
        Item tgtItem = new Item();
        tgtItem.setIdentifier(srcItem.getIdentifier());
        tgtItem.setClassName((String) equivMap.get(srcItem.getClassName()));
        StringTokenizer tokenizer = new StringTokenizer(srcItem.getImplementations(),
                                                        " ", false);
        String imps = "";
        while (tokenizer.hasMoreTokens()) {
            imps += (String) equivMap.get(tokenizer.nextToken()) + " ";
        }
        tgtItem.setImplementations(imps.trim());

        Iterator fieldIter = srcItem.getFields().iterator();
        while (fieldIter.hasNext()) {
            Field field = (Field) fieldIter.next();
            Field newField = new Field();
            newField.setName(OntologyUtil.getFragmentFromURI(
                (String) equivMap.get(ns + field.getName())));
            newField.setValue(field.getValue());
            tgtItem.addField(newField);
        }
        Iterator refIter = srcItem.getReferences().iterator();
        while (refIter.hasNext()) {
            Field field = (Field) refIter.next();
            Field newField = new Field();
            newField.setName(OntologyUtil.getFragmentFromURI(
                (String) equivMap.get(ns + field.getName())));
            newField.setValue(field.getValue());
            tgtItem.addReference(newField);
        }
        Iterator colIter = srcItem.getCollections().iterator();
        while (colIter.hasNext()) {
            ReferenceList col = (ReferenceList) colIter.next();
            ReferenceList newCol = new ReferenceList();
            newCol.setName(OntologyUtil.getFragmentFromURI(
                (String) equivMap.get(ns + col.getName())));
            Iterator i = col.getReferences().iterator();
            while (i.hasNext()) {
                newCol.addValue((String) i.next());
            }
            tgtItem.addCollection(newCol);
        }
        return tgtItem;
    }


    /**
     * Build a map of resources in source namespaces to their equivalent resources
     * in target namespace.
     * @param model an OWL model specifying mapping
     * @return mappings between source and target namespaces
     */
    protected static Map buildEquivalenceMap(OntModel model) {
        Map equivMap = new HashMap();

        Iterator stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getPredicate().getLocalName().equals("equivalentClass")
                || stmt.getPredicate().getLocalName().equals("equivalentProperty")
                || stmt.getPredicate().getLocalName().equals("sameAs")) {
                Resource res = stmt.getResource();
                equivMap.put(res.getURI(), stmt.getSubject().getURI());
//                if (equivMap.containsKey(res.getURI())) {
//                     Object obj = equivMap.get(res.getURI());
//                     if (!(obj instanceof HashSet)) {
//                         obj = new HashSet();
//                         ((Set) obj).add(equivMap.get(res.getURI()));
//                         equivMap.put(res.getURI(), obj);
//                     }
//                     ((Set) obj).add(stmt.getSubject().getURI());
            }
        }
        return equivMap;
    }
}
