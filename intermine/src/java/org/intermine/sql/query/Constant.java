package org.flymine.sql.query;

/**
 * A representation of a constant value in an SQL query.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class Constant extends AbstractValue
{
    protected String value;

    /**
     * Constructor for this Constant object.
     *
     * @param value the constant, as referenced in the SQL query, including any ' characters
     * surrounding it.
     */
    public Constant(String value) {
        this(value, null);
    }

    /**
     * Constructor for this Constant object.
     *
     * @param value the constant, as referenced in the SQL query, including any ' characters
     * surrounding it.
     * @param alias an alias for the value
     */
    public Constant(String value, String alias) {
        if (value == null) {
            throw (new NullPointerException("Constants cannot have a null value"));
        }
        this.value = value;
        this.alias = alias;
    }

    /**
     * Returns a String representation of this Constant object, suitable for forming part of an
     * SQL query.
     *
     * @return the String representation
     */
    public String getSQLString() {
        return (alias == null ? value : value + " AS " + alias);
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is of the same class, and with the same value
     */
    public boolean equals(Object obj) {
        if (obj instanceof Constant) {
            Constant objConstant = (Constant) obj;
            return value.equals(objConstant.value) 
                   && (((alias == null) && (objConstant.alias == null)) 
                       || ((alias != null) && (alias.equals(objConstant.alias))));
        }
        return false;
    }

    /**
     * Overrides Object.hashcode().
     *
     * @return an arbitrary integer based on the value of the Constant
     */
    public int hashCode() {
        return value.hashCode() + (alias == null ? 0 : alias.hashCode());
    }

    /**
     * Compare this Constant to another AbstractValue, ignoring alias.
     *
     * @param obj an AbstractValue to compare to
     * @return true if the object is of the same class, and with the same value
     */
    public boolean equalsIgnoreAlias(AbstractValue obj) {
        if (obj instanceof Constant) {
            return value.equals(((Constant) obj).value);
        }
        return false;
    }

    /**
     * Compare this Constant to another AbstractValue, ignoring alias.
     * This method is capable of spotting some situations when one Constant is strictly less or
     * greater than another.
     *
     * @see AbstractValue#compare
     */
    public int compare(AbstractValue obj) {
        if (equalsIgnoreAlias(obj)) {
            return EQUAL;
        }
        if (obj instanceof Constant) {
            Constant objC = (Constant) obj;
            if ((value.charAt(0) == '\'') && (value.charAt(value.length() - 1) == '\'') 
                    && (objC.value.charAt(0) == '\'')
                    && (objC.value.charAt(value.length() - 1) == '\'')) {
                return (value.compareTo(objC.value) < 0 ? LESS : GREATER);
            }
            try {
                return (Double.parseDouble(value) < Double.parseDouble(objC.value) ? LESS
                        : GREATER);
            } catch (NumberFormatException e) {
                // That's not a problem
            }
        }
        return INCOMPARABLE;
    }
}
