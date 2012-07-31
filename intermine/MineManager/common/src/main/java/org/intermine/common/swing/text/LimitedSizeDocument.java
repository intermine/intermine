package org.intermine.common.swing.text;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Document implementation that limits the number of characters that can be entered.
 */
public class LimitedSizeDocument extends PlainDocument
{
    private static final long serialVersionUID = 3061860802362473252L;

    /**
     * The maximum limit on the number of digits allowed.
     * @serial
     */
    private int limit;

    /**
     * Create a new LimitedSizeDocument with the given limit on the number of characters.
     * 
     * @param limit The maximum number of characters.
     */
    public LimitedSizeDocument(int limit) {
        super();
        this.limit = limit;
    }

    /**
     * Insert the given text into this document, as long as it leaves the document valid.
     * 
     * @param offset The starting offset &gt;= 0.
     * @param str The string to insert; does nothing with null/empty strings.
     * @param attr The attributes for the inserted content.
     * 
     * @throws BadLocationException if the given insert position is not a valid
     * position within the document.
     *   
     * @see javax.swing.text.Document#insertString
     */
    @Override
    public void insertString(int offset, String  str, AttributeSet attr)
    throws BadLocationException {
        if (str == null) {
            return;
        }

        if (getLength() + str.length() <= limit) {
            super.insertString(offset, str, attr);
        }
    }
}