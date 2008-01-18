<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<TABLE width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">

        <p>Orthologue and inparalogue relationships calculated by <A href="http://inparanoid.sbc.su.se/" target="_new">InParanoid</A> between the following organisms:</p>
        <ul>
          <li><I>D. melanogaster</I></li>
          <li><I>D. pseudoobscura</I></li>
          <li><I>A. gambiae</I></li>
          <li><I>A. mellifera</I></li>
          <li><I>C. elegans</I></li>
        </ul><br/>

        <p>
          In addition, orthologues/paralogues from these five species to several others:
        </p>
        <p>
          <i>C. familiaris , D. discoideum, D. rerio, G. gallus, H. sapiens, M. musculus, P. troglodytes, R. norvegicus, S. cerevisiae, S. pombe</i>
        </p>

          <p><im:querylink text="Show all pairs of organisms linked by orthologues" skipBuilder="true">
            <query name="" model="genomic" view="Orthologue.gene.organism.shortName Orthologue.orthologue.organism.shortName"><node path="Orthologue" type="Orthologue"></node></query>
          </im:querylink></p>

<br/>
  

  <h4>
   <a href="javascript:toggleDiv('hiddenDiv1');">
    <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>

      Glossary according to Sonnhammer and Koonin </a> (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=12446146" target="_new">PubMed: 12446146</a>) ...
 
  </h4>

<div id="hiddenDiv1" class="dataSetDescription">
      <ul>
       <li>Homologous genes: genes with common ancestry.</li>
       <li>Orthologous genes: genes in two species that have directly evolved from a single gene in the last common ancestor and are likely to be functionally related.</li>
       <li>Paralogous genes: homologous genes related by a duplication event. Might be in the same or in a different genome.</li>
       <li>Inparalogous genes: genes that derive from a duplication event after a speciation of interest. Inparalogs are together orthologs to the corresponding orthologous gene/genes in the other species.</li>
       <li>Outparalogous genes: genes that derive from a duplication event before a speciation event of interest, thus not orthologs according to definition.</li> 
      </ul>
     </div>
    </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
       Bulk download
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>A. gambiae</i> " skipBuilder="true">
             <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="A and B">
 				 <node path="Orthologue" type="Orthologue">
 				 </node>
 				 <node path="Orthologue.gene" type="Gene">
 				 </node>
 				 <node path="Orthologue.gene.organism" type="Organism">
 				 </node>
  				 <node path="Orthologue.gene.organism.name" type="String">
    				<constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    				</constraint>
  				 </node>
 				 <node path="Orthologue.orthologue" type="Gene">
 				 </node>
 				 <node path="Orthologue.orthologue.organism" type="Organism">
 				 </node>
 				 <node path="Orthologue.orthologue.organism.name" type="String">
   					<constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="B">
    				</constraint>
  				 </node>
			 </query>
            </im:querylink>
          </li> 

          <li>            
            <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>C. elegans</i> " skipBuilder="true">
              <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.gene" type="Gene">
  </node>
  <node path="Orthologue.gene.organism" type="Organism">
  </node>
  <node path="Orthologue.gene.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Orthologue.orthologue" type="Gene">
  </node>
  <node path="Orthologue.orthologue.organism" type="Organism">
  </node>
  <node path="Orthologue.orthologue.organism.name" type="String">
    <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

          <li>
           <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>H. sapiens</i> " skipBuilder="true">
            <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.gene" type="Gene">
  </node>
  <node path="Orthologue.gene.organism" type="Organism">
  </node>
  <node path="Orthologue.gene.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Orthologue.orthologue" type="Gene">
  </node>
  <node path="Orthologue.orthologue.organism" type="Organism">
  </node>
  <node path="Orthologue.orthologue.organism.name" type="String">
    <constraint op="=" value="Homo sapiens" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
           </li>

          <li>            
           <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>M. musculus</i> " skipBuilder="true">
<query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.gene" type="Gene">
  </node>
  <node path="Orthologue.gene.organism" type="Organism">
  </node>
  <node path="Orthologue.gene.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Orthologue.orthologue" type="Gene">
  </node>
  <node path="Orthologue.orthologue.organism" type="Organism">
  </node>
  <node path="Orthologue.orthologue.organism.name" type="String">
    <constraint op="=" value="Mus musculus" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
           </im:querylink>
          </li>

          <li>
          <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>D. pseudoobscura</i> " skipBuilder="true">
           <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.gene" type="Gene">
  </node>
  <node path="Orthologue.gene.organism" type="Organism">
  </node>
  <node path="Orthologue.gene.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Orthologue.orthologue" type="Gene">
  </node>
  <node path="Orthologue.orthologue.organism" type="Organism">
  </node>
  <node path="Orthologue.orthologue.organism.name" type="String">
    <constraint op="=" value="Drosophila pseudoobscura" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
          </im:querylink>
         </li>

          <li>
            <im:querylink text="Orthologues: <i>A. gambiae</i> vs <i>C. elegans</i> " skipBuilder="true">
              <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.gene" type="Gene">
  </node>
  <node path="Orthologue.gene.organism" type="Organism">
  </node>
  <node path="Orthologue.gene.organism.name" type="String">
    <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Orthologue.orthologue" type="Gene">
  </node>
  <node path="Orthologue.orthologue.organism" type="Organism">
  </node>
  <node path="Orthologue.orthologue.organism.name" type="String">
    <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

        </ul>
      </div>
    </td>
  </tr>
</TABLE>
 

