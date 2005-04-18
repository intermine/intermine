package org.flymine.biojava1.app.useCase;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.biojava.bio.seq.io.SeqIOTools;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.biojava1.query.QueryFM;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * Simple Example how to use IOTools to make flatfiles <b>Config file: disable hasA, invHasA; enable
 * synonyms and memoryMode </b>
 * 
 * @author Markus Brosch
 */
public class IOExampleUseCase {

  /**
   * the underlying sequence we are going to work on.
   */
  protected SequenceFM _sequence;

  /**
   * setting up the sequence - we use the smallest Chromosome IV of Drosophila melanogaster (DM) for
   * our investigation
   * 
   * @throws Exception
   *         to avoid complexity, we simply get rid of all Exceptions here. <br>
   *         Don't do that in your real code ;-)
   */
  public IOExampleUseCase() throws Exception {
    _sequence = SequenceFM.getInstance("Drosophila melanogaster", "4");
    assert (_sequence != null);
    QueryFM.getInstance(_sequence);
    System.out.print("\u001b[2J");
  }

  /**
   * Example: <br>
   * You want to dump the Sequence with it's features to an EMBL/GenBank/FASTA file? Nothing easier
   * than this. There are also more formats available, check out the SeqIOTools.
   */
  public void toFile() throws IOException {
    System.out.println("\ntoFile() --------------------------------------------------------------");
    PrintStream out = new PrintStream(new FileOutputStream("/tmp/test.embl"));
    SeqIOTools.writeEmbl(out, _sequence);
    //out = new PrintStream(new FileOutputStream("/tmp/test.gbk"));
    //SeqIOTools.writeGenbank(out, _sequence);
    //out = new PrintStream(new FileOutputStream("/tmp/test.fasta"));
    //SeqIOTools.writeFasta(out, _sequence);
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
    IOExampleUseCase useCase = new IOExampleUseCase();
    useCase.toFile();
  }
}