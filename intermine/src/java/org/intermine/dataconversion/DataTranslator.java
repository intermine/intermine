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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

import org.flymine.model.fulldata.Attribute;
import org.flymine.model.fulldata.Item;
import org.flymine.model.fulldata.Reference;
import org.flymine.model.fulldata.ReferenceList;
import org.flymine.ontology.OntologyUtil;
import org.flymine.util.StringUtil;

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
        Map equivMap = OntologyUtil.buildEquivalenceMap(model);

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
        tgtItem.setClassName(((Resource) equivMap.get(srcItem.getClassName())).getURI());

        //implementations
        String imps = srcItem.getImplementations();
        String newImps = "";
        if (imps != null) {
            for (Iterator i = StringUtil.tokenize(imps).iterator(); i.hasNext();) {
                newImps += ((Resource) equivMap.get((String) i.next())).getURI() + " ";
            }
        }
        tgtItem.setImplementations(newImps.trim());

        //attributes
        for (Iterator i = srcItem.getAttributes().iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            Attribute newAttr = new Attribute();
            newAttr.setName(OntologyUtil.getFragmentFromURI(
                ((Resource) equivMap.get(ns + attr.getName())).getURI()));
            newAttr.setValue(attr.getValue());
            tgtItem.addAttributes(newAttr);
        }

        //references
        for (Iterator i = srcItem.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            Reference newRef = new Reference();
            newRef.setName(OntologyUtil.getFragmentFromURI(
                ((Resource) equivMap.get(ns + ref.getName())).getURI()));
            newRef.setIdentifier(ref.getIdentifier());
            tgtItem.addReferences(newRef);
        }

        //collections
        for (Iterator i = srcItem.getCollections().iterator(); i.hasNext();) {
            ReferenceList col = (ReferenceList) i.next();
            ReferenceList newCol = new ReferenceList();
            newCol.setName(OntologyUtil.getFragmentFromURI(
                ((Resource) equivMap.get(ns + col.getName())).getURI()));
            newCol.setIdentifiers(col.getIdentifiers());
            tgtItem.addCollections(newCol);
        }
        return tgtItem;
    }
}
