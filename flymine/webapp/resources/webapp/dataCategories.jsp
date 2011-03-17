<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->

<c:set var="note1" value="Only genes that have been mapped to the genome have been loaded"/>
<c:set var="note2" value="Also orthologues from these organisms to <i>C. familiaris</i>, <i>D. discoideum</i>, <i>D. rerio</i>, <i>G. gallus</i>, <i>H. sapiens</i>, <i>M. musculus</i>, <i>P. troglodytes</i>, <i>R. norvegicus</i>, <i>S. pombe</i>." />
<c:set var="note3" value="These data have been re-mapped to genome sequence release 5.0 as of FlyMine release 7.0."/>


<html:xhtml/>

<div class="body">
<im:boxarea title="Data" stylename="plainbox"><p><fmt:message key="dataCategories.intro"/></p></im:boxarea>


<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr>
    <th>Data Category</th>
    <th>Organism</th>
    <th>Data</th>
    <th>Source</th>
    <th>PubMed</th>
    <th>Note</th>
  </tr>
  <tr><td rowspan="3" class="leftcol">
        <h2><html:link action="/aspect?name=Genes"><p>Genes</p></html:link></h2></td>
    <td> <i>Drosophila</i> </td>
    <td> Genome annotation for D. melanogaster (R5.34), D. ananassae (R1.3), D. erecta (R1.3), D. grimshawi (R1.3), D. mojavensis (R1.3), D. persimilis (R1.3), D. pseudoobscura (R2.17), D. sechellia (R1.3), D. simulans (R1.3), D. virilis (R1.2), D. willistoni (R1.3) and D. yakuba (R1.3).</td>
    <td><a href="http://www.flybase.org" target="_new" class="extlink">Flybase</a> - Version FB2011_02</td>
    <td> Crosby et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099233" target="_new" class="extlink">PubMed: 17099233</a></td>
    <td> <html:link action="/dataCategories" anchor="note1" title="${note1}">#1</html:link></td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i></td>
    <td> Genome annotation - Release AgamP3.5</td>
    <td> <a href="http://www.ensembl.org" target="_new" class="extlink">Ensembl</a> - Release 56.3l</td>
    <td> Hubbard et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17148474 " target="_new" class="extlink">PubMed: 17148474</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i></td>
    <td> EST data set</td>
    <td> <a href="http://web.bioinformatics.ic.ac.uk/vectorbase/AnoEST.v8/index.php" target="_new" class="extlink">anoEST database</a> - Version 8</td>
    <td> Kriventseva et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/15899967" target="_new" class="extlink">PubMed: 15899967</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="3"  class="leftcol">
        <html:link action="/aspect?name=Homology">
          <p>  <h2>Homology <h2> </p></html:link></td>
    <td>
       <p><i>D. melanogaster</i></p>
       <p><i>S. cerevisiae</i></p>
       <!-- <p><i>A. gambiae</i></p>
       <p><i>A. mellifera</i></p> -->
       <p><i>C. elegans</i></p>
       <p><i>C. lupus familiaris</i></p>
       <p><i>D. rerio</i></p>
       <p><i>D. discoideum</i></p>
       <p><i>G. gallus</i></p>
       <p><i>H. sapiens</i></p>
       <p><i>M. musculus</i></p>
       <p><i>P. troglodytes</i></p>
       <p><i>R. norvegicus</i></p>
       <p><i>S. cerevisiae</i></p>
       <p><i>S. pombe</i></p>
       <p><i>A. aegypti</i></p>

    </td>
    <td> Orthologue and paralogue relationships between these 14 organisms</td>
    <td> <a href="http://inparanoid.sbc.su.se/" target="_new" class="extlink">InParanoid</a> - Version 7</td>
    <td> O'Brien et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/15608241" target="_new" class="extlink">PubMed: 15608241</a></td>
    <td><html:link action="/dataCategories" anchor="note2" title="${note2}">#2</html:link></td>
  </tr>

  <tr>
    <td><i>Drosophila</i></td>
    <td>Orthologues and paralogues between the 12 <i>Drosophila</i> genomes.</td>
    <td><a href="http://www.flybase.org" target="_new" class="extlink">Flybase</a> - Version FB2011_02</td>
    <td>Crosby et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099233 " target="_new" class="extlink">PubMed: 17099233</a></td>
    <td> &nbsp; </td>
  </tr>

  <tr>
    <td>
       <p><i>D. melanogaster</i></p>
       <p><i>D. rerio</i></p>
       <p><i>C. elegans</i></p>
       <p><i>M. musculus</i></p>
       <p><i>R. norvegicus</i></p>
       <p><i>H. sapiens</i></p>
       <p><i>A. gambiae</i></p>
       <p><i>S. cerevisiae</i></p>
    </td>
     <td>Orthologue and paralogue relationships between these 8 organisms</td>
    <td><a href="http://www.treefam.org/" target="_new" class="extlink">Treefam</a> - release 7.0</td>
    <td>Ruan et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/18056084 " target="_new" class="extlink">PubMed: 18056084</a></td>
    <td> &nbsp; </td>
  </tr>

  <tr><td rowspan="8"  class="leftcol">
        <html:link action="/aspect?name=Proteins">
        <p> <h2>Proteins</h2> </p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new" class="extlink">UniProt</a> - Release 2011_03</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17142230" target="_new" class="extlink">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i></td>
    <td> Protein annotation</td>
    <td> <a href="http://www.uniprot.org" target="_new" class="extlink">UniProt</a> - Release 2011_03</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17142230" target="_new" class="extlink">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i> </td>
    <td> Protein annotation</td>
    <td> <a href="http://www.uniprot.org" target="_new" class="extlink">UniProt</a> - Release 2011_03</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17142230" target="_new" class="extlink">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>H. sapiens</i></td>
    <td> Protein annotation</td>
    <td> <a href="http://www.uniprot.org" target="_new" class="extlink">UniProt</a> - Release 2011_03</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17142230">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>M. musculus</i></td>
    <td> Protein annotation</td>
    <td> <a href="http://www.uniprot.org" target="_new" class="extlink">UniProt</a> - Release 2011_03</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17142230" target="_new" class="extlink">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i></td>
    <td> Protein family and domain assignments to proteins</td>
    <td> <a href="http://www.ebi.ac.uk/interpro" target="_new" class="extlink">InterPro</a> (from UniProt Release 2011_03)</td>
    <td> Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17202162" target="_new" class="extlink">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i></td>
    <td> Protein family and domain assignments to proteins</td>
    <td> <a href="http://www.ebi.ac.uk/interpro" target="_new" class="extlink">InterPro</a> (from UniProt Release 2011_03)</td>
    <td> Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17202162" target="_new" class="extlink">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i> </td>
    <td> Protein family and domain assignments to proteins</td>
    <td> <a href="http://www.ebi.ac.uk/interpro" target="_new" class="extlink">InterPro</a> (from UniProt Release 2011_03)</td>
    <td> Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17202162" target="_new" class="extlink">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>


  <tr><td rowspan="3"  class="leftcol">
        <html:link action="/aspect?name=Protein%20Structure">
        <p> <h2>Protein Structure</h2></p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> 3-D structure predictions for protein domains</td>
    <td> <a href="http://www-cryst.bioc.cam.ac.uk/~kenji/NEW/index.htm" target="_new" class="extlink">Kenji Mizuguchi</a> - 9 April 2006</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i> </td>
    <td> 3-D structure predictions for protein domains</td>
    <td> <a href="http://www-cryst.bioc.cam.ac.uk/~kenji/NEW/index.htm" target="_new" class="extlink">Kenji Mizuguchi</a> - 27 June 2007</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>


  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Experimentally determined 3-D structures</td>
    <td> <a href="http://www.rcsb.org/pdb/home/home.do" target="_new" class="extlink">PDB [Protein Data Bank]</a> - 15 March 2011</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>


  <tr><td rowspan="7"  class="leftcol">
        <html:link action="/aspect?name=Interactions">
        <p> <h2>Interactions</h2></p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> High-throughput yeast 2-hybrid protein interaction datasets </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new" class="extlink">IntAct</a> - 11 March 2011</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17145710" target="_new" class="extlink">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i></td>
    <td> High-throughput yeast 2-hybrid protein interaction datasets </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new" class="extlink">IntAct</a> - 11 March 2011</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17145710" target="_new" class="extlink">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>S. cerevisiae</i></td>
    <td> High-throughput yeast 2-hybrid protein interaction datasets </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new" class="extlink">IntAct</a> - 11 March 2011</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17145710" target="_new" class="extlink">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>
 <tr>
    <td> <i>D. melanogaster</i></td>
    <td> Interactions from the BioGRID</td>
    <td> <a href="http://www.thebiogrid.org" target="_new" class="extlink">BioGRID</a> - Version 2.0.74 </td>
    <td> Stark et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16381927" target="_new" class="extlink">PubMed:16381927</a></td>
    <td> &nbsp;</td>
  </tr>

 <tr>
    <td> <i>C. elegans</i></td>
    <td> Interactions from the BioGRID</td>
    <td> <a href="http://www.thebiogrid.org" target="_new" class="extlink">BioGRID</a> - Version 2.0.74 </td>
    <td> Stark et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16381927" target="_new" class="extlink">PubMed:16381927</a></td>
    <td> &nbsp;</td>
  </tr>

 <tr>
    <td> <i>S. cerevisiae</i></td>
    <td> Interactions from the BioGRID</td>
    <td> <a href="http://www.thebiogrid.org" target="_new" class="extlink">BioGRID</a> - Version 2.0.74 </td>
    <td> Stark et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16381927" target="_new" class="extlink">PubMed:16381927</a></td>
    <td> &nbsp;</td>
  </tr>

 <tr>
    <td> <i>D. melanogaster</i></td>
    <td> miRNA target predictions from miRBase</td>
    <td> <a href="http://microrna.sanger.ac.uk/targets/v5" target="_new" class="extlink">miRBase</a> - Version 5 </td>
    <td> Enright et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/14709173" target="_new" class="extlink">PubMed:14709173</a></td>
    <td> &nbsp;</td>
  </tr>


  <tr><td rowspan="7"  class="leftcol">
        <html:link action="/aspect?name=Gene%20Ontology">
        <p> <h2>Gene Ontology</h2> </p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new" class="extlink">Gene Ontology Site</a> - 4 March 2011</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/10802651" target="_new" class="extlink">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new" class="extlink">Gene Ontology Site</a> - 14 January 2011</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/10802651" target="_new" class="extlink">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>M. musculus</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new" class="extlink">Gene Ontology Site</a> - 7 March 2011</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/10802651" target="_new" class="extlink">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>S. cerevisiae</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new" class="extlink">Gene Ontology Site</a> - 12 March 2011</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/10802651" target="_new" class="extlink">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>S. pombe</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new" class="extlink">Gene Ontology Site</a> - 4 March 2011</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/10802651" target="_new" class="extlink">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i> </td>
    <td> GO annotations </td>
    <td> <a href="http://www.ebi.ac.uk/GOA/uniprot_release.html" target="_new" class="extlink">UniProt GOA</a> - 10 March 2011</td>
    <td> Camon et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/14681408" target="_new" class="extlink">PubMed: 14681408</a></td>
    <td> &nbsp;</td>
  </tr>

    <tr>
    <td> <i>D. melanogaster, A. gambiae, C. elegans</i></td>
    <td> InterPro domains to GO annotations </td>
    <td> <a href="http://www.ebi.ac.uk/interpro" target="_new" class="extlink">InterPro</a> (from <a href="http://www.geneontology.org" target="_new" class="extlink">Gene Ontology Site</a>) - 9 February 2011</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/10802651" target="_new" class="extlink">PubMed:10802651</a>, Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17202162" target="_new" class="extlink">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="5" class="leftcol">
        <html:link action="/aspect?name=Expression">
        <p> <h2>Expression</h2></p>
         </html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> Microarray-based gene expression data for the life cycle of D. melanogaster</td>
    <td> <a href="http://www.ebi.ac.uk/arrayexpress" target="_new" class="extlink"> ArrayExpress </a> - Experiment E-FLYC-6</td>
    <td> Arbeitman et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/ 12351791" target="_new" class="extlink">PubMed: 12351791</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Affymetrix microarray-based atlas of gene expression in larval and adult tissues</td>
    <td> <a href="http://www.flyatlas.org" target="_new" class="extlink">FlyAtlas</a> - 25th April 2010</td>
    <td> Chintapalli et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17534367" target="_new" class="extlink">PubMed: 17534367</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Expression patterns of mRNAs at the subcellular level during early embryogenesis</td>
    <td> <a href="http://fly-fish.ccbr.utoronto.ca" target="_new" class="extlink">Fly-FISH</a> - 16th October 2007 </td>
    <td> Lecuyer et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17923096" target="_new" class="extlink">PubMed: 17923096</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Expression patterns of mRNAs during embryogenesis</td>
    <td> <a href="http://www.fruitfly.org/cgi-bin/ex/insitu.pl" target="_new" class="extlink">BDGP</a> - 9th May 2008 </td>
    <td> Tomancak et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17645804" target="_new" class="extlink">PubMed:17645804</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i> </td>
    <td> Microarray-based gene expression data for the life cycle of A. gambiae</td>
    <td> <a href="http://www.ebi.ac.uk/arrayexpress" target="_new" class="extlink"> ArrayExpress </a> - Experiment E-TABM-186</td>
    <td> Koutsos et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/ 17563388" target="_new" class="extlink">PubMed: 17563388</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="4" class="leftcol">
        <html:link action="/aspect?name=Regulation">
        <p> <h2>Regulation</h2></p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> Transcriptional cis-regulatory modules (CRMs)</td>
    <td> <a href="http://redfly.ccr.buffalo.edu" target="_new" class="extlink">REDfly</a> - 11th August 2009</td>
    <td> Gallo et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16303794" target="_new" class="extlink">PubMed: 16303794</a></td>
    <td>  &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Transcription factor binding sites</td>
    <td> <a href="http://www.flyreg.org" target="_new" class="extlink">REDfly</a> - 11th August 2009</td>
    <td> Bergman et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/15572468" target="_new" class="extlink">PubMed: 15572468</a></td>
    <td>  &nbsp; </td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Enhancers</td>
    <td> <a href="http://www.flybase.org" target="_new" class="extlink">FlyBase</a> - Version FB2011_02</td>
    <td> Crosby et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099233" target="_new" class="extlink">PubMed: 17099233</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Binding site predictions</td>
    <td> <a href="http://servlet.sanger.ac.uk/tiffin" target="_new" class="extlink">Tiffin</a> - 1.2</td>
    <td> Down et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17238282" target="_new" class="extlink">PubMed: 17238282</a></td>
    <td><html:link action="/dataCategories" anchor="note3" title="${note3}">#3</html:link></td>
  </tr>

  <tr><td rowspan="3" class="leftcol">
       <html:link action="/aspect?name=Phenotypes">
        <p> <h2>Phenotypes</h2></p></html:link></td>
   <td> <i>D. melanogaster</i> </td>
    <td> Alleles and phenotypes</td>
    <td> <a href="http://www.flybase.org" target="_new" class="extlink">FlyBase</a> - Version FB2011_02</td>
    <td> Crosby et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099233" target="_new" class="extlink">PubMed: 17099233</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> High-throughput cell-based RNAi screens</td>
    <td> <a href="http://flyrnai.org" target="_new" class="extlink">Drosophila RNAi Screening Center</a> - 22nd Mar 2010</td>
    <td> Flockhart et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16381918" target="_new" class="extlink">PubMed: 16381918</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i> </td>
    <td> RNAi data from in vivo experiments</td>
    <td> <a href="http://www.wormbase.org" target="_new" class="extlink">WormBase</a> - Release WS200 </td>
    <td> Bieri et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099234" target="_new" class="extlink">PubMed: 17099234</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="3" class="leftcol">
       <html:link action="/aspect?name=Pathways">
        <p> <h2>Pathways</h2></p></html:link></td>
    <td> <i>D. melanogaster</i>, <i>C. elegans</i>, <i>S. cerevisiae</i></td>
    <td> Curated pathway information and the genes involved in them</td>
    <td> <a href="http://www.genome.jp/kegg/" target="_new" class="extlink">KEGG</a> - Release 57, 12 March 2011</td>
    <td> Kanehisa et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16381885" target="_new" class="extlink">PubMed: 16381885</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i>, <i>C. elegans</i>, <i>S. cerevisiae</i> </td>
    <td> Pathway information and the genes involved in them, inferred through orthologues from Human curated pathways</td>
    <td> <a href="http://www.reactome.org/" target="_new" class="extlink">Reactome</a> - Version 34, 14 March 2011</td>
    <td> &nbsp;</td>
    <td>&nbsp;</td>
  </tr>

