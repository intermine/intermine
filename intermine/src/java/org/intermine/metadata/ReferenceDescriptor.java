package org.flymine.metadata;


/**
 * Describes a field that references a single other class (i.e. not a collection
 * of objects).  getReverseReferenceDescriptor() allows one to work out the multiplicity
 * of the association's other end.
 *
 * @author Richard Smith
 */

public class ReferenceDescriptor extends FieldDescriptor
{

    protected ReferenceDescriptor reverseRef; // can be a reference, collection or null
    protected String reverseRefName;
    protected String refName;
    protected ClassDescriptor refClassDescriptor;
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
        this.refName = referencedType;
    }


    /**
     * Returns a ClassDescriptor for the object referenced by this field.
     * @return ClassDescriptor for the referenced object
     * @throws IllegalStateException if model has not been set
     */
    public ClassDescriptor getReferencedClassDescriptor() throws IllegalStateException {
        if (!modelSet) {
            throw new IllegalStateException("This ReferenceDescriptor (" + this.getName()
                                            + ") is not yet part of a metadata Model");
        }
        return this.refClassDescriptor;
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
            throw new IllegalStateException("This ReferenceDescriptor (" + this.getName()
                                            + ") is not yet part of a metadata Model");
        }
        return this.reverseRef;
    }

    /**
     * sort out references from this class
     * @throws MetaDataException if references not found
     */
    protected void findReferencedDescriptor() throws MetaDataException {

        // find ClassDescriptor for referenced class
        if (this.cld.getModel().hasDescriptorFor(refName)) {
            this.refClassDescriptor = this.cld.getModel().getDescriptorByName(refName);

        } else {
            throw new MetaDataException("Unable to find ClassDescriptor for: "
                                            + refName + " in model.");
        }

        // find ReferenceDescriptor for the reverse reference
        if (reverseRefName != null && reverseRefName != "") {
            this.reverseRef = this.refClassDescriptor
                .getReferenceDescriptorByName(reverseRefName);
            if (reverseRef == null) {
                throw new MetaDataException("Unable to find named reverse reference ("
                                            + reverseRefName + ") in class ("
                                            + this.cld.getClassName() + ").");
            }
        }

        modelSet = true;
    }

}
