package org.flymine.sql.query;

/**
 * A representation of a field in a table.
 *
 * @author Andrew Varley
 */
public class Field extends AbstractValue
{
    protected String name;
    protected Table table;

    /**
     * Constructor for this Field object.
     *
     * @param name the name of the field, as the database knows it
     * @param table the name of the table to which the field belongs, as the database knows it
     * @param alias an arbitrary name that the field has been renamed to for the rest of the query
     */
    public Field(String name, Table table, String alias) {
        if (name == null) {
            throw (new NullPointerException("Field names cannot be null"));
        }
        if (table == null) {
            throw (new NullPointerException("Cannot accept null values for table"));
        }
        this.name = name;
        this.table = table;
        this.alias = alias;
    }

    /**
     * Constructor for this Field object, without an alias.
     *
     * @param name the name of the field, as the database knows it. This name is also used in
     * the rest of the query.
     * @param table the name of the table to which the field belongs, as the database knows it
     */
    public Field(String name, Table table) {
        this(name, table, null);
    }

    /**
     * Returns a String representation of this Field object, suitable for forming part of an SQL
     * query.
     *
     * @return the String representation
     */
    public String getSQLString() {
        String tableName = (table.getAlias() != null) ? table.getAlias() : table.getName();
        String ret = tableName + "." + name;
        return ((alias == null) || (alias.equals(name)) ? ret : ret + " AS " + alias);
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is of the same class, and with the same name
     */
    public boolean equals(Object obj) {
        if (obj instanceof Field) {
            Field objField = (Field) obj;
            return name.equals(objField.name)
                && table.equals(objField.table)
                && (((alias == null) && (objField.alias == null))
                    || ((alias != null) && (alias.equals(objField.alias))));
        }
        return false;
    }

    /**
     * Overrides Object.hashcode().
     *
     * @return an arbitrary integer based on the name of the Table
     */
    public int hashCode() {
        return name.hashCode() + table.hashCode() + (alias == null ? 0 : alias.hashCode());
    }

    /**
     * Compare this Field to another AbstractValue, ignoring the field alias, but including the
     * table alias.
     *
     * @param obj an AbstractField to compare to
     * @return true if the object is of the same class, and with the same value
     */

    public boolean equalsIgnoreAlias(AbstractValue obj) {
        if (obj instanceof Field) {
            Field objField = (Field) obj;
            return name.equals(objField.name) && table.equalsOnlyAlias(objField.table);
        }
        return false;
    }
}
