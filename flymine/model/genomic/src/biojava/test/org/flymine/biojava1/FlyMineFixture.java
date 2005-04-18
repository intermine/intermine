package org.flymine.tests;

import junit.framework.TestCase;
import org.flymine.biojava1.utils.Config;
import org.flymine.biojava1.utils.ObjectStoreManager;
import org.flymine.model.genomic.*;
import org.flymine.postprocess.CalculateLocations;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

import java.util.*;

/**
 * This class represents basically a JUnit test fixture of FlyMine BioEntities/Features which can be
 * used by subclassing. Instead of having accessor methods to all relevant fields they are protected
 * in their visibility to use them directly in the subclasses (fine for these testcases) <br>
 * <b>requirement: genomic-test specified in intermine.properties </b>
 * 
 * @author Markus Brosch
 */
public class FlyMineFixture extends TestCase {

  // Testcases overview, details see further documentation within the code
  //
  ///////
  //
  // ----------------- CHROMOSOME1 C1 pos strand -------------------
  //  
  // 3--------------------------GENE10------------------------23
  //    7--------------------TRANSCRIPT20--------------------22
  //      8---EXON50---12 _____________ 15---EXON52---18
  //  4----------------------TRANSCRIPT21-----------------20
  //   6---EXON51---10 12---EXON53---14 15---EXON52---18
  //
  ///////
  //
  // -------------------- CHROMOSOME2 C2 neg strand -----------------
  //  2---CSE1---4
  //
  ///////
  //
  // -------------------- CHROMOSOME2 C2 unknown strand -------------
  //               5---CSE2---8

  private Set _fixture;

  protected ObjectStoreWriter _osw = null;
  protected ObjectStore _os = null;
  protected Model _model;

  protected List _C1BioEntities = new ArrayList();
  protected List _C2BioEntities = new ArrayList();

  protected final Class _BIOENTITYcLASS = BioEntity.class;
  protected final Class _SEQUENCEcLASS = Sequence.class;
  protected final Class _CSTRUCTURALELcLASS = ChromosomalStructuralElement.class;
  protected final Class _CHROMOSOMEcLASS = Chromosome.class;
  protected final Class _CONTIGcLASS = Contig.class;
  protected final Class _GENEcLASS = Gene.class;
  protected final Class _TRANSCRIPTcLASS = Transcript.class;
  protected final Class _EXONcLASS = Exon.class;
  protected final Class _REGULATORYREGIONcLASS = RegulatoryRegion.class;
  protected final Class _LOCATIONcLASS = Location.class;
  protected final Class _RELATIONcLASS = Relation.class;
  protected final Class _ANNOTATIONcLASS = Annotation.class;
  protected final Class _PHENOTYPEcLASS = Phenotype.class;
  protected final Class _GOTERMcLASS = GOTerm.class;

  protected final String _EXONsTRING = "Exon";
  protected final String _GENEsTRING = "Gene";
  protected final String _TRANSCRIPTsTRING = "Transcript";

  protected final Integer _POSITIVE = new Integer(1);
  protected final Integer _NEGATIVE = new Integer(-1);
  protected final Integer _UNKNOWN = new Integer(0);

  protected final Sequence _S1;
  protected final Integer _S1ID = new Integer(0);

  protected final ChromosomalStructuralElement _CSE1;
  protected final Integer _CSE1ID = new Integer(-1);
  protected final ChromosomalStructuralElement _CSE2;
  protected final Integer _CSE2ID = new Integer(-2);

  protected final Chromosome _C1;
  protected final Integer _C1ID = new Integer(1);
  protected final Chromosome _C2;
  protected final Integer _C2ID = new Integer(2);;

  protected final Gene _GENE10;
  protected final Integer _GENE10ID = new Integer(10);

  protected Transcript _TRANSCRIPT20;
  protected final Integer _TRANSCRIPT20ID = new Integer(20);
  protected Transcript _TRANSCRIPT21;
  protected final Integer _TRANSCRIPT21ID = new Integer(21);
  
  protected final Relation _TRANSCRIPT20GENE10;
  protected final Relation _TRANSCRIPT21GENE10;

