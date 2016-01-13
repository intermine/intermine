package org.intermine.web.logic.querybuilder;

/*
 * Copyright (C) 2002-2016 FlyMine
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

    /**
     * An object that will descend through the model and build a tree of metadata nodes.
     * This class partly exists so that we don't have static methods with 14 parameters.
     *
     * @author Alex Kalderimis
     */
    protected static final class NodeBuilder
    {
        private final class FieldNameComparator implements
                Comparator<FieldDescriptor>
        {
            public int compare(FieldDescriptor a, FieldDescriptor b) {
                String fieldName1 = a.getName().toLowerCase();
                String fieldName2 = b.getName().toLowerCase();
                return fieldName1.compareTo(fieldName2);
            }
        }

        final Map<String, MetadataNode> nodes;
        final boolean isSuperUser;
        final Model model;
        final Map<String, String> subclasses;
        final PathQuery query;
        final Map<String, List<FieldDescriptor>> classKeys;
        final BagManager bagManager;
        final Profile profile;
        final ObjectStoreSummary oss;
        final WebConfig webConfig;

        /**
         * Construct a NodeBuilder
         * @param isSuperUser true if the user is the superuser
         * @param webConfig the web configuration.
         * @param query a PathQuery, for working out possible loops
         * @param classKeys a Map of class keys, for working out if a path has any
         * @param bagManager a BagManager object, for working out if it is possible to
         *                    constrain by bag.
         * @param profile the profile of the current user, for fetching bags from the BagManager
         * @param oss to determine which nodes are null
         * @throws PathException if the path-query is invalid.
         */
        protected NodeBuilder(
                boolean isSuperUser,
                PathQuery query,
                Map<String, List<FieldDescriptor>> classKeys,
                BagManager bagManager,
                Profile profile,
                ObjectStoreSummary oss,
                WebConfig webConfig) throws PathException {
            this.isSuperUser = isSuperUser;
            this.query = query;
            this.model = query.getModel();
            this.classKeys = classKeys;
            this.bagManager = bagManager;
            this.profile = profile;
            this.oss = oss;
            this.webConfig = webConfig;
            this.nodes = new LinkedHashMap<String, MetadataNode>();
            if (!query.isEmpty()) {
                subclasses = query.getSubclasses();
            } else {
                this.subclasses = Collections.emptyMap();
            }
        }

        /** Recurse through the model and build a tree of metadata nodes based on this
         * @param path The starting point for the tree.
         */
        protected void buildTree(String path) {
            String className, subPath;
            if (path.indexOf(".") == -1) {
                className = path;
                subPath = "";
            } else {
                className = path.substring(0, path.indexOf("."));
                subPath = path.substring(path.indexOf(".") + 1);
            }
            List<String> empty = Collections.emptyList();
            nodes.put(className, new MetadataNode(
                    className, empty, query, classKeys, bagManager, profile));

            makeLeaves(model.getClassDescriptorByName(className), subPath, className);
        }

        private void makeLeaves(ClassDescriptor cld, String subPath, String className) {
            makeLeaves(cld, subPath, className, new ArrayList<String>(), null);
        }

        private void makeLeaves(
                ClassDescriptor cld,
                String path,
                String currentPath,
                List<String> structure,
                String reverseFieldName) {
            // null attributes, references and collections
            Set<String> nullAttrs = oss.getNullAttributes(cld.getName());
            Set<String> nullRefsCols = oss.getNullReferencesAndCollections(cld.getName());

            List<FieldDescriptor> filteredNodes = makeFilteredNodes(cld, reverseFieldName);

            Iterator<FieldDescriptor> nodeIter = filteredNodes.iterator();
            while (nodeIter.hasNext()) {
                FieldDescriptor fd = nodeIter.next();
                boolean hasMore = nodeIter.hasNext();
                addNodeAndChildren(cld, path, currentPath, structure,
                        nullAttrs, nullRefsCols, nodeIter, fd, hasMore);
            }
        }

        private void addNodeAndChildren(ClassDescriptor cld, String path,
                String currentPath, List<String> structure,
                Set<String> nullAttrs, Set<String> nullRefsCols,
                Iterator<FieldDescriptor> nodeIter, FieldDescriptor fd,
                boolean hasMore) {
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
            if (hasMore) {
                newStructure.add("tee");
            } else {
                newStructure.add("ell");
            }
            MetadataNode parent = nodes.get(currentPath);

            MetadataNode node = new MetadataNode(
                    parent,
                    fieldName,
                    button,
                    newStructure,
                    query,
                    classKeys, bagManager, profile,
                    (nullAttrs.contains(fieldName) || nullRefsCols.contains(fieldName)));
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
                makeLeaves(refCld, tail, currentPath + "." + head, newStructure, reverse);
            }
        }

        private List<FieldDescriptor> makeFilteredNodes(ClassDescriptor cld,
                String reverseFieldName) {
            List<FieldDescriptor> sortedNodes = new ArrayList<FieldDescriptor>();

            // compare FieldDescriptors by name
            Comparator<FieldDescriptor> cmp = new FieldNameComparator();

            Set<FieldDescriptor> attributeNodes = new TreeSet<FieldDescriptor>(cmp);
            Set<FieldDescriptor> referenceAndCollectionNodes = new TreeSet<FieldDescriptor>(cmp);
            for (FieldDescriptor fd : cld.getAllFieldDescriptors()) {
                FieldConfig fc = FieldConfigHelper.getFieldConfig(webConfig, cld, fd);
                if (fc == null || fc.getShowInQB()) {
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
            return filteredNodes;
        }
    }

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
     * @param webConfig the web configuration.
     * @param query a PathQuery, for working out possible loops
     * @param classKeys a Map of class keys, for working out if a path has any
     * @param bagManager a BagManager object, for working out if it is possible to constrain by bag
     * @param profile the profile of the current user, for fetching bags from the BagManager
     * @param oss to determine which nodes are null
     *
     * @return an ordered Set of nodes
     * @throws PathException if the query is invalid.
     */
    public static Collection<MetadataNode> makeNodes(
            String path,
            Model model,
            boolean isSuperUser,
            PathQuery query,
            WebConfig webConfig,
            Map<String, List<FieldDescriptor>> classKeys,
            BagManager bagManager,
            Profile profile,
            ObjectStoreSummary oss)
        throws PathException {

        NodeBuilder builder = new NodeBuilder(
                isSuperUser, query, classKeys, bagManager, profile, oss, webConfig);

        builder.buildTree(path);

        return builder.nodes.values();
    }

}
