package org.intermine.objectstore.intermine;

import junit.framework.*;

public class RangeDefinitionTest extends TestCase {


   public void testRageDefinition() throws Exception {
       RangeDefinition rd = new RangeDefinition();
       rd.addRange("location", "intermine_locrange", "int8range", "start", "end");
       String j = rd.toJson();
       String s = "s";
   }

   public void testRageDefinitionConstruct() throws Exception {
       String input = "{\"ranges\":[{\"rangeType\":\"int8range\",\"startCol\":\"start\",\"rangeColName\":\"intermine_locrange\",\"tableName\":\"location\",\"endCol\":\"end\"}]}";
       RangeDefinition rd = RangeDefinition(input);

   }
}