package org.intermine.web.aspects;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.intermine.web.logic.aspects.Aspect;
import org.intermine.web.logic.aspects.AspectBinding;
import org.intermine.web.logic.aspects.AspectSource;

import junit.framework.TestCase;

public class AspectBindingTest extends TestCase
{
    public void testUnmarshal() {
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream("AspectBindingTest.xml"));
        Map sets = AspectBinding.unmarshal(reader);
        assertNotNull(sets);
        assertEquals(2, sets.keySet().size());
        
        Iterator iter = sets.values().iterator();
        Aspect set1 = (Aspect) iter.next();
        Aspect set2 = (Aspect) iter.next();
        assertNotNull(set1);
        assertNotNull(set2);
        
        assertEquals("source1", set1.getName());
        assertEquals("subtitle1", set1.getSubTitle());
        assertEquals("iconImage1", set1.getIconImage());
        assertEquals("largeImage1", set1.getLargeImage());
        assertEquals("tile1", set1.getTileName());
        assertEquals(2, set1.getAspectSources().size());
        assertEquals(Arrays.asList(new String[]{"Class1", "Class2"}), set1.getStartingPoints());
        
        AspectSource source1 = (AspectSource) set1.getAspectSources().get(0);
        AspectSource source2 = (AspectSource) set1.getAspectSources().get(1);
        assertEquals("dataSource1.1", source1.getName());
        assertEquals("dataSource1.2", source2.getName());
        assertEquals("dataSource1.1URL", source1.getUrl());
        assertEquals("dataSource1.2URL", source2.getUrl());
        
        assertEquals("source2", set2.getName());
        assertEquals("subtitle2", set2.getSubTitle());
        assertEquals("iconImage2", set2.getIconImage());
        assertEquals("largeImage2", set2.getLargeImage());
        assertEquals("tile2", set2.getTileName());
        assertEquals(2, set2.getAspectSources().size());
        assertEquals(Arrays.asList(new String[]{"Class3", "Class4"}), set2.getStartingPoints());
        
        source1 = (AspectSource) set2.getAspectSources().get(0);
        source2 = (AspectSource) set2.getAspectSources().get(1);
        assertEquals("dataSource2.1", source1.getName());
        assertEquals("dataSource2.2", source2.getName());
        assertEquals("dataSource2.1URL", source1.getUrl());
        assertEquals("dataSource2.2URL", source2.getUrl());
    }
}
