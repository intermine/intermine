<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <h4>
          <a href="javascript:toggleDiv('hiddenDiv1');">
            <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
            Interactions datasets ...
          </a>
        </h4>

        <div id="hiddenDiv1" class="dataSetDescription">

          <ul><li><dt>Genetic and protein interaction data for <i>S. cerevisiae</i> have been loaded from the <a href="http://www.thebiogrid.org/" target="_new">BioGRID</a>. These data include both high-throughput studies and conventional focused studies and have been curated from the literature.</dt></li></ul>

        </div>

    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
        <ul>


   <li>
            <im:querylink text="All <i>S. cerevisiae</i> interactions from BioGRID " skipBuilder="true">
<query name="" model="genomic" view="Interaction.experiment.publication.pubMedId Interaction.gene.primaryIdentifier Interaction.role Interaction.interactingGenes.primaryIdentifier Interaction.type.name" sortOrder="Interaction.experiment.publication.pubMedId asc" constraintLogic="A and B">
  <node path="Interaction" type="Interaction">
  </node>
  <node path="Interaction.gene" type="Gene">
  </node>
  <node path="Interaction.gene.organism" type="Organism">
  </node>
  <node path="Interaction.gene.organism.shortName" type="String">
    <constraint op="=" value="S. cerevisiae" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Interaction.dataSets" type="DataSet">
  </node>
  <node path="Interaction.dataSets.dataSource" type="DataSource">
  </node>
  <node path="Interaction.dataSets.dataSource.name" type="String">
    <constraint op="=" value="BioGRID" description="" identifier="" code="B">
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
