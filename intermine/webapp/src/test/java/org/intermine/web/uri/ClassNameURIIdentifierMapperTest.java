package org.intermine.web.uri;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.junit.Assert;
import org.junit.Test;

public class ClassNameURIIdentifierMapperTest {

    @Test
    public void getIdentifier() {
        ClassNameURIIdentifierMapper mapper = ClassNameURIIdentifierMapper.getMapper();
        Assert.assertEquals("vatNumber", mapper.getIdentifier("Company"));
    }

    @Test
    public void getIdentifierKeyTypo() {
        ClassNameURIIdentifierMapper mapper = ClassNameURIIdentifierMapper.getMapper();
        Assert.assertNull(mapper.getIdentifier("Bank"));
    }

    @Test
    public void getIdentifierWithNoKey() {
        ClassNameURIIdentifierMapper mapper = ClassNameURIIdentifierMapper.getMapper();
        Assert.assertNull(mapper.getIdentifier("ThisisNotAKey"));
    }
}