<tr>
    <td> <i>D. melanogaster</i></td>
    <td> Curated pathway information and the genes involved in them</td>
    <td> <a href="http://fly.reactome.org/" target="_new" class="extlink">FlyReactome</a> - Version 2.0</td>
    <td> &nbsp;</td>
    <td>&nbsp;</td>
  </tr>



  <tr><td rowspan="1" class="leftcol">
       <html:link action="/aspect?name=Disease"><p> <h2>Diseases</h2></p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> Human disease to Drosophila gene data set</td>
    <td> <a href="http://superfly.ucsd.edu/homophila/" target="_new" class="extlink">Homophila</a> - Version 2.1</td>
    <td> Reiter et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/11381037" target="_new" class="extlink">PubMed: 11381037</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="5" class="leftcol">
       <html:link action="/aspect?name=Resources">
        <p> <h2>Resources</h2></p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> <a href="http://www.drosdel.org.uk" target="_new" class="extlink">DrosDel</a> artificial deletions</td>
    <td> <a href="http://www.flybase.org" target="_new" class="extlink">FlyBase </a> - Version FB2011_02</td>
    <td> Ryder et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/15238529" target="_new" class="extlink">PubMed: 15238529</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Insertions (including DrosDel and Exelixis) </td>
    <td> <a href="http://www.flybase.org" target="_new" class="extlink">Flybase</a> - Version FB2011_02</td>
    <td> Crosby et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099233" target="_new" class="extlink">PubMed: 17099233</a></td>
    <td>&nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Probe sets from the Affymetrix GeneChip Drosophila Genome 1.0 Array</td>
    <td> <a href="http://www.ensembl.org" target="_new" class="extlink">Ensembl</a> - Release 50</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Probe sets from the Affymetrix GeneChip Drosophila Genome 2.0 Array</td>
    <td> <a href="http://www.ensembl.org" target="_new" class="extlink">Ensembl</a> - Release 50</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> INDAC Microarray oligo set - Version 1.0</td>
    <td> <a href="http://www.indac.net" target="_new" class="extlink">International Drosophila Array Consortium</a></td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="4" class="leftcol">
       <html:link action="/aspect?name=Publications">
        <p> <h2>Publications</h2></p></html:link></td>
    <td> <i>Drosophila</i> </td>
    <td> Gene versus publications</td>
    <td><a href="http://www.flybase.org" target="_new" class="extlink">Flybase</a> - Version FB2011_02</td>
    <td> Crosby et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099233" target="_new" class="extlink">PubMed: 17099233</a></td>
    <td> &nbsp;</td>
  </tr>
  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Gene versus publications</td>
    <td> <a href="http://www.ncbi.nlm.nih.gov" target="_new" class="extlink">NCBI</a> - 15 March 2011</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i></td>
    <td> Gene versus publications</td>
    <td> <a href="http://www.ncbi.nlm.nih.gov" target="_new" class="extlink">NCBI</a> - 15 March 2011</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>S. cerevisiae</i> </td>
    <td> Gene versus publications</td>
    <td> <a href="http://www.ncbi.nlm.nih.gov" target="_new" class="extlink">NCBI</a> - 15 March 2011</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

</table>

<div class="body">
<ol>
  <li><a name="note1">${note1}</a></li>
  <li><a name="note2">${note2}</a></li>
  <li><a name="note3">${note3}</a></li>
</ol>
</div>

</div>
<!-- /dataCategories -->
