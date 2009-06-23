<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->

<c:set var="note1" value="Only genes that have been mapped to the genome have been loaded"/>
<c:set var="note2" value="Also orthologues from these 5 organisms to <i>C. familiaris</i>, <i>D. discoideum</i>, <i>D. rerio</i>, <i>G. gallus</i>, <i>H. sapiens</i>, <i>M. musculus</i>, <i>P. troglodytes</i>, <i>R. norvegicus</i>, <i>S. cerevisiae</i>, <i>S. pombe</i>." />
<c:set var="note3" value="These data have been re-mapped to genome sequence release 5.0 as of FlyMine release 7.0."/>


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

  <tr><td rowspan="2" class="leftcol">
        <html:link action="/aspect?name=Genomics"> <p><img src="model/images/genomics.gif" /></p>
        <p> Genomics </p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> Genome annotation for Homo Sapiens</td>
    <td><a href="http://www.uniprot.org" target="_new">UniProt</a></td>
    <td> &nbsp;</td>
    <td> <html:link action="/dataCategories" anchor="note1" title="${note1}">#1</html:link></td>
  </tr>

  <tr>
    <td> <i>Mus Musculus</i></td>
    <td> Genome annotation for Mus Musculus</td>
    <td> <a href="http://www.uniprot.org" target="_new">Uniprot</a></td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1"  class="leftcol">
        <html:link action="/aspect?name=Comparative%20Genomics">
          <p>  <img src="model/images/comparativeGenomics.png" /></p>
          <p> Comparative Genomics </p></html:link></td>
    <td>
       <p><i>Homo Sapiens</i></p>
       <p><i>Mus Musculus</i></p>
    </td>
    <td> Orthologue and paralogue relationships between these 2 organisms</td>
    <td> <a href="http://inparanoid.sbc.su.se/" target="_new">InParanoid</a></td>
    <td> O'Brien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15608241" target="_new">PubMed: 15608241</a></td>
    <td><html:link action="/dataCategories" anchor="note2" title="${note2}">#2</html:link></td>    
  </tr>

  <tr><td rowspan="2"  class="leftcol">
        <html:link action="/aspect?name=Proteins">
        <p> <img src="model/images/proteins.png" /></p>
        <p> Proteins </p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a> - Release 13.4</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230" target="_new">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>Mus Musculus</i></td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a> - Release 13.4</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230" target="_new">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="2"  class="leftcol">
        <html:link action="/aspect?name=Genetic Interactions">
         <p> <img src="model/images/interaction.gif" /></p>
        <p> Genetic Interactions</p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> Genetic interactions from BioGRID </td>
    <td> <a href="http://www.thebiogrid.org/" target="_new">BioGrid</a> - 27th May 2008</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17145710" target="_new">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>Mus Musculus</i></td>
    <td> Genetic interactions from BioGRID </td>
    <td> <a href="http://www.thebiogrid.org/" target="_new">BioGrid</a> - 27th May 2008</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17145710" target="_new">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="2"  class="leftcol">
        <html:link action="/aspect?name=Protein Interactions">
         <p> <img src="model/images/interaction.gif" /></p>
        <p> Protein Interactions</p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> Protein interaction datasets from Intact </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new">IntAct</a> - 27th May 2008</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17145710" target="_new">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>Mus Musculus</i></td>
    <td> Protein interaction datasets from IntAct </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new">IntAct</a> - 27th May 2008</td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17145710" target="_new">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="2"  class="leftcol">

        <html:link action="/aspect?name=Gene%20Ontology">
         <p> <img src="model/images/geneOntology.png" /></p>
        <p> Gene Ontology </p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology
    Site</a> - 11th May 2008</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>M. musculus</i></td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology Site</a> - 24th May 2008</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="2" class="leftcol">
       <html:link action="/aspect?name=Pathways">
        <p> <img src="model/images/pathways.png" /></p>
        <p> Pathways</p></html:link></td>
    <td> <i>Homo Sapiens</i></td>
    <td> Pathway information and the genes involved in them</td>
    <td> <a href="http://www.genome.jp/kegg/" target="_new">KEGG</a> - Release 46.0, 23rd May 2008</td>
    <td> Kanehisa et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16381885" target="_new">PubMed: 16381885</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>Mus Musculus</i> </td>
    <td> Pathway information and the genes involved in them</td>
    <td> <a href="http://www.genome.jp/kegg/" target="_new">KEGG</a> - Release 46.0, 23rd May 2008</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16381885" target="_new">PubMed:16381885</a></td>
    <td> &nbsp;</td>
  </tr>


  <tr><td rowspan="1" class="leftcol">
       <html:link action="/aspect?name=Disease">
        <p> <img src="model/images/disease.png" /></p>
        <p> OMIM-Diseases</p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> Human disease</td>
    <td> <a href="http://www.ncbi.nlm.nih.gov/sites/entrez?db=omim" target="_new">OMIM</a></td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1" class="leftcol">
       <html:link action="/aspect?name=Literature">
        <p> <img src="model/images/book.png" /></p>
        <p> Literature</p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> Gene versus publications</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1"  class="leftcol">
        <html:link action="/aspect?name=Haem Atlas">
         <p> <img src="model/images/haem.gif" /></p>
        <p> Haematology Atlas</p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> Haematology gene expression data</td>
    <td> July 2008</td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1"  class="leftcol">
        <html:link action="/aspect?name=Height">
         <p> <img src="model/images/height.png" /></p>
        <p> Height data</p></html:link></td>
    <td> <i>Homo Sapiens</i> </td>
    <td> Height data </td>
    <td> July 2008</td>
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
