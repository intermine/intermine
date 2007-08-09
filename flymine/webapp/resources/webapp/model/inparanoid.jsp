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
        <p>
          Orthologue and paralogue relationships calculated by <A
          href="http://inparanoid.sbc.su.se/">InParanoid</A> - Version 5.1 - between the following organisms:
        </p>
        <ul>
          <li><I>D. melanogaster</I></li>
          <li><I>D. pseudoobscura</I></li>
          <li><I>A. gambiae</I></li>
          <li><I>A. mellifera</I></li>
          <li><I>C. elegans</I></li>
        </ul>
        <p>
          In addition, orthologues from these five species to several others:
        </p>
        <p>
          <i>C. familiaris , D. discoideum, D. rerio, G. gallus, H. sapiens, M. musculus, P. troglodytes, R. norvegicus, S. cerevisiae, S. pombe</I>
        <p>
          <im:querylink text="Show all pairs of organisms linked by orthologues" skipBuilder="true">
            <query name="" model="genomic" view="Orthologue.object.organism.shortName Orthologue.subject.organism.shortName"><node path="Orthologue" type="Orthologue"></node></query>
          </im:querylink>
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
             <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="C and A">
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
              <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="C and A">
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
            <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="C and A">
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
    <constraint op="=" value="Homo sapiens" description="" identifier="" code="C">
    </constraint>
  </node>
</query>
            </im:querylink>
           </li>

          <li>            
           <im:querylink text="Orthologues: <i>D. melanogaster</i> vs <i>M. musculus</i> " skipBuilder="true">
<query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="C and A">
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
           <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="C and A">
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
              <query name="" model="genomic" view="Orthologue.gene.identifier Orthologue.gene.organismDbId Orthologue.gene.symbol Orthologue.orthologue.identifier Orthologue.orthologue.organismDbId Orthologue.orthologue.symbol" constraintLogic="C and A">
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
 

