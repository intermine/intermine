package org.flymine.metadata;

/**
 * Describes a field that references a collection of other objects.
 * getReverseReferenceDescriptor()
 * allows one ot work out the multiplicity of the association's other end.
 *
 * @author Richard Smith
 */

public class CollectionDescriptor extends ReferenceDescriptor
{

    protected boolean ordered;
    private boolean setReverseRef = false;
    protected Class collectionClass;

    /**
     * Construct a CollectionDescriptor.  name and referencedType may not be null.
     * @param name name of this field in parent class
     * @param primaryKey is this field part of the primary key
     * @param referencedType the fully qualified name of the business object type in this collection
     * @param reverseRefName name of field in the referenced class that points back to this class
     *                       (may be null)
     * @param ordered true if the collection ordered
     * @throws IllegalArgumentException if arguments are null
     */
    public CollectionDescriptor(String name, boolean primaryKey, String referencedType,
                                   String reverseRefName, boolean ordered) {
        // should define type of collection properly somehow
        super(name, primaryKey, referencedType, reverseRefName);
        this.ordered = ordered;
        if (ordered) {
            collectionClass = java.util.ArrayList.class;
        } else {
            collectionClass = java.util.HashSet.class;
        }
    }

    /**
     * Returns a ClassDescriptor for the type of object contained in this collection.
     * @return ClassDescriptor for the type of object contained within the collection
     * @throws IllegalStateException if the model is not yet set
     */
    public ClassDescriptor getReferencedClassDescriptor() throws IllegalStateException {
        return super.getReferencedClassDescriptor();
    }

    /**
     * Gets the field in the referenced object (i.e. objects in the collection) that
     * refers back to this class.  Note that this will be null in a unidirectional
     * relationship, a ReferenceDescriptor in a N:1 and a CollectionDescriptor in a M:N.
     * @return a FieldDescriptor referring back to this class.
     * @throws IllegalStateException if the model is not yet set
     */
    public ReferenceDescriptor getReverseReferenceDescriptor() throws IllegalStateException {
        return super.getReverseReferenceDescriptor();
    }


    /**
     * Get the class of collection this field represents
     * @return the type of collection
     */
    public Class getCollectionClass() {
        return this.collectionClass;
    }

    /**
     * True if the collection is ordered.
     * @return true if the collection is ordered
     */
    public boolean isOrdered() {
        return this.isOrdered();
    }

   /**
    * @see Object#toString
    */
    public String toString() {
        return "<collection name=\"" + name + "\" referenced-type=\"" + refName + "\" ordered=\"" 
            + ordered + "\" reverseReference=\"" + reverseRefName + "\" primarykey=\"" 
            + primaryKey + "\"/>";
    }
}
