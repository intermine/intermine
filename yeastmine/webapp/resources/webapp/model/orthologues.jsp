<!-- inparanoid.jsp -->
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<TABLE width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">

 <h4>
  <a href="javascript:toggleDiv('hiddenDiv1');">
    <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
      Homology data from inparanoid ...
  </a>
 </h4>

<div id="hiddenDiv1" class="dataSetDescription">

7227 6239 7165 4932 9606 10090 10116 7955
        <p>Orthologue relationships calculated by <A href="http://www.treefam.org/" target="_new">TreeFam</A> between the following organisms:</p>
        <ul>
          <li><I>S. cerevisiae</I></li>
          <li><I>D. rerio</I></li>
          <li><I>D. melanogaster</I></li>
          <li><I>M. musculus</I></li>
          <li><I>A. gambiae</I></li>
          <li><I>H. Sapiens</I></li>
          <li><I>R. norvegicus</I></li>
          <li><I>C. elegans</I></li>
        </ul><br/>



          <p><im:querylink text="Show all pairs of organisms linked by orthologues" skipBuilder="true">
            <query name="" model="genomic" view="Homologue.gene.organism.shortName Homologue.homologue.organism.shortName"><node path="Homologue" type="Homologue"></node></query>
          </im:querylink></p>


</div>

    <td width="40%" valign="top">
      <div class="heading2">
       Bulk download
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="Homologues: <i>S. cerevisiae</i> vs <i>D. Rerio</i> " skipBuilder="true">
<query name="" model="genomic" view="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.secondaryIdentifier Homologue.type " sortOrder="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.type" constraintLogic="A and B">
  <node path="Homologue" type="Homologue">
  </node>
  <node path="Homologue.gene" type="Gene">
  </node>
  <node path="Homologue.gene.organism" type="Organism">
  </node>
  <node path="Homologue.gene.organism.name" type="String">
    <constraint op="=" value="Saccharomyces cerevisiae" description="Show the predicted orthologues between:" identifier="" editable="true" code="A">
    </constraint>
  </node>
  <node path="Homologue.homologue" type="Gene">
  </node>
  <node path="Homologue.homologue.organism" type="Organism">
  </node>
  <node path="Homologue.homologue.organism.name" type="String">
    <constraint op="=" value="Danio rerio" description="" identifier="" editable="true" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="Homologues: <i>S. cerevisiae</i> vs <i>C. elegans</i> " skipBuilder="true">
<query name="" model="genomic" view="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.homologue.symbol Homologue.type " sortOrder="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.homologue.symbol Homologue.type " constraintLogic="A and B">
  <node path="Homologue" type="Homologue">
  </node>
  <node path="Homologue.gene" type="Gene">
  </node>
  <node path="Homologue.gene.organism" type="Organism">
  </node>
  <node path="Homologue.gene.organism.name" type="String">
    <constraint op="=" value="Saccharomyces cerevisiae" description="Show the predicted orthologues between:" identifier="" editable="true" code="A">
    </constraint>
  </node>
  <node path="Homologue.homologue" type="Gene">
  </node>
  <node path="Homologue.homologue.organism" type="Organism">
  </node>
  <node path="Homologue.homologue.organism.name" type="String">
    <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" editable="true" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

          <li>
           <im:querylink text="Homologues: <i>S. cerevisiae</i> vs <i>H. sapiens</i> " skipBuilder="true">
<query name="" model="genomic" view="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.type " sortOrder="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.type " constraintLogic="A and B">
  <node path="Homologue" type="Homologue">
  </node>
  <node path="Homologue.gene" type="Gene">
  </node>
  <node path="Homologue.gene.organism" type="Organism">
  </node>
  <node path="Homologue.gene.organism.name" type="String">
    <constraint op="=" value="Saccharomyces cerevisiae" description="Show the predicted orthologues between:" identifier="" editable="true" code="A">
    </constraint>
  </node>
  <node path="Homologue.homologue" type="Gene">
  </node>
  <node path="Homologue.homologue.organism" type="Organism">
  </node>
  <node path="Homologue.homologue.organism.name" type="String">
    <constraint op="=" value="Homo sapiens" description="" identifier="" editable="true" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
           </li>

          <li>
           <im:querylink text="Homologues: <i>S. cerevisiae</i> vs <i>M. musculus</i> " skipBuilder="true">
<query name="" model="genomic" view="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.secondaryIdentifier Homologue.type" sortOrder="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.symbol Homologue.type" constraintLogic="A and B">
  <node path="Homologue" type="Homologue">
  </node>
  <node path="Homologue.gene" type="Gene">
  </node>
  <node path="Homologue.gene.organism" type="Organism">
  </node>
  <node path="Homologue.gene.organism.name" type="String">
    <constraint op="=" value="Saccharomyces cerevisiae" description="Show the predicted orthologues between:" identifier="" editable="true" code="A">
    </constraint>
  </node>
  <node path="Homologue.homologue" type="Gene">
  </node>
  <node path="Homologue.homologue.organism" type="Organism">
  </node>
  <node path="Homologue.homologue.organism.name" type="String">
    <constraint op="=" value="Mus musculus" description="" identifier="" editable="true" code="B">
    </constraint>
  </node>
</query>
           </im:querylink>
          </li>


          <li>
          <im:querylink text="Homologues: <i>S. cerevisiae</i> vs <i>R. norvegicus</i> " skipBuilder="true">
<query name="" model="genomic" view="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.homologue.symbol Homologue.type" sortOrder="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.homologue.symbol Homologue.type" constraintLogic="A and B">
  <node path="Homologue" type="Homologue">
  </node>
  <node path="Homologue.gene" type="Gene">
  </node>
  <node path="Homologue.gene.organism" type="Organism">
  </node>
  <node path="Homologue.gene.organism.name" type="String">
    <constraint op="=" value="Saccharomyces cerevisiae" description="Show the predicted orthologues between:" identifier="" editable="true" code="A">
    </constraint>
  </node>
  <node path="Homologue.homologue" type="Gene">
  </node>
  <node path="Homologue.homologue.organism" type="Organism">
  </node>
  <node path="Homologue.homologue.organism.name" type="String">
    <constraint op="=" value="Rattus norvegicus" description="" identifier="" editable="true" code="B">
    </constraint>
  </node>
</query>
          </im:querylink>
         </li>

          <li>
            <im:querylink text="Homologues: <i>S. cerevisiae </i> vs <i>D. melanogaster</i> " skipBuilder="true">
<query name="" model="genomic" view="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.homologue.primaryIdentifier Homologue.type " sortOrder="Homologue.gene.primaryIdentifier Homologue.gene.secondaryIdentifier Homologue.homologue.primaryIdentifier Homologue.homologue.secondaryIdentifier Homologue.homologue.symbol Homologue.type" constraintLogic="A and B">
  <node path="Homologue" type="Homologue">
  </node>
  <node path="Homologue.gene" type="Gene">
  </node>
  <node path="Homologue.gene.organism" type="Organism">
  </node>
  <node path="Homologue.gene.organism.name" type="String">
    <constraint op="=" value="Saccharomyces cerevisiae" description="Show the predicted orthologues between:" identifier="" editable="true" code="A">
    </constraint>
  </node>
  <node path="Homologue.homologue" type="Gene">
  </node>
  <node path="Homologue.homologue.organism" type="Organism">
  </node>
  <node path="Homologue.homologue.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" editable="true" code="B">
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

<!-- /inparanoid.jsp -->
