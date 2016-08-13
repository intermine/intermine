package org.intermine.api.search;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.tag.TagTypes;

/**
 * An object for encapsulating the scope and type of object requested from a Lucene search
 * of the user profile web searchable items.
 *
 * @author Alex Kalderimis
 *
 */
public final class SearchTarget
{
    private final String scope;
    private final String type;

    /** The search target for all bags **/
    public static final SearchTarget ALL_BAGS =
            new SearchTarget(Scope.ALL, TagTypes.BAG);
    /** The search target for global bags **/
    public static final SearchTarget GLOBAL_BAGS =
            new SearchTarget(Scope.GLOBAL, TagTypes.BAG);
    /** The search target for user bags **/
    public static final SearchTarget USER_BAGS =
            new SearchTarget(Scope.USER, TagTypes.BAG);
    /** The search target for all templates **/
    public static final SearchTarget ALL_TEMPLATES =
            new SearchTarget(Scope.ALL, TagTypes.TEMPLATE);
    /** The search target for global templates **/
    public static final SearchTarget GLOBAL_TEMPLATES =
            new SearchTarget(Scope.GLOBAL, TagTypes.TEMPLATE);
    /** The search target for user templates **/
    public static final SearchTarget USER_TEMPLATES =
            new SearchTarget(Scope.USER, TagTypes.TEMPLATE);

    /**
     * Constructor.
     *
     * @see Scope.SCOPES
     * @see TagTypes
     *
     * @param s One of the valid Scope.SCOPES
     * @param t One of TagTypes.BAG or TagTypes.TEMPLATE.
     */
    public SearchTarget(String s, String t) {
        if (!Scope.SCOPES.contains(s)) {
            throw new IllegalArgumentException("scope must be one of " + Scope.SCOPES);
        }
        scope = s;
        if (!(TagTypes.BAG.equals(t) || TagTypes.TEMPLATE.equals(t))) {
            throw new IllegalArgumentException("Unknown type: " + t);
        }
        type = t;
    }

    /**
     * If the scope of the request is only for user data.
     * @return a truth value.
     */
    boolean isUserOnly() {
        return Scope.USER.equals(scope);
    }

    /**
     * If the scope of the request is only for global data.
     * @return a truth value.
     */
    boolean isGlobalOnly() {
        return Scope.GLOBAL.equals(scope);
    }

    /**
     * If the scope of the request is for user data and global data.
     * @return a truth value.
     */
    boolean isAll() {
        return !(isUserOnly() || isGlobalOnly());
    }

    /**
     * Get the type of object targeted.
     * @return A TagType.
     */
    String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "<SearchTarget scope=\"" + scope + "\" type=\"" + type + "\">";
    }
}