  protected Exon _EXON50;
  protected final Integer _EXON50ID = new Integer(50);
  protected Exon _EXON51;
  protected final Integer _EXON51ID = new Integer(51);
  protected Exon _EXON52;
  protected final Integer _EXON52ID = new Integer(52);
  protected Exon _EXON53;
  protected final Integer _EXON53ID = new Integer(53);

  protected Location _LOcCHROMOSOME1GENE10;
  protected final Integer _LOcCHROMOSOME1GENE10ID = new Integer(110);
  protected Location _LOcCHROMOSOME1REGRE15;
  protected final Integer _LOcCHROMOSOME1REGRE15ID = new Integer(130);
  protected Location _LOcCHROMOSOME1TRANSCRIPT20;
  protected final Integer _LOcCHROMOSOME1TRANSCRIPT20ID = new Integer(120);
  protected Location _LOcCHROMOSOME1TRANSCRIPT21;
  protected final Integer _LOcCHROMOSOME1TRANSCRIPT21ID = new Integer(121);
  protected Location _LOcCHROMOSOME1EXON50;
  protected final Integer _LOcCHROMOSOME1EXON50ID = new Integer(150);
  protected Location _LOcCHROMOSOME1EXON51;
  protected final Integer _LOcCHROMOSOME1EXON51ID = new Integer(151);
  protected Location _LOcCHROMOSOME1EXON52;
  protected final Integer _LOcCHROMOSOME1EXON52ID = new Integer(152);
  protected Location _LOcCHROMOSOME1EXON53;
  protected final Integer _LOcCHROMOSOME1EXON53ID = new Integer(153);

  protected Location _LOcCHROMOSOME2CSE1;
  protected final Integer _LOcCHROMOSOME2CSE1ID = new Integer(-21);
  protected Location _LOcCHROMOSOME2CSE2;
  protected final Integer _LOcCHROMOSOME2CSE2ID = new Integer(-22);

  protected Annotation _ANNOTATION70;
  protected final Integer _ANNOTATION70ID = new Integer(70);
  protected Annotation _ANNOTATION71;
  protected final Integer _ANNOTATION71ID = new Integer(71);
  protected BioProperty _PHENOTYPE72;
  protected final Integer _PHENOTYPE72ID = new Integer(72);
  protected GOTerm _GOTERM73;
  protected final Integer _GOTERM73ID = new Integer(73);

