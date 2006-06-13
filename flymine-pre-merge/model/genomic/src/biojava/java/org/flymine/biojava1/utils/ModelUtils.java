package org.flymine.biojava1.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.flymine.biojava1.exceptions.ModelException;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Class for various model centric utilities.
 * 
 * @author Markus Brosch
 */
public class ModelUtils {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * singleton; As ModelUtils ca be instanciated with different models, these different instances
   * are kept
   */
  private static Map _instances = new HashMap();

  /**
   * the underlying model this class works on
   */
  private final Model _model; //required

  /**
   * @return Returns the _model.
   */
  protected Model getModel() {
    return _model;
  }

  // =======================================================================
  // Constructor
  // =======================================================================

  /**
   * Constructor
   * 
   * @param pModel
   *        The model you want to use the utilities with
   */
  protected ModelUtils(final Model pModel) {
    if (pModel == null) { throw new NullPointerException("model cannot be null"); }
    _model = pModel;
  }

  // =======================================================================
  // Class specific methods
  // =======================================================================

  /**
   * Get an ModelUtil instance for a specific model
   * 
   * @param pModel
   *        get the instance of ModelUtils for this underlying model
   * @return instance of ModelUtils for pModel
   */
  public static ModelUtils getInstance(final Model pModel) {
    ModelUtils utils = (ModelUtils) _instances.get(pModel);
    if (utils == null) {
      utils = new ModelUtils(pModel);
      _instances.put(pModel, utils);
    }
    return utils;
  }

  /**
   * Method to get a class within the model by either a fully qualified or unqualified type name
   * 
   * @param pTypeName
   *        the name of the class, either fully qualified or unqualified
   * @return class of type pTypeName
   * @throws ModelException
   *         if Class not available in model
   */
  public Class getClassForTypeName(final String pTypeName) throws ModelException {
    final String fullyQualifiedClassName = getFullyQualifiedClassName(pTypeName);
    try {
      return Class.forName(fullyQualifiedClassName);
    } catch (ClassNotFoundException e) {
      //class have to exist when getFullyQualifiedClassName was not throwing a ModelException
      throw new RuntimeException("for " + fullyQualifiedClassName + " no class found");
    }
  }

  /**
   * Method to retrieve a fully qualified class name from an unqualified class name
   * 
   * @param pTypeName
   *        the unqualified name (if you pass in a qualified name, it returns this qualified name)
   * @return the fully qualified class name
   * @throws ModelException
   *         if no class was found (within the underlying model) for pTypeName
   */
  public String getFullyQualifiedClassName(final String pTypeName) throws ModelException {
    try {
      //if typeName is a fully qualified class name
      DynamicUtil.decomposeClass(Class.forName(pTypeName));
      return pTypeName;

    } catch (ClassNotFoundException e) {
      String packageName = null;
      try {
        //if typeName is an unqualified class name
        packageName = _model.getPackageName();
        final Class clazz = Class.forName(packageName + "." + pTypeName);
        assert (clazz != null);
        return clazz.getName();

      } catch (ClassNotFoundException ee) {
        //throws exception if typeName is not a class name at all
        throw new ModelException(ModelException.CLASSNOTINMODEL, "Class: " + packageName + "."
            + pTypeName + " does not exist in the " + _model.getName() + " model", e);
      }
    }
  }

  /**
   * Get a unqualified class name for a dynamic class in model. Extends the simple class.getName()
   * method, as this method is not appropriate, for dynamic objects like e.g.
   * org.flymine.model.genomic.Exon$$EnhancerByCGLIB$$4a057d14. In this example we would expect a
   * return value of "Exon". If pClass does somehow does not exist, it returns null (unexpected)
   * 
   * @param pClazz
   *        a class
   * @return unqualified class name for pClazz; null if pClazz does not exist (not expected)
   */
  public String getUnqualifiedClassName(final Class pClazz) {
    final Set clazzes = DynamicUtil.decomposeClass(pClazz);
    for (Iterator it = clazzes.iterator(); it.hasNext();) {
      Class aClass = (Class) it.next();
      return TypeUtil.unqualifiedName(aClass.getName());
    }
    return null;
  }

  /**
   * Method to check wether clazz has specified fieldName
   * 
   * @param pClazz
   *        underlying class
   * @param pFieldName
   *        field to check if available in pClazz
   * @return true, if fieldName is part of clazz fields <code>  
   public boolean checkFieldInClass(final Class pClazz, final String pFieldName) {
   Set keysOfFields = keysOfFields(pClazz).keySet();
   for (Iterator it = keysOfFields.iterator(); it.hasNext();) {
   String o = (String) it.next();
   if (o.equals(pFieldName)) { return true; }
   }
   return false;
   }
   </code>
   */

  /**
   * Method to check wether a specific field of a class is of type fieldType
   * 
   * @param pClazz
   *        underlying class
   * @param pFieldName
   *        field to check in pClass
   * @param pFieldType
   *        field type to check in pClazz according to pFieldName
   * @return true, if pFieldType is the type of the field with pFieldName in Class pClazz; <br>
   *         else throws ModelExceptionFM
   * @throws ModelException
   *         First case, fieldType is NOT the type of the field with fieldName in Class clazz.
   *         Excpetion of Type ModelException.FIELDTYPE <br>
   *         Second case, fieldType is not a attribute; ModelException.FIELDNOATTRIBUTE
   */
  public boolean checkFieldType(final Class pClazz, final String pFieldName, final Object pFieldType)
      throws ModelException {
    //test wether field type corresponds to parameter "value"
    final Map possibleFields = _model.getFieldDescriptorsForClass(pClazz);
    final FieldDescriptor fieldDesc = (FieldDescriptor) possibleFields.get(pFieldName);
    if (fieldDesc.isAttribute()) {
      final AttributeDescriptor attrDesc = (AttributeDescriptor) fieldDesc;
      if (pFieldType.getClass().getName().equals(attrDesc.getType())) {
        return true;
      } else {
        throw new ModelException(ModelException.FIELDTYPE, "FieldType must be of type: "
            + attrDesc.getType() + " and is of type: " + pFieldType.getClass().getName());
      }
    } else {
      throw new ModelException(ModelException.FIELDNOATTRIBUTE, "\"" + pFieldName
          + "\" is not an attribute");
    }
  }

  /**
   * check wether pSubClass IS_A pSuperClass within the model
   * 
   * @param pSuperClass
   *        the superclass name
   * @param pSubClass
   *        the subclass to test
   * @return true, if pSubClass IS_A pSuperClass, else false
   * @throws ModelException
   *         if pSuperClass or pSubClass not part of model
   */
  public boolean checkSuperClass(final String pSuperClass, final Class pSubClass)
      throws ModelException {
    final String superClassName = getFullyQualifiedClassName(pSuperClass);
    if (superClassName.equals(pSubClass.getName())) { return true; }

    final ClassDescriptor cld = _model.getClassDescriptorByName(superClassName);
    final Set bioPropertiesAndChilds = _model.getAllSubs(cld);
    for (Iterator iterator = bioPropertiesAndChilds.iterator(); iterator.hasNext();) {
      final ClassDescriptor classDescriptor = (ClassDescriptor) iterator.next();
      final String unqualClassName = classDescriptor.getUnqualifiedName();
      final String keyClassNameShort = getUnqualifiedClassName(pSubClass);
      if (unqualClassName.equals(keyClassNameShort)) { return true; }
    }
    return false;
  }
}