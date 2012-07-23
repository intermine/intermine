<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<html:xhtml/>

<div class="body">

<%--
<tiles:insert name="submissions.tile"/>
--%>

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
        <html:link action="/projectsSummary.do"> <p><img src="model/images/modENCODE.png" /></p>
        <p> modENCODE </p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td>
      <im:querylink text="See all fly modENCODE submissions" skipBuilder="true">
        <query name="" model="genomic" view="Submission.title Submission.DCCid Submission:project.name Submission:project.surnamePI" sortOrder="Submission.title asc">
          <node path="Submission" type="Submission">
          </node>
          <node path="Submission.organism" type="Organism">
          </node>
          <node path="Submission.organism.shortName" type="String">
            <constraint op="=" value="D. melanogaster" description="" identifier="" code="A">
            </constraint>
          </node>
        </query>
      </im:querylink>
    </td>
    <td> </td>
    <td> </td>
    <td> &nbsp;</td>
  </tr>
  <tr>
  <td> <i>C. elegans</i> </td>
    <td>
        <im:querylink text="See all worm modENCODE submissions" skipBuilder="true">
       <query name="" model="genomic" view="Submission.title Submission.DCCid Submission:project.name Submission:project.surnamePI" sortOrder="Submission.title asc">
         <node path="Submission" type="Submission">
         </node>
         <node path="Submission.organism" type="Organism">
         </node>
         <node path="Submission.organism.shortName" type="String">
           <constraint op="=" value="C. elegans" description="" identifier="" code="A">
           </constraint>
         </node>
       </query>
       </im:querylink>
     </td>
    <td> </td>
    <td> </td>
    <td> &nbsp;</td>


  <tr><td rowspan="2" class="leftcol">
        <html:link action="/aspect?name=Genomics"> <p><img src="model/images/genomics.gif" /></p>
        <p> Genomics </p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td> Genome annotation - Release ${WEB_PROPERTIES['genomeVersion.fly']} </td>
    <td><a href="http://www.flybase.org" target="_new" class="extlink">FlyBase</a></td>
    <td> Crosby et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17099233" target="_new" class="extlink">PubMed: 17099233</a></td>
    <td> &nbsp;</td>
  </tr>
  <tr>
  <td> <i>C. elegans</i> </td>
    <td> Genome annotation - Release ${WEB_PROPERTIES['genomeVersion.worm']} </td>
    <td><a href="http://www.wormbase.org" target="_new" class="extlink">WormBase</a> - ${WEB_PROPERTIES['genomeVersion.worm']}</td>
    <td> - </td>
    <td> &nbsp;</td>
</tr>
  <tr><td rowspan="2"  class="leftcol">
        <html:link action="/aspect?name=Genomics">
          <p>  <img src="model/images/comparativeGenomics.png" /></p>
          <p> Comparative Genomics </p></html:link></td>
    <td>
       <p><i>D. melanogaster</i></p>

<p><i>C. elegans</i></p>
    </td>
    <td> Orthologue and paralogue relationships between these 2 organisms</td>
    <td> <a href="http://inparanoid.sbc.su.se/" target="_new" class="extlink">InParanoid</a></td>
    <td> O'Brien et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15608241" target="_new" class="extlink">PubMed: 15608241</a></td>
    <td> &nbsp;</td>
<!--
    <td><html:link action="/dataCategories" anchor="note1" title="${note1}">#1</html:link></td>
-->
  </tr>
  <tr>
  <td>
     <p><i>D. melanogaster</i></p>
     <p><i>C. elegans</i></p>
  </td>
   <td>Orthologue and paralogue relationships between these 2 organisms</td>
  <td><a href="http://www.treefam.org/" target="_new" class="extlink">Treefam</a></td>
  <td>Ruan et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/18056084 " target="_new" class="extlink">PubMed: 18056084</a></td>
  <td> &nbsp; </td>
</tr>


  <tr><td rowspan="4"  class="leftcol">
        <html:link action="/aspect?name=Proteins">
        <p> <img src="model/images/proteins.png" /></p>
        <p> Proteins </p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td rowspan="2"> Protein annotation</td>
    <td rowspan="2"> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new" class="extlink">UniProt</a></td>
    <td rowspan="2"> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17142230" target="_new" class="extlink">PubMed: 17142230</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i> </td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>D. melanogaster</i></td>
    <td rowspan="2"> Protein family and domain assignments to proteins</td>
    <td rowspan="2"> <a href="http://www.ebi.ac.uk/interpro" target="_new" class="extlink">InterPro</a></td>
    <td rowspan="2"> Mulder et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=17202162" target="_new" class="extlink">PubMed: 17202162</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i> </td>
    <td> &nbsp;</td>
  </tr>

<!-- INTERACTIONS -->
  <tr><td rowspan="4"  class="leftcol">
  <html:link action="/aspect?name=Interactions">
   <p> <img src="model/images/interaction.gif" /></p>
  <p> Interactions</p></html:link></td>
<td> <i>D.&nbsp;melanogaster</i> </td>
<td rowspan="2"> High-throughput yeast 2-hybrid protein interaction datasets </td>
<td rowspan="2"> <a href="http://www.ebi.ac.uk/intact" target="_new" class="extlink">IntAct</a></td>
<td rowspan="2"> Kerrien et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17145710" target="_new" class="extlink">PubMed:17145710</a></td>
<td> &nbsp;</td>
</tr>

<tr>
<td> <i>C.&nbsp;elegans</i></td>
<td> &nbsp;</td>
</tr>

<tr>
<td> <i>D. melanogaster</i></td>
<td rowspan="2"> Interactions from the BioGRID</td>
<td rowspan="2"> <a href="http://www.thebiogrid.org" target="_new" class="extlink">BioGRID</a></td>
<td rowspan="2"> Stark et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16381927" target="_new" class="extlink">PubMed:16381927</a></td>
<td> &nbsp;</td>
</tr>

<tr>
<td> <i>C. elegans</i></td>
<td> &nbsp;</td>
</tr>

<!-- PHENOTYPES -->
<tr><td rowspan="3" class="leftcol">
<html:link action="/aspect?name=Functions">
 <p> <img src="model/images/phenotypes.png" /></p>
 <p> Phenotypes</p></html:link></td>
<td> <i>D. melanogaster</i> </td>
<td> Alleles and phenotypes</td>
<td> <a href="http://www.flybase.org" target="_new" class="extlink">FlyBase</a></td>
<td> Crosby et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099233" target="_new" class="extlink">PubMed: 17099233</a></td>
<td> &nbsp;</td>
</tr>

<td> <i>D. melanogaster</i> </td>
<td> High-throughput cell-based RNAi screens</td>
<td> <a href="http://genomernai.org" target="_new" class="extlink">GenomeRNAi</a> - Version 6.0; 12th Jan 2012</td>
<td> Gilsdorf et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/19910367" target="_new" class="extlink">PubMed: 19910367</a></td>
<td> &nbsp;</td>

<%--
<tr>
<td> <i>D. melanogaster</i> </td>
<td> High-throughput cell-based RNAi screens</td>
<td> <a href="http://flyrnai.org" target="_new" class="extlink">Drosophila RNAi Screening Center</a></td>
<td> Flockhart et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16381918" target="_new" class="extlink">PubMed: 16381918</a></td>
<td> &nbsp;</td>
</tr>
--%>

<tr>
<td> <i>C. elegans</i> </td>
<td> RNAi data from in vivo experiments</td>
<td> <a href="http://www.wormbase.org" target="_new" class="extlink">WormBase</a></td>
<td> Bieri et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17099234" target="_new" class="extlink">PubMed: 17099234</a></td>
<td> &nbsp;</td>
</tr>


<!-- PATHWAYS -->

<tr><td rowspan="2" class="leftcol">
<html:link action="/aspect?name=Functions">
 <p> <img src="model/images/pathways.png" /></p>
 <p> Pathways</p></html:link></td>

<!--
<td> <i>D. melanogaster</i>, <i>C. elegans</i></td>
<td> Curated pathway information and the genes involved in them</td>
<td> <a href="http://www.genome.jp/kegg/" target="_new" class="extlink">KEGG</a></td>
<td> Kanehisa et al - <a href="http://www.ncbi.nlm.nih.gov/pubmed/16381885" target="_new" class="extlink">PubMed: 16381885</a></td>
<td> &nbsp;</td>
</tr>
<tr>
-->

<td> <i>D. melanogaster</i>, <i>C. elegans</i> </td>
<td> Pathway information and the genes involved in them, inferred through orthologues from Human curated pathways</td>
<td> <a href="http://www.reactome.org/" target="_new" class="extlink">Reactome</a></td>
<td> &nbsp;</td>
<td>&nbsp;</td>
</tr>

<tr>
<td> <i>D. melanogaster</i></td>
<td> Curated pathway information and the genes involved in them</td>
<td> <a href="http://fly.reactome.org/" target="_new" class="extlink">FlyReactome</a></td>
<td> &nbsp;</td>
<td>&nbsp;</td>
</tr>


  <tr><td rowspan="2"  class="leftcol">

        <html:link action="/aspect?name=Functions">
         <p> <img src="model/images/geneOntology.png" /></p>
        <p> Gene Ontology </p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td rowspan="2"> GO annotations </td>
    <td rowspan="2"> <a href="http://www.geneontology.org" target="_new" class="extlink">Gene Ontology Site</a></td>
    <td rowspan="2"> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids= 10802651" target="_new" class="extlink">PubMed:10802651</a></td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i></td>
    <td> &nbsp;</td>
  </tr>


  <tr><td rowspan="2" class="leftcol">
       <html:link action="/aspect?name=Literature">
        <p> <img src="model/images/book.png" /></p>
        <p> Literature</p></html:link></td>
    <td> <i>D. melanogaster</i> </td>
    <td rowspan="2"> Gene versus publications</td>
    <td rowspan="2"> <a href="http://www.ncbi.nlm.nih.gov" target="_new" class="extlink">NCBI</a></td>
    <td rowspan="2"> &nbsp;</td>
    <td> &nbsp;</td>
  </tr>

  <tr>
    <td> <i>C. elegans</i></td>
    <td> &nbsp;</td>
  </tr>

</table>

<!--
<div class="body">
<ol>
	<li><a name="note1">${note1}</a></li>
	<li><a name="note2">${note2}</a></li>
	<li><a name="note3">${note3}</a></li>
</ol>
</div>
-->
</div>
<!-- /dataCategories -->
