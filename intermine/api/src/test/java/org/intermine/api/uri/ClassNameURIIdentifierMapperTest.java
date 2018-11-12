package org.intermine.api.uri;

import org.junit.Assert;
import org.junit.Test;

public class ClassNameURIIdentifierMapperTest {

    @Test
    public void getIdentifier() {
        ClassNameURIIdentifierMapper mapper = ClassNameURIIdentifierMapper.getMapper();
        Assert.assertEquals("vatNumber", mapper.getIdentifier("Company"));
    }

    @Test
    public void getIdentifierCaseInSensitive() {
        ClassNameURIIdentifierMapper mapper = ClassNameURIIdentifierMapper.getMapper();
        Assert.assertEquals("address", mapper.getIdentifier("Address"));
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
