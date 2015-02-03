/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.bio.chado.config.ConfigAction;
import org.intermine.bio.chado.config.DoNothingAction;
import org.intermine.bio.chado.config.SetFieldConfigAction;
import org.intermine.bio.dataconversion.PhytozomeProcessor;

/**
 * @author jcarlson
 *
 */
public final class PhytozomeRelationProcessor extends PhytozomeProcessor {

  /**
   * @param parent
   */
  public PhytozomeRelationProcessor(PhytozomeProcessor parent) {
    super(parent);
  }
  /**
   * Use the feature_relationship table to set relations (references
   * and collections) between features.
   */
  @Override
  public void process(Connection connection)
      throws SQLException, ObjectStoreException {

    process(connection,true);
    process(connection,false);
  }
  private void process(Connection connection,
      boolean subjectFirst) throws SQLException, ObjectStoreException {
    ResultSet res =
        getFeatureRelationshipResultSet(connection, subjectFirst);
    Integer lastSubjectId = null;

    // Map from relation type to Map from object type to FeatureData
    // - used to collect up all the collection/reference information
    // for one subject feature
    // i.e. derives_from -> ( transcript -> [list of transcript(s)]
    //           ( gene    -> [list of gene(s)]
    Map<String, Map<String, List<FeatureData>>> relTypeMap =
        new HashMap<String, Map<String, List<FeatureData>>>();
    int featureWarnings = 0;
    int collectionWarnings = 0;
    int count = 0;
    int collectionTotal = 0;
    while (res.next()) {
      Integer featRelationshipId =
          new Integer(res.getInt("feature_relationship_id"));
      Integer firstFeature1Id = new Integer(res.getInt("feature1_id"));
      Integer secondFeatureId = new Integer(res.getInt("feature2_id"));
      String relationTypeName = res.getString("type_name");

      if (lastSubjectId != null &&
          !firstFeature1Id.equals(lastSubjectId)) {
        if (!processCollectionData(lastSubjectId, relTypeMap,
            collectionWarnings, subjectFirst)) {
          collectionWarnings++;
          if (collectionWarnings == 20) {
            LOG.warn("ignoring further unknown warnings from "
                + "processCollectionData()");
          }
        }
        collectionTotal += relTypeMap.size();
        relTypeMap =
            new HashMap<String, Map<String, List<FeatureData>>>();
      }

      if (PART_OF_RELATIONS.contains(relationTypeName) && !subjectFirst) {
        // special case for part_of relations - they are directional
        continue;
      }

      if (featureMap.containsKey(firstFeature1Id)) {
        if (featureMap.containsKey(secondFeatureId)) {
          FeatureData objectFeatureData =
              featureMap.get(secondFeatureId);
          if (!relTypeMap.containsKey(relationTypeName)) {
            relTypeMap.put(relationTypeName,
                new HashMap<String, List<FeatureData>>());
          }
          Map<String, List<FeatureData>> objectClassFeatureDataMap =
              relTypeMap.get(relationTypeName);

          String objectFeatureType =
              objectFeatureData.getInterMineType();
          if (!objectClassFeatureDataMap.containsKey(objectFeatureType)) {
            objectClassFeatureDataMap.put(objectFeatureType,
                new ArrayList<FeatureData>());
          }
          List<FeatureData> featureDataList =
              objectClassFeatureDataMap.get(objectFeatureType);
          featureDataList.add(objectFeatureData);
        } else {
          if (featureWarnings <= 20) {
            if (featureWarnings < 20) {
              LOG.warn("object_id " + secondFeatureId
                  + " from feature_relationship "
                  + featRelationshipId
                  + " was not found in the feature table");
            } else {
              LOG.warn("further warnings ignored");
            }
            featureWarnings++;
          }
        }
      } else {
        if (featureWarnings <= 20) {
          if (featureWarnings < 20) {
            LOG.warn("subject_id " + firstFeature1Id
                + " from feature_relationship "
                + featRelationshipId
                + " was not found in the feature table");
          } else {
            LOG.warn("further warnings ignored");
          }
          featureWarnings++;
        }
      }
      count++;
      lastSubjectId = firstFeature1Id;
    }
    if (lastSubjectId != null) {
      processCollectionData(lastSubjectId, relTypeMap,
          collectionWarnings, subjectFirst);
      collectionTotal += relTypeMap.size();
    }
    LOG.info("processed " + count + " relations");
    LOG.info("total collection elements created: " + collectionTotal);
    res.close();
  }

