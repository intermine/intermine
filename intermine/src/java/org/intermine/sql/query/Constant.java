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
        if (value == null) {
            throw (new NullPointerException("Constants cannot have a null value"));
        }
        this.value = value;
    }

    /**
     * Returns a String representation of this Constant object, suitable for forming part of an
     * SQL query.
     *
     * @return the String representation
     */
    public String getSQLString() {
        return value;
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
            return value.equals(objConstant.value);
        }
        return false;
    }

    /**
     * Overrides Object.hashcode().
     *
     * @return an arbitrary integer based on the value of the Constant
     */
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Compare this Constant to another AbstractValue.
     * This method is capable of spotting some situations when one Constant is strictly less or
     * greater than another.
     *
     * @see AbstractValue#compare
     */
    public int compare(AbstractValue obj) {
        if (equals(obj)) {
            return EQUAL;
        }
        if (obj instanceof Constant) {
            Constant objC = (Constant) obj;
            if ((value.charAt(0) == '\'') && (value.charAt(value.length() - 1) == '\'')
                    && (objC.value.charAt(0) == '\'')
                    && (objC.value.charAt(objC.value.length() - 1) == '\'')) {
                // Both this and obj are string constants.
                return (value.compareTo(objC.value) < 0 ? LESS : GREATER);
            }
            try {
                return (Double.parseDouble(value) < Double.parseDouble(objC.value) ? LESS
                        : GREATER);
            } catch (NumberFormatException e) {
                // That's not a problem
            }
            if ((value.charAt(0) == '\'') && (value.charAt(value.length() - 1) == '\'')) {
                try {
                    double a = Double.parseDouble(objC.value);
                    return NOT_EQUAL;
                } catch (NumberFormatException e) {
                    // That's okay - obj is not a number
                }
            }
            if ((objC.value.charAt(0) == '\'')
                    && (objC.value.charAt(objC.value.length() - 1) == '\'')) {
                try {
                    double a = Double.parseDouble(value);
                    return NOT_EQUAL;
                } catch (NumberFormatException e) {
                    // That's okay - this is not a number
                }
            }
        }
        return INCOMPARABLE;
    }
}
