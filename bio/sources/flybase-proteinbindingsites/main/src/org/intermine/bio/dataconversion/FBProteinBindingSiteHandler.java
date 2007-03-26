package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.intermine.dataloader.DirectDataLoader;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A DefaultHandler for parsing protein binding site information from FlyBase.
 *
 * @author Kim Rutherford
 */
class FBProteinBindingSiteHandler extends DefaultHandler
{
    private List stack = new ArrayList();
    private DirectDataLoader ddl;
    private String geneName;
    private StringBuffer charData = new StringBuffer(1000);

    /**
     * Create a new 
     * @param ddl
     */
    public FBProteinBindingSiteHandler(DirectDataLoader ddl) {
        this.ddl = ddl;
    }

    /**
     * @see DefaultHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        stack.add(0, new StackElement(qName, attrs));
    }
    
    /**
     * @see DefaultHandler#endElement(String, String, String)
     */
    public void endElement(String uri, String localName, String qName) {
        stack.remove(0);
        if (qName.equals("fbid") && stackGet(0).name.equals("ID") 
            && stackGet(1).name.equals("GADR")) {
            System.err.println("qName: " + charData.toString());
            geneName = charData.toString();
        }

        charData.setLength(0);
    }

    public void characters (char ch[], int start, int length) {
        charData.append(ch, start, length);
    }
    
    private StackElement stackGet(int i) {
        return (StackElement) stack.get(i);
    }


    
    private class StackElement
    {
        final String name;
        final Attributes attrs;

        StackElement(String name, Attributes attrs) {
            this.name = name;
            this.attrs = attrs;
        }

    }
}
