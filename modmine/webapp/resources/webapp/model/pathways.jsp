<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">

      <dt>Pathway data in modMine is available for
      <i>D. melanogaster</i> and <i>C. elegans</i> and comes from <a
      href="http://www.reactome.org" target="_new">Reactome</a>.  In
      addition some curated <i>D. melanogaster</i> pathways come from
      <a href="http://www.fly.reactome.org"
      target="_new">FlyReactome</a></dt>
<p>
       <dt>REACTOME: Data from <a href="http://www.reactome.org"
         target="_new">Reactome</a> includes the Reactome pathway name
         and identifier and the genes involved in each pathway.  The
         Reactome data are not curated - they are orthology mapped from
         the human curated pathways</dt>
<p>
       <dt>FlyReactome: A number of pathways are now loaded from the <a
       href="http://www.fly.reactome.org" target="_new">FlyReactome</a>
       project. Unlike the fly data from Reactome, which are inferred
       from orthologues, the data from FlyReactome have been manually
       curated.

       </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
         <ul>

         <!--
          <li>
            <im:querylink text="All KEGG pathways with associated genes " skipBuilder="true">

            <query name="" model="genomic" view="Pathway.identifier Pathway.name Pathway.genes.identifier Pathway.genes.symbol"></query>
            </im:querylink>

          </li>
          -->
          
         </ul>
      </div>
    </td>
  </tr>
</table>
