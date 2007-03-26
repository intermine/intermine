package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;

import antlr.collections.AST;

/**
 * ItemPaths provide an easy way to load single or multiple items from an ItemReader given a
 * starting Item and destination (the path).
 * 
 * <h3>Basic paths</h3>
 * 
 * Examples of path expressions:
 * <pre>
 *   Company.ceo.address
 *   Employee.company.departments
 * </pre>
 * The path should end with a reference to an object or a collection. It should not end with an
 * attribute name.
 * 
 * <h3>Constraints</h3>
 * 
 * You can also add constraints at points in the path. You can constrain an attribute to be a
 * certain value. Often you will want to put a constraint on a collection in order to pick
 * a single element to traverse through. For example:
 * <pre>
 *   Company.departments[name='DeptA'].employees
 * </pre>
 * You can add multiple constraints at a single point with the && operator:
 * <pre>
 *   Company.departments[name='DeptA' && size='57'].employees
 * </pre>
 * As well as constraining the immediate attributes you can constraint a attribute of an object
 * at the end of some sub-path, for example:
 * <pre>
 *   Company.departments[manager.name='Bob'].employees
 * </pre>
 * 
 * <h3>Variables in constraints</h3>
 * 
 * As well as having fixed values operands for constraints, you can make an operand a variable.
 * The actual values used for traversal are provided when the path is used (see ItemReader).
 * A variable constraint is used by inserting $n in the path for a constraint operand. n is the
 * variable index. Variable indices should start from zero and increase consecutively. You may
 * reuse the same index twice to repeat the same variable. For example:
 * <pre>
 *   Company.departments[manager.name=$0].employees
 *   Employee.company[address.postcode=$0].departments[name=$1]
 * </pre>
 * 
 * @see org.intermine.dataconversion.ItemReader
 * @author Thomas Riley
 */
public class ItemPath
{
    private static final Logger LOG = Logger.getLogger(ItemPath.class);
    
    /** The abstract syntax tree produced by the parser. */
    private AST ast;
    /** Namespace. */
    private String namespace;
    /** Map from IPD to Set of FieldNameAndValue constraints (simple immediate field constraints) */
    private Map constraints = new HashMap();
    /** Map from IPD to Set of IPDs that represent sub-path-field constraints. */
    private Map subPathConstraints = new HashMap();
    /** Class of root object in path. */
    private String startType;
    /** Root ItemPrefetchDescriptor. */
    private ItemPrefetchDescriptor ipd;
    
    /**
     * Construct a new instance of ItemPath for a given path expression.
     * 
     * @param path the path expression
     * @param namespace the namespace that this path
     */
    public ItemPath(String path, String namespace) {
        this.namespace = namespace;
        
        try {
            InputStream is = new ByteArrayInputStream(path.getBytes());
            ItemPathLexer lexer = new ItemPathLexer(is);
            ItemPathParser parser = new ItemPathParser(lexer);
            // The root context
            parser.expr();
            ast = parser.getAST();
            computePrefetchDescriptors();
            
            if (ast == null || getItemPrefetchDescriptor() == null) {
                throw new IllegalArgumentException("Invalid path " + path);
            }
        } catch (antlr.RecognitionException e) {
            //new antlr.DumpASTVisitor().visit(ast);
            IllegalArgumentException e2 = new IllegalArgumentException(e.getMessage());
            e2.initCause(e);
            throw e2;
        } catch (antlr.TokenStreamException e) {
            //new antlr.DumpASTVisitor().visit(ast);
            IllegalArgumentException e2 = new IllegalArgumentException(e.getMessage());
            e2.initCause(e);
            throw e2;
        } catch (IllegalArgumentException e) {
            //new antlr.DumpASTVisitor().visit(ast);
            throw e;
        }
    }
    
    /**
     * Walk over the AST building ItemPrefetchDescriptors and parsing constraints.
     */
    protected void computePrefetchDescriptors() {
        processASTExpression(ast, null);
    }
    
