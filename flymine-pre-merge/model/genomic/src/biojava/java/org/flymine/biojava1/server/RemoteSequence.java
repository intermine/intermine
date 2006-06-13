package org.flymine.biojava1.server;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;

import org.biojava.bio.Annotation;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.Feature.Template;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.Edit;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.utils.ChangeListener;
import org.biojava.utils.ChangeType;
import org.biojava.utils.ChangeVetoException;
import org.flymine.biojava1.bio.SequenceFM;

/**
 * This RemoteSequence can be used to instantiate a sequences remotely and keep it in a remote
 * "repository". Therefore you can work with it interactivly by using a RMI client.
 * <p>
 * <b>Why using a client/server? </b> <br>
 * Instatiation of a Sequence can take a while, depending on its size and the amount of features. If
 * you want to work interactivly with this sequence, you don't want to instantiate it again and
 * again. Therefore use the SequenceServer and connect with this client by using a remote sequence.
 * The first attempt to get the Sequence takes as long as the normal instatiation of the Sequence
 * (as it is added to the server and has to be instantiated), but once you connect the 2nd time,
 * you'll get the sequence immediately. That makes life much more convenient ;-)
 * <p>
 * <b>USAGE: </b> <br>
 * You can use this SemoteSequence by simply compiling and generating the stub (see RMI docu or use
 * the provided ANT script) starting the main() and connecting from an RMI client: <br>
 * <code>
 * IRemoteSequence remoteSequence = 
 * (IRemoteSequence) Naming.lookup("rmi://localhost:" + Registry.REGISTRY_PORT + "/RemoteSequence");
 * </code>
 * <br>
 * The first thing you have to do is to select the working Chromosome. By executing the method: <br>
 * <code>
 * remoteSequence.selectWorkingChromosome("Drosophila melanogaster", "4");
 * </code><br>
 * If you are not doing so, you'll get a NullPointerException pointing out that you havn't choosen
 * "your" chromosome.
 * <p>
 * <b>MEMORY: </b> <br>
 * This RemoteSequence referes to a SequenceRepository, containing as much Sequences/Chromosomes as
 * fit into memory. Once there is a shortage in memory, old Sequences/Chromosomes are released -
 * they have to be instantiated again once you refer to them. Take this rule: <br>
 * For a Chromosome with 20.000.000 bps and about 50.000 features, you'll need about 150-200MB.
 * Thus, giving the RemoteSequence 1GB-2GB RAM isn't too bad for some Sequences. Define the amount
 * of memory you want to use within the ANT script
 * <p>
 * <b>DEVELOPERS: </b> <br>
 * This RemoteSequences is a bit tricky; It implements IRemoteSequence where each method throws an
 * RemoteException as it is needed for a RMI method. This is a connvention and the RMI stub is
 * generated with these Exceptions. This implementation on the other hand does not throw these
 * RemoteExceptions, as I wouldn't be allowed to implement Sequence interface. Be aware of this
 * fact!
 * 
 * @author Markus Brosch
 */
public class RemoteSequence extends UnicastRemoteObject implements IRemoteSequence, Sequence {

  /**
   * the remote SequenceFM
   */
  private SequenceFM _seq;

  // =======================================================================
  // Constructor
  // =======================================================================

  /**
   * constructor
   * 
   * @throws RemoteException
   *         if an RemoteException occurs
   */
  public RemoteSequence() throws RemoteException {
    super();
  }

  // =======================================================================
  // Implements IRemoteSequence
  // =======================================================================

  /**
   * select the Chromosome/Sequence you want to work on. Always the first thing to do once you have
   * the remote sequence on your client.
   * 
   * @param pOrganism
   *        the organism, e.g. "Drosophila melanogaster"
   * @param pIdentifier
   *        the identifier, e.g. "4"
   */
  public void selectWorkingChromosome(final String pOrganism, final String pIdentifier) {
    _seq = SequenceRepository.getSequenceFM(pOrganism, pIdentifier);
  }

  /**
   * @see org.biojava.bio.seq.Sequence#getURN()
   */
  public String getURN() {
    if (_seq == null)
        throw new NullPointerException("you have not choosen a chromosome; _seq is null");
    return _seq.getURN();
  }

