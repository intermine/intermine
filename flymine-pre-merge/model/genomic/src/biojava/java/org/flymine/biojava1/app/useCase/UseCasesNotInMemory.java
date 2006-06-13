package org.flymine.biojava1.app.useCase;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * First check out UseCasesInMemory.java - then come back!
 * <p>
 * <b>For this demo the config file bjMapping.properties must ENABLE hasA, invHasA and DISABLE
 * synonyms and memoryMode </b>
 * <p>
 * In this configuration mode all filters and queries are executed dynamically. All objects are
 * instantiated as late as possible (lazy instantiatiation). Filters are mapped to equivalent
 * FlyMine queries, which for specific and simple queries are working very reliable and fast. On the
 * other hand, some queries can get even worse than instantiation of the whole Sequence/Chromosome
 * in the InMemoryMode. The optimization is currently up to the Postgres subquery optimizer, which
 * does not bring the expected intelligence. However; Use the dynamic approach for simpe and
 * straight forward tasks. Use in memory mode if you want to to complex filters.
 * 
 * @author Markus Brosch
 */
public class UseCasesNotInMemory extends UseCasesInMemory {

  public UseCasesNotInMemory() throws Exception {
    super();
  }

  /**
   * main
   * 
   * @param args
   *        nothing
   * @throws Exception
   *         for your convenience
   */
  public static void main(String[] args) throws Exception {
    UseCasesNotInMemory uc = new UseCasesNotInMemory();
    uc.containedByLocation();
    uc.overlapsLocation();
    uc.hasA();
    //uncomment uc.and() to see how slow it CAN be to use the dynamic/non memory mode
    //uc.and();
  }
}