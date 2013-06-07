<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->

<c:set var="note1" value="Only genes that have been mapped to the genome have been loaded"/>
<!--  ML comment - don't need these two notes as they relate to FlyMine
<c:set var="note2" value="Also orthologues from these organisms to <i>C. familiaris</i>, <i>D. discoideum</i>, <i>D. rerio</i>, <i>G. gallus</i>, <i>H. sapiens</i>, <i>M. musculus</i>, <i>P. troglodytes</i>, <i>R. norvegicus</i>, <i>S. pombe</i>." />
<c:set var="note3" value="These data have been re-mapped to genome sequence release 5.0 as of FlyMine release 7.0."/>
 -->


<html:xhtml/>

<div class="body">
<im:boxarea title="Data" stylename="plainbox"><p>SynBioMine integrates data from a large number of sources into a single data warehouse.  This page lists the data that are included in the current release.  Many more data sets will be added in future releases, please contact us if there are any particular data you would like to see included.</p></im:boxarea>


<div style="padding: 10px 40px">
<h3>The following data are loaded in SynBioMine:</h3>

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
               <td><a href="ftp://ftp.ncbi.nlm.nih.gov/genomes/Bacteria/">Genbank</a></td>
               <td><i>E. coli</i> K12 (MG1655), <i>B. subtilis</i> (168)</td>
               <td>Genbank (Mar, 2013)</td>
       </tr>
       <tr>
               <td>Gene names and symbol</td>
               <td><a href="ftp://ftp.ncbi.nlm.nih.gov/genomes/Bacteria/">Genbank</a></td>
               <td><i>E. coli</i> K12 (MG1655), <i>B. subtilis</i> (168)</td>
               <td>Genbank (Mar, 2013)</td>
       </tr>
       <tr>
               <td>Protein sequence and annotation</a></td>
               <td><a href="http://www.uniprot.org/">UniProt</a></td>
               <td><i>E. coli</i> K12 (MG1655), <i>B. subtilis</i> (168)</td>
               <td>Mar, 2013</td>
       </tr>
       <tr>
               <td>Protein domains</td>
               <td><a href="http://www.ebi.ac.uk/interpro/">InterPro</a></td>
               <td><i>E. coli</i> K12 (MG1655), <i>B. subtilis</i> (168)</td>
               <td>Mar, 2013</td>
       </tr>
       <tr>
               <td>Gene Ontology</td>
               <td><a href="http://www.uniprot.org/">UniProt</a></td>
               <td><i>E. coli</i> K12 (MG1655), <i>B. subtilis</i> (168)</td>
               <td>Mar, 2013</td>
       </tr>
       <tr>
               <td>Pathways</td>
               <td><a href="http://www.reactome.org/">Reactome</a></td>
               <td><i>E. coli</i> K12 (MG1655), <i>B. subtilis</i> (168)</td>
               <td>May, 2013</td>
       </tr>
       <tr>
               <td>Interactions</td>
               <td><a href="http://thebiogrid.org/">BioGRID</a></td>
               <td><i>E. coli</i> K12 (MG1655), <i>B. subtilis</i> (168)</td>
               <td>Apr, 2013</td>
       </tr>
       <tr>
               <td>Publications</td>
               <td><a href="ftp://ftp.ncbi.nih.gov/gene/DATA/">NCBI PubMed (gene to PubMed id mappings)</a></td>
               <td><i>E. coli</i> K12 (MG1655), <i>B. subtilis</i> (168)</td>
               <td>Apr, 2013</td>
       </tr>
</table>

</div>
</div>
<!-- /dataCategories -->
