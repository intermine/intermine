package org.flymine.metadata;

/**
 * Abstract representation of a field within a class - could be an attribute, an
 * object reference or a collection.
 *
 * @author Richard Smith
 */

public abstract class FieldDescriptor
{

    protected String name; // name of field
    protected boolean primaryKey;
    protected ClassDescriptor cld; // parent class of this field
    private boolean cldSet = false;


    /**
     * Construct, name of field must not be null
     * @param name name of field in class
     * @param primaryKey true if part of the class' primary key
     * @throws IllegalArgumentException if name argument is null
     */
    protected FieldDescriptor(String name, boolean primaryKey)
        throws IllegalArgumentException {

        if (name == null || name == "") {
            throw new IllegalArgumentException("name (" + name + ") parameter cannot be null");
        }
        this.name = name;
        this.primaryKey = primaryKey;
    }


    /**
     * Set the parent ClassDescriptor - should be called when this is added to
     * a ClassDescriptor.  ClassDescriptor in this class is final so a MetadataException
     * is thrown if method called again.
     * @param cld the parent ClassDescriptor
     * @throws IllegalStateException if the parent ClassDescriptor is not set
     */
     public void setClassDescriptor(ClassDescriptor cld) throws IllegalStateException {
         if (cldSet) {
            throw new IllegalStateException("ClassDescriptor has already been set and "
                                            + "may not be changed.");
        }
        this.cld = cld;
        cldSet = true;
     }

    /**
     * Get the ClassDescriptor for this field's class.
     * @return a the ClassDescriptor for this field's class
     */
    public ClassDescriptor getClassDescriptor() {
        return this.cld;
    }

    /**
     * Get the name of the described field.
     * @return name of the field
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns true if this fields makes up part of the Class' primary key
     * @return true if part of primary key
     */
    public boolean isPrimaryKey() {
        return this.primaryKey;
    }

}
