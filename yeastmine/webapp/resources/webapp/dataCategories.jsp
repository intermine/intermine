<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- dataCategories -->

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
  </tr>
  <tr>
    <td class="leftcol"><html:link action="/aspect?name=Genomics"> <p><img src="model/images/genomics.gif" /></p><p> Genomics </p></html:link></td>
    <td><i>Saccharomyces cerevisiae</i></td>
    <td>Genome annotation for <i>Saccharomyces cerevisiae</i>.  Data loaded includes:
      <ul>
        <li>PrimaryIdentifier
        <li>SecondaryIdentifier
        <li>Symbol
        <li>Name
        <li>Length
        <li>Description
      </ul>
    </td>
    <td><a href="http://www.yeastgenome.org" target="_new">SGD</a> - 20 Mar, 2009</td>
    <td>Saccharomyces Genome Database - <a href="http://www.ncbi.nlm.nih.gov/pubmed/9169866" target="_new">PubMed: 9169866</a></td>
  </tr>


  <tr><td class="leftcol">
        <html:link action="/aspect?name=Proteins">
        <p> <img src="model/images/proteins.png" /></p>
        <p> Proteins </p></html:link></td>
    <td> <i>Saccharomyces cerevisiae</i> </td>
    <td> Protein annotation</td>
    <td> <a href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt</a> - Release 15.8</td>
    <td> UniProt Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/17142230" target="_new">PubMed: 17142230</a></td>
  </tr>

  <tr><td class="leftcol">

        <html:link action="/aspect?name=Gene%20Ontology">
         <p> <img src="model/images/geneOntology.png" /></p>
        <p> Gene Ontology </p></html:link></td>
    <td> <i>Saccharomyces cerevisiae</i> </td>
    <td> GO annotations </td>
    <td> <a href="http://www.geneontology.org" target="_new">Gene Ontology Site</a> - 11 Mar 2009</td>
    <td> Gene Ontology Consortium - <a href="http://www.ncbi.nlm.nih.gov/pubmed/10802651" target="_new">PubMed:10802651</a></td>
  </tr>

</table>


</div>
<!-- /dataCategories -->
