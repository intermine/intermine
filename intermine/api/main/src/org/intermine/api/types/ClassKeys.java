package org.intermine.api.types;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.metadata.FieldDescriptor;

/**
 * Type alias for classkeys - simplifies matters and prevents mistakes.
 * @author Alex Kalderimis
 *
 */
public interface ClassKeys extends Map<String, List<FieldDescriptor>>
{
    // I'm an interface
}
