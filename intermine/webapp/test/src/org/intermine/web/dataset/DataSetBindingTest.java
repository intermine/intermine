package org.intermine.web.dataset;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

public class DataSetBindingTest extends TestCase
{
    public void testUnmarshal() {
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream("DataSetBindingTest.xml"));
        Map sets = DataSetBinding.unmarshal(reader);
        assertNotNull(sets);
        assertEquals(2, sets.keySet().size());
        
        Iterator iter = sets.values().iterator();
        DataSet set1 = (DataSet) iter.next();
        DataSet set2 = (DataSet) iter.next();
        assertNotNull(set1);
        assertNotNull(set2);
        
        assertEquals("source1", set1.getName());
        assertEquals("subtitle1", set1.getSubTitle());
        assertEquals("iconImage1", set1.getIconImage());
        assertEquals("largeImage1", set1.getLargeImage());
        assertEquals("tile1", set1.getTileName());
        assertEquals(2, set1.getDataSetSources().size());
        assertEquals(Arrays.asList(new String[]{"Class1", "Class2"}), set1.getStartingPoints());
        
        DataSetSource source1 = (DataSetSource) set1.getDataSetSources().get(0);
        DataSetSource source2 = (DataSetSource) set1.getDataSetSources().get(1);
        assertEquals("dataSource1.1", source1.getName());
        assertEquals("dataSource1.2", source2.getName());
        assertEquals("dataSource1.1URL", source1.getUrl());
        assertEquals("dataSource1.2URL", source2.getUrl());
        
        assertEquals("source2", set2.getName());
        assertEquals("subtitle2", set2.getSubTitle());
        assertEquals("iconImage2", set2.getIconImage());
        assertEquals("largeImage2", set2.getLargeImage());
        assertEquals("tile2", set2.getTileName());
        assertEquals(2, set2.getDataSetSources().size());
        assertEquals(Arrays.asList(new String[]{"Class3", "Class4"}), set2.getStartingPoints());
        
        source1 = (DataSetSource) set2.getDataSetSources().get(0);
        source2 = (DataSetSource) set2.getDataSetSources().get(1);
        assertEquals("dataSource2.1", source1.getName());
        assertEquals("dataSource2.2", source2.getName());
        assertEquals("dataSource2.1URL", source1.getUrl());
        assertEquals("dataSource2.2URL", source2.getUrl());
    }
}
