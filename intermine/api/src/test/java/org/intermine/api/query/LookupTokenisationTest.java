package org.intermine.api.query;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class LookupTokenisationTest {

    LookupTokeniser lookups;
    LookupTokeniser listUploads;

    @Before
    public void setup() {
        lookups = LookupTokeniser.getLookupTokeniser();
        listUploads = LookupTokeniser.getListUploadTokeniser();
    }

    @Test
    public void commaSeparationQuoted() {
        List<String> expected = Arrays.asList("Alex", "Comma , Here", "Brenda");
        String input = "\"Alex\", \"Comma , Here\", \"Brenda\"";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }

    @Test
    public void commaSeparationUnquoted() {
        List<String> expected = Arrays.asList("Alex", "Brenda", "Carol");
        String input = "Alex, Brenda, Carol";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }


    @Test
    public void mixedDelimiters() {
        List<String> expected = Arrays.asList("Alex", "Comma , Here", "Brenda");
        String input = "\"Alex\", \"Comma , Here\"\n\"Brenda\"";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }

    @Test
    public void newLineSeparationOnlyNecessaryQuotes() {
        List<String> expected = Arrays.asList("Alex", "Comma , Here", "Brenda");
        String input = "Alex\n\"Comma , Here\"\nBrenda";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }

    @Test
    public void newLineSeparationCommasTabsNewLinesAndQuotes() {
        List<String> expected = Arrays.asList("Alex", "Comma , Here", "NL \n here", "Tab \t here", "DblQuote \" Here");
        String input = "Alex\n\"Comma , Here\"\n\"NL \n here\"\n\"Tab \t here\"\n\"DblQuote \"\" Here\"";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }

    @Test
    public void tabSeparationUnQuoted() {
        List<String> expected = Arrays.asList("Alex", "Brenda", "Carol");
        String input = "Alex\tBrenda\tCarol";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }

    @Test
    public void tabSeparationQuoted() {
        List<String> expected = Arrays.asList("Alex", "Brenda", "Carol");
        String input = "\"Alex\"\t\"Brenda\"\t\"Carol\"";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }

    @Test
    public void newLineUnquoted() {
        List<String> expected = Arrays.asList("Alex", "Brenda", "Carol");
        String input = "Alex\nBrenda\nCarol";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }

    @Test
    public void newLineQuoted() {
        List<String> expected = Arrays.asList("Alex", "Brenda", "Carol");
        String input = "\"Alex\"\n\"Brenda\"\n\"Carol\"";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }

    @Test
    public void spaceSeparation() {
        List<String> expected = Arrays.asList("Alex Brenda Carol");
        String input = "Alex Brenda Carol";
        List<String> got = lookups.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);

        expected = Arrays.asList("Alex", "Brenda", "Carol");
        got = listUploads.tokenise(input);

        assertEquals(expected.size(), got.size());
        assertEquals(expected, got);
    }


}
