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
            All proteins from the <A
            href="http://www.ebi.uniprot.org">UniProt
            Knowledgebase</A> (version 7.5) for the following organisms have
            been loaded:
          </p>
          <UL>
              <LI><I>D. melanogaster</I></LI>
              <li><i>M. musculus</i></li>
          </UL>
          <p>
            For each protein record in UniProt for each species the following
            information is extracted and loaded into UniMine:
          </p>  
          <UL>
              <LI>Entry name</LI>
              <LI>Primary accession number</LI>
              <LI>Secondary accession number</LI>
              <LI>Protein name</LI>
              <LI>Comments</LI>
              <LI>Publications</LI>
              <LI>Sequence</LI>
              <LI>Gene ORF name</LI>
              <li>Protein domain assignments from Interpro - see below</li>
          </UL>
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
            are loaded from <a href="http://www.ebi.ac.uk/interpro" target="_new">InterPro</a>
        </p>
      </ul>
    </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download <i>Drosophila Melanogaster</i> data
      </div>
      <div class="body">
	<ul>
  	  <li>        
	    <im:querylink text="<i>D. melanogaster</i> proteins and corresponding genes " skipBuilder="true">
              <query name="drosophilaProteinCorrespondingGenes" model="genomic" view="Protein.primaryIdentifier Protein.name Protein.primaryAccession Protein.genes.primaryIdentifier Protein.genes.ncbiGeneNumber" sortOrder="Protein.primaryIdentifier asc">
                <node path="Protein" type="Protein">
                </node>
                <node path="Protein.organism" type="Organism">
                </node>
                <node path="Protein.organism.taxonId" type="Integer">
                  <constraint op="=" value="7227" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="<i>D. melanogaster</i> protein domains " skipBuilder="true">
              <query name="drosophilaProteinDomains" model="genomic" view="Protein.proteinDomains.primaryIdentifier Protein.proteinDomains.name Protein.proteinDomains.shortName Protein.proteinDomains.type" sortOrder="Protein.proteinDomains.primaryIdentifier asc">
                <node path="Protein" type="Protein">
                </node>
                <node path="Protein.organism" type="Organism">
                </node>
                <node path="Protein.organism.taxonId" type="Integer">
                  <constraint op="=" value="7227" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="<i>D. melanogaster</i> proteins with corresponding protein domains " skipBuilder="true">
               <query name="drosophilaProteinsWithCorrespondingProteinDomains" model="genomic" view="Protein.name Protein.primaryIdentifier Protein.proteinDomains.primaryIdentifier Protein.proteinDomains.name Protein.proteinDomains.shortName Protein.proteinDomains.type" sortOrder="Protein.name asc">
                 <node path="Protein" type="Protein">
                 </node>
                 <node path="Protein.organism" type="Organism">
                 </node>
                 <node path="Protein.organism.taxonId" type="Integer">
                   <constraint op="=" value="7227" description="" identifier="" code="A">
                   </constraint>
                 </node>
               </query>
            </im:querylink>
           </li>
         </ul>
      </div>
    </td>
  </tr>
  <td valign="top">
    <div class="heading2">
        Bulk download <i>Mus Musculus</i> data
    </div>
    <div class="body">
      <ul>
        <li>
	  Demo... will be included in future
        </li>
      </ul>
    </div>
   </td>
  </tr>
</table>
