package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.objectstore.query.Clob.CLOB_PAGE_SIZE;

import java.io.PrintStream;

import org.intermine.objectstore.query.ClobAccess;

/**
 * Subclass of ClobAccess that reverses and complements the DNA sequence contained within it.
 *
 * @author Matthew Wakeling
 */
public class ClobAccessReverseComplement extends ClobAccess
{
    private ClobAccess originalClobAccess;

    /**
     * Construct this object from an existing ClobAccess object
     *
     * @param ca a ClobAccess object
     */
    public ClobAccessReverseComplement(ClobAccess ca) {
        if (ca instanceof ClobAccessReverseComplement) {
            throw new IllegalArgumentException("Cannot reversecomplement a Clob that is already "
                    + "reversecomplemented");
        }
        originalClobAccess = ca;
        os = ca.getOs();
        results = ca.getResultsWithoutInit();
        clob = ca.getClob();
        offset = ca.getOffset();
        length = ca.getLengthWithoutInit();
        subSequence = ca.getSubSequence();
    }

    /**
     * Translates a single character to the complement base for DNA.
     *
     * @param in the character to translate
     * @return the complement character
     */
    public char translate(char in) {

        boolean inputWasLowerCase = false;
        if (Character.isLowerCase(in)) {
            in = Character.toUpperCase(in);
            inputWasLowerCase = true;
        }

        switch (in) {
            case 'C':
                return returnChar('G', inputWasLowerCase);
            case 'G':
                return returnChar('C', inputWasLowerCase);
            case 'A':
                return returnChar('T', inputWasLowerCase);
            case 'T':
                return returnChar('A', inputWasLowerCase);
            case 'N':
                return returnChar('N', inputWasLowerCase);
            case 'U':
                return returnChar('A', inputWasLowerCase);
            case 'Y':
                return returnChar('R', inputWasLowerCase);
            case 'R':
                return returnChar('Y', inputWasLowerCase);
            case 'S':
                return returnChar('S', inputWasLowerCase);
            case 'W':
                return returnChar('W', inputWasLowerCase);
            case 'K':
                return returnChar('M', inputWasLowerCase);
            case 'M':
                return returnChar('K', inputWasLowerCase);
            case 'B':
                return returnChar('V', inputWasLowerCase);
            case 'V':
                return returnChar('B', inputWasLowerCase);
            case 'D':
                return returnChar('H', inputWasLowerCase);
            case 'H':
                return returnChar('D', inputWasLowerCase);
            case '.':
                return returnChar('.', inputWasLowerCase);
            default:
                throw new IllegalArgumentException("DNA sequence is invalid - cannot contain "
                        + in);
        }
    }

    private static char returnChar(char c, boolean toLowerCase) {
        if (toLowerCase) {
            return Character.toLowerCase(c);
        }
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char charAt(int index) {
        init();
        char originalChar = super.charAt(length - index - 1);
        return translate(originalChar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClobAccessReverseComplement subSequence(int start, int end) {
        init();
        ClobAccess sub = originalClobAccess.subSequence(length - end, length - start);
        return new ClobAccessReverseComplement(sub);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        init();
        StringBuilder retval = new StringBuilder();
        int lowestPage = offset / CLOB_PAGE_SIZE;
        int highestPage = (offset + length - 1) / CLOB_PAGE_SIZE;
        for (int page = highestPage; page >= lowestPage; page--) {
            String pageText = (String) results.get(page);
            if (page == highestPage) {
                pageText = pageText.substring(0, offset + length - page * CLOB_PAGE_SIZE);
            }
            if (page == lowestPage) {
                pageText = pageText.substring(offset - page * CLOB_PAGE_SIZE, pageText.length());
            }
            for (int cNo = pageText.length() - 1; cNo >= 0; cNo--) {
                char origC = pageText.charAt(cNo);
                retval.append(translate(origC));
            }
        }
        return retval.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drainToPrintStream(PrintStream out) {
        init();
        int lowestPage = offset / CLOB_PAGE_SIZE;
        int highestPage = (offset + length - 1) / CLOB_PAGE_SIZE;
        for (int page = highestPage; page >= lowestPage; page--) {
            StringBuilder retval = new StringBuilder();
            String pageText = (String) results.get(page);
            if (page == highestPage) {
                pageText = pageText.substring(0, offset + length - page * CLOB_PAGE_SIZE);
            }
            if (page == lowestPage) {
                pageText = pageText.substring(offset - page * CLOB_PAGE_SIZE, pageText.length());
            }
            for (int cNo = pageText.length() - 1; cNo >= 0; cNo--) {
                char origC = pageText.charAt(cNo);
                retval.append(translate(origC));
            }
            out.print(retval.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDbDescription() {
        return  super.getDbDescription() + "," + ClobAccessReverseComplement.class.getName();
    }

    /**
     * Returns a ClobAccessSubclassFactory object that will access the constructor of this class
     * quickly, in order to avoid using reflection for every clob.
     *
     * @return a ClobAccessSubclassFactory associated with this class
     */
    public static ClobAccessSubclassFactory getFactory() {
        return FACTORY;
    }

    private static final ClobAccessSubclassFactory FACTORY = new Factory();

    private static class Factory extends ClobAccessSubclassFactory
    {
        @Override
        public ClobAccess invokeConstructor(ClobAccess clobAccess) {
            return new ClobAccessReverseComplement(clobAccess);
        }
    }
}