  /**
   * @see org.biojava.bio.seq.Sequence#getName()
   */
  public String getName() {
    if (_seq == null)
        throw new NullPointerException("you have not choosen a chromosome; _seq is null");
    return _seq.getName();
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#getAlphabet()
   */
  public Alphabet getAlphabet() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.getAlphabet();
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#length()
   */
  public int length() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.length();
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#symbolAt(int)
   */
  public Symbol symbolAt(final int pIndex) throws IndexOutOfBoundsException {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.symbolAt(pIndex);
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#toList()
   */
  public List toList() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.toList();
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#iterator()
   */
  public Iterator iterator() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.iterator();
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#subList(int, int)
   */
  public SymbolList subList(final int pStart, final int pEnd) throws IndexOutOfBoundsException {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.subList(pStart, pEnd);
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#seqString()
   */
  public String seqString() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.seqString();
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#subStr(int, int)
   */
  public String subStr(final int pStart, final int pEnd) throws IndexOutOfBoundsException {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.subStr(pStart, pEnd);
  }

  /**
   * @see org.biojava.bio.symbol.SymbolList#edit(org.biojava.bio.symbol.Edit)
   */
  public void edit(final Edit pEdit) throws ChangeVetoException {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    _seq.edit(pEdit);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#countFeatures()
   */
  public int countFeatures() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.countFeatures();
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#features()
   */
  public Iterator features() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.features();
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#filter(org.biojava.bio.seq.FeatureFilter, boolean)
   * @deprecated
   */
  public FeatureHolder filter(final FeatureFilter pFilter, final boolean pRecurse) {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.filter(pFilter, pRecurse);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#filter(org.biojava.bio.seq.FeatureFilter)
   */
  public FeatureHolder filter(final FeatureFilter pFilter) {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.filter(pFilter);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#createFeature(org.biojava.bio.seq.Feature.Template)
   */
  public Feature createFeature(final Template pTempl) throws BioException, ChangeVetoException {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.createFeature(pTempl);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#removeFeature(org.biojava.bio.seq.Feature)
   */
  public void removeFeature(final Feature pFeature) throws ChangeVetoException, BioException {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    _seq.removeFeature(pFeature);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#containsFeature(org.biojava.bio.seq.Feature)
   */
  public boolean containsFeature(final Feature pFeature) {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.containsFeature(pFeature);
  }

  /**
   * @see org.biojava.bio.seq.FeatureHolder#getSchema()
   */
  public FeatureFilter getSchema() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.getSchema();
  }

  /**
   * @see org.biojava.bio.Annotatable#getAnnotation()
   */
  public Annotation getAnnotation() {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.getAnnotation();
  }

  /**
   * @see org.flymine.biojava1.server.IRemoteSequence#addChangeListener(org.biojava.utils.ChangeListener)
   */
  public void addChangeListener(final ChangeListener pCL) {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    _seq.addChangeListener(pCL);
  }

  /**
   * @see org.flymine.biojava1.server.IRemoteSequence#addChangeListener(org.biojava.utils.ChangeListener,
   *      org.biojava.utils.ChangeType)
   */
  public void addChangeListener(final ChangeListener pCL, final ChangeType pCT) {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    _seq.addChangeListener(pCL, pCT);
  }

  /**
   * @see org.flymine.biojava1.server.IRemoteSequence#removeChangeListener(org.biojava.utils.ChangeListener)
   */
  public void removeChangeListener(final ChangeListener pCL) {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    _seq.removeChangeListener(pCL);
  }

  /**
   * @see org.flymine.biojava1.server.IRemoteSequence#removeChangeListener(org.biojava.utils.ChangeListener,
   *      org.biojava.utils.ChangeType)
   */
  public void removeChangeListener(final ChangeListener pCL, final ChangeType pCT) {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    _seq.removeChangeListener(pCL, pCT);
  }

  /**
   * @see org.flymine.biojava1.server.IRemoteSequence#isUnchanging(org.biojava.utils.ChangeType)
   */
  public boolean isUnchanging(final ChangeType pCT) {
    if (_seq == null)
        throw new NullPointerException("you have not selected a chromosome; _seq is null");
    return _seq.isUnchanging(pCT);
  }

  // =======================================================================
  // main method to start the server/remoteSequence 
  // =======================================================================   
  
  /**
   * Start the server; you can attach to this server with a RMI client: <br>
   * <code>
   * IRemoteSequence s = 
   * (IRemoteSequence) Naming.lookup("rmi://localhost:" + Registry.REGISTRY_PORT + "/RemoteSequence");
   * </code>
   * 
   * @param pArgs
   *        not used
   */
  public static void main(String pArgs[]) {
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }
    try {
      LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
      final RemoteSequence obj = new RemoteSequence();
      final String url = "rmi://localhost:" + Registry.REGISTRY_PORT + "/RemoteSequence";
      Naming.rebind(url, obj);
      System.out.println("RemoteSequence to " + url + " bound\n" +
          "You can start your RMI clients and connect to RemoteSequence - have fun ;-)");
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}