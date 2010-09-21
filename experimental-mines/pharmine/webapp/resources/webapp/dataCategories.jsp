<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->

<c:set var="note1" value="Only genes that have been mapped to the genome have been loaded"/>
<c:set var="note2" value="Also orthologues from these 5 organisms to <i>C. familiaris</i>, <i>D. discoideum</i>, <i>D. rerio</i>, <i>G. gallus</i>, <i>H. sapiens</i>, <i>M. musculus</i>, <i>P. troglodytes</i>, <i>R. norvegicus</i>, <i>S. cerevisiae</i>, <i>S. pombe</i>." />
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

  <tr><td rowspan="2" class="leftcol">
        <html:link action="/aspect?name=Nomenclature">
        <p> Nomenclature data </p></html:link></td>
    <td> <i>H. Sapiens</i> </td>
    <td> Nomenclature data from the HUGO Gene Nomenclature Committee (Symbol, Name, Pubmed IDs, RefSeq IDs, GDB ID, Entrez ID, OMIM ID, UniProt ID, Ensemble ID).</td>
    <td><a href="http://www.genenames.org/" target="_new">HGNC</a></td>
    <td> </td>
    <td></td>
  </tr>

  <tr>
    <td> <i>H. Sapiens</i></td>
    <td> Reference names from the Biogrid</td>
    <td> <a href="http://www.thebiogrid.org/" target="_new">BioGrid</a></td>
    <td> </td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="2" class="leftcol">
        <html:link action="/aspect?name=Genomics"> <p><img src="model/images/genomics.gif" /></p>
        <p> Genomics </p></html:link></td>
    <td> <i>H. Sapiens</i> </td>
    <td> Genome annotation for Homo Sapiens.</td>
    <td><a href="http://www.ensembl.org" target="_new">Ensembl</a></td>
    <td> </td>
    <td></td>
  </tr>

  <tr>
    <td> <i>H. Sapiens</i></td>
    <td> Genomic data from Entrez. </td>
    <td> <a href="http://www.ncbi.nlm.nih.gov/sites/entrez?db=gene" target="_new">Entrez</a></td>
    <td></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1"  class="leftcol">
        <html:link action="/aspect?name=Comparative%20Genomics">
          <p>  <img src="model/images/comparativeGenomics.png" /></p>
          <p> Comparative Genomics </p></html:link></td>
    <td>
       <p><i>H. Sapiens</i></p>
    </td>
    <td> Orthologues and paralogues between Homo Sapiens and all other species provided by Ensembl Compara</td>
    <td> <a href="http://inparanoid.sbc.su.se/" target="_new">InParanoid</a> - Version 6.1</td>
    <td> O'Brien et al - <a href="http://www.ensembl.org/info/docs/api/compara/index.html" target="_new">Ensembl Compara</a></td>
    <td></td>
  </tr>

  <tr><td rowspan="1" class="leftcol">
        <html:link action="/aspect?name=Phenotypes">
        <p> Phenotypic data </p></html:link></td>
    <td> <i>H. Sapiens</i> </td>
    <td> Phenotypic data from MGI-Jackson.</td>
    <td><a href="www.informatics.jax.org/" target="_new">MGI-Jackson</a></td>
    <td> </td>
    <td></td>
  </tr>

  <tr><td rowspan="2"  class="leftcol">
        <html:link action="/aspect?name=Chemicals">
        <p> Chemical data </p></html:link></td>
    <td> <i>H. Sapiens</i> </td>
    <td> Chemical Annotation from ChEBI</td>
    <td> <a href="http://www.ebi.ac.uk/chebi/" target="_new">ChEBI</a></td>
    <td> </td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>H. Sapiens</i></td>
    <td> Chemical-Chemical and Chemical-Protein links (unannotated) from Stitch</td>
    <td> <a href="http://stitch.embl.de/" target="_new">Stitch</a></td>
    <td> </td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="4"  class="leftcol">
        <html:link action="/aspect?name=Proteins">
        <p> <img src="model/images/proteins.png" /></p>
        <p> Proteins </p></html:link></td>
    <td> <i>H. Sapiens</i> </td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a></td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230" target="_new">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>H. Sapiens</i></td>
    <td> Protein family and domain assignments to proteins</td>
    <td> <a href="http://www.ebi.ac.uk/interpro" target="_new">InterPro</a> (from UniProt)</td>
    <td> Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17202162" target="_new">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>H. Sapiens</i></td>
    <td> Protein Annotation from Ensembl</td>
    <td> <a href="http://www.ensembl.org" target="_new">Ensembl</a></td>
    <td</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>H. Sapiens</i></td>
    <td> Protein - Chemical Links (Unannotated)</td>
    <td> <a href="http://stitch.embl.de/" target="_new">Stitch</a></td>
    <td</td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1"  class="leftcol">
        <html:link action="/aspect?name=Protein%20Structure">
        <p> <img src="model/images/pstructure.gif" /></p>
        <p> Protein Structure</p></html:link></td>
    <td> <i>H. Sapiens</i> </td>
    <td> Experimentally determined 3-D structures</td>
    <td> <a href="http://www.rcsb.org/pdb/home/home.do" target="_new">PDB [Protein Data Bank]</a></td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>




  <tr><td rowspan="2"  class="leftcol">
        <html:link action="/aspect?name=Interactions">
         <p> <img src="model/images/interaction.gif" /></p>
        <p> Interactions</p></html:link></td>
    <td> <i>H. Sapiens</i></td>
    <td> Interactions from IntAct </td>
    <td> <a href="http://www.ebi.ac.uk/intact" target="_new">IntAct</a></td>
    <td> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17145710" target="_new">PubMed:17145710</a></td>
    <td> &nbsp;</td>
  </tr>

 <tr>
    <td> <i>H. Sapiens</i></td>
    <td> Interactions from the BioGRID</td>
    <td> <a href="http://www.thebiogrid.org/" target="_new">BioGRID</a></td>
    <td> Stark et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16381927" target="_new">PubMed:16381927</a></td>
    <td> &nbsp;</td>
  </tr>


  <tr><td rowspan="1"  class="leftcol">

        <html:link action="/aspect?name=Gene%20Ontology">
         <p> <img src="model/images/geneOntology.png" /></p>
        <p> Gene Ontology </p></html:link></td>
    <td> <i>H. Sapiens</i> </td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology Site</a></td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="2" class="leftcol">
       <html:link action="/aspect?name=Pathways">
        <p> <img src="model/images/pathways.png" /></p>
        <p> Pathways</p></html:link></td>
    <td> <i>H. Sapiens</i></td>
    <td> Pathways and Proteins from Reactome</td>
    <td> <a href="http://www.reactome.org/" target="_new">Reactome</a></td>
    <td> </td>
    <td> &nbsp;</td>
  </tr>

 <tr>
    <td> <i>H. Sapiens</i></td>
    <td> Pathways from Pathway Commons</td>
    <td> <a href="http://www.pathwaycommons.org/" target="_new">BioGRID</a></td>
    <td> </td>
    <td> &nbsp;</td>
  </tr>

  <tr><td rowspan="1" class="leftcol">
       <html:link action="/aspect?name=Disease">
        <p> <img src="model/images/disease.png" /></p>
        <p> Diseases</p></html:link></td>
    <td> <i>H. Sapiens</i> </td>
    <td> Disease data taken from HUGE</td>
    <td> <a href="http://www.kazusa.or.jp/huge/" target="_new">HUGE Database</a></td>
    <td></td>
    <td> &nbsp;</td>
  </tr>


  <tr><td rowspan="2" class="leftcol">
       <html:link action="/aspect?name=Literature">
        <p> <img src="model/images/book.png" /></p>
        <p> Literature</p></html:link></td>
    <td> <i>H. Sapiens</i></td>
    <td> Gene versus publications</td>
    <td> <a href="http://www.ncbi.nlm.nih.gov" target="_new">NCBI</a></td>
    <td> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>H. Sapiens</i> </td>
    <td> Publication data taken from HUGE</td>
    <td> <a href="http://www.kazusa.or.jp/huge/" target="_new">HUGE Database</a></td>
    <td></td>
    <td> &nbsp;</td>
  </tr>

</table>



</div>
<!-- /dataCategories -->
