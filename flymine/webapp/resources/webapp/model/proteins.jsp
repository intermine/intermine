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
              <li><i>A. gambiae</i></li>
              <li><i>C. elegans</i></li>
              <li><i>H. sapiens</i></li>
              <li><i>M. musculus</i></li>
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
              <li><i>A. gambiae</i></li>
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
                <query name="" model="genomic" view="Protein.identifier Protein.name Protein.primaryAccession Protein.genes.identifier Protein.genes.chromosomeLocation.start Protein.genes.chromosomeLocation.end">
                  <node path="Protein" type="Protein">
                  </node>
                  <node path="Protein.organism" type="Organism">
                  </node>
                  <node path="Protein.organism.name" type="String">
                    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
                    </constraint>
                  </node>
                </query>
              </im:querylink>
          </li>

          <li>
             <im:querylink text="<i>D. melanogaster</i> protein domains " skipBuilder="true">
              <query name="" model="genomic" view="ProteinDomain.identifier ProteinDomain.interproId ProteinDomain.name ProteinDomain.shortName">
                <node path="ProteinDomain" type="ProteinDomain">
                </node>
                <node path="ProteinDomain.proteins" type="Protein">
                </node>
                <node path="ProteinDomain.proteins.organism" type="Organism">
                </node>
                <node path="ProteinDomain.proteins.organism.name" type="String">
                </node>
                <node path="ProteinDomain.proteins.organism.genus" type="String">
                  <constraint op="=" value="Drosophila" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>


          <li>              
              <im:querylink text="<i>D. melanogaster</i> proteins with corresponding protein domains " skipBuilder="true">
               <query name="" model="genomic" view="ProteinDomain.proteins.name ProteinDomain.proteins.identifier ProteinDomain.identifier ProteinDomain.interproId ProteinDomain.name ProteinDomain.shortName">
                <node path="ProteinDomain" type="ProteinDomain">
                 </node>
                <node path="ProteinDomain.proteins" type="Protein">
                 </node>
                <node path="ProteinDomain.proteins.organism" type="Organism">
                 </node>
                <node path="ProteinDomain.proteins.organism.name" type="String">
                 </node>
                <node path="ProteinDomain.proteins.organism.genus" type="String">
                  <constraint op="=" value="Drosophila" description="" identifier="" code="A">
                  </constraint>
                 </node>
                </query>
              </im:querylink>
          </li>

          <li>
               <im:querylink text="<i>D. melanogaster</i> protein families " skipBuilder="true">
                <query name="" model="genomic" view="ProteinFamily.identifier ProteinFamily.interproId ProteinFamily.name ProteinFamily.shortName">
                  <node path="ProteinFamily" type="ProteinFamily">
                  </node>
                  <node path="ProteinFamily.proteins" type="Protein">
                  </node>
                  <node path="ProteinFamily.proteins.organism" type="Organism">
                  </node>
                  <node path="ProteinFamily.proteins.organism.name" type="String">
                  </node>
                  <node path="ProteinFamily.proteins.organism.genus" type="String">
                    <constraint op="=" value="Drosophila" description="" identifier="" code="A">
                    </constraint>
                  </node>
                </query>
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>D. melanogaster</i> proteins with corresponding protein families " skipBuilder="true">
               <query name="" model="genomic" view="ProteinFamily.proteins.name ProteinFamily.proteins.identifier ProteinFamily.identifier ProteinFamily.interproId ProteinFamily.name ProteinFamily.shortName">
                <node path="ProteinFamily" type="ProteinFamily">
                 </node>
                <node path="ProteinFamily.proteins" type="Protein">
                 </node>
                <node path="ProteinFamily.proteins.organism" type="Organism">
                 </node>
                <node path="ProteinFamily.proteins.organism.name" type="String">
                 </node>
                <node path="ProteinFamily.proteins.organism.genus" type="String">
                  <constraint op="=" value="Drosophila" description="" identifier="" code="A">
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
        Bulk download <i>Anopheles</i> data
      </div>
      <div class="body">
        <ul>
          <li>
              <im:querylink text="<i>A. gambiae</i> proteins and corresponding genes " skipBuilder="true">
<query name="" model="genomic" view="Protein.identifier Protein.name Protein.primaryAccession Protein.genes.identifier"> <node path="Protein" type="Protein"> </node> <node path="Protein.organism" type="Organism"> </node> <node path="Protein.organism.name" type="String">   <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="A">   </constraint> </node></query> 
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>A. gambiae</i> protein domains " skipBuilder="true">
                <query name="" model="genomic" view="ProteinDomain.identifier ProteinDomain.interproId ProteinDomain.name ProteinDomain.shortName">
                  <node path="ProteinDomain" type="ProteinDomain">
                  </node>
                  <node path="ProteinDomain.proteins" type="Protein">
                  </node>
                  <node path="ProteinDomain.proteins.organism" type="Organism">
                  </node>
                  <node path="ProteinDomain.proteins.organism.name" type="String">
                  </node>
                  <node path="ProteinDomain.proteins.organism.genus" type="String">
                    <constraint op="=" value="Anopheles" description="" identifier="" code="A">
                    </constraint>
                  </node>
                </query>
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>A. gambiae</i> proteins with corresponding protein domains " skipBuilder="true">
               <query name="" model="genomic" view="ProteinDomain.proteins.name ProteinDomain.proteins.identifier ProteinDomain.identifier ProteinDomain.interproId ProteinDomain.name ProteinDomain.shortName">
                <node path="ProteinDomain" type="ProteinDomain">
                 </node>
                <node path="ProteinDomain.proteins" type="Protein">
                 </node>
                <node path="ProteinDomain.proteins.organism" type="Organism">
                 </node>
                <node path="ProteinDomain.proteins.organism.name" type="String">
                 </node>
                <node path="ProteinDomain.proteins.organism.genus" type="String">
                  <constraint op="=" value="Anopheles" description="" identifier="" code="A">
                   </constraint>
                 </node>
                </query>
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>A. gambiae</i> protein families " skipBuilder="true">
                <query name="" model="genomic" view="ProteinFamily.identifier ProteinFamily.interproId ProteinFamily.name ProteinFamily.shortName">
                  <node path="ProteinFamily" type="ProteinFamily">
                  </node>
                  <node path="ProteinFamily.proteins" type="Protein">
                  </node>
                  <node path="ProteinFamily.proteins.organism" type="Organism">
                  </node>
                  <node path="ProteinFamily.proteins.organism.name" type="String">
                  </node>
                  <node path="ProteinFamily.proteins.organism.genus" type="String">
                    <constraint op="=" value="Anopheles" description="" identifier="" code="A">
                    </constraint>
                  </node>
                </query>
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>A. gambiae</i> proteins with corresponding protein families " skipBuilder="true">
               <query name="" model="genomic" view="ProteinFamily.proteins.name ProteinFamily.proteins.identifier ProteinFamily.identifier ProteinFamily.interproId ProteinFamily.name ProteinFamily.shortName">
                <node path="ProteinFamily" type="ProteinFamily">
                 </node>
                <node path="ProteinFamily.proteins" type="Protein">
                 </node>
                <node path="ProteinFamily.proteins.organism" type="Organism">
                 </node>
                <node path="ProteinFamily.proteins.organism.name" type="String">
                 </node>
                <node path="ProteinFamily.proteins.organism.genus" type="String">
                  <constraint op="=" value="Anopheles" description="" identifier="" code="A">
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
