package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.struts.QueryBuilderForm;

import junit.framework.TestCase;

import org.apache.struts.action.ActionErrors;

/**
 * Test the static parseValue method on MainForm.
 *
 * @author tom
 */
public class MainFormTest extends TestCase
{
    public MainFormTest(String arg) {
        super(arg);
    }

    public void testEmptyValues() throws Exception {
        ActionErrors errors = new ActionErrors ();
        QueryBuilderForm.parseValue("", Integer.TYPE, ConstraintOp.EQUALS, Locale.getDefault(), errors);
        assertTrue(errors.size() > 0);

        errors = new ActionErrors ();
        QueryBuilderForm.parseValue("", Float.TYPE, ConstraintOp.EQUALS, Locale.getDefault(), errors);
        assertTrue(errors.size() > 0);
    }

    public void testFloats() throws Exception {
        ActionErrors errors = new ActionErrors ();
        QueryBuilderForm.parseValue("1.1.1", Float.TYPE, ConstraintOp.EQUALS, Locale.getDefault(), errors);
        assertTrue("no error on bad float format", errors.size() > 0);

        errors = new ActionErrors ();
        Object value = QueryBuilderForm.parseValue("1.1", Float.TYPE, ConstraintOp.EQUALS, Locale.getDefault(), errors);
        assertNotNull(value);
        assertEquals(Float.class, value.getClass());
        assertTrue(1.1f == ((Float)value).floatValue());
    }

    public void testDateUK() throws Exception {
        ActionErrors errors = new ActionErrors ();
        Object value = QueryBuilderForm.parseValue("25/09/04", Date.class, ConstraintOp.EQUALS, Locale.UK, errors);
        assertNotNull("good UK date didn't parse", value);

        Calendar calendar = new GregorianCalendar(2004,8,25);
        assertEquals(calendar.getTime(), value);
    }

    public void testBadDateUK() throws Exception {
        ActionErrors errors = new ActionErrors ();
        Object value = QueryBuilderForm.parseValue("asdfsdfsdf", Date.class, ConstraintOp.EQUALS, Locale.UK, errors);
        assertNull("baddly formatted date parsed to non-null value", value);
        assertTrue("bad date format should give error", errors.size() > 0);
    }

    public void testWildcards() throws Exception {
        ActionErrors errors = new ActionErrors ();
        // empty like/not-like value
        Object value = QueryBuilderForm.parseValue("", String.class, ConstraintOp.MATCHES, Locale.UK, errors);
        assertTrue("empty string in like/not-like should give error", errors.size() > 0);

        errors = new ActionErrors ();
        value = QueryBuilderForm.parseValue("a", String.class, ConstraintOp.MATCHES, Locale.UK, errors);
        assertTrue(errors.isEmpty());
        assertEquals("a", value);

        errors = new ActionErrors ();
        value = QueryBuilderForm.parseValue("a*", String.class, ConstraintOp.MATCHES, Locale.UK, errors);
        assertTrue(errors.isEmpty());
        assertEquals("a%", value);

        errors = new ActionErrors ();
        value = QueryBuilderForm.parseValue("*a", String.class, ConstraintOp.MATCHES, Locale.UK, errors);
        assertTrue(errors.isEmpty());
        assertEquals("%a", value);

        errors = new ActionErrors ();
        value = QueryBuilderForm.parseValue("*a*", String.class, ConstraintOp.MATCHES, Locale.UK, errors);
        assertTrue(errors.isEmpty());
        assertEquals("%a%", value);
    }

}
