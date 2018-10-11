package org.intermine.api.profile;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Date;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.types.ClassKeys;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.junit.Test;

public class QueryHistoryTest extends InterMineAPITestCase {

    private ProfileManager pm;
    private Profile bobProfile;

    public QueryHistoryTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        pm = im.getProfileManager();
        bobProfile = pm.createBasicLocalProfile("bob", "bob is gr8", "superbob");
        for (int i = 0; i < 5; i++) {
            addHistory(i);
        }
    }

    public void testQueryOrdering() {
        String[] names = {"query_0", "query_1", "query_2", "query_3", "query_4"};
        assertArrayEquals(names, bobProfile.getHistory().keySet().toArray(new String[5]));
    }

    public void testQueryOrderingAfterRemoval() {
        String[] names = {"query_0", "query_1", "query_2", "query_4"};
        bobProfile.deleteHistory("query_3");
        assertArrayEquals(names, bobProfile.getHistory().keySet().toArray(new String[4]));
    }

    public void testQueryHistoryRename() {
        String[] names = {"query_0", "query_1", "foo", "query_3", "query_4"};
        SavedQuery q2 = bobProfile.getHistory().get("query_2");
        bobProfile.renameHistory("query_2", "foo");
        assertArrayEquals(names, bobProfile.getHistory().keySet().toArray(new String[5]));
        assertEquals(q2.getPathQuery(), bobProfile.getHistory().get("foo").getPathQuery());
    }

    public void testQueryHistoryAdd() {
        String[] names = {"query_0", "query_1", "query_2", "query_3", "query_4"};
        assertArrayEquals(names, bobProfile.getHistory().keySet().toArray(new String[5]));
        addHistory(5);
        String[] namesAfter = {"query_0", "query_1", "query_2", "query_3", "query_4", "query_5"};
        assertArrayEquals(namesAfter, bobProfile.getHistory().keySet().toArray(new String[6]));
    }

    private void addHistory(int n) {
        Model m = im.getModel();
        PathQuery q = new PathQuery(m);
        q.addViews("Employee.name", "Employee.department.name");
        q.addConstraint(Constraints.eq("Employee.age", String.valueOf(n)));
        Date d = new Date(1000 * n);
        String name = "query_" + n;
        bobProfile.saveHistory(new SavedQuery(name, d, q));
    }

}
