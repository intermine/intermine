package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A Path Expression that has a Select list.
 *
 * @author Matthew Wakeling
 */
public interface QueryPathExpressionWithSelect extends QueryPathExpression
{
    /**
     * Adds an element to the SELECT list. If the SELECT list is left empty, then the collection
     * will use default behaviour.
     *
     * @param selectable a QuerySelectable
     */
    public void addToSelect(QuerySelectable selectable);
}
