package org.flymine.biojava1.exceptions;

import java.lang.reflect.Field;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * Exception class for Exceptions in model
 * 
 * @author Markus Brosch
 */
public class ModelException extends Exception {

  // =======================================================================
  // public static attributes - used to descibe what type this exception
  // =======================================================================

  /**
   * Specified class type is not in model
   */
  public static final int CLASSNOTINMODEL = -1001;

  /**
   * Specified field does not exist
   */
  public static final int FIELDNOTEXISTING = -1002;

  /**
   * Specified field has the wrong type
   */
  public static final int FIELDTYPE = -1003;

  /**
   * Specified field is not an attribute
   */
  public static final int FIELDNOATTRIBUTE = -1004;

  // =======================================================================
  // Attributes private
  // =======================================================================

  /**
   * represents one of the above types
   */
  protected final int _type; //required

  /**
   * an optional nested exception message
   */
  protected String _nestedMessage;

  /**
   * an optional root exception
   */
  protected Exception _rootCause;

  // =======================================================================
  // Constructor
  // =======================================================================

  /**
   * Constructor
   * 
   * @param pType
   *          any of the public static TYPE values available in ModelException
   */
  public ModelException(final int pType) {
    this(pType, null, null);
  }

  /**
   * Constructor
   * 
   * @param pType
   *          any of the public static TYPE values available in ModelException
   * @param pMessage
   *          a message
   */
  public ModelException(final int pType, String pMessage) {
    this(pType, pMessage, null);
  }

  /**
   * Constructor
   * 
   * @param pType
   *          any of the public static TYPE values available in ModelException
   * @param pRootException
   *          a root exception
   */
  public ModelException(final int pType, final Exception pRootException) {
    this(pType, null, pRootException);
  }

  /**
   * Constructor
   * 
   * @param pType
   *          any of the public static TYPE values available in ModelException
   * @param pMessage
   *          a message
   * @param pRootException
   *          a root exception
   */
  public ModelException(final int pType, final String pMessage, final Exception pRootException) {
    super(pMessage);

    if (!this.typeAvailable(pType)) { throw new IllegalArgumentException("wrong type; type "
        + pType + " is not available"); }

    this._type = pType;
    if (pRootException != null) {
      pRootException.fillInStackTrace();
      this._nestedMessage = pRootException.getMessage();
      this._rootCause = pRootException;
    }
  }

  // =======================================================================
  // Class specific methods
  // =======================================================================

  /**
   * to get the type of this exception
   * 
   * @return type as an int value
   */
  public int getType() {
    return this._type;
  }

  /**
   * @see Throwable#printStackTrace()
   */
  public void printStackTrace() {
    System.err.print("ModelExceptionFM of type ");
    final Class clazz = this.getClass();
    final Object[] fields = clazz.getFields();
    for (int i = 0; i < fields.length; i++) {
      final Field o = (Field) fields[i];
      try {
        int typeAvailable = o.getInt(o.getName());
        if (typeAvailable == this._type) {
          System.err.println(o.getName());
          printStackTrace(System.err);
          break;
        }
      } catch (IllegalArgumentException e) {
        //ignore fields which are not of type int
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * @return a nested message which is eventually provided with a root cause/exception
   */
  public String getNestedMessage() {
    if (this._nestedMessage != null) { return this._nestedMessage; }
    return "No nested messages";
  }

  /**
   * @return test if there is a root cause / exception was passed in
   */
  public boolean rootCauseExists() {
    return (this._rootCause != null);
  }

  /**
   * @return the root cause / exception
   */
  public Exception getRootCause() {
    return this._rootCause;
  }

  //-----------------------------------------------------------------------
  // private
  // -----------------------------------------------------------------------

  /**
   * Checks if passedInType is available in this class as a public field.
   * 
   * @param pPassedInType
   *          description of ModelException. Basically valid for any of the public final static
   *          fields of this class.
   * @return true, if passedInType is available, false else
   */
  protected boolean typeAvailable(final int pPassedInType) {
    //use introspection to check if type is available
    final Object[] fields = this.getClass().getFields();
    for (int i = 0; i < fields.length; i++) {
      final Field o = (Field) fields[i];
      try {
        final int typeAvailable = o.getInt(o.getName());
        if (typeAvailable == pPassedInType) return true;
      } catch (IllegalArgumentException e) {
        //ignore fields which are not of type int
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return false;
  }
}