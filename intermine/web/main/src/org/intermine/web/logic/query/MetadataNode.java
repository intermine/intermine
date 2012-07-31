package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.api.bag.BagManager;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.pathquery.Node;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;

/**
 * Node used in displaying metadata
 * @author Mark Woodbridge
 */
public class MetadataNode extends Node
{
    String button;
    boolean selected = false;
    List<String> structure;
    boolean hasSubclasses = false;
    String origType = null;
    boolean isReverseReference = false;
    PathQuery query;
    Map<String, List<FieldDescriptor>> classKeys;
    BagManager bagManager;
    Profile profile;
	private Boolean isNullNode = false;

    /**
     * Constructor for a root node
     * @param type the root type of this tree
     * @param structure a List of Strings - for definition, see getStructure()
     * @param query a PathQuery, for working out possible loops
     * @param classKeys a Map of class keys, for working out if a path has any
     * @param bagManager a BagManager object, for working out if it is possible to constrain by bag
     * @param profile the profile of the current user, for fetching bags from the BagManager
     */
    public MetadataNode(String type, List<String> structure, PathQuery query,
            Map<String, List<FieldDescriptor>> classKeys, BagManager bagManager, Profile profile) {
        super(type);
        button = " ";
        this.structure = structure;
        this.hasSubclasses = false;
        this.query = query;
        this.classKeys = classKeys;
        this.bagManager = bagManager;
        this.profile = profile;
    }

    /**
     * Constructor for a non-root node
     * @param parent the parent node of this node
     * @param fieldName the name of the field that this node represents
     * @param button the button displayed next to this node's name
     * @param structure a List of Strings - for definition, see getStructure()
     * @param query a PathQuery, for working out possible loops
     * @param classKeys a Map of class keys, for working out if a path has any
     * @param bagManager a BagManager object, for working out if it is possible to constrain by bag
     * @param profile the profile of the current user, for fetching bags from the BagManager
     * @param is the field null or empty? (OSS determined)
     */
    public MetadataNode(MetadataNode parent, String fieldName, String button,
            List<String> structure, PathQuery query, Map<String, List<FieldDescriptor>> classKeys,
            BagManager bagManager, Profile profile, Boolean isNull) {
    	
        super(parent, fieldName, false);
        this.button = button;
        this.structure = structure;
        this.query = query;
        this.classKeys = classKeys;
        this.bagManager = bagManager;
        this.profile = profile;
        this.isNullNode = isNull;
    }

    @Override
    public void setModel(Model model) {
        super.setModel(model);
        ClassDescriptor cld = model.getClassDescriptorByName(getType());
        if (cld == null) {
            hasSubclasses = false;
        } else {
            hasSubclasses = !(cld.getSubDescriptors().isEmpty());
        }
        Node parent = getParent();
        if (parent != null) {
            Node parentParent = parent.getParent();
            if (parentParent != null) {
                ClassDescriptor ppCld = model.getClassDescriptorByName(parentParent.getType());
                FieldDescriptor pFd = ppCld.getFieldDescriptorByName(parent.getFieldName());
                if (pFd instanceof AttributeDescriptor) {
                    throw new Error("Cannot have an attribute as a parent node");
                }
                ReferenceDescriptor pRd = (ReferenceDescriptor) pFd;
                String reverseFieldName = pRd.getReverseReferenceFieldName();
                if (getFieldName().equals(reverseFieldName)) {
                    isReverseReference = true;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(String type) {
        if (origType == null) {
            origType = getType();
        }
        super.setType(type);
    }

    /**
     * Gets the value of button
     *
     * @return the value of button
     */
    public String getButton()  {
        return button;
    }

    /**
     * Get the value of selected
     * @return the value of selected as a boolean
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the value of selected
     * @param selected a boolean
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Returns the structure of the tree. This is an array of Strings, which are one of four
     * possible strings, in order to draw a tree structure:
     * <UL><LI><B>blank</B> - Do not draw any lines.</LI>
     *     <LI><B>straight</B> - Draw a straight vertical line.</LI>
     *     <LI><B>ell</B> - Draw an L-shaped line.</LI>
     *     <LI><B>tee</B> - Draw a T-junction, with lines going vertically and to the right.</LI>
     * </UL>
     * If the sequence of these is correctly drawn in front of each tree node, then a tree structure
     * will be drawn correctly.
     *
     * @return a List of Strings
     */
    public List<String> getStructure() {
        return structure;
    }

    /**
     * Returns the type of this node if it did not have a subclass constraint.
     *
     * @return a String type name
     */
    public String getOrigType() {
        return origType;
    }

    /**
     * Returns true if the original type has subclasses in the model.
     *
     * @return a boolean
     */
    public boolean getHasSubclasses() {
        return hasSubclasses;
    }

    /**
     * Returns true if this path is a reverse reference back onto the previous node.
     *
     * @return a boolean
     */
    public boolean isReverseReference() {
        return isReverseReference;
    }

    /**
     * Returns true if this path in the query has possible loop constraints that are not already in
     * the query.
     *
     * @return a boolean
     * @throws PathException if the query is invalid
     */
    public boolean getHasPossibleLoops() throws PathException {
        if (isAttribute()) {
            return false;
        } else {
            return !query.getCandidateLoops(getPathString()).isEmpty();
        }
    }

    /**
     * Returns true if the type of this path has class keys configured.
     *
     * @return a boolean
     */
    public boolean getHasClassKeys() {
        if (isAttribute()) {
            return false;
        } else {
            return ClassKeyHelper.hasKeyFields(classKeys, getType());
        }
    }

    /**
     * Returns true if there are any available bags for the type of this path.
     *
     * @return a boolean
     */
    public boolean getHasAvailableBags() {
        if (isAttribute()) {
            return false;
        } else {
            return !bagManager.getUserOrGlobalBagsOfType(profile, getType()).isEmpty();
        }
    }

    /**
     * Returns true if there is any possibility of creating a constraint on this path.
     *
     * @return a boolean
     * @throws PathException if the query is invalid
     */
    public boolean getCanCreateConstraint() throws PathException {
        return isAttribute() || getHasSubclasses() || getHasPossibleLoops() || getHasClassKeys()
            || getHasAvailableBags();
    }

    /**
     * 
     * @return a boolean is this Node is null or empty as determined by OSS
     */
    public boolean getIsNull() {
    	return isNullNode;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString() + ", structure: " + structure;
    }
}
