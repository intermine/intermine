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
            Protein interactions datasets ...
          </a>
        </h4>

        <div id="hiddenDiv1" class="dataSetDescription">
          
          <dl>   
            <dt>Protein interactions have been loaded for <i>Homo Sapiens</i> and <i>Mus Musculus</i> from <a href="http://www.ebi.ac.uk/intact/" target="_new">IntAct</a>.</dt>

           <ul>

            <li>
            <im:querylink text=" <i>Homo Sapiens</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Protein.proteinInteractions.experiment.publication.year Protein.proteinInteractions.experiment.publication.firstAuthor Protein.proteinInteractions.experiment.publication.journal Protein.proteinInteractions.experiment.publication.title Protein.proteinInteractions.experiment.publication.pubMedId" sortOrder="Protein.proteinInteractions.experiment.publication.year desc">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.shortName" type="String">
    <constraint op="=" value="H. sapiens" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Protein.proteinInteractions" type="ProteinInteraction">
  </node>
  <node path="Protein.proteinInteractions.experiment" type="ProteinInteractionExperiment">
  </node>
</query>
              </im:querylink>
            </li>
          
            <li>   
              <im:querylink text=" <i>Mus Musculus</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Protein.proteinInteractions.experiment.publication.year Protein.proteinInteractions.experiment.publication.firstAuthor Protein.proteinInteractions.experiment.publication.journal Protein.proteinInteractions.experiment.publication.title Protein.proteinInteractions.experiment.publication.pubMedId" sortOrder="Protein.proteinInteractions.experiment.publication.year desc">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.shortName" type="String">
    <constraint op="=" value="M. musculus" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Protein.proteinInteractions" type="ProteinInteraction">
  </node>
  <node path="Protein.proteinInteractions.experiment" type="ProteinInteractionExperiment">
  </node>
</query>
           
              </im:querylink>
            </li></br>
          </dl>
        </div>

    </td>
  </tr>
</table>
