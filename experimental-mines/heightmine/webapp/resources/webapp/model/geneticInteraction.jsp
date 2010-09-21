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

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv2');">
            <img id='hiddenDiv2Toggle' src="images/disclosed.gif"/>
            Genetic interactions datasets ...
          </a>
        </h4>


        <div id="hiddenDiv2" class="dataSetDescription">
          <dl>
            <dt>
              Genetic interaction data for <i>Homo Sapiens</i> and <i>Mus Musculus</i> <a href="http://www.thebiogrid.org/" target="_new">BioGrid</a>. These data include both high-throughput studies and conventional focussed studies and have been curated from the literature. </dt>

          <ul>

            <li>
            <im:querylink text=" <i>Homo Sapiens</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Gene.geneticInteractions.experiment.publication.year Gene.geneticInteractions.experiment.publication.firstAuthor Gene.geneticInteractions.experiment.publication.journal Gene.geneticInteractions.experiment.publication.title Gene.geneticInteractions.experiment.publication.pubMedId" sortOrder="Gene.geneticInteractions.experiment.publication.year desc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.shortName" type="String">
    <constraint op="=" value="H. sapiens" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
            </li>

            <li>
            <im:querylink text=" <i>Mus Musculus</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Gene.geneticInteractions.experiment.publication.year Gene.geneticInteractions.experiment.publication.firstAuthor Gene.geneticInteractions.experiment.publication.journal Gene.geneticInteractions.experiment.publication.title Gene.geneticInteractions.experiment.publication.pubMedId" sortOrder="Gene.geneticInteractions.experiment.publication.year desc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.shortName" type="String">
    <constraint op="=" value="M. musculus" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
            </li></br>

            </dt>
          </dl>
        </div>

    </td>
  </tr>
</table>
