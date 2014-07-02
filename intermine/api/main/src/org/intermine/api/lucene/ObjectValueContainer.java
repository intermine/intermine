package org.intermine.api.lucene;

/**
 * container class to hold the name and value of an attribute for an object to
 * be added as a field to the document
 * @author nils
 */
class ObjectValueContainer
{
    final String className;
    final String name;
    final String value;

    /**
     * constructor
     * @param className
     *            name of the class the attribute belongs to
     * @param name
     *            name of the field
     * @param value
     *            value of the field
     */
    public ObjectValueContainer(String className, String name, String value) {
        super();
        this.className = className;
        this.name = name;
        this.value = value;
    }

    /**
     * className
     * @return className
     */
    public String getClassName() {
        return className;
    }

    /**
     * name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * value
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * generate the name to be used as a field name in lucene
     * @return lowercase classname and field name
     */
    public String getLuceneName() {
        return (className + "_" + name).toLowerCase();
    }
}