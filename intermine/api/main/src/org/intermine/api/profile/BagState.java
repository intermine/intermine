package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Enumeration for describing the state of a bag:
 * current
 * not current(= the upgrading process has not been executed yet)
 * to upgrade (= the upgrading process has not been able to upgrade it because there are some
 * conflicts that the user has to solve manually ))
 * @author Daniela Butano
 */
public enum BagState { CURRENT, NOT_CURRENT, TO_UPGRADE }
