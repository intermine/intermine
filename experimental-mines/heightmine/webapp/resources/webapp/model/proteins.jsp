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
              <li><i>Homo Sapiens</i></li>
              <li><i>Mus Musculus</i></li>
          </ul>
          <p>           
            For each protein record in HeightProt for each species the following
            information is extracted and loaded into HeightMine:</p>
         <ul>             
              <li>Entry name</li>
              <li>Primary accession number</li>
              <li>Secondary accession number</li>
              <li>Protein name</li>
              <li>Comments</li>
              <li>Publications</li>
              <li>Sequence</li>
              <li>Gene ORF name</li>
              <li>Protein domain assignments from Interpro - see below</li>
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
            Protein family and domain assignments to proteins are
            loaded from Uniprot (see above).  Details for each family or domain
            are loaded from <a
            href="http://www.ebi.ac.uk/interpro" target="_new">InterPro</a></p>
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
        Bulk download <i>Homo Sapiens</i> data
      </div>
      <div class="body">
        <ul>
          <li>            
              <im:querylink text="<i>Homo Sapiens</i> proteins and corresponding genes " skipBuilder="true">
<query name="" model="genomic" view="Protein.name Protein.primaryAccession Protein.primaryIdentifier Protein.genes.primaryIdentifier Protein.genes.secondaryIdentifier Protein.genes.symbol" sortOrder="Protein.name Protein.primaryAccession Protein.primaryIdentifier Protein.genes.primaryIdentifier Protein.genes.secondaryIdentifier Protein.genes.symbol">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.name" type="String">
    <constraint op="=" value="Homo Sapiens" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
           </im:querylink>
          </li>

          <li>
             <im:querylink text="<i>Homo Sapiens</i> protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type">
  <node path="ProteinDomain" type="ProteinDomain">
  </node>
  <node path="ProteinDomain.proteins" type="Protein">
  </node>
  <node path="ProteinDomain.proteins.organism" type="Organism">
  </node>
  <node path="ProteinDomain.proteins.organism.shortName" type="String">
    <constraint op="=" value="H. sapiens" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
             </im:querylink>
          </li>


          <li>              
              <im:querylink text="<i>Homo Sapiens</i> proteins with corresponding protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.proteins.primaryAccession ProteinDomain.proteins.primaryIdentifier ProteinDomain.proteins.name ProteinDomain.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.proteins.name asc">
  <node path="ProteinDomain" type="ProteinDomain">
  </node>
  <node path="ProteinDomain.proteins" type="Protein">
  </node>
  <node path="ProteinDomain.proteins.organism" type="Organism">
  </node>
  <node path="ProteinDomain.proteins.organism.shortName" type="String">
    <constraint op="=" value="H. sapiens" description="" identifier="" code="A">
    </constraint>
  </node>
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
        Bulk download <i>Mus Musculus</i> data
      </div>
      <div class="body">
        <ul>
          <li>
              <im:querylink text="<i>Mus Musculus</i> proteins and corresponding genes " skipBuilder="true">
<query name="" model="genomic" view="Protein.primaryAccession Protein.primaryIdentifier Protein.genes.primaryIdentifier" sortOrder="Protein.primaryAccession asc">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.name" type="String">
    <constraint op="=" value="Mus Musculus" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>Mus Musculus</i> protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type">
  <node path="ProteinDomain" type="ProteinDomain">
  </node>
  <node path="ProteinDomain.proteins" type="Protein">
  </node>
  <node path="ProteinDomain.proteins.organism" type="Organism">
  </node>
  <node path="ProteinDomain.proteins.organism.shortName" type="String">
    <constraint op="=" value="M. musculus" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>Mus Musculus</i> proteins with corresponding protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.proteins.primaryAccession ProteinDomain.proteins.primaryIdentifier ProteinDomain.proteins.name ProteinDomain.primaryIdentifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.proteins.primaryAccession asc">
  <node path="ProteinDomain" type="ProteinDomain">
  </node>
  <node path="ProteinDomain.proteins" type="Protein">
  </node>
  <node path="ProteinDomain.proteins.organism" type="Organism">
  </node>
  <node path="ProteinDomain.proteins.organism.shortName" type="String">
    <constraint op="=" value="M. musculus" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
      </im:querylink>
          </li>

        </ul>
      </div>
    </td>
  </tr>
</table>