  /**
   * Return the interesting rows from the feature_relationship table. The feature pairs are
   * returned in both subject, object and object, subject orientations so that the relationship
   * processing can be configured in a natural way.
   * This is a protected method so that it can be overridden for testing
   * @param connection the db connection
   * @param subjectFirst if true the subject_id column from the relationship table will be before
   *  the object_id in the results, otherwise it will be after. ie.
   *  "feature_relationship_id, subject_id as feature1_id, object_id as feature2_id, ..."
   *  vs "feature_relationship_id, object_id as feature1_id, subject_id as feature2_id, ..."
   * @return the SQL result set
   * @throws SQLException if a database problem occurs
   */
  protected ResultSet getFeatureRelationshipResultSet(Connection connection,
      boolean subjectFirst) throws SQLException {
    String subObjString;
    if (subjectFirst) {
      subObjString = "subject_id as feature1_id, object_id as feature2_id,";
    } else {
      subObjString = "object_id as feature1_id, subject_id as feature2_id,";
    }
    String extraQueryBits = "";
    if (subjectFirst) {
      extraQueryBits = getGenesProteinsQuery();
    }
    String query = "SELECT feature_relationship_id, " + subObjString
        + " cvterm.name AS type_name"
        + " FROM feature_relationship, cvterm,"
        + " " + tempFeatureTableName + " t1,"
        + " " + tempFeatureTableName + " t2 "
        + " WHERE cvterm.cvterm_id = feature_relationship.type_id"
        + "   AND subject_id = t1.feature_id "
        + "   AND object_id = t2.feature_id "
        + extraQueryBits
        + " ORDER BY feature1_id";
    LOG.info("executing getFeatureRelationshipResultSet(): " + query);
    Statement stmt = connection.createStatement();
    ResultSet res = stmt.executeQuery(query);
    LOG.info("got resultset.");
    return res;
  }

  /**
   * Return extra SQL that will be added to the feature_relationships
   * table query. It will add fake relationships to the query to
   * make it look like there are gene <-> protein relations with type
   * 'derives_from'.
   */
  private String getGenesProteinsQuery() {
    String partOfConstraints =
        makeOrConstraints("fr12type.name",PART_OF_RELATIONS);
    String derivesFromConstraints =
        makeOrConstraints("fr23type.name",DERIVES_FROM_RELATIONS);

    return " UNION ALL SELECT "
    + "0, f1.feature_id AS feature1_id, f3.feature_id AS feature2_id, "
    + " 'derives_from' "
    + " FROM "
    + " feature_relationship fr12, cvterm fr12type, "
    + " feature_relationship fr23, cvterm fr23type, "
    +  tempFeatureTableName + " f1, "
    +  tempFeatureTableName + " f2, "
    +  tempFeatureTableName + " f3 "
    + " WHERE fr12.subject_id = fr23.object_id "
    + " AND fr12.type_id = fr12type.cvterm_id "
    + " AND (" + partOfConstraints + ") "
    + " AND fr23.type_id = fr23type.cvterm_id "
    + " AND (" + derivesFromConstraints + ")"
    + " AND f1.feature_id = fr12.object_id "
    + " AND f2.feature_id = fr12.subject_id "
    + " AND f2.feature_id = fr23.object_id "
    + " AND f3.feature_id = fr23.subject_id "
    + " AND f1.type_id=" + cvTermMap.get("sequence").get("gene")
    + " AND f2.type_id=" + cvTermMap.get("sequence").get("mRNA")
    + " AND f3.type_id=" + cvTermMap.get("sequence").get("polypeptide");
  }


