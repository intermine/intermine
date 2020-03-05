package org.intermine.web.uri;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.intermine.objectstore.ObjectStoreWriter;
import org.junit.Test;

public class InterMineLUITest {
    @Test
    public void getClassName() {
        InterMineLUI lui = new InterMineLUI("Protein", "P31946");
        assertEquals("Protein", lui.getClassName());
    }

    @Test
    public void getIdentifier() {
        InterMineLUI lui = new InterMineLUI("Protein", "P31946");
        assertEquals("P31946", lui.getIdentifier());
    }

    @Test
    public void getClassNameFromAccessURL() {
        try {
            InterMineLUI lui = new InterMineLUI("humanmine/Protein:P31946");
            assertEquals("Protein", lui.getClassName());
        } catch (InvalidPermanentURLException ex) {
        }
    }

    @Test
    public void getClassNameCaseInsentitiveFromAccessURL() {
        try {
            InterMineLUI lui = new InterMineLUI("humanmine/protein:P31946");
            assertEquals("Protein", lui.getClassName());
        } catch (InvalidPermanentURLException ex) {
        }
    }

    @Test
    public void getIdentifierFromAccessURL() {
        try {
            InterMineLUI lui = new InterMineLUI("humanmine/protein:P31946");
            assertEquals("P31946", lui.getIdentifier());
        } catch (InvalidPermanentURLException ex) {
        }
    }


    @Test
    public void throwExceptionWhenThereIsNotClass() {
        try {
            new InterMineLUI("humanmine/P31946");
            fail("Should throw an InvalidPermanentURLException");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(InvalidPermanentURLException.class));
        }
    }

    @Test
    public void throwExceptionWhenThereIsNotIdentifier() {
        try {
            new InterMineLUI("humanmine/Protein");
            fail("Should throw an InvalidPermanentURLException");
        } catch (Exception ex){
            assertThat(ex, instanceOf(InvalidPermanentURLException.class));
        }
    }

    @Test
    public void throwExceptionWhenThereIsNotSeparator() {
        try {
            new InterMineLUI("humanmine/ProteinP31946");
            fail("Should throw an InvalidPermanentURLException");
        } catch (Exception ex){
            assertThat(ex, instanceOf(InvalidPermanentURLException.class));
        }
    }
}
