package org.flymine.biojava1.app.useCase;

import org.flymine.biojava1.server.IRemoteSequence;
import org.flymine.biojava1.utils.TextProgressBar;

import java.rmi.ConnectException;
import java.rmi.Naming;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * This is an example RMI client. Why using a client/server? Instatiation of a Sequence can take a
 * while, depending on its size and the amount of features. If you want to work interactivly with
 * this sequence, you don't want to instantiate it again and again. Therefore use the RemoteSequence
 * and connect with this client by using a remote sequence. The first attempt to get the Sequence
 * takes as long as the normal instatiation of the Sequence (as it is added to the server and has to
 * be instantiated), but once you connect the 2nd time, you'll get the sequence immediately. That
 * makes life much more convenient ;-)
 * <p>
 * Before you can execute this client, start the RemoteSequence - it provides a main() to start a
 * RMI registry and connects itself to the registry. Check out the ANT tasks to compile and start
 * the server
 * 
 * @author Markus Brosch
 */
public class UseCaseRmiClient {

  /**
   * Example client - execute at least twice to see the advantage of time saving using a
   * RemoteSequence
   * 
   * @param args
   *        not used
   * @throws Exception
   *         for demonstration - don't throw your Exceptions like that in your production code!
   */
  public static void main(String[] args) throws Exception {
    long t1 = System.currentTimeMillis();
    final String rmiAddress = "rmi://localhost:1099/RemoteSequence";
    try {
    //get the remote sequence
    TextProgressBar bar = new TextProgressBar();
    bar.start();
    IRemoteSequence remoteSeq = (IRemoteSequence) Naming
        .lookup(rmiAddress);
    //first thing you have to do: select working chromosome / sequence
    remoteSeq.selectWorkingChromosome("Drosophila melanogaster", "4");
    bar.stop();
    //operate on remote sequence
    System.out.println("seqName: " + remoteSeq.getName());
    System.out.println("# of features on this sequence/chromosome: " + remoteSeq.countFeatures());
    } catch(ConnectException e) {
      System.err.println("Not found -> " + rmiAddress);
      System.err.println("No connection to RemoteSequence - possibly no RemoteSequence running?");
      System.exit(1);
    }
    long t2 = System.currentTimeMillis();
    System.out.println("total time: " + (t2 - t1) / 1000.0 + "sec");
    System.out.println("\nthe full Sequence is now hold in the remote sequence!\n" +
        "run client again to see the differnce!");
  }

}