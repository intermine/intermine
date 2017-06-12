package org.intermine.api.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.FastPathObject;
import org.intermine.pathquery.Path;

/**
 * An interface for result cells
 * @author Alex Kalderimis
 */
public interface ResultCell
{
    /** @return the object this cell projects **/
    Object getField();

    /** @return whether this cell represents a key field **/
    boolean isKeyField();

    /** @return the id of the object of this cell. **/
    Integer getId();

    /** @return the path of this cell **/
    Path getPath();

    /** @return the object this cell contains **/
    FastPathObject getObject();

    /** @return the type of the value of this cell. **/
    String getType();
}
