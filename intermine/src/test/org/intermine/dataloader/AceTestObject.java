/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.dataloader;

import java.util.Collection;
import java.util.HashSet;

public class AceTestObject {
    public AceTestObject() {
    }

    public String identifier;

    public int intValue;
    public String stringValue;
    public String stringValue_2;

    public Collection stringValues = new HashSet();

    public Boolean onOrOff;

    public AceTestObject reference;
    public Collection references = new HashSet();

    public AceTestObject hashValue;

    public Collection hashValues = new HashSet();

}

