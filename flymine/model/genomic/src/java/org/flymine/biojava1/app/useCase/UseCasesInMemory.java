package org.flymine.biojava1.app.useCase;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.seq.StrandedFeature;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.bio.symbol.SymbolList;
import org.flymine.biojava1.bio.FeatureFM;
import org.flymine.biojava1.bio.FeatureHolderFMAnno;
import org.flymine.biojava1.bio.SequenceFM;
import org.flymine.biojava1.query.QueryFM;

import java.util.Collection;
import java.util.Iterator;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * Some UseCases show how to use the BioJava/FlyMine mapping <br>
 * <b>For this demo the config file bjMapping.properties must
 * ENABLE hasA, invHasA, synonyms and memoryMode </b>
 * <p>
 * Before we start our tour with some examples you'll need some theory about BioJava's most
 * important classes used in this mapping: SequenceFM, FeatureHolderFM and FeatureFM (FM indicates
 * FlyMine), Annotation
 * <ul>
 * <li>SequenceFM is an implementation of Sequence which is a sub-interface of SymbolList. A
 * SymbolList in our case represents chromosome DNA. It has validation and only allows DNA, takes
 * care about ambiguity of symbols (e.g. 'W' which can stand for 'A' or 'T') and limit the alphabet
 * to the chosen Alphabet. A Sequence has additionally global annotations (e.g. name, database id,
 * literature references) and location specific annotations (Features).
 * <li>FeatureFM is an implementation of Feature. A Feature represents a region of the Sequence
 * with some information attached. A Feature always comes along with information. Most important to
 * know is: type (e.g. Gene), Location (range location, point location), Annotation (which can
 * contain any data attached to that Feature).
 * <li>Feature and Sequence both implment FeatureHolder. A FH represents a way to attach Features
 * to a FH. Therefore BioJava provides a hierarchy of Features; Example: A Sequence can have some
 * Features directly attached, and these Features again can have some Features attached. The only
 * drawback of this is that each Feature has exactly one parent.
 * <li>The drawback that a Feature always has exactly one parent does not fit our needs in FlyMine.
 * Therefore we havn't used the internal hierarchy in BioJava at all. A Sequence in the mapping
 * reflects a specific Chromosome of FlyMine. A Sequence has attached the Features, but a Feature
 * does not contain further Features. Instead, another approach is used: Annotations.
 * <li>Annotations of Features are used for three different things in FlyMine mapping:
 * <ul>
 * <li>identifier: the identifier of the Feature, like "CG1449".
 * <li>synonyms: Synonyms of Features are provided in the synonyms annotation bundle
 * <li>hasA: hasA represents the missing flexible hierarchy of BioJava. In Flymine the genomic
 * model relies on the Sequence Ontology (light - SOFA), and allows multiple parents. Parents is
 * probably not the right term; partOf/hasA relationships between Features, e.g. a Gene hasA
 * Transcript(s) and a Transcript hasA Exon(s). This relationship is represented in the annotation
 * bundle "hasA:\ <aType\>", e.g. hasA:Transcript
 * <li>invHasA: the inverse of a hasA relation. Example: You have an Exon and want to know about
 * it's Transcripts. Therefore, you have an inverse annotation bundle "invHasA:\ <aType\>", e.g.
 * invHasA:Transcript.
 * </ul>
 * </ul>
 * BTW: Code here is written as easy as possible without any fancy stuff to make it easy to
 * concentrate on the examples; Now let't move on to some real exciting examples:
 */
public class UseCasesInMemory {

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
  public UseCasesInMemory() throws Exception {    
    _sequence = SequenceFM.getInstance("Drosophila melanogaster", "4");
    assert (_sequence != null);
    QueryFM.getInstance(_sequence); //you don't need to do this - I do it for a better overview, as
    //debug output is generated and I want to show you a clear nice output in the methods below...
  }

  /**
   * Example: 
   * Getting a Feature on Chromosom 4 of DM by it's identifier and print out all of it's synonyms.
   * Also an interesting feature is that you are allowed to navigate the Onotology for each feature.
   * Your can ask wich hasA relations and invHasA relations the ontology provide.
   */
  public void byIdentifier() {
    System.out.println("\nbyIdentifier() --------------------------------------------------------");
    FeatureFilter filter = new FeatureFilter.ByAnnotation("identifier", "CG1449");
    FeatureHolder fh = _sequence.filter(filter);
    for (Iterator it = fh.features(); it.hasNext();) {
      FeatureFM f = (FeatureFM) it.next();
      //print FeatureFM in detail
      System.out.println(f.toStringDumpAll());
      //list all synonyms
      Collection synonyms = (Collection) f.getAnnotation().getProperty("synonyms");
      for (Iterator itSyn = synonyms.iterator(); itSyn.hasNext();) {
        System.out.println("\t" + itSyn.next());
      }
      //list all hasA relations of this Feature according to the Sequence Onotology light
      System.out.println("hasA according to SoFa: " + f.getHasA());
      System.out.println("invHasA according to SoFa: " + f.getInvHasA());
    }
  }

  /**
   * Example: 
   * Getting all Features within a specific Region on Chromosom 4 of D. melanogaster
   * <p>
   * There are a lot of FeatureFilters which can be used to get Features. In this case, we use
   * FeatureFilters.ContainedByLocation. Investigate to check out different FeatureFilters, some of
   * them are coverd here later
   */
  public void containedByLocation() {
    System.out.println("\ncontainedByLocation() location ----------------------------------------");
    //you can simply set up a filter
    FeatureFilter filter = new FeatureFilter.ContainedByLocation(new RangeLocation(1, 500));
    System.out.println(filter);
    //and apply this filter to the sequence. Filtering Features itself is not possible, as I
    //do not provide hierarchical Features (see intro)
    FeatureHolder fh = _sequence.filter(filter);
    for (Iterator it = fh.features(); it.hasNext();) {
      FeatureFM f = (FeatureFM) it.next();
      System.out.println(f.toStringDumpAll());
    }
  }

  /**
   * Example: 
   * Just another filter; getting all Features wich overlaps a specific Region on Chromosom 4 of DM.
   * <p>
   */
  public void overlapsLocation() {
    System.out.println("\noverlapsLocation() ----------------------------------------------------");
    FeatureFilter filter = new FeatureFilter.OverlapsLocation(new RangeLocation(1, 500));
    System.out.println(filter);
    FeatureHolder fh = _sequence.filter(filter);
    for (Iterator it = fh.features(); it.hasNext();) {
      FeatureFM f = (FeatureFM) it.next();
      System.out.println(f.toStringDumpAll());
    }
  }

  /**
   * Example: 
   * You can make complex filters by joining them by and, or, not - you end up with a filter tree.
   * Here is a simple Example how you set up a filter which filters by a range location, by a
   * certain type of Feature, and on a specific strand. Thereby two new FeatureFilters are
   * introduced.
   */
  public void and() {
    System.out.println("\nand() -----------------------------------------------------------------");
    FeatureFilter locFilter = new FeatureFilter.OverlapsLocation(new RangeLocation(1, 500));
    //you can use the FeatureFilter.ByType to filter for any FlyMine subclass of BioEntity. Simply
    //use the unqualified class name as shown in this example
    FeatureFilter typeFilter = new FeatureFilter.ByType("RepeatRegion");
    FeatureFilter strandFilter = new FeatureFilter.StrandFilter(StrandedFeature.POSITIVE);
    FeatureFilter locType = new FeatureFilter.And(locFilter, typeFilter);
    FeatureFilter locTypeStrand = new FeatureFilter.And(locType, strandFilter);
    System.out.println(locTypeStrand);
    FeatureHolder fh = _sequence.filter(locTypeStrand);
    for (Iterator it = fh.features(); it.hasNext();) {
      FeatureFM f = (FeatureFM) it.next();
      System.out.println(f.toStringDumpAll());
    }
  }

  /**
   * Example:
   * Similar than the above filter session, here we introduce the FeatureFilter.Or. Internally, all
   * the filters are tried to be optimized and pruned if possible. In our example it is generating
   * one fancy filter to cover region from 1-2500
   */
  public void or() {
    System.out.println("\nor() ------------------------------------------------------------------");
    FeatureFilter locFilter = new FeatureFilter.ContainedByLocation(new RangeLocation(1, 500));
    FeatureFilter loc2Filter = new FeatureFilter.ContainedByLocation(new RangeLocation(500, 2500));
    FeatureFilter or = new FeatureFilter.Or(locFilter, loc2Filter);
    System.out.println(or);
    FeatureHolder fh = _sequence.filter(or);
    for (Iterator it = fh.features(); it.hasNext();) {
      FeatureFM f = (FeatureFM) it.next();
      System.out.println(f.toStringDumpAll());
    }
  }

