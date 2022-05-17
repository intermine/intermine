package org.intermine.web.uri;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Thrown to indicate that the http request is not a permanent URL
 * The valid patters is: domain/context/classname:identifier
 * (e.g. humanmine.org/humanmine/protein:P31946).
 *
 * @author danielabutano
 */
public class InvalidPermanentURLException extends Exception
{
}