    private ItemPrefetchDescriptor processASTExpression(AST expression,
                                                        ItemPrefetchDescriptor parent) {
        AST type = expression.getFirstChild();
        AST fieldPath = type.getNextSibling();
        
        parent = processASTType(type, parent);
        return processASTFieldPath(fieldPath, parent);
    }
    
    private ItemPrefetchDescriptor processASTType(AST type, ItemPrefetchDescriptor parent) {
        AST child = type.getFirstChild();
        // identifier or revref
        
        // if identifier, process identifier as class type
        switch (child.getType()) {
            case ItemPathTokenTypes.IDENTIFIER:
                if (parent == null) {
                    setStartType(child.getText());
                }
                return null;
            case ItemPathTokenTypes.AST_REVREF:
                return processASTRevRef(child, parent);
            default:
                throw new IllegalArgumentException("Unknown AST node: " + type.getText() + " ["
                        + type.getType() + "]");
        }
    }
    
    private ItemPrefetchDescriptor processASTFieldPath(AST type, ItemPrefetchDescriptor parent) {
        // step through field identifiers calling processASTIdentifier
        AST child = type.getFirstChild();
        
        while (child != null) {
            parent = processASTField(child, parent);
            child = child.getNextSibling();
        }
        
        return parent;
    }
    
    private ItemPrefetchDescriptor processASTField(AST type, ItemPrefetchDescriptor parent) {
        // field might be followed by constraint
        AST identifier = type.getFirstChild();
        AST constraint = identifier.getNextSibling();
        parent = processASTIdentifier(identifier, parent);
        if (constraint != null) {
            processASTConstraint(constraint, parent);
        }
        return parent;
    }
    
    private void processASTConstraint(AST constraint, ItemPrefetchDescriptor collection) {
        // field might be followed by constraint
        AST field = constraint.getFirstChild();
        while (field != null) {
            AST value = field.getNextSibling();
            if (field.getType() == ItemPathTokenTypes.IDENTIFIER) {
                String cvalue = value.getText();
                if (cvalue.charAt(0) == '\'') {
                    cvalue = cvalue.substring(1, cvalue.length() - 1);
                    recordConstraint(collection, field.getText(), cvalue);
                } else {
                    recordConstraint(collection, field.getText(),
                            Integer.parseInt(cvalue.substring(1)));
                }
            } else {
                //setStartType("<<CONTEXT>>"); // we don't know the start type
                // ipd will be set on first call to processASTIdentifier
                ItemPrefetchDescriptor root = processASTSubPath(field, value);
                recordConstraint(collection, root);
            }
            field = value.getNextSibling();
        }
    }
    
    /**
     * @return root IPD for the subpath.
     */
    private ItemPrefetchDescriptor processASTSubPath(AST type, AST value) {
        // step through field identifiers calling processASTIdentifier
        AST child = type.getFirstChild();
        ItemPrefetchDescriptor parent = null;
        ItemPrefetchDescriptor root = null;
        
        // avoid processing field name at the end
        while (child.getNextSibling() != null) {
            parent = processASTIdentifier(child, parent);
            if (root == null) {
                root = parent;
            }
            child = child.getNextSibling();
        }
        
        // record actual field constraint on last IPD
        String cvalue = value.getText();
        if (cvalue.charAt(0) == '\'') {
            cvalue = cvalue.substring(1, cvalue.length() - 1);
            recordConstraint(parent, child.getText(), cvalue);
        } else {
            recordConstraint(parent, child.getText(), Integer.parseInt(cvalue.substring(1)));
        }
        
        return root;
    }
    
    private ItemPrefetchDescriptor processASTRevRef(AST revref, ItemPrefetchDescriptor parent) {
        AST expr = revref.getFirstChild();
        AST typeFieldPath = expr.getNextSibling();
        
        parent = processASTExpression(expr, parent);
        return processASTTypeFieldPath(typeFieldPath, parent);
    }
    
    private ItemPrefetchDescriptor processASTTypeFieldPath(AST typeFieldPath,
                                                           ItemPrefetchDescriptor parent) {
        AST type = typeFieldPath.getFirstChild();
        AST field = type.getNextSibling();
     
        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor("("
                + (parent != null ? parent.getDisplayName() : getStartType()) + " <- "
                + type.getText() + "." + field.getText() + ")");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, field.getText()));
        desc2.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    namespace + type.getText(), false));
        if (parent != null) {
            parent.addPath(desc2);
        } else {
            // This means that this is this descriptor is the root
            // descriptor and the path started with something like:
            // (gene <- transcript.gene) and not
            // (some.thing <- transcript.gene)
            setItemPrefetchDescriptor(desc2);
        }
        return desc2;
    }
    
    private ItemPrefetchDescriptor processASTIdentifier(AST identifier,
                                                        ItemPrefetchDescriptor parent) {
        ItemPrefetchDescriptor desc
                = new ItemPrefetchDescriptor((parent != null ? parent.getDisplayName()
                        : getStartType()) + "." + identifier.getText());
        desc.addConstraint(new ItemPrefetchConstraintDynamic(identifier.getText(),
                                            ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        // avoid recording ipd for subpaths
        if (parent == null && ipd == null) {
            setItemPrefetchDescriptor(desc); // This is the root descriptor
        } else if (parent != null) {
            parent.addPath(desc);
        }
        return desc;
    }
    
    /**
     * Record a new field value constraint for a given descriptor.
     * 
     * @param collection the prefetch descriptor
     * @param attrib the name of attribute to constrain
     * @param value the value for constraining the attribute
     */
    protected void recordConstraint(ItemPrefetchDescriptor collection,
            String attrib, String value) {
        LOG.debug("recordConstraint variable value " + value + " on ipd "
                + collection.getDisplayName());
        Set set = (Set) MapUtils.getObject(constraints, collection, new HashSet());
        set.add(new Constraint(attrib, value));
        constraints.put(collection, set);
    }
    
    /**
     * Record a new field value constraint for a given descriptor that has a variable
     * operand.
     * 
     * @param collection the prefetch descriptor
     * @param attrib the name of attribute to constrain
     * @param index the variable index
     */
    protected void recordConstraint(ItemPrefetchDescriptor collection,
            String attrib, int index) {
        LOG.debug("recordConstraint variable index " + index + " on ipd "
                + collection.getDisplayName());
        Set set = (Set) MapUtils.getObject(constraints, collection, new HashSet());
        set.add(new Constraint(attrib, index));
        constraints.put(collection, set);
    }
    
    /**
     * Record a subpath constraint at a given point in the main path.
     * 
     * @param parent the point in the main path that the constraint applies
     * @param constraint the constrant in the form of a path that should return items
     */
    protected void recordConstraint(ItemPrefetchDescriptor parent,
            ItemPrefetchDescriptor constraint) {
        LOG.debug("recordConstraint IPD " + constraint.getDisplayName() + " on ipd "
                + parent.getDisplayName());
        Set set = (Set) MapUtils.getObject(subPathConstraints, parent, new HashSet());
        set.add(constraint);
        subPathConstraints.put(parent, set);
    }
    
    /**
     * For the prefetch descriptor that will load a collection of objects, get the extra, simple
     * field constraints to impose. These are expressed in the path as:
     * <pre>
     * Type.path.to.ref[field='value']
     * </pre>
     * whereas
     * <pre>
     * Type.path.to.ref[sub.path.to.ref.field='value']
     * </pre>
     * ... stored as IPDs (with at least one field value constraint) and are accessed via
     * getSubItemPathConstraints.
     * 
     * @param collection item prefetch descriptor
     * @param values the variable values to use
     * @return Set of FieldNameAndValue constraints or an empty set
     */
    public Set getFieldValueConstrainsts(ItemPrefetchDescriptor collection, Object values[]) {
        if (values == null) {
            throw new IllegalArgumentException("values == null");
        }
        Set cs = (Set) constraints.get(collection);
        if (cs == null) {
            return new HashSet();
        }
        HashSet favc = new HashSet();
        for (Iterator iter = cs.iterator(); iter.hasNext(); ) {
            Constraint c = (Constraint) iter.next();
            favc.add(c.getFieldNameAndValue(values));
        }
        return favc;
    }
    
    /**
     * For a given step along the main, path get the PIDs that represent constraints on the
     * items collected at this point.
     * 
     * @param descriptor the descriptor from the main path
     * @return Set of ItemPrefetchDescriptors that constraint items at this point
     */
    public Set getSubItemPathConstraints(ItemPrefetchDescriptor descriptor) {
        // descriptor may load a single item or a collection
        return (Set) MapUtils.getObject(subPathConstraints, descriptor, new HashSet());
    }
    
    /**
     * Get the ItemPrefetchDescriptors for this path.
     * 
     * @return an ItemPrefetchDescriptor for this path
     */
    public ItemPrefetchDescriptor getItemPrefetchDescriptor() {
        return ipd;
    }
    
    /**
     * Set the root ItemPrefetchDescriptor for the current context.
     * 
     * @param ipd root ItemPrefetchDescriptor for current context
     */
    private void setItemPrefetchDescriptor(ItemPrefetchDescriptor ipd) {
        if (this.ipd != null) {
            throw new IllegalStateException("ipd != null");
        }
        this.ipd = ipd;
        LOG.debug("set root IPD " + ipd.getDisplayName());
    }
    
    /**
     * Get the starting class that this path starts at.
     * 
     * @return starting point of this path
     */
    public String getStartType() {
        return startType;
    }
    
    /**
     * Set the startType of the current context.
     * 
     * @param startType class type
     */
    private void setStartType(String startType) {
        if (this.startType != null) {
            throw new IllegalStateException("startType != null");
        }
        this.startType = startType;
        LOG.debug("found start type " + startType);
    }
    
    /**
     * Get the namespace that this path operates within.
     * 
     * @return namespace of the path
     */
    public String getNamespace() {
        return namespace;
    }
    
    /**
     * Test two ItemPaths for equality.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ItemPath)) {
            return false;
        }
        return (ObjectUtils.equals(getItemPrefetchDescriptor(),
                    ((ItemPath) obj).getItemPrefetchDescriptor())
                && getStartType().equals(((ItemPath) obj).getStartType())
                && namespace.equals(((ItemPath) obj).namespace));
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getItemPrefetchDescriptor().hashCode() + 3 * namespace.hashCode();
    }
    
    /**
     * Represents a FieldNameAndValue constraint. The operand value of the constraint might
     * be a variable so the actual FieldNameAndValue is created when variable values are
     * provided on a call to getFieldValueConstraints.
     */
    protected class Constraint
    {
        private String attrib;
        private String value;
        private int index = -1;
        
        /**
         * Create a constraint with a fixed operand.
         * 
         * @param attrib the attribute name
         * @param value the value for the constraint
         */
        public Constraint(String attrib, String value) {
            this.attrib = attrib;
            this.value = value;
            if (attrib == null) {
                throw new NullPointerException("attrib");
            }
            if (value == null) {
                throw new NullPointerException("value");
            }
        }
        
        /**
         * Create a constraint with a variable operand.
         * 
         * @param attrib the attribute name
         * @param varIndex the index of the variable as specified in the path expression ($0 etc)
         */
        public Constraint(String attrib, int varIndex) {
            this(attrib, "");
            index = varIndex;
            if (index < 0) {
                throw new IllegalArgumentException("varIndex");
            }
        }
        
        /**
         * Get the actual FieldNameAndValue item reader constraint.
         * 
         * @param variables set of variables being used for this path traversal
         * @return FieldNameAndValue constraint
         */
        FieldNameAndValue getFieldNameAndValue(Object variables[]) {
            if (index >= 0) {
                if (index >= variables.length) {
                    throw new IllegalArgumentException("too few variables passed to "
                            + "getFieldNameAndValue. Need variable $" + index + " but only "
                            + variables.length + " variables passed");
                }
                Object variable = variables[index];
                if (variable == null) {
                    throw new NullPointerException("variable " + index + " is null");
                }
                return new FieldNameAndValue(attrib, variable.toString(), false);
            } else {
                return new FieldNameAndValue(attrib, value, false);
            }
            
        }
    }
}
