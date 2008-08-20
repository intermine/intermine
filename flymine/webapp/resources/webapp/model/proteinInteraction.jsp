<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
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
          
          <dl>   
            <dt>Protein interactions have been loaded for <i>D. melanogaster</i>, <i>C. elegans</i> and <i>S. cerevisiae</i> from <a href="http://www.ebi.ac.uk/intact/" target="_new">IntAct</a>.</dt>

<br/> 

            <dt>
              Genetic and protein interaction data for <i>D. melanogaster</i>, <i>C. elegans</i> and <i>S. cerevisiae</i> have been loaded from the <a href="http://www.thebiogrid.org/" target="_new">BioGrid</a>. These data include both high-throughput studies and conventional focussed studies and have been curated from the literature. </dt>
          </dl>
        </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv3');">
            <img id='hiddenDiv2Toggle' src="images/disclosed.gif"/>
            <i> D. melanogaster </i> miRNA targets ...
          </a>
        </h4>


        <div id="hiddenDiv2" class="dataSetDescription">
          <dl>
            <dt>miRNA target predictions for <i>D. melanogaster</i> miRNAs from <a href="http://microrna.sanger.ac.uk/targets/v4/" target="_new">miRBase</a>. The miRanda algorithm was used to scan all available miRNA sequences for a given genome against 3' UTR sequences of that genome. Each predicted target has a score and a p-value. The algorithm and its results have been published: Enright et al - <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14709173" target="_new">PubMed:14709173</a>.
            </dt>
          </dl>
        </div>

    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
        <ul>

          <li>
            <im:querylink text="All <i>D. melanogaster</i> interactions from BioGRID " skipBuilder="true">
<query name="" model="genomic" view="Interaction.experiment.publication.pubMedId Interaction.gene.primaryIdentifier Interaction.interactingGenes.primaryIdentifier Interaction.type.name" sortOrder="Interaction.experiment.publication.pubMedId asc" constraintLogic="A and B">
  <node path="Interaction" type="Interaction">
  </node>
  <node path="Interaction.gene" type="Gene">
  </node>
  <node path="Interaction.gene.organism" type="Organism">
  </node>
  <node path="Interaction.gene.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster" description="" identifier="" code="A">
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

   <li>
            <im:querylink text="All <i>C. elegans</i> interactions from BioGRID " skipBuilder="true">
<query name="" model="genomic" view="Interaction.experiment.publication.pubMedId Interaction.gene.primaryIdentifier Interaction.interactingGenes.primaryIdentifier Interaction.type.name" sortOrder="Interaction.experiment.publication.pubMedId asc" constraintLogic="A and B">
  <node path="Interaction" type="Interaction">
  </node>
  <node path="Interaction.gene" type="Gene">
  </node>
  <node path="Interaction.gene.organism" type="Organism">
  </node>
  <node path="Interaction.gene.organism.shortName" type="String">
    <constraint op="=" value="C. elegans" description="" identifier="" code="A">
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


   <li>
            <im:querylink text="All <i>S. cerevisiae</i> interactions from BioGRID " skipBuilder="true">
<query name="" model="genomic" view="Interaction.experiment.publication.pubMedId Interaction.gene.primaryIdentifier Interaction.interactingGenes.primaryIdentifier Interaction.type.name" sortOrder="Interaction.experiment.publication.pubMedId asc" constraintLogic="A and B">
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


   <li>
            <im:querylink text="All <i>D. melanogaster</i> interactions from IntAct " skipBuilder="true">
<query name="" model="genomic" view="Interaction.experiment.publication.pubMedId Interaction.experiment.name Interaction.gene.primaryIdentifier Interaction.interactingGenes.primaryIdentifier Interaction.type.name" sortOrder="Interaction.experiment.publication.pubMedId asc" constraintLogic="A and B">
  <node path="Interaction" type="Interaction">
  </node>
  <node path="Interaction.gene" type="Gene">
  </node>
  <node path="Interaction.gene.organism" type="Organism">
  </node>
  <node path="Interaction.gene.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Interaction.dataSets" type="DataSet">
  </node>
  <node path="Interaction.dataSets.dataSource" type="DataSource">
  </node>
  <node path="Interaction.dataSets.dataSource.name" type="String">
    <constraint op="=" value="IntAct" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
   </li>


   <li>
            <im:querylink text="All <i>C. elegans</i> interactions from IntAct " skipBuilder="true">
<query name="" model="genomic" view="Interaction.experiment.publication.pubMedId Interaction.experiment.name Interaction.gene.primaryIdentifier Interaction.interactingGenes.primaryIdentifier Interaction.type.name" sortOrder="Interaction.experiment.publication.pubMedId asc" constraintLogic="A and B">
  <node path="Interaction" type="Interaction">
  </node>
  <node path="Interaction.gene" type="Gene">
  </node>
  <node path="Interaction.gene.organism" type="Organism">
  </node>
  <node path="Interaction.gene.organism.shortName" type="String">
    <constraint op="=" value="C. elegans" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Interaction.dataSets" type="DataSet">
  </node>
  <node path="Interaction.dataSets.dataSource" type="DataSource">
  </node>
  <node path="Interaction.dataSets.dataSource.name" type="String">
    <constraint op="=" value="IntAct" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
            </im:querylink>
   </li>


   <li>
            <im:querylink text="All <i>S. cerevisiae</i> interactions from BioGRID " skipBuilder="true">
<query name="" model="genomic" view="Interaction.experiment.publication.pubMedId Interaction.experiment.name Interaction.gene.primaryIdentifier Interaction.interactingGenes.primaryIdentifier Interaction.type.name" sortOrder="Interaction.experiment.publication.pubMedId asc" constraintLogic="A and B">
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
    <constraint op="=" value="IntAct" description="" identifier="" code="B">
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
