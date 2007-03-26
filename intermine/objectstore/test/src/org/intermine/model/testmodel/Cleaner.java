package org.intermine.model.testmodel;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class Cleaner extends Employee
{
    protected boolean evenings;
    public boolean getEvenings() { return evenings; }
    public void setEvenings(boolean evenings) { this.evenings = evenings; }

}
