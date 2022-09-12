package org.intermine.api.identifiers;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.identifiers.IdentifiersMapper;
import org.junit.Assert;
import org.junit.Test;

public class IdentifiersMapperTest {

    @Test
    public void getIdentifier() {
        IdentifiersMapper mapper = IdentifiersMapper.getMapper();
        Assert.assertEquals("vatNumber", mapper.getIdentifier("Company"));
    }

    @Test
    public void getIdentifierKeyTypo() {
        IdentifiersMapper mapper = IdentifiersMapper.getMapper();
        Assert.assertNull(mapper.getIdentifier("Bank"));
    }

    @Test
    public void getIdentifierWithNoKey() {
        IdentifiersMapper mapper = IdentifiersMapper.getMapper();
        Assert.assertNull(mapper.getIdentifier("ThisisNotAKey"));
    }
}
