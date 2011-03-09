<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
  
    <td valign="top" rowspan="2">
      <div class="heading2">
        Current data
      </div>
      <div class="body">

 <h4>  
  <a href="javascript:toggleDiv('hiddenDiv1');">
    <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
      Data from the UniProt Knowledgebase (UniProtKB) ...
  </a>
 </h4>

<div id="hiddenDiv1" class="dataSetDescription">
          <p>
            All proteins from the <a
            href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt
            Knowledgebase</a> for the following organisms have
            been loaded:</p>

          <ul>             
              <li><i>D. melanogaster</i></li>
              <li><i>C. elegans</i></li>
          </ul>
          <p>           
            For each protein record in UniProt for each species the following
            information is extracted and loaded into FlyMine:</p>
         <ul>             
              <li>Entry name</li>
              <li>Primary accession number</li>
              <li>Secondary accession number</li>
              <li>Protein name</li>
              <li>Comments</li>
              <li>Publications</li>
              <li>Sequence</li>
              <li>Gene ORF name</li>
          </ul>     

  </div>
     
   <h4>  
    <a href="javascript:toggleDiv('hiddenDiv2');">
     <img id='hiddenDiv2Toggle' src="images/disclosed.gif"/>
      Data from InterPro ...
    </a>
   </h4>

<div id="hiddenDiv2" class="dataSetDescription">

          <p>
            Protein family and domain assignments to proteins in UniProt have
            been loaded from <a
            href="http://www.ebi.ac.uk/interpro" target="_new">InterPro</a> for the following organisms:</p>
          <ul> 
              <li><i>D. melanogaster</i></li>
              <li><i>C. elegans</i></li>
          </ul> 
</div>

<%--  // add later:
        <p>
          Search for a protein identifier: <tiles:insert name="browse.tile"/>
        </p>
--%>
      </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download <i>Drosophila</i> data
      </div>
      <div class="body">

      <ul>
      <li>
          <im:querylink text="<i>D. melanogaster</i> proteins and corresponding genes " skipBuilder="true">
<query name="" model="genomic" view="Protein.name Protein.primaryAccession Protein.primaryIdentifier Protein.genes.secondaryIdentifier Protein.genes.symbol" sortOrder="Protein.name asc">
<constraint path="Protein.genes.organism.name" op="=" value="Drosophila melanogaster"/>
</query>
       </im:querylink>
      </li>

      <li>
         <im:querylink text="<i>D. melanogaster</i> protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.primaryIdentifier asc">
<constraint path="ProteinDomain.proteins.organism.name" op="=" value="Drosophila melanogaster"/>
</query>
         </im:querylink>
      </li>


      <li>
          <im:querylink text="<i>D. melanogaster</i> proteins with corresponding protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.proteins.primaryAccession ProteinDomain.proteins.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.proteins.primaryAccession asc ProteinDomain.proteins.primaryAccession asc">
<constraint path="ProteinDomain.proteins.organism.name" op="=" value="Drosophila melanogaster"/>
</query>
  </im:querylink>
  </li>
    </ul>
      </div>
    </td>
  </tr>


  <tr>
    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download <i>C. elegans</i> data
      </div>
      <div class="body">
      <ul>
      <li>
      <im:querylink text="<i>C. elegans</i> proteins and corresponding genes " skipBuilder="true">
<query name="" model="genomic" view="Protein.name Protein.primaryAccession Protein.primaryIdentifier Protein.genes.secondaryIdentifier Protein.genes.symbol" sortOrder="Protein.name asc">
<constraint path="Protein.genes.organism.shortName" op="=" value="C. elegans"/>
</query>
   </im:querylink>
  </li>

  <li>
     <im:querylink text="<i>C. elegans</i> protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.primaryIdentifier asc">
<constraint path="ProteinDomain.proteins.organism.shortName" op="=" value="C. elegans"/>
</query>
     </im:querylink>
  </li>


  <li>
      <im:querylink text="<i>C. elegans</i> proteins with corresponding protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.proteins.primaryAccession ProteinDomain.proteins.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.proteins.primaryAccession asc ProteinDomain.proteins.primaryAccession asc">
<constraint path="ProteinDomain.proteins.organism.shortName" op="=" value="C. elegans"/>
</query>
</im:querylink>
</li>

      
      </ul>
      </div>
    </td>
  </tr>

  </table>
