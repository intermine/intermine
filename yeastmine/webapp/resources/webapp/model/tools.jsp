
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>


<html:xhtml/>

<div class="body">

<!-- tools -->


<h2>Advanced Search</h2>

<p>Below are tools to help you find your data quickly.  You can export your results for any query by 
clicking on the export button in the results toolbar.</p>

<h3>Chromosome</h3>

	<ul>
		<li><a href="http://www.flymine.org/sgd/template.do?name=Chromosome_Gene">Chromosome --> Genes</a></li>
	</ul>

<h3>Chromosome Location</h3>

	<ul>		
 		<li><a href="http://www.flymine.org/sgd/template.do?name=ChromosomeLocation_GO">Chromosomal Location --> GO Terms</a></li>
 		<li><a href="http://www.flymine.org/sgd/template.do?name=ChromosomeLocation_Phenotypes">Chromosomal Location --> Phenotypes</a></li>
 		<li><a href="http://www.flymine.org/sgd/template.do?name=ChromosomeLocation_Interaction">Chromosomal Location --> Interactions</a></li>
 		<li><a href="http://www.flymine.org/sgd/template.do?name=ChromosomeLocation_Feature">Chromosomal Location --> Genes</a></li>
	</ul>

<h3>Gene identifiers</h3>

<ol>
<li>use the quick search in the top menu bar to go to the gene report page
<li>click on the 'List' tab in the top menu bar to upload your list of identifiers
</ol>

<br>

The report page will display the results of several relevant queries, including:

	<ul>
		<li><a href="http://www.flymine.org/sgd/template.do?name=Gene_GO">Gene --> GO Terms</a></li>
 		<li><a href="http://www.flymine.org/sgd/template.do?name=Gene_Phenotypes">Gene --> Phenotypes</a></li>
 		<li><a href="http://www.flymine.org/sgd/template.do?name=Gene_Interaction">Gene --> Interactions</a></li>
 		<li><a href="http://www.flymine.org/sgd/template.do?name=Gene_To_Publications">Gene --> Publications</a></li>
	</ul>


	
<br><br>

<h2>Feature Search</h2>

This search allows you to retrieve chromosomal features that match the selected criteria.

<br><br>

<ul>
	<li><a href="http://www.flymine.org/sgd/template.do?name=FeatureType_Gene">Feature Type --> Genes</a>
 	<li><a href="http://www.flymine.org/sgd/template.do?name=GOTerm_Genes">GO Term --> Genes</a>
	<li><a href="http://www.flymine.org/sgd/template.do?name=Phenotype_Genes">Phenotype --> Genes</a>
</ul>

<br><br>

To view all template queries, click on the 'Templates' tab in the top menu bar.

            
<!-- /tools.jsp -->

</div>