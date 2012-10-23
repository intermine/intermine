package org.intermine.web.logic.querybuilder;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.query.MetadataNode;

/**
 * Static class holding methods for managing the model browser in the query builder.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public final class ModelBrowserHelper
{
    private ModelBrowserHelper() {
    }

    /**
     * Finds the collection of MetadataNode objects that the model browser needs to display.
     *
     * @param stringPath a deeper path in the query that must be added to the nodes
     * @param prefix the root class of the query, even if the query is empty
     * @param model a Model
     * @param isSuperUser true if the current user is a superuser
     * @param query a PathQuery
     * @param webConfig a WebConfig object
     * @return a Collection of MetadataNode objects
     * @param classKeys a Map of class keys, for working out if a path has any
     * @param bagManager a BagManager object, for working out if it is possible to constrain by bag
     * @param profile the profile of the current user, for fetching bags from the BagManager
     * @param oss to determine which collections/references/attributes are empty
     * @throws PathException if the query is not valid
     */
    public static Collection<MetadataNode> makeSelectedNodes(String stringPath, String prefix,
            Model model, boolean isSuperUser, PathQuery query, WebConfig webConfig,
            Map<String, List<FieldDescriptor>> classKeys, BagManager bagManager, Profile profile,
            ObjectStoreSummary oss)
        throws PathException {

        // Gene.crossReferences, ...
        Collection<MetadataNode> nodes = makeNodes(stringPath, model, isSuperUser,
                query, webConfig, classKeys, bagManager, profile, oss);
        List<String> view = query.getView();
        for (MetadataNode node : nodes) {
            // Update view nodes
            String pathName = node.getPathString();
            int firstDot = pathName.indexOf('.');
            String fullPath;
            if (firstDot == -1) {
                fullPath = prefix;
            } else {
                String pathNameWithoutClass = pathName.substring(firstDot + 1);
                fullPath = prefix + "." + pathNameWithoutClass;
            }
            if (view.contains(fullPath)) {
                node.setSelected(true);
            } else {
                Path path;
                try {
                    path = query.makePath(pathName);
                } catch (PathException e) {
                    // Should never happen, as the path came from a node
                    throw new Error("There must be a bug", e);
                }
                // If an object has been selected, select its fields instead
                if (path.getEndFieldDescriptor() == null || path.endIsReference()
                        || path.endIsCollection()) {
                    if (view.contains(path)) {
                        ClassDescriptor cld = path.getEndClassDescriptor();
                        for (FieldConfig fc
                                : FieldConfigHelper.getClassFieldConfigs(webConfig, cld)) {
                            String pathFromField = pathName + "." + fc.getFieldExpr();
                            if (view.contains(pathFromField)) {
                                node.setSelected(true);
                            } else {
                                node.setSelected(false);
                            }
                        }
                    }
                }
            }
        }
        return nodes;
    }

    /**
     * Given a path, render a set of metadata Nodes to the relevant depth
     * @param path of form Gene.organism.name
     * @param model the model used to resolve class names
     * @param isSuperUser true if the user is the superuser
     * @return an ordered Set of nodes
     * @param subclasses a Map from path in the query to class name if there is a subclass
     * constraint
     * @param query a PathQuery, for working out possible loops
     * @param classKeys a Map of class keys, for working out if a path has any
     * @param bagManager a BagManager object, for working out if it is possible to constrain by bag
     * @param profile the profile of the current user, for fetching bags from the BagManager
     * @param oss to determine which nodes are null
     * @throws PathException if the query is invalid.
     */
    public static Collection<MetadataNode> makeNodes(String path, Model model, boolean isSuperUser,
            PathQuery query, WebConfig webConfig, Map<String,
            List<FieldDescriptor>> classKeys, BagManager bagManager, Profile profile,
            ObjectStoreSummary oss)
        throws PathException {

        Map<String, String> subclasses;
        if (query.isEmpty()) {
            subclasses = Collections.EMPTY_MAP;
        } else {
            subclasses = query.getSubclasses();
        }

        String className, subPath;
        if (path.indexOf(".") == -1) {
            className = path;
            subPath = "";
        } else {
            className = path.substring(0, path.indexOf("."));
            subPath = path.substring(path.indexOf(".") + 1);
        }

        Map<String, MetadataNode> nodes = new LinkedHashMap<String, MetadataNode>();
        List<String> empty = Collections.emptyList();
        nodes.put(className, new MetadataNode(className, empty, query, classKeys, bagManager,
                profile));
        makeNodes(model.getClassDescriptorByName(className), subPath, className, nodes,
                isSuperUser, empty, subclasses, null, query, classKeys, bagManager,
                profile, oss, webConfig);
        return nodes.values();
    }

    /**
     * Recursive method used to add nodes to a set representing a path from a given ClassDescriptor.
     *
     * @param cld the root ClassDescriptor
     * @param path current path prefix (eg Gene)
     * @param currentPath current path suffix (eg organism.name)
     * @param nodes the current Node set
     * @param isSuperUser true if the user is the superuser
     * @param structure a List of Strings - for definition, see MetadataNode.getStructure()
     * @param subclasses a Map from path in the query to class name if there is a subclass
     * constraint
     * @param reverseFieldName the name of the field that is the reverse reference back onto the
     * parent, so that it can be sorted to the top of the list
     * @param query a PathQuery, for working out possible loops
     * @param classKeys a Map of class keys, for working out if a path has any
     * @param bagManager a BagManager object, for working out if it is possible to constrain by bag
     * @param profile the profile of the current user, for fetching bags from the BagManager
     * @param oss to determine which nodes are null
     */
    protected static void makeNodes(ClassDescriptor cld, String path, String currentPath,
            Map<String, MetadataNode> nodes, boolean isSuperUser, List<String> structure,
            Map<String, String> subclasses, String reverseFieldName, PathQuery query,
            Map<String, List<FieldDescriptor>> classKeys, BagManager bagManager, Profile profile,
            ObjectStoreSummary oss, WebConfig webConfig) {

        // null atrtributes, references and collections
        Set<String> nullAttr = oss.getNullAttributes(cld.getName());
        Set<String> nullRefsCols = oss.getNullReferencesAndCollections(cld.getName());

        List<FieldDescriptor> sortedNodes = new ArrayList<FieldDescriptor>();

        // compare FieldDescriptors by name
        Comparator<FieldDescriptor> comparator = new Comparator<FieldDescriptor>() {
            public int compare(FieldDescriptor o1, FieldDescriptor o2) {
                String fieldName1 = o1.getName().toLowerCase();
                String fieldName2 = o2.getName().toLowerCase();
                return fieldName1.compareTo(fieldName2);
            }
        };

        Set<FieldDescriptor> attributeNodes = new TreeSet<FieldDescriptor>(comparator);
        Set<FieldDescriptor> referenceAndCollectionNodes = new TreeSet<FieldDescriptor>(comparator);
        for (Iterator<FieldDescriptor> i = cld.getAllFieldDescriptors().iterator(); i.hasNext();) {
            FieldDescriptor fd = i.next();
            FieldConfig fc = FieldConfigHelper.getFieldConfig(webConfig, fd);
            if (fc == null || !fc.isHideInQueryBuilder()) {
                if (!fd.isReference() && !fd.isCollection()) {
                    attributeNodes.add(fd);
                } else {
                    if (fd.getName().equals(reverseFieldName)) {
                        sortedNodes.add(fd);
                    } else {
                        referenceAndCollectionNodes.add(fd);
                    }
                }
            }
        }

        sortedNodes.addAll(attributeNodes);
        sortedNodes.addAll(referenceAndCollectionNodes);

        List<FieldDescriptor> filteredNodes = new ArrayList<FieldDescriptor>();
        for (FieldDescriptor fd : sortedNodes) {
            String fieldName = fd.getName();
            if ("id".equals(fieldName) && !isSuperUser) {
                continue;
            }

            filteredNodes.add(fd);
        }

        Iterator<FieldDescriptor> nodeIter = filteredNodes.iterator();
        while (nodeIter.hasNext()) {
            FieldDescriptor fd = nodeIter.next();
            String fieldName = fd.getName();

            String head, tail;
            if (path.indexOf(".") != -1) {
                head = path.substring(0, path.indexOf("."));
                tail = path.substring(path.indexOf(".") + 1);
            } else {
                head = path;
                tail = "";
            }

            String button;
            if (fieldName.equals(head)) {
                button = "-";
            } else if (fd.isReference() || fd.isCollection()) {
                button = "+";
            } else {
                button = " ";
            }

            List<String> newStructure = new ArrayList<String>(structure);
            if (nodeIter.hasNext()) {
                newStructure.add("tee");
            } else {
                newStructure.add("ell");
            }
            MetadataNode parent = nodes.get(currentPath);

            // is the field null/empty
            Boolean isNull = (nullAttr.contains(fieldName) || nullRefsCols.contains(fieldName));

            MetadataNode node = new MetadataNode(parent, fieldName, button, newStructure, query,
                    classKeys, bagManager, profile, isNull);
            node.setModel(cld.getModel());
            String subclass = subclasses.get(node.getPathString());
            if (subclass != null) {
                node.setType(subclass);
            }

            nodes.put(node.getPathString(), node);
            if (fieldName.equals(head)) {
                newStructure = new ArrayList<String>(structure);
                if (nodeIter.hasNext()) {
                    newStructure.add("straight");
                } else {
                    newStructure.add("blank");
                }
                ClassDescriptor refCld;
                if (subclass == null) {
                    refCld = ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
                } else {
                    refCld = cld.getModel().getClassDescriptorByName(subclass);
                }
                String reverse = null;
                if (fd instanceof ReferenceDescriptor) {
                    reverse = ((ReferenceDescriptor) fd).getReverseReferenceFieldName();
                }
                makeNodes(refCld, tail, currentPath + "." + head, nodes, isSuperUser, newStructure,
                        subclasses, reverse, query, classKeys, bagManager, profile, oss, webConfig);
            }
        }
    }
}
