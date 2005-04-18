package org.flymine.biojava1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

import org.biojava.bio.Annotation;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.Feature.Template;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.Edit;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.utils.ChangeListener;
import org.biojava.utils.ChangeType;
import org.biojava.utils.ChangeVetoException;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * defines the methods of a RemoteSequence
 * <p>
 * all methods of Sequence interface are defined here again, with the difference that all methods
 * throw RemoteException; as all methods have to be defined with RemoteException for RMI, I can't
 * extend Sequence. Therefore this interface is identical to all methods of Sequence and the
 * implementation of this interface implements this interface and the sequence interface. 
 * 
 * @author Markus Brosch
 */
public interface IRemoteSequence extends Remote {

  /**
   * Server works on a specific Chromosome/Sequence
   * 
   * @param pOrganism
   *        the organism, e.g. "Drosophila melanogaster"
   * @param pIdentifier
   *        the identifier, e.g. "4"
   */
  void selectWorkingChromosome(String pOrganism, String pIdentifier) throws RemoteException;

  // ==============================================================================================
  //
  // =============================================================================================

  /**
   * @see org.biojava.bio.seq.Sequence#getURN()
   */
  String getURN() throws RemoteException;

  /**
   * @see org.biojava.bio.seq.Sequence#getName()
   */
  String getName() throws RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#getAlphabet()
   */
  Alphabet getAlphabet() throws RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#length()
   */
  int length() throws RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#symbolAt(int)
   */
  Symbol symbolAt(int pIndex) throws IndexOutOfBoundsException, RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#toList()
   */
  List toList() throws RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#iterator()
   */
  Iterator iterator() throws RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#subList(int, int)
   */
  SymbolList subList(int pStart, int pEnd) throws IndexOutOfBoundsException, RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#seqString()
   */
  String seqString() throws RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#subStr(int, int)
   */
  String subStr(int pStart, int pEnd) throws IndexOutOfBoundsException, RemoteException;

  /**
   * @see org.biojava.bio.symbol.SymbolList#edit(org.biojava.bio.symbol.Edit)
   */
  void edit(Edit pEdit) throws IndexOutOfBoundsException, IllegalAlphabetException,
      ChangeVetoException, RemoteException;

  /**
   * @see org.biojava.utils.Changeable#addChangeListener(org.biojava.utils.ChangeListener)
   */
  void addChangeListener(ChangeListener pCL) throws RemoteException;

  /**
   * @see org.biojava.utils.Changeable#addChangeListener(org.biojava.utils.ChangeListener,
   *      org.biojava.utils.ChangeType)
   */
  void addChangeListener(ChangeListener pCL, ChangeType pCT) throws RemoteException;

  /**
   * @see org.biojava.utils.Changeable#removeChangeListener(org.biojava.utils.ChangeListener)
   */
  void removeChangeListener(ChangeListener pCL) throws RemoteException;

  /**
   * @see org.biojava.utils.Changeable#removeChangeListener(org.biojava.utils.ChangeListener,
   *      org.biojava.utils.ChangeType)
   */
  void removeChangeListener(ChangeListener pCL, ChangeType pCT) throws RemoteException;

  /**
   * @see org.biojava.utils.Changeable#isUnchanging(org.biojava.utils.ChangeType)
   */
  boolean isUnchanging(ChangeType pCT) throws RemoteException;

  /**
   * @see org.biojava.bio.seq.FeatureHolder#countFeatures()
   */
  int countFeatures() throws RemoteException;

  /**
   * @see org.biojava.bio.seq.FeatureHolder#features()
   */
  Iterator features() throws RemoteException;

  /**
   * @see org.biojava.bio.seq.FeatureHolder#filter(org.biojava.bio.seq.FeatureFilter, boolean)
   */
  FeatureHolder filter(FeatureFilter pFilter, boolean pRecurse) throws RemoteException;

  /**
   * @see org.biojava.bio.seq.FeatureHolder#filter(org.biojava.bio.seq.FeatureFilter)
   */
  FeatureHolder filter(FeatureFilter pFilter) throws RemoteException;

  /**
   * @see org.biojava.bio.seq.FeatureHolder#createFeature(org.biojava.bio.seq.Feature.Template)
   */
  Feature createFeature(Template pTempl) throws BioException, ChangeVetoException, RemoteException;

  /**
   * @see org.biojava.bio.seq.FeatureHolder#removeFeature(org.biojava.bio.seq.Feature)
   */
  void removeFeature(Feature pFeature) throws ChangeVetoException, BioException, RemoteException;

  /**
   * @see org.biojava.bio.seq.FeatureHolder#containsFeature(org.biojava.bio.seq.Feature)
   */
  boolean containsFeature(Feature pFeature) throws RemoteException;

  /**
   * @see org.biojava.bio.seq.FeatureHolder#getSchema()
   */
  FeatureFilter getSchema() throws RemoteException;

  /**
   * @see org.biojava.bio.Annotatable#getAnnotation()
   */
  Annotation getAnnotation() throws RemoteException;
}