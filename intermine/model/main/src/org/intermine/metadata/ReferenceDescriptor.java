package org.intermine.metadata;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;

/**
 * Describes a field that references a single other class (i.e. not a collection
 * of objects).  getReverseReferenceDescriptor() allows one to work out the multiplicity
 * of the association's other end.
 *
 * @author Richard Smith
 */
public class ReferenceDescriptor extends FieldDescriptor
{
    protected final String referencedType;
    protected ClassDescriptor referencedClassDesc;
    protected final String reverseRefName; // can be reference, collection or null
    protected ReferenceDescriptor reverseRefDesc;
    private boolean modelSet = false;

    /**
     * Construct a ReferenceDescriptor.  Requires the name of Class referenced and
     * the field in the referenced class that refers back to this (will be null in
     * a unidirectional relationship).
     * @param name name of the field
     * @param referencedType fully qualfied class name of another business object
     * @param reverseRefName name of the field in remote object that refers back to this one
     * @throws IllegalArgumentException if fields are null
     */
    public ReferenceDescriptor(String name, String referencedType, String reverseRefName) {
        super(name);
        if (referencedType == null || "".equals(referencedType)) {
            throw new IllegalArgumentException("A value must be provided for "
                    + "the referenced type");
        }
        this.reverseRefName = reverseRefName;
        this.referencedType = referencedType;
    }

    /**
     * Returns a ClassDescriptor for the object referenced by this field.
     * @return ClassDescriptor for the referenced object
     * @throws IllegalStateException if model has not been set
     */
    public ClassDescriptor getReferencedClassDescriptor() {
        if (!modelSet) {
            throw new IllegalStateException("This ReferenceDescriptor (" + getName()
                    + ") is not yet part of a metadata Model");
        }
        return referencedClassDesc;
    }

    /**
     * Gets the name of the reverse reference field.
     * @return the name of the reverse reference field
     */
    public String getReverseReferenceFieldName() {
        return reverseRefName;
    }

    /**
     * Returns the class name of the object referenced by this field.
     * @return the class name of the object referenced
     */
    public String getReferencedClassName() {
        return referencedType;
    }

    /**
     * Gets the field in the referenced object that refers back to this class.
     * Note that this will be null in a unidirectional relationship,
     * a ReferenceDescriptor in a 1:1 and a CollectionDescriptor in a 1:N.
     * @return a FieldDescriptor referring back to this class.
     * @throws IllegalStateException if model has not been set
     */
    public ReferenceDescriptor getReverseReferenceDescriptor() {
        if (!modelSet) {
            throw new IllegalStateException("This ReferenceDescriptor (" + getName()
                    + ") is not yet part of a metadata Model");
        }
        return reverseRefDesc;
    }

    /**
     * sort out references from this class
     * @throws MetaDataException if references not found
     */
    protected void findReferencedDescriptor() throws MetaDataException {
        // find ClassDescriptor for referenced class
        if (cld.getModel().hasClassDescriptor(referencedType)) {
            referencedClassDesc = cld.getModel().getClassDescriptorByName(referencedType);

        } else {
            throw new MetaDataException("Unable to find ClassDescriptor for '"
                    + referencedType + "' in model while processing: " + cld.getName() + "."
                    + name);
        }

        // find ReferenceDescriptor for the reverse reference
        if (!StringUtils.isBlank(reverseRefName)) {
            reverseRefDesc = referencedClassDesc.getReferenceDescriptorByName(reverseRefName);
            if (reverseRefDesc == null) {
                reverseRefDesc = referencedClassDesc
                    .getCollectionDescriptorByName(reverseRefName);
            }
            if (reverseRefDesc == null) {
                throw new MetaDataException("Unable to find named reverse reference '"
                        + reverseRefName + "' in class " + referencedClassDesc.getName()
                        + " while processing: " + getClassDescriptor().getName() + "." + getName());
            }
            // Reverse references need to point to one another and have correct types
            // NOTE these checks were added when models already exist that fail them, so we can't
            // throw a MetaDataException, instead add problems to the model that are checked by
            // the ModelMerger when creating new models.
            if (reverseRefDesc.getReverseReferenceFieldName() != null) {
                if (!reverseRefDesc.getReverseReferenceFieldName().equals(this.name)) {
                    modelSet = true;
                    throw new NonFatalMetaDataException("Reverse reference names do not match: "
                            + reverseRefDesc.getReverseReferenceFieldName() + " and " + this.name
                            + " (" + this.cld.getName() + ": " + this.toString() + ", "
                            + referencedType + ": " + reverseRefDesc.toString() + ")");
                }
                if (!reverseRefDesc.referencedType.equals(this.cld.getName())) {
                    modelSet = true;
                    throw new NonFatalMetaDataException("Reverse reference types do not match: "
                            + this.cld.getName() + ": " + this.toString() + ", "
                            + referencedType + ": " + reverseRefDesc.toString());
                }
            }
        }
        modelSet = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int relationType() {
        ReferenceDescriptor rd = getReverseReferenceDescriptor();
        if ((rd == null) || (rd instanceof CollectionDescriptor)) {
            return N_ONE_RELATION;
        } else {
            return ONE_ONE_RELATION;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReferenceDescriptor) {
            ReferenceDescriptor ref = (ReferenceDescriptor) obj;
            return name.equals(ref.name)
                && referencedType.equals(ref.referencedType)
                && Util.equals(reverseRefName, ref.reverseRefName);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 3 * name.hashCode()
            + 7 * referencedType.hashCode()
            + 11 * Util.hashCode(reverseRefName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<reference name=\"" + name + "\" referenced-type=\""
                + referencedType.substring(referencedType.lastIndexOf(".") + 1) + "\"")
            .append(reverseRefName != null ? " reverse-reference=\"" + reverseRefName + "\"" : "")
            .append("/>");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toJSONString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{\"name\":\"" + name + "\","
            + "\"referencedType\":\""
            + referencedType.substring(referencedType.lastIndexOf(".") + 1) + "\"");
        if (reverseRefName != null) {
            sb.append(",\"reverseReference\":\"" + reverseRefName + "\"");
        }
        sb.append("}");
        return sb.toString();
    }
}
