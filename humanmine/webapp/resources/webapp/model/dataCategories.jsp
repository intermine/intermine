<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->



<html:xhtml/>

<div class="body">
<im:boxarea title="Data" stylename="plainbox"><p>HumanMine integrates data from a large number of sources into a single data warehouse.  This page lists the data that are included in the current release.  Many more data sets will be added in future releases, please contact us if there are any particular data you would like to see included.</p></im:boxarea>


<div style="padding: 10px 40px">
<h3>The following data are loaded in the HumanMine:</h3>

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
               <td>Ensembl 79</td>
       </tr>
       <tr>
               <td>SNPs</td>
               <td><a href="http://www.ensembl.org/info/docs/variation/sources_documentation.html">Ensembl (including dbSNP, HGMD, COSMIC)</td>
               <td>Human</td>
               <td>Ensembl 79 (dbSNP 142)</td>
       </tr>
       <tr>
               <td>GWAS</td>
               <td><a href="http://www.hugenavigator.org/HuGENavigator/gWAHitStartPage.do">HuGE navigator (including GWAS catalog)</a></td>
               <td>Human</td>
               <td>December 2014</td>
       </tr>
       <tr>
               <td>Gene names and symbol</td>
               <td><a href="http://www.genenames.org/">HGNC</a>, <a href="http://www.ncbi.nlm.nih.gov/gene/">NCBI</a>, <a href="http://www.informatics.jax.org/">MGI</a>, <a href="http://rgd.mcw.edu/">RGD</a></td>
               <td>Human, Mouse, Rat</td>
               <td>December 2014</td>
       </tr>
       <tr>
               <td>Protein sequence and annotation</a></td>
               <td><a href="http://www.uniprot.org/">UniProt</a></td>
               <td>Human, Mouse</td>
               <td>2015_04</td>
       </tr>
       <tr>
               <td>Protein domains</td>
               <td><a href="http://www.ebi.ac.uk/interpro/">InterPro</a></td>
               <td>Human, Mouse</td>
               <td>2015-04-23</td>
       </tr>
       <tr>
               <td>Gene Ontology</td>
               <td><a href="http://www.uniprot.org/">UniProt</a>, <a href="http://www.informatics.jax.org/">MGI</a>, <a href="http://www.geneontology.org/">GO Consortium</a></td>
               <td>Human, Mouse</td>
               <td>April 2015</td>
       </tr>
       <tr>
               <td>Pathways</td>
               <td><a href="http://www.reactome.org/">Reactome</a></td>
               <td>Human, Mouse</td>
               <td>April 2015</td>
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
               <td>(v. 13.0) Nov 2014</td>
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
               <td>April 2015</td>
       </tr>  
       <tr>
               <td>Interactions</td>
               <td><a href="http://thebiogrid.org/">BioGRID</a></td>
               <td>Human, Mouse</td>
               <td>3.3.123 (April 2015)</td>
       </tr>
       <tr>
               <td>Publications</td>
               <td><a href="ftp://ftp.ncbi.nih.gov/gene/DATA/">NCBI PubMed (gene to PubMed id mappings)</a></td>
               <td>Human, Mouse</td>
               <td>April 2015</td>
       </tr>
       <tr>
               <td>Disease</td>
               <td><a href="http://www.omim.org/">OMIM</a></td>
               <td>Human</td>
               <td>April 2015</td>
       </tr>  
       <tr>
               <td>Orthologues</td>
               <td><a href="http://www.pantherdb.org/>Panther</a></td>
               <td>Human, Mouse, Rat</td>
               <td>(v. 9.0) April 2015</td>
       </tr>
       <tr>
               <td>Alleles</td>
               <td><a href="http://www.informatics.jax.org/">MGI</a></td>
               <td>Mouse</td>
               <td>April 2015</td>
       </tr>  
</table>

</div>
</div>
<!-- /dataCategories -->
