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

