package org.intermine.codegen;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;

import org.intermine.metadata.*;


/**
 * Extend abstract model output with dummy method implementations to test
 * superclass methods.
 */
public class TestModelOutput extends ModelOutput
{
    public TestModelOutput(Model model, File file) throws Exception {
        super(model, file);
    }

    public void process() {
    }

    protected String generate(Model model) {
        return "";
    }

    protected String generate(ClassDescriptor cld) {
        return "";
    }

    protected String generate(AttributeDescriptor attr) {
        return "";
    }

    protected String generate(ReferenceDescriptor ref) {
        return "";
    }

    protected String generate(CollectionDescriptor col) {
        return "";
    }
}
