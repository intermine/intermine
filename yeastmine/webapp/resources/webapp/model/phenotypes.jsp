<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- RNAi.jsp -->

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
     <i>S. cerevisiae</i>  - Phenotypes from SGD ...
   </a>
  </h4>

<div id="hiddenDiv1" class="dataSetDescription">
       <dl>

          <dt>Phenotypes. </dt>

       </dl>
  <br/>
</div>



   <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">

          <ul>           

            <li>
              <im:querylink text="All <i>S. cerevisiae</i> phenotypes " skipBuilder="true">

<query name="" model="genomic" view="Gene.primaryIdentifier Gene.phenotypeAnnotations.phenotype.observable Gene.phenotypeAnnotations.phenotype.experimentType Gene.phenotypeAnnotations.phenotype.mutantType" sortOrder="Gene.primaryIdentifier asc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.phenotypeAnnotations" type="PhenotypeAnnotation">
  </node>
  <node path="Gene.phenotypeAnnotations.phenotype" type="Phenotype">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.name" type="String">
    <constraint op="=" value="Saccharomyces cerevisiae" description="" identifier="" code="A">
    </constraint>
  </node>
</query>

              </im:querylink>
            </li>
          </ul>

        </div>
      </td>
</table>
<!-- /RNAi.jsp -->