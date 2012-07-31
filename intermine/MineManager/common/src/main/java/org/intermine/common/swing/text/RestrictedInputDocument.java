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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.apache.commons.lang.StringUtils;

/**
 * Document implementation that limits the characters that can be added to a
 * predefined set. The document optionally allows letters to have their case
 * changed if only one case is permitted and the letter is of the other case.
 */
public class RestrictedInputDocument extends PlainDocument
{
    private static final long serialVersionUID = -4546349207969196976L;

    /**
     * Predefined character set for upper case letters.
     */
    public static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    /**
     * Predefined character set for lower case letters.
     */
    public static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    
    /**
     * Predefined character set for all letters.
     */
    public static final String LETTERS = UPPER_CASE + LOWER_CASE;
    
    /**
     * Predefined character set for digits.
     */
    public static final String DIGITS = "0123456789";
    
    /**
     * Predefined character set for all letters plus digits.
     */
    public static final String ALPHANUMERIC = LETTERS + DIGITS;
    
    /**
     * Predefined character set for word characters (letters, digits and underscore).
     */
    public static final String WORD_CHARACTERS = ALPHANUMERIC + "_";
    
    
    /**
     * The characters allowable in this document.
     * @serial
     */
    private String allowableCharacters;
    
    /**
     * The limit on the maximum number of characters allowed in this document.
     * Negative values indicate no limit.
     * @serial
     */
    private int limit = -1;
    
    /**
     * Whether converting the case of a letter that is disallowed can be tried.
     * @serial
     */
    private boolean caseConvert;
    
    /**
     * Flag indicating that checks should be (temporarily) suppressed, allowing
     * any input to be made.
     * @serial
     */
    private boolean overrideChecks;

    /**
     * Character array for temporary work.
     */
    private transient char[] convertArray;

    
    /**
     * Create a new RestrictedInputDocument with the given set of allowable characters.
     * There is no size limit and no case conversion.
     * 
     * @param allowableCharacters The set of permissible characters.
     */
    public RestrictedInputDocument(String allowableCharacters) {
        this.allowableCharacters = allowableCharacters;
    }

    /**
     * Create a new RestrictedInputDocument with the given set of allowable characters
     * and size limit. There is no case conversion.
     * 
     * @param allowableCharacters The set of permissible characters.
     * @param sizeLimit The maximum number of characters allowed. Negative values indicate
     * no limit.
     */
    public RestrictedInputDocument(String allowableCharacters, int sizeLimit) {
        this(allowableCharacters);
        limit = sizeLimit;
    }

    /**
     * Create a new RestrictedInputDocument with the given set of allowable characters,
     * explicitly stating whether the case of disallowed letters can be changed.
     * There is no size limit.
     * 
     * @param allowableCharacters The set of permissible characters.
     * @param convertCase Whether disallowed letters can have their case changed to make
     * them permissible.
     */
    public RestrictedInputDocument(String allowableCharacters, boolean convertCase) {
        this(allowableCharacters);
        caseConvert = convertCase;
    }

    /**
     * Create a new RestrictedInputDocument with the given set of allowable characters
     * and size limit, explicitly stating whether the case of disallowed letters can be changed.
     * 
     * @param allowableCharacters The set of permissible characters.
     * @param sizeLimit The maximum number of characters allowed. Negative values indicate
     * no limit.
     * @param convertCase Whether disallowed letters can have their case changed to make
     * them permissible.
     */
    public RestrictedInputDocument(String allowableCharacters, int sizeLimit, boolean convertCase) {
        this(allowableCharacters, sizeLimit);
        caseConvert = convertCase;
    }

    /**
     * Get whether restriction checks are currently suppressed.
     * 
     * @return <code>true</code> if this Document is currently not checking for compliance,
     * <code>false</code> if it is.
     */
    public boolean isOverrideChecks() {
        return overrideChecks;
    }

    /**
     * Set whether restriction checks are suppressed. This is sometimes useful when fields need
     * to be populated programmatically, and one wants the field to display unchanged regardless
     * of whether the string has invalid characters or is too long.
     * 
     * @param overrideChecks <code>true</code> to prevent the checks on permissible characters
     * taking place, allowing all input into the document. <code>false</code> for normal
     * restriction behaviour.
     */
    public void setOverrideChecks(boolean overrideChecks) {
        this.overrideChecks = overrideChecks;
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

        if (limit < 0 || getLength() + str.length() <= limit) {
            if (convertArray == null || convertArray.length < str.length()) {
                convertArray = new char[str.length()];
            }

            CharacterIterator iter = new StringCharacterIterator(str);
            char c = iter.first();
            int index;
            for (index = 0; c != CharacterIterator.DONE; c = iter.next()) {
                if (!overrideChecks && !StringUtils.contains(allowableCharacters, c)) {
                    
                    // At this point, c is invalid. See if a case change remedies this.
                    
                    if (caseConvert && Character.isLetter(c)) {
                        if (Character.isLowerCase(c)) {
                            c = Character.toUpperCase(c);
                        } else if (Character.isUpperCase(c)) {
                            c = Character.toLowerCase(c);
                        }
                        if (!StringUtils.contains(allowableCharacters, c)) {
                            // Don't insert but otherwise ignore.
                            return;
                        }
                    } else {
                        return;
                    }
                }
                convertArray[index++] = c;

            }
            super.insertString(offset, new String(convertArray, 0, index), attr);
        }
    }
}
