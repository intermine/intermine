<!-- pathways.jsp -->
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">


     <dt>Pathway data in FlyMine is available for D. melanogaster and
     comes from KEGG and from FlyReactome</dt>


        <dt>KEGG; The pathway data in the KEGG database have been manually
        entered from published materials. Current KEGG data in FlyMine only
        include Drosophila KEGG pathway names with their IDs and
        the genes involved.</dt>
 
      <dt>REACTOME: Data from FlyReactome includes the Reactome
      pathway name and identifier and the fly genes involved in each
      pathway.</dt>

       </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
         <ul>

          <li>
            <im:querylink text="All KEGG pathways with associated genes " skipBuilder="true">

            <query name="" model="genomic" view="Pathway.identifier Pathway.name Pathway.genes.primaryIdentifier Pathway.genes.identifier"></query>
            </im:querylink>

          </li>
         </ul>
      </div>
    </td>
  </tr>
</table>
<!-- /pathways.jsp -->
