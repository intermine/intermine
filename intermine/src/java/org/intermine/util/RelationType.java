package org.flymine.util;

/**
 * Holder for static integers describing entity relationship types.
 *
 * @author Richard Smith
 */

public class RelationType
{
    private RelationType() {
    }

    /**
     * Not a relationship between objects.
     */
    public static final int NOT_RELATION = 0;

    /**
     * A 1:1 relationship
     */
    public static final int ONE_TO_ONE = 1;

    /**
     * A 1:N relationship.
     */
    public static final int ONE_TO_N = 2;

    /**
     * A N:1 relationship.
     */
    public static final int N_TO_ONE = 3;

    /**
     * A M:N relationship.
     */
    public static final int M_TO_N = 4;

}
