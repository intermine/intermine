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
<im:boxarea title="Data" stylename="plainbox"><p>metabolicMine integrates data from a large number of sources into a single data warehouse.  This page lists the data that are included in the current release.  Many more data sets will be added in future releases, please contact us if there are any particular data you would like to see included.</p></im:boxarea>


<div style="padding: 10px 40px">
<h3>The following data are loaded in the metabolicMine BETA:</h3>

<ul>
  <li>Human genome sequence and annotation - Ensembl 62</li>
  <li>Mouse genome sequence and annotation - Ensembl 62</li>
  <li>Human SNPs (including dbSNP 132) - Ensembl 61</li>
  <li>Human GWAS results - Ensembl 62</li>
  <li>Human gene names and symbols - NCBI and HGNC</li>
  <li>Mouse gene names and symbols - MGI</li>
  <li>Human and Mouse protein sequences and information - Uniprot 2011_05</li>
  <li>Human and Mouse protein domains - InterPro</li>
  <li>Human and Mouse Gene Ontology (GO) terms - UniProt and MGI</li>
  <li>Human and Mouse pathways - KEGG</li>
  <li>Human pathways - Reactome</li>
  <li>Human protein expression - Protein Atlas</li>
  <li>Human and Mouse protein interactions - Intact, BioGRID</li>
  <li>Human and Mouse publication to gene relationships - PubMed</li>
  <li>Human diseases - OMIM</li>
  <li>Human, Mouse and Rat orthologues - Ensembl Compara</li>

</ul>
</div>
</div>
<!-- /dataCategories -->
