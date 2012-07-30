<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->


<html:xhtml/>

<div class="body">
<im:boxarea title="Data" stylename="plainbox"><p>PlantMine includes data from Ensembl Genomes release 12 integrated with data from several other sources.</p></im:boxarea>


<div style="padding: 10px 40px">
<h3>The following data are loaded in PlantMine:</h3>

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
               <td><a href="http://www.ensemblgenomes.org">Ensembl Genomes</a></td>
               <td><i>A. thaliana</i></td>
               <td>Release 12 (December 2011)</td>
       </tr>
       <tr>
               <td>SNPs</td>
               <td><a href="http://plants.ensembl.org/info/docs/variation/index.html">Ensembl Genomes</td>
               <td><i>A. thaliana</i></td>
               <td>Release 12 (December 2011)</td>
       </tr>
       <tr>
               <td>Phenotypes</td>
               <td><a href="http://plants.ensembl.org/info/docs/variation/index.html">Ensembl Genomes</a></td>
               <td><i>A. thaliana</i></td>
               <td>Release 12 (December 2011)</td>
       </tr>
       <tr>
               <td>Protein sequence and annotation</a></td>
               <td><a href="http://www.uniprot.org/">UniProt</a></td>
               <td><i>A. thaliana</i></td>
               <td>2011_12</td>
       </tr>
       <tr>
               <td>Protein domains</td>
               <td><a href="http://www.ebi.ac.uk/interpro/">InterPro</a></td>
               <td><i>A. thaliana</i></td>
               <td>8th February 2012</td>
       </tr>
       <tr>
               <td>Gene Ontology</td>
               <td><a href="http://www.geneontology.org/">GO Consortium</a></td>
               <td><i>A. thaliana</i></td>
               <td>21st February 2012</td>
       </tr>

       <tr>
               <td>Publications</td>
               <td><a href="ftp://ftp.ncbi.nih.gov/gene/DATA/">NCBI PubMed (gene to PubMed id mappings)</a></td>
               <td><i>A. thaliana</i></td>
               <td>8th February 2012</td>
       </tr>

</table>

</div>
</div>
<!-- /dataCategories -->
