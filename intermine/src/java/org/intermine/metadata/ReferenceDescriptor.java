package org.flymine.metadata;

import org.flymine.util.Util;

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
     * @param primaryKey true if field is part of the class' primary key
     * @param referencedType fully qualfied class name of another business object
     * @param reverseRefName name of the field in remote object that refers back to this one
     * @throws IllegalArgumentException if fields are null
     */
    public ReferenceDescriptor(String name, boolean primaryKey, String referencedType,
                               String reverseRefName)
        throws IllegalArgumentException {
        super(name, primaryKey);
        if (referencedType == null || referencedType == "") {
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
    public ClassDescriptor getReferencedClassDescriptor() throws IllegalStateException {
        if (!modelSet) {
            throw new IllegalStateException("This ReferenceDescriptor (" + getName()
                                            + ") is not yet part of a metadata Model");
        }
        return referencedClassDesc;
    }

    /**
     * Gets the field in the referenced object that refers back to this class.
     * Note that this will be null in a unidirectional relationship,
     * a ReferenceDescriptor in a 1:1 and a CollectionDescriptor in a 1:N.
     * @return a FieldDescriptor referring back to this class.
     * @throws IllegalStateException if model has not been set
     */
    public ReferenceDescriptor getReverseReferenceDescriptor() throws IllegalStateException {
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
                                            + referencedType + "' in model");
        }

        // find ReferenceDescriptor for the reverse reference
        if (reverseRefName != null && reverseRefName != "") {
            reverseRefDesc = referencedClassDesc
                .getReferenceDescriptorByName(reverseRefName);
            if (reverseRefDesc == null) {
                reverseRefDesc = referencedClassDesc
                    .getCollectionDescriptorByName(reverseRefName);
            }
           if (reverseRefDesc == null) {
                throw new MetaDataException("Unable to find named reverse reference '"
                                            + reverseRefName + "' in class "
                                            + referencedClassDesc.getClassName());
            }
        }
        modelSet = true;
    }

    /**
     * @see FieldDescriptor#relationType
     */
    public int relationType() {
        if (getReverseReferenceDescriptor() instanceof CollectionDescriptor) {
            return N_ONE_RELATION;
        } else {
            return ONE_ONE_RELATION;
        } 
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof ReferenceDescriptor) {
            ReferenceDescriptor ref = (ReferenceDescriptor) obj;
            return name.equals(ref.name)
                && primaryKey == ref.primaryKey
                && referencedType.equals(ref.referencedType)
                && Util.equals(reverseRefName, ref.reverseRefName);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 3 * name.hashCode()
            + 5 * (primaryKey ? 1 : 0)
            + 7 * referencedType.hashCode()
            + 11 * Util.hashCode(reverseRefName);
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<reference name=\"" + name + "\" referenced-type=\"" + referencedType + "\"")
            .append(reverseRefName != null ? " reverse-reference=\"" + reverseRefName + "\"" : "")
            .append(" primary-key=\"" + primaryKey + "\"/>");
        return sb.toString();
    }
}