  /**
   * Example: 
   * Investigate FeatureFM relations; let's filter for a Gene within a specific region and then find
   * out about it's Transcripts and Exons. More about the hasA Annotation bundle you can find in the
   * introduction of this class. This technique basically provides the hierarchy information of
   * Sequence Ontology. Check out Sequence Ontology, SOFA, to find the relations between Features or
   * use the feature.getHasA() and feature.getInvHasA() to investigate the relations within the
   * ontology. For now you have to know, that a Gene hasA relation to Transcripts and these hasA
   * Exons.
   */
  public void hasA() {
    System.out.println("\nhasA() ----------------------------------------------------------------");
    //first filtering step - get a Gene in a specific region
    FeatureFilter loc = new FeatureFilter.ContainedByLocation(new RangeLocation(1100000, 1140000));
    FeatureFilter gene = new FeatureFilter.ByType("Gene");
    FeatureHolder fh = _sequence.filter(new FeatureFilter.And(loc, gene));

    //iterate all found genes
    for (Iterator it = fh.features(); it.hasNext();) {
      FeatureFM aGene = (FeatureFM) it.next();
      System.out.println(aGene.toStringDumpAll());
      System.out.println(aGene.getHasA());
      Annotation annoGene = aGene.getAnnotation();

      //and get the transcripts for each Gene
      FeatureHolderFMAnno trans = (FeatureHolderFMAnno) annoGene.getProperty("hasA:Transcript");
      //iterate these transcripts
      for (Iterator itTranscripts = trans.features(); itTranscripts.hasNext();) {
        FeatureFM aTranscript = (FeatureFM) itTranscripts.next();
        System.out.println("\t" + aTranscript.toStringDumpAll());
        System.out.println("\t" + aGene.getHasA());
        Annotation annoTranscript = aTranscript.getAnnotation();

        //get Exons for each Transcript
        FeatureHolderFMAnno exons = (FeatureHolderFMAnno) annoTranscript.getProperty("hasA:Exon");
        for (Iterator itExons = exons.features(); itExons.hasNext();) {
          FeatureFM aExon = (FeatureFM) itExons.next();
          System.out.println("\t\t" + aExon.toStringDumpAll());
        }
      }
    }
  }

  /**
   * Example: 
   * Yet another FeatureFM relation example, but this time the other way round. You have a specific
   * Exon and want to know about it's Transcripts where it is contained. You can simply extend this
   * example to find out about the Genes the Exons are involved ... good luck!
   */
  public void invHasA() {
    System.out.println("\ninvHasA() -------------------------------------------------------------");
    FeatureFilter filter = new FeatureFilter.ByAnnotation("identifier", "CG11049:7-1");
    FeatureHolder fh = _sequence.filter(filter);
    for (Iterator it = fh.features(); it.hasNext();) {
      FeatureFM f = (FeatureFM) it.next();
      System.out.println(f.toStringDumpAll());
      System.out.println(f.getInvHasA());
      Annotation exonAnno = f.getAnnotation();
      FeatureHolderFMAnno t = (FeatureHolderFMAnno) exonAnno.getProperty("invHasA:Transcript");
      for (Iterator itTrans = t.features(); itTrans.hasNext();) {
        FeatureFM aTranscript = (FeatureFM) itTrans.next();
        System.out.println(aTranscript.toStringDumpAll());
      }
    }
  }

  /**
   * Example: 
   * You want to transcribe and translate the sequence of a Feature? Here we go ... <br>
   * You want to prove yourself? Find out how to make a reverse complement!
   * <p>
   * Important note: currenty in FlyMine the start and endposition of transciption of a Transcript
   * is not provided. This will be fixed soon.
   */
  public void transX() throws IllegalAlphabetException {
    System.out.println("\ntransX() -----------------------------------------------------------");
    //first filtering step - Gene in a specific region
    FeatureFilter loc = new FeatureFilter.ContainedByLocation(new RangeLocation(106479, 106559));
    FeatureFilter exon = new FeatureFilter.ByType("Exon");
    FeatureHolder fh = _sequence.filter(new FeatureFilter.And(loc, exon));
    for (Iterator it = fh.features(); it.hasNext();) {
      FeatureFM aFeature = (FeatureFM) it.next();
      System.out.println(aFeature.toStringDumpAll());
      SymbolList sl = aFeature.getSymbols();
      sl = RNATools.transcribe(sl);
      System.out.println("transcript:\t\t" + sl.seqString());
      sl = RNATools.translate(sl);
      System.out.println("translation:\t" + sl.seqString());
    }
  }

  /**
   * Example: 
   * A very basic GUI example is provided to give you an idea that writing a SequenceViewer with
   * BioJava's help isn't that hard. Here we only render RepeatRegions, Genes and it's Exons of a
   * specific region. Have fun to extend this example and make your own Viewer.
   */
  public void gui() {
    System.out.println("\ngui() -----------------------------------------------------------------");
    new FeatureView(_sequence, 110800, 1146000);
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
    long t1 = System.currentTimeMillis();
    UseCasesInMemory uc = new UseCasesInMemory();
    uc.byIdentifier();
    long t2 = System.currentTimeMillis();
    System.out.println((t2 - t1) / 1000.0);
    uc.containedByLocation();
    uc.overlapsLocation();
    uc.and();
    uc.or();
    uc.hasA();
    uc.invHasA();
    uc.transX();
    uc.gui();
  }
}