  /**
   * Create collections and references for the Item given by chadoSubjectId.
   * @param relTypeMap
   * reltionship -> Map<feature type,list of features>
   * i.e. derives_from -> ( transcript -> [list of transcript(s)]
   *            ( gene    -> [list of gene(s)]
   * @param collectionWarnings
   */
  private boolean processCollectionData(Integer chadoSubjectId,
      Map<String, Map<String, List<FeatureData>>> relTypeMap,
      int collectionWarnings, boolean subjectIsFirst)
          throws ObjectStoreException {
    FeatureData subjectData = featureMap.get(chadoSubjectId);
    if (subjectData == null) {
      if (collectionWarnings < 20) {
        LOG.warn("unknown feature " + chadoSubjectId
            + " passed to processCollectionData - "
            + "ignoring");
      }
      return false;
    }

    // map from collection name to list of item id's
    Map<String, List<String>> collectionsToStore =
        new HashMap<String, List<String>>();

    String subjectInterMineType = subjectData.getInterMineType();
    ClassDescriptor cd = getModel().getClassDescriptorByName(
        subjectInterMineType);
    Integer intermineObjectId = subjectData.getIntermineObjectId();
    for (Map.Entry<String, Map<String, List<FeatureData>>> entry: relTypeMap.entrySet()) {
      // i.e. derives_from
      String relationType = entry.getKey();
      // i.e. <gene, <list of genes>> <mrna, <list of mrnas>>
      Map<String, List<FeatureData>> objectClassFeatureDataMap = entry.getValue();

      Set<Entry<String, List<FeatureData>>> mapEntries = objectClassFeatureDataMap.entrySet();
      for (Map.Entry<String, List<FeatureData>> featureDataMap: mapEntries) {
        String objectClass = featureDataMap.getKey();
        List<FeatureData> featureDataCollection = featureDataMap.getValue();
        List<FieldDescriptor> fds = null;

        FeatureData subjectFeatureData = featureMap.get(chadoSubjectId);
        // key example: ("relationship", "Protein", "producedby", "MRNA")
        String relType;
        if (subjectIsFirst) {
          relType = "relationship";
        } else {
          relType = "rev_relationship";
        }
        
        MultiKey key = new MultiKey(relType, subjectFeatureData.getInterMineType(),
            relationType, objectClass);
        List<ConfigAction> actionList =
            getConfig(subjectData.organismData.getTaxonId()).get(key);

        if (actionList != null) {
          if (actionList.size() == 0
              || actionList.size() == 1
              && actionList.get(0) instanceof DoNothingAction) {
            // do nothing
            continue;
          }
          fds = new ArrayList<FieldDescriptor>();
          for (ConfigAction action: actionList) {
            if (action instanceof SetFieldConfigAction) {
              SetFieldConfigAction setAction = (SetFieldConfigAction) action;
              String fieldName = setAction.getFieldName();
              FieldDescriptor fd = cd.getFieldDescriptorByName(fieldName);
              if (fd == null) {
                throw new RuntimeException("can't find field " + fieldName
                    + " in class " + cd + " configured for "
                    + key);
              }
              fds.add(fd);

            }
          }
          if (fds.size() == 0) {
            throw new RuntimeException("no actions found for " + key);
          }
        } else {
          if (PART_OF_RELATIONS.contains(relationType)) {
            // special case for part_of relations - try
            // to find a reference or collection that has
            // a name that looks right for these objects (of
            // class objectClass). eg. If the subject is a
            // Transcript and the objectClass is Exon then
            // find collections called "exons", "geneParts"
            // (GenePart is a superclass of Exon)
            fds = getReferenceForRelationship(objectClass, cd);
          } else {
            continue;
          }
        }

        if (fds.size() == 0) {
          if (!loggedMissingCols.contains(subjectInterMineType + relationType)) {
            LOG.error("can't find collection for type " + objectClass
                + " with relationship " + relationType
                + " in " + subjectInterMineType + " (was processing feature "
                + chadoSubjectId + ")");
            loggedMissingCols.add(subjectInterMineType + relationType);
          }
          continue;
        }

        for (FieldDescriptor fd: fds) {
          if (fd.isReference()) {
            if (objectClassFeatureDataMap.size() > 1) {
              throw new RuntimeException("found more than one object for reference "
                  + fd + " in class "
                  + subjectInterMineType
                  + " current subject identifier: "
                  + subjectData.getUniqueName());
            }
            if (objectClassFeatureDataMap.size() == 1) {
              Reference reference = new Reference();
              reference.setName(fd.getName());
              FeatureData referencedFeatureData = featureDataCollection.get(0);
              reference.setRefId(referencedFeatureData.getItemIdentifier());

             
              getChadoDBConverter().store(reference, intermineObjectId);

              // special case for 1-1 relations - we need to set the reverse
              // reference
              ReferenceDescriptor rd = (ReferenceDescriptor) fd;
              ReferenceDescriptor reverseRD = rd.getReverseReferenceDescriptor();
              if (reverseRD != null && !reverseRD.isCollection()) {
                Reference revReference = new Reference();
                revReference.setName(reverseRD.getName());
                revReference.setRefId(subjectData.getItemIdentifier());
                Integer refObjectId = referencedFeatureData.getIntermineObjectId();
                getChadoDBConverter().store(revReference, refObjectId);
              }
            }

          } else {
            List<String> itemIds;
            if (collectionsToStore.containsKey(fd.getName())) {
              itemIds = collectionsToStore.get(fd.getName());
            } else {
              itemIds = new ArrayList<String>();
              collectionsToStore.put(fd.getName(), itemIds);
            }
            for (FeatureData featureData: featureDataCollection) {
              itemIds.add(featureData.getItemIdentifier());
            }
          }
        }
      }
    }

    for (Map.Entry<String, List<String>> entry: collectionsToStore.entrySet()) {
      ReferenceList referenceList = new ReferenceList();
      String collectionName = entry.getKey();
      referenceList.setName(collectionName);
      List<String> idList = entry.getValue();
      referenceList.setRefIds(idList);
      getChadoDBConverter().store(referenceList, intermineObjectId);

      // if there is a field called <classname>Count that matches the name of the collection
      // we just stored, set it
      String countName;
      if (collectionName.endsWith("s")) {
        countName = collectionName.substring(0, collectionName.length() - 1);
      } else {
        countName = collectionName;
      }
      countName += "Count";
      if (cd.getAttributeDescriptorByName(countName, true) != null) {
        setAttribute(intermineObjectId, countName, String.valueOf(idList.size()));
      }
    }

    return true;
  }
  /**
   * Search ClassDescriptor cd class for refs/collections with the right name for the objectType
   * eg. find CDSs collection for objectType = CDS and find gene reference for objectType = Gene.
   */
  protected List<FieldDescriptor> getReferenceForRelationship(String objectType,
      ClassDescriptor cd) {
    List<FieldDescriptor> fds = new ArrayList<FieldDescriptor>();
    LinkedHashSet<String> allClasses = new LinkedHashSet<String>();
    allClasses.add(objectType);
    try {
      Set<String> parentClasses = ClassDescriptor.findSuperClassNames(getModel(), objectType);
      allClasses.addAll(parentClasses);
    } catch (MetaDataException e) {
      throw new RuntimeException("class not found in the model", e);
    }

    for (String clsName: allClasses) {
      List<String> possibleRefNames = new ArrayList<String>();
      String unqualifiedClsName = TypeUtil.unqualifiedName(clsName);
      possibleRefNames.add(unqualifiedClsName);
      possibleRefNames.add(unqualifiedClsName + 's');
      possibleRefNames.add(StringUtil.decapitalise(unqualifiedClsName));
      possibleRefNames.add(StringUtil.decapitalise(unqualifiedClsName) + 's');
      for (String possibleRefName: possibleRefNames) {
        FieldDescriptor fd = cd.getFieldDescriptorByName(possibleRefName);
        if (fd != null) {
          fds.add(fd);
        }
      }
    }
    return fds;
  }
}
