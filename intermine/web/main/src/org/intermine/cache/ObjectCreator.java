package org.intermine.cache;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Implementations of this interface are passed to InterMineCache.register().  If
 * InterMineCache.get() can't find an object the ObjectCreator.create() method is called to make a
 * new object.  The Implementation of ObjectCreator should include a create() method that has
 * parameters of the right type and returns Serializable.
 * @author Kim Rutherford
 */
public interface ObjectCreator
{
   // empty - the implementation should include a create() method that returns Serializable
}
