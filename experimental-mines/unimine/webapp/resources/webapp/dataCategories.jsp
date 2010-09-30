<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->

<c:set var="note1" value="Also orthologues from these 5 organisms to <i>C. familiaris</i>, <i>D. discoideum</i>, <i>D. rerio</i>, <i>G. gallus</i>, <i>H. sapiens</i>, <i>M. musculus</i>, <i>P. troglodytes</i>, <i>R. norvegicus</i>, <i>S. cerevisiae</i>, <i>S. pombe</i>" />
<c:set var="note2" value="These data have been re-mapped to genome sequence release 5.0 as of FlyMine release 7.0"/>


<html:xhtml/>

<div class="body">
<im:boxarea title="Data" stylename="plainbox"><p><fmt:message key="dataCategories.intro"/></p></im:boxarea>


<table cellpadding="0" cellpadding="0" border="0" class="dbsources">
  <tr>
    <th>Data Category</th>
    <th>Organism</th>
    <th>Data</th>
    <th>Source</th>
    <th>PubMed</th>
    <th>Note</th>
  </tr>


  <tr><td rowspan="8"  class="leftcol">
        <html:link action="/aspect?name=Proteins">
        <p> <img src="model/images/proteins.png" /></p>
        <p> Proteins </p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a> - Release 12.4</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230" target="_new">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i></td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a> - Release 12.4</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230" target="_new">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i> </td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a> - Release 12.4</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230" target="_new">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>H. sapiens</i></td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a> - Release 12.4</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>M. musculus</i></td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a> - Release 12.4</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230" target="_new">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i></td>
    <td> Protein family and domain assignments to proteins</td>
    <td> <a href="http://www.ebi.ac.uk/interpro" target="_new">InterPro</a> (from Uniprot release 12.4)</td>
    <td> Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17202162" target="_new">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i></td>
    <td> Protein family and domain assignments to proteins</td>
    <td> <a href="http://www.ebi.ac.uk/interpro" target="_new">InterPro</a> (from Uniprot release 12.4)</td>
    <td> Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17202162" target="_new">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i> </td>
    <td> Protein family and domain assignments to proteins</td>
    <td> <a href="http://www.ebi.ac.uk/interpro" target="_new">InterPro</a> (from Uniprot release 12.4)</td>
    <td> Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17202162" target="_new">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>


  <tr><td rowspan="5"  class="leftcol">
        <html:link action="/aspect?name=Protein%20Structure">
        <p> <img src="model/images/pstructure.gif" /></p>
        <p> Protein Structure</p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> 3-D structure predictions for protein domains</td>
    <td> <a href="http://www-cryst.bioc.cam.ac.uk/~kenji/NEW/index.htm" target="_new">Kenji Mizuguchi</a> - 9th April 2006</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>H. sapiens</i> </td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i> </td>
    <td> Experimentally determined 3-D structures</td>
    <td> <a href="http://www.rcsb.org/pdb/home/home.do" target="_new">PDB [Protein Data Bank]</a> - 9th August 2007</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>M. musculus</i> </td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>S. cerevisiae</i> </td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>




  <tr><td rowspan="4"  class="leftcol">
        <html:link action="/aspect?name=Interactions">
         <p> <img src="model/images/interaction.gif" /></p>
        <p> Interactions</p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> High-throughput yeast 2-hybrid protein interaction datasets </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new">IntAct</a> - 7th January 2008</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17145710" target="_new">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i></td>
    <td> High-throughput yeast 2-hybrid protein interaction datasets </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new">IntAct</a> - 7th January 2008</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17145710" target="_new">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>S. cerevisiae</i></td>
    <td> High-throughput yeast 2-hybrid protein interaction datasets </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new">IntAct</a> - 7th January 2008</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17145710" target="_new">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>
 <tr>
    <td> <i>D. melanogaster</i></td>
    <td> Genetic interactions from the BioGRID</td>
    <td> <a href="http://www.thebiogrid.org/" target="_new">BioGRID</a> - Version 2.0.38 </td>
    <td> Stark et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16381927" target="_new">PubMed:16381927</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="7"  class="leftcol">
        <html:link action="/aspect?name=Gene%20Ontology">
         <p> <img src="model/images/geneOntology.png" /></p>
        <p> Gene Ontology </p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology Site</a> - 11th February 2008</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology Site</a> - 21st December 2007</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>M. musculus</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology Site</a> - 18th January 2008</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>S. cerevisiae</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology Site</a> - 19th January 2008</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>S. pombe</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology Site</a> - 17th December 2007</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. gambiae</i> </td>
    <td> GO annotations </td>
    <td> <a href="http://www.ebi.ac.uk/GOA/uniprot_release.html" target="_new">Uniprot GOA</a> - 21st January 2008</td>
    <td> Camon et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14681408" target="_new">PubMed: 14681408</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>A. mellifera</i> </td>
    <td> GO annotations </td>
    <td> <a href="http://www.ebi.ac.uk/GOA/uniprot_release.html" target="_new">Uniprot GOA</a> - 21st January 2008</td>
    <td> Camon et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14681408" target="_new">PubMed: 14681408</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1" class="leftcol">
       <html:link action="/aspect?name=Pathways">
        <p> <img src="model/images/pathways.png" /></p>
        <p> Pathways</p></html:link></td>
    <td> <i>D. melanogaster</i></td>
    <td> Pathway information and the genes involved in them</td>
    <td> <a href="http://www.genome.jp/kegg/" target="_new">KEGG</a> - Release 45</td>
    <td> Kanehisa et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16381885" target="_new">PubMed: 16381885</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1" class="leftcol">
       <html:link action="/aspect?name=Pride">
        <p> <img src="model/images/pride.jpg" /></p>
        <p> PRIDE</p></html:link></td>
    <td> &nbsp;</td>
    <td> PRoteomics IDEntifications database</td>
    <td> <a href="http://www.ebi.ac.uk/pride/" target="_new">PRIDE</a> </td>
    <td> OBBeC Life - Quick Guide to PRIDE <br><br>
         Proteomics Vol 5 - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=16041671&query_hl=2" target="_new">PubMed: 16041671</a> <br><br>
         Nucleic Acids Research Vol 1 - <br><a href="http://nar.oxfordjournals.org/cgi/content/full/34/suppl_1/D659" target="_new">PubMed: 16381953</a><br><br>
         Nucleic Acids Research 36 - <br><a href="http://nar.oxfordjournals.org/cgi/content/full/36/suppl_1/D878" target="_new">PubMed: 18033805</a></td>
    <td> &nbsp;</td>
 


  </tr>

</table>

<div class="body">
<ol>
	<li><a name="note1">${note1}</a></li>
	<li><a name="note2">${note2}</a></li>
</ol>
</div>

</div>
<!-- /dataCategories -->