  public FlyMineFixture() {

    _S1 = (Sequence) DynamicUtil.createObject(Collections.singleton(_SEQUENCEcLASS));
    _S1.setId(_S1ID);
    _S1.setResidues("gatccgtaccgtacaattggcatgt");

    _CSE1 = (ChromosomalStructuralElement) DynamicUtil.createObject(Collections
        .singleton(_CSTRUCTURALELcLASS));
    _CSE1.setId(_CSE1ID);

    _CSE2 = (ChromosomalStructuralElement) DynamicUtil.createObject(Collections
        .singleton(_CSTRUCTURALELcLASS));
    _CSE2.setId(_CSE2ID);

    _EXON50 = (Exon) DynamicUtil.createObject(Collections.singleton(_EXONcLASS));
    _EXON50.setId(_EXON50ID);

    _EXON51 = (Exon) DynamicUtil.createObject(Collections.singleton(_EXONcLASS));
    _EXON51.setId(_EXON51ID);

    _EXON52 = (Exon) DynamicUtil.createObject(Collections.singleton(_EXONcLASS));
    _EXON52.setId(_EXON52ID);

    _EXON53 = (Exon) DynamicUtil.createObject(Collections.singleton(_EXONcLASS));
    _EXON53.setId(_EXON53ID);

    _TRANSCRIPT20 = (Transcript) DynamicUtil.createObject(Collections.singleton(_TRANSCRIPTcLASS));
    _TRANSCRIPT20.setId(_TRANSCRIPT20ID);
    _TRANSCRIPT20.addExons(_EXON50);
    _EXON50.addTranscripts(_TRANSCRIPT20);
    _TRANSCRIPT20.addExons(_EXON52);
    _EXON52.addTranscripts(_TRANSCRIPT20);

    _TRANSCRIPT21 = (Transcript) DynamicUtil.createObject(Collections.singleton(_TRANSCRIPTcLASS));
    _TRANSCRIPT21.setId(_TRANSCRIPT21ID);
    _TRANSCRIPT21.addExons(_EXON51);
    _EXON51.addTranscripts(_TRANSCRIPT21);
    _TRANSCRIPT21.addExons(_EXON52);
    _EXON52.addTranscripts(_TRANSCRIPT21);

    _GENE10 = (Gene) DynamicUtil.createObject(Collections.singleton(_GENEcLASS));
    _GENE10.setId(_GENE10ID);
    _GENE10.setIdentifier("geneIdentifier");
    _GENE10.addExons(_EXON50);
    _GENE10.addTranscripts(_TRANSCRIPT20);
    _TRANSCRIPT20.setGene(_GENE10);
    _GENE10.addTranscripts(_TRANSCRIPT21);
    _TRANSCRIPT21.setGene(_GENE10);

    _TRANSCRIPT20GENE10 = (Relation) DynamicUtil.createObject(Collections.singleton(_RELATIONcLASS));
    _TRANSCRIPT20GENE10.setSubject(_TRANSCRIPT20);
    _TRANSCRIPT20GENE10.setObject(_GENE10);    
    
    _TRANSCRIPT21GENE10 = (Relation) DynamicUtil.createObject(Collections.singleton(_RELATIONcLASS));
    _TRANSCRIPT21GENE10.setSubject(_TRANSCRIPT21);
    _TRANSCRIPT21GENE10.setObject(_GENE10);
    
    _C1 = (Chromosome) DynamicUtil.createObject(Collections.singleton(_CHROMOSOMEcLASS));
    _C1.setId(_C1ID);
    _C1.setSequence(_S1);
    _C1.setLength(new Integer(25));
    _C1.addTranscripts(_TRANSCRIPT20);
    
    _TRANSCRIPT20.setChromosome(_C1);
    _C1BioEntities.add(_TRANSCRIPT20);
    _C1.addTranscripts(_TRANSCRIPT21);
    
    _TRANSCRIPT21.setChromosome(_C1);
    _C1BioEntities.add(_TRANSCRIPT21);
    _C1.addExons(_EXON50);
    
    _EXON50.setChromosome(_C1);
    _C1BioEntities.add(_EXON50);
    _C1.addExons(_EXON51);
    
    _EXON51.setChromosome(_C1);
    _C1BioEntities.add(_EXON51);
    _C1.addExons(_EXON52);
    
    _EXON52.setChromosome(_C1);
    _C1BioEntities.add(_EXON52);
    _C1.addExons(_EXON53);
    
    _EXON53.setChromosome(_C1);
    _C1BioEntities.add(_EXON53);
    _C1.addGenes(_GENE10);
    
    _GENE10.setChromosome(_C1);
    _C1BioEntities.add(_GENE10);

    _C2 = (Chromosome) DynamicUtil.createObject(Collections.singleton(_CHROMOSOMEcLASS));
    _C2.setId(_C2ID);
    _C2.setLength(new Integer(30));
    _C2.addChromosomalStructuralElements(_CSE1);
    _CSE1.setChromosome(_C2);
    _C2BioEntities.add(_CSE1);
    _C2.addChromosomalStructuralElements(_CSE2);
    _CSE2.setChromosome(_C2);
    _C2BioEntities.add(_CSE2);

    _LOcCHROMOSOME1EXON50 = (Location) DynamicUtil.createObject(Collections
        .singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME1EXON50.setId(_LOcCHROMOSOME1EXON50ID);
    _LOcCHROMOSOME1EXON50.setObject(_C1);
    _LOcCHROMOSOME1EXON50.setSubject(_EXON50);
    _LOcCHROMOSOME1EXON50.setStrand(_POSITIVE);
    _LOcCHROMOSOME1EXON50.setStart(new Integer(8));
    _LOcCHROMOSOME1EXON50.setEnd(new Integer(12));

    _LOcCHROMOSOME1EXON51 = (Location) DynamicUtil.createObject(Collections
        .singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME1EXON51.setId(_LOcCHROMOSOME1EXON51ID);
    _LOcCHROMOSOME1EXON51.setObject(_C1);
    _LOcCHROMOSOME1EXON51.setSubject(_EXON51);
    _LOcCHROMOSOME1EXON51.setStrand(_POSITIVE);
    _LOcCHROMOSOME1EXON51.setStart(new Integer(6));
    _LOcCHROMOSOME1EXON51.setEnd(new Integer(10));

    _LOcCHROMOSOME1EXON52 = (Location) DynamicUtil.createObject(Collections
        .singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME1EXON52.setId(_LOcCHROMOSOME1EXON52ID);
    _LOcCHROMOSOME1EXON52.setObject(_C1);
    _LOcCHROMOSOME1EXON52.setSubject(_EXON52);
    _LOcCHROMOSOME1EXON52.setStrand(_POSITIVE);
    _LOcCHROMOSOME1EXON52.setStart(new Integer(15));
    _LOcCHROMOSOME1EXON52.setEnd(new Integer(18));

    _LOcCHROMOSOME1EXON53 = (Location) DynamicUtil.createObject(Collections
        .singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME1EXON53.setId(_LOcCHROMOSOME1EXON53ID);
    _LOcCHROMOSOME1EXON53.setObject(_C1);
    _LOcCHROMOSOME1EXON53.setSubject(_EXON53);
    _LOcCHROMOSOME1EXON53.setStrand(_POSITIVE);
    _LOcCHROMOSOME1EXON53.setStart(new Integer(12));
    _LOcCHROMOSOME1EXON53.setEnd(new Integer(14));

    _LOcCHROMOSOME1GENE10 = (Location) DynamicUtil.createObject(Collections
        .singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME1GENE10.setId(_LOcCHROMOSOME1GENE10ID);
    _LOcCHROMOSOME1GENE10.setObject(_C1);
    _LOcCHROMOSOME1GENE10.setSubject(_GENE10);
    _LOcCHROMOSOME1GENE10.setStrand(_POSITIVE);
    _LOcCHROMOSOME1GENE10.setStart(new Integer(3));
    _LOcCHROMOSOME1GENE10.setEnd(new Integer(23));

    _LOcCHROMOSOME1TRANSCRIPT20 = (Location) DynamicUtil.createObject(Collections
        .singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME1TRANSCRIPT20.setId(_LOcCHROMOSOME1TRANSCRIPT20ID);
    _LOcCHROMOSOME1TRANSCRIPT20.setObject(_C1);
    _LOcCHROMOSOME1TRANSCRIPT20.setSubject(_TRANSCRIPT20);
    _LOcCHROMOSOME1TRANSCRIPT20.setStrand(_POSITIVE);
    _LOcCHROMOSOME1TRANSCRIPT20.setStart(new Integer(7));
    _LOcCHROMOSOME1TRANSCRIPT20.setEnd(new Integer(22));

    _LOcCHROMOSOME1TRANSCRIPT21 = (Location) DynamicUtil.createObject(Collections
        .singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME1TRANSCRIPT21.setId(_LOcCHROMOSOME1TRANSCRIPT21ID);
    _LOcCHROMOSOME1TRANSCRIPT21.setObject(_C1);
    _LOcCHROMOSOME1TRANSCRIPT21.setSubject(_TRANSCRIPT21);
    _LOcCHROMOSOME1TRANSCRIPT21.setStrand(_POSITIVE);
    _LOcCHROMOSOME1TRANSCRIPT21.setStart(new Integer(4));
    _LOcCHROMOSOME1TRANSCRIPT21.setEnd(new Integer(20));

    _LOcCHROMOSOME2CSE1 = (Location) DynamicUtil
        .createObject(Collections.singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME2CSE1.setId(_LOcCHROMOSOME2CSE1ID);
    _LOcCHROMOSOME2CSE1.setObject(_C2);
    _LOcCHROMOSOME2CSE1.setSubject(_CSE1);
    _LOcCHROMOSOME2CSE1.setStrand(_NEGATIVE);
    _LOcCHROMOSOME2CSE1.setStart(new Integer(2));
    _LOcCHROMOSOME2CSE1.setEnd(new Integer(4));

    _LOcCHROMOSOME2CSE2 = (Location) DynamicUtil
        .createObject(Collections.singleton(_LOCATIONcLASS));
    _LOcCHROMOSOME2CSE2.setId(_LOcCHROMOSOME2CSE2ID);
    _LOcCHROMOSOME2CSE2.setObject(_C2);
    _LOcCHROMOSOME2CSE2.setSubject(_CSE2);
    _LOcCHROMOSOME2CSE2.setStrand(_UNKNOWN);
    _LOcCHROMOSOME2CSE2.setStart(new Integer(5));
    _LOcCHROMOSOME2CSE2.setEnd(new Integer(8));

    _PHENOTYPE72 = (Phenotype) DynamicUtil.createObject(Collections.singleton(_PHENOTYPEcLASS));
    _PHENOTYPE72.setId(_PHENOTYPE72ID);

    _ANNOTATION70 = (Annotation) DynamicUtil.createObject(Collections.singleton(_ANNOTATIONcLASS));
    _ANNOTATION70.setId(_ANNOTATION70ID);
    _ANNOTATION70.setProperty(_PHENOTYPE72);
    _ANNOTATION70.setSubject(_EXON50);

    _GOTERM73 = (GOTerm) DynamicUtil.createObject(Collections.singleton(_GOTERMcLASS));
    _GOTERM73.setId(_GOTERM73ID);
    _GOTERM73.setName("'de novo' IMP biosynthesis");
    _GOTERM73.setIdentifier("GO:0006189");

    _ANNOTATION71 = (Annotation) DynamicUtil.createObject(Collections.singleton(_ANNOTATIONcLASS));
    _ANNOTATION71.setId(_ANNOTATION71ID);
    _ANNOTATION71.setProperty(_GOTERM73);
    _ANNOTATION71.setSubject(_EXON51);
  }

  public void setUp() throws Exception {

    _osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
    _osw.getObjectStore().flushObjectById();
    _model = Model.getInstanceByName("genomic");
    assert (_osw != null);
    assert (_model != null);

    _fixture = new HashSet();
    _fixture.add(_S1);
    _fixture.add(_CSE1);
    _fixture.add(_CSE2);
    _fixture.add(_EXON50);
    _fixture.add(_EXON51);
    _fixture.add(_EXON52);
    _fixture.add(_EXON53);
    _fixture.add(_TRANSCRIPT20);
    _fixture.add(_TRANSCRIPT21);
    _fixture.add(_GENE10);
    _fixture.add(_TRANSCRIPT20GENE10);
    _fixture.add(_TRANSCRIPT21GENE10);
    _fixture.add(_LOcCHROMOSOME1EXON50);
    _fixture.add(_LOcCHROMOSOME1EXON51);
    _fixture.add(_LOcCHROMOSOME1EXON52);
    _fixture.add(_LOcCHROMOSOME1EXON53);
    _fixture.add(_LOcCHROMOSOME1GENE10);
    _fixture.add(_LOcCHROMOSOME1TRANSCRIPT20);
    _fixture.add(_LOcCHROMOSOME1TRANSCRIPT21);
    _fixture.add(_LOcCHROMOSOME2CSE1);
    _fixture.add(_LOcCHROMOSOME2CSE2);
    _fixture.add(_PHENOTYPE72);
    _fixture.add(_ANNOTATION70);
    _fixture.add(_GOTERM73);
    _fixture.add(_ANNOTATION71);
    _fixture.add(_C1);
    _fixture.add(_C2);

    for (Iterator i = _fixture.iterator(); i.hasNext();) {
      _osw.store((InterMineObject) i.next());
    }

    CalculateLocations cl = new CalculateLocations(_osw);
    cl.fixPartials();
    cl.createLocations();

    _os = _osw.getObjectStore();
    assert (_os != null);
    ObjectStoreManager.setOsAndGetOsManager(Config.OS_DEFAULT, _os);
  }

  public void tearDown() throws Exception {
    Query q = new Query();
    QueryClass qc = new QueryClass(InterMineObject.class);
    q.addFrom(qc);
    q.addToSelect(qc);
    SingletonResults res = new SingletonResults(q, _osw.getObjectStore(), _osw.getObjectStore()
        .getSequence());
    for (Iterator resIter = res.iterator(); resIter.hasNext();) {
      InterMineObject o = (InterMineObject) resIter.next();
      _osw.delete(o);
    }
    _osw.close();
  }
}