package org.flymine.biojava1.exceptions;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * Exception class centric about exceptions which can occur in FlyMine genomic model.
 * 
 * @author Markus Brosch
 */
public class ModelExceptionFM extends ModelException {

  // =======================================================================
  // Attributes public
  // =======================================================================

  /**
   * Specified class is not a FlyMine BioProperty
   */
  public static final int CLASSNOTABIOPROPERTY = -2001;

  /**
   * Specified class is not a FlyMine BioEntity
   */
  public static final int CLASSNOTABIOENTITY = -2002;

  /**
   * Specified class in not a FlyMine Chromosome
   */
  public static final int CLASSNOTACHROMOSOME = -2003;

  // =======================================================================
  // Constructor
  // =======================================================================

  /**
   * Constructor
   * 
   * @param pType
   *          any of the public static TYPE values available in ModelExceptionFM/ModelException
   */
  public ModelExceptionFM(final int pType) {
    super(pType);
  }

  /**
   * Constructor
   * 
   * @param pType
   *          any of the public static TYPE values available in ModelExceptionFM/ModelException
   * @param pMessage
   *          a message
   */
  public ModelExceptionFM(final int pType, final String pMessage) {
    super(pType, pMessage);
  }

  /**
   * Constructor
   * 
   * @param pType
   *          any of the public static TYPE values available in ModelExceptionFM/ModelException
   * @param pRootException
   *          a root exception
   */
  public ModelExceptionFM(final int pType, final Exception pRootException) {
    super(pType, pRootException);
  }

  /**
   * Constructor
   * 
   * @param pType
   *          any of the public static TYPE values available in ModelExceptionFM/ModelException
   * @param pMessage
   *          a message
   * @param pRootException
   *          a root exception
   */
  public ModelExceptionFM(final int pType, final String pMessage, final Exception pRootException) {
    super(pType, pMessage, pRootException);
  }

}