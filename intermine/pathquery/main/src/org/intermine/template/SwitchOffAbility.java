package org.intermine.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Enumeration for describing the configuration on constraints on a TemplateQuery that determines
 * whether a user is allowed to switch off a constraint, and if so what state it is in.
 *
 * @author Matthew Wakeling
 */
public enum SwitchOffAbility {
    /**
     * the template constraint is active
     */
    ON,
    /**
     * template constraint is not active
     */
    OFF,
    /**
     * template constraint can't be edited by the user
     */
    LOCKED
}
