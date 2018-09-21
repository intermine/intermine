package org.intermine.api.url;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Thrown to indicate that the url is not a valid intermine permanent URL
 * The valid patters is: domain/context/prefix:external_local_id
 * (e.g. humanmine.org/humanmine/uniprot:P31946). The prefix has to be defined
 * in the prefixes.properties file
 *
 * @author danielabutano
 */
public class InvalidPermanentURLException extends Exception
{
}
