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

<br/>

<table>
       <tr>
               <th>Type</th>
               <th>Source</th>
               <th>Organisms</th>
               <th>Version</th>
       </tr>
       <tr>
               <td>Genome sequence and annotation</td>
               <td><a href="http://www.ensembl.org">Ensembl</a></td>
               <td>Human, Mouse</td>
               <td>Ensembl 66</td>
       </tr>
       <tr>
               <td>SNPs</td>
               <td><a href="http://www.ensembl.org/info/docs/variation/sources_documentation.html">Ensembl (including dbSNP, HGMD, COSMIC)</td>
               <td>Human</td>
               <td>Ensembl 66 (dbSNP 135)</td>
       </tr>
       <tr>
               <td>GWAS</td>
               <td><a href="http://www.hugenavigator.org/HuGENavigator/gWAHitStartPage.do">HuGE navigator (including GWAS catalog)</a></td>
               <td>Human</td>
               <td>March 2012</td>
       </tr>
       <tr>
               <td>Gene names and symbol</td>
               <td><a href="http://www.genenames.org/">HGNC</a>, <a href="http://www.ncbi.nlm.nih.gov/gene/">NCBI</a>, <a href="http://www.informatics.jax.org/">MGI</a>, <a href="http://rgd.mcw.edu/">RGD</a></td>
               <td>Human, Mouse, Rat</td>
               <td>March 2012</td>
       </tr>
       <tr>
               <td>Protein sequence and annotation</a></td>
               <td><a href="http://www.uniprot.org/">UniProt</a></td>
               <td>Human, Mouse</td>
               <td>2012_04</td>
       </tr>
       <tr>
               <td>Protein domains</td>
               <td><a href="http://www.ebi.ac.uk/interpro/">InterPro</a></td>
               <td>Human, Mouse</td>
               <td>19th January 2012</td>
       </tr>
       <tr>
               <td>Gene Ontology</td>
               <td><a href="http://www.uniprot.org/">UniProt</a>, <a href="http://www.informatics.jax.org/">MGI</a>, <a href="http://www.geneontology.org/">GO Consortium</a></td>
               <td>Human, Mouse</td>
               <td>March 2012</td>
       </tr>
       <tr>
               <td>Pathways</td>
               <td><a href="http://www.reactome.org/">Reactome</a></td>
               <td>Human, Mouse</td>
               <td>March 2012</td>
       </tr>
       <tr>
               <td>Pathways</td>
               <td><a href="http://www.genome.jp/kegg/pathway.html">KEGG</a></td>
               <td>Human, Mouse</td>
               <td>May 2011</td>
       </tr>
       <tr>
               <td>Protein localisation</td>
               <td><a href="http://www.proteinatlas.org/">Human Protein Atlas (HPA)</a></td>
               <td>Human</td>
               <td>(v. 9.0) Nov 2011</td>
       </tr>
       <tr>
               <td>Gene expression</td>
               <td><a href="http://www.ebi.ac.uk/arrayexpress/experiments/E-MTAB-62">ArrayExpress Atlas (experiment E-MTAB-62)</a></td>
               <td>Human</td>
               <td>June 2011</td>
       </tr>
       <tr>
               <td>Interactions</td>
               <td><a href="http://www.ebi.ac.uk/intact/">IntAct</a></td>
               <td>Human, Mouse</td>
               <td>February 2012</td>
       </tr>  
       <tr>
               <td>Interactions</td>
               <td><a href="http://thebiogrid.org/">BioGRID</a></td>
               <td>Human, Mouse</td>
               <td>3.1.86 (March 2012)</td>
       </tr>
       <tr>
               <td>Publications</td>
               <td><a href="ftp://ftp.ncbi.nih.gov/gene/DATA/">NCBI PubMed (gene to PubMed id mappings)</a></td>
               <td>Human, Mouse</td>
               <td>March 2012</td>
       </tr>
       <tr>
               <td>Disease</td>
               <td><a href="http://www.omim.org/">OMIM</a></td>
               <td>Human</td>
               <td>October 2010</td>
       </tr>  
       <tr>
               <td>Orthologues</td>
               <td><a href="http://www.ensembl.org/info/docs/compara/index.html">Ensembl Compara</a></td>
               <td>Human, Mouse, Rat</td>
               <td>Ensembl 66</td>
       </tr>
       <tr>
               <td>Alleles</td>
               <td><a href="http://www.informatics.jax.org/">MGI</a></td>
               <td>Mouse</td>
               <td>March 2012</td>
       </tr>  
</table>

</div>
</div>
<!-- /dataCategories -->
