package org.flymine.biojava1.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.flymine.biojava1.exceptions.ModelException;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * @author Markus Brosch
 */
public class ModelUtilsSoFa extends ModelUtils {

  // =======================================================================
  // Attributes
  // =======================================================================

  /**
   * logger
   */
  private static final Logger _log = Logger.getLogger(ModelUtilsSoFa.class);

  /**
   * singleton - instance
   */
  private static ModelUtilsSoFa _instance = null;

  /**
   * the underlying SoFa model
   */
  private Model _sofaModel;

  /**
   * Lookuptable for type2fieldName; e.g. "RepeatRegion" -\> "repeatRegions"
   */
  private Map _typeName2fieldName = null;

  /**
   * Lookuptable for has_a/part_of relations; e.g. "Gene" -\> {"Transcript","RegulatoryRegion"}
   */
  private Map _hasA = null;

  /**
   * Lookuptable for inverse has_a/part_of relation; e.g. "Transcript" -\>
   * {"Gene","TransposableElementGene"}
   */
  private Map _invHasA = null;

  // =======================================================================
  // Constructor
  // =======================================================================

  /**
   * private constructor
   */
  private ModelUtilsSoFa() {
    super(ModelFactory.getSofaModel());
    _sofaModel = super.getModel();
  }

  // =======================================================================
  // Class specific methods
  // =======================================================================

  /**
   * Get an ModelUtilSoFa instance for the SoFa model
   * 
   * @return instance of ModelUtils for pModel
   */
  public static ModelUtilsSoFa getInstance() {
    if (_instance == null) {
      _instance = new ModelUtilsSoFa();
    }
    return _instance;
  }

  /**
   * type -> field name, e.g. Transcript will return transcripts, as this is the fieldname of a
   * Transcript, e.g. of a Gene
   * 
   * @param pTypeName
   *        Type name, e.g. Gene
   * @return return field name of pTypeName, e.g. genes
   */
  public String typeName2FieldName(final String pTypeName) {
    if (_typeName2fieldName == null) {
      generateLookupTable();
    }
    assert (_typeName2fieldName != null);
    return (String) _typeName2fieldName.get(pTypeName);
  }

  /**
   * get all hasA/partOf relations of pUnqualifiedClassNameOfRoot
   * 
   * @param pUnqualifiedClassNameOfRoot
   *        the unqualified class name of your type you are interested
   * @return null if pUnqualifiedClassNameOfRoot does not exist, else Collection of all inverse hasA
   *         relations to pUnqualifiedClassNameOfRoot
   */
  public Collection hasA(final String pUnqualifiedClassNameOfRoot) {
    if (_hasA == null) {
      generateLookupTable();
    }
    assert (_hasA != null);
    return (Collection) _hasA.get(pUnqualifiedClassNameOfRoot);
  }

  /**
   * get all inverse hasA/partOf relations of pUnqualifiedClassNameOfRoot
   * 
   * @param pUnqualifiedClassNameOfRoot
   *        the unqualified class name of your type you are interested
   * @return null if pUnqualifiedClassNameOfRoot does not exist, else Collection of all hasA
   *         relations to pUnqualifiedClassNameOfRoot
   */
  public Collection invHasA(final String pUnqualifiedClassNameOfRoot) {
    if (_invHasA == null) {
      generateLookupTable();
    }
    assert (_invHasA != null);
    return (Collection) _invHasA.get(pUnqualifiedClassNameOfRoot);
  }

  /**
   * check wether a type is part of model or not.
   * 
   * @param pType
   *        a unqualified class name you want to check
   * @return true if part of model
   */
  public boolean isPartOfModel(final String pType) {
    if (_typeName2fieldName == null) {
      generateLookupTable();
    }
    assert (_typeName2fieldName != null);
    if (_typeName2fieldName.containsKey(pType)) { return true; }
    return false;
  }

  // -----------------------------------------------------------------------
  // private
  // -----------------------------------------------------------------------

  /**
   * TODO
   */
  private void generateLookupTable() {
    //Lookuptable for type2fieldName; e.g. "RepeatRegion" -> "repeatRegions"
    _typeName2fieldName = new HashMap();
    //Lookuptable for has_a/part_of relations; e.g. "Gene" -> {"Transcript","RegulatoryRegion"}
    _hasA = new HashMap();
    //Lookuptable for inverse has_a/part_of relation; e.g. "Transcript" -> {"Gene"}
    _invHasA = new HashMap();

    //process all classes
    final Collection classNames = _sofaModel.getClassNames();
    for (Iterator it = classNames.iterator(); it.hasNext();) {
      final String fullyQualifiedClassName = (String) it.next();
      try {
        final Class clazz = Class.forName(fullyQualifiedClassName);
        //get ClassDescriptor and the fields of clazz
        final ClassDescriptor cd = _sofaModel.getClassDescriptorByName(fullyQualifiedClassName);
        final Map fieldsMap = _sofaModel.getFieldDescriptorsForClass(clazz);
        final Collection fields = new ArrayList();
        //for this class iterate over all fields
        for (Iterator fieldsMapIt = fieldsMap.keySet().iterator(); fieldsMapIt.hasNext();) {
          final String aKey = (String) fieldsMapIt.next();
          //skip "id"
          if (!(aKey.equals("id"))) {
            try {
              final ReferenceDescriptor rd = (ReferenceDescriptor) fieldsMap.get(aKey);
              if (rd.isCollection()) {
                //get fieldTypeName; e.g. "RepeatRegion"
                final String fieldTypeName = rd.getReferencedClassDescriptor().getUnqualifiedName();
                fields.add(fieldTypeName);
              }
            } catch (ClassCastException e) {
              //if it is not a ReferenceDescriptor, it is not interesting for us. We check only
              //classes active part of the model, like a Gene or Exon. On the other hand, a
              // identifier or name ... isn't interesting at all, so we skip these elements.
              if (Config.LOG) _log.debug(aKey + " is not a RefDesc -> not used for model");
            }
          } //if not "id"
        } //for next field

        //put has_a relation; e.g. "Gene"->{"Transcript","RegulatoryRegion"}
        final String unqName = cd.getUnqualifiedName();
        _hasA.put(unqName, fields);
        if (Config.LOG) _log.debug("_hasA.put: " + cd.getUnqualifiedName() + " -> " + fields);

        //if not contained, add fieldTypeName->fieldName; e.g. "Gene" -> "genes"
        if (!(_typeName2fieldName.containsKey(unqName))) {
          //TODO remove hardcoded beans convention
          String className = unqName.substring(1, unqName.length());
          className = (new Character(unqName.charAt(0))).toString().toLowerCase() + className + "s";
          _typeName2fieldName.put(unqName, className);
          if (Config.LOG) _log.debug("_typeName2fieldName.put: " + unqName + " -> " + className);
        }

      } catch (ClassNotFoundException e) {
        _log.error(fullyQualifiedClassName + " is not a class in model; maybe root?!");
      }

    }

    //generate inverse has_a/part_of relation
    for (Iterator it = _hasA.keySet().iterator(); it.hasNext();) {
      String aKey = (String) it.next();
      Collection values = (Collection) _hasA.get(aKey);
      for (Iterator itValues = values.iterator(); itValues.hasNext();) {
        String aValue = (String) itValues.next();
        if (!(_invHasA.containsKey(aValue))) {
          Set newValues = new HashSet();
          _invHasA.put(aValue, newValues);
        }
        Set newValues = (Set) _invHasA.get(aValue);
        newValues.add(aKey);
      }
    }
  }
}