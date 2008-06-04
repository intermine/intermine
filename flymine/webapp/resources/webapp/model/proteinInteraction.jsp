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
            <dt>Protein interactions have been loaded for <i>D. melanogaster</i>, <i>C. elegans</i> and <i>S. cerevisiae</i> from <a href="http://www.ebi.ac.uk/intact/" target="_new">IntAct</a>.</dt>

           <ul>

            <li>
            <im:querylink text=" <i>D. melanogaster</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Protein.proteinInteractions.experiment.publication.year Protein.proteinInteractions.experiment.publication.firstAuthor Protein.proteinInteractions.experiment.publication.journal Protein.proteinInteractions.experiment.publication.title Protein.proteinInteractions.experiment.publication.pubMedId" sortOrder="Protein.proteinInteractions.experiment.publication.year desc">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster" description="" identifier="" code="A">
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
              <im:querylink text=" <i>C. elegans</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Protein.proteinInteractions.experiment.publication.year Protein.proteinInteractions.experiment.publication.firstAuthor Protein.proteinInteractions.experiment.publication.journal Protein.proteinInteractions.experiment.publication.title Protein.proteinInteractions.experiment.publication.pubMedId" sortOrder="Protein.proteinInteractions.experiment.publication.year desc">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.shortName" type="String">
    <constraint op="=" value="C. elegans" description="" identifier="" code="A">
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
              <im:querylink text=" <i>S. cerevisiae</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Protein.proteinInteractions.experiment.publication.year Protein.proteinInteractions.experiment.publication.firstAuthor Protein.proteinInteractions.experiment.publication.journal Protein.proteinInteractions.experiment.publication.title Protein.proteinInteractions.experiment.publication.pubMedId" sortOrder="Protein.proteinInteractions.experiment.publication.year desc">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.shortName" type="String">
    <constraint op="=" value="S. cerevisiae" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Protein.proteinInteractions" type="ProteinInteraction">
  </node>
  <node path="Protein.proteinInteractions.experiment" type="ProteinInteractionExperiment">
  </node>
</query>
              </im:querylink>
            </li><br/> 

          </dl>
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
              Genetic interaction data for <i>D. melanogaster</i>, <i>C. elegans</i> and <i>S. cerevisiae</i> have been loaded from the <a href="http://www.thebiogrid.org/" target="_new">BioGrid</a>. These data include both high-throughput studies and conventional focussed studies and have been curated from the literature. </dt>

          <ul>

            <li>
            <im:querylink text=" <i>D. melanogaster</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Gene.geneticInteractions.experiment.publication.year Gene.geneticInteractions.experiment.publication.firstAuthor Gene.geneticInteractions.experiment.publication.journal Gene.geneticInteractions.experiment.publication.title Gene.geneticInteractions.experiment.publication.pubMedId" sortOrder="Gene.geneticInteractions.experiment.publication.year desc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
            </li>

            <li>
            <im:querylink text=" <i>C. elegans</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Gene.geneticInteractions.experiment.publication.year Gene.geneticInteractions.experiment.publication.firstAuthor Gene.geneticInteractions.experiment.publication.journal Gene.geneticInteractions.experiment.publication.title Gene.geneticInteractions.experiment.publication.pubMedId" sortOrder="Gene.geneticInteractions.experiment.publication.year desc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.shortName" type="String">
    <constraint op="=" value="C. elegans" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
            </li>


            <li>
            <im:querylink text=" <i>S. cerevisiae</i> experiment list" skipBuilder="true">
<query name="" model="genomic" view="Gene.geneticInteractions.experiment.publication.year Gene.geneticInteractions.experiment.publication.firstAuthor Gene.geneticInteractions.experiment.publication.journal Gene.geneticInteractions.experiment.publication.title Gene.geneticInteractions.experiment.publication.pubMedId" sortOrder="Gene.geneticInteractions.experiment.publication.year desc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.shortName" type="String">
    <constraint op="=" value="S. cerevisiae" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
            </li><br/> 


            </dt>
          </dl>
        </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv3');">
            <img id='hiddenDiv3Toggle' src="images/disclosed.gif"/>
            <i> D. melanogaster </i> miRNA targets ...
          </a>
        </h4>


        <div id="hiddenDiv3" class="dataSetDescription">
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
            <im:querylink text="All <i>D. melanogaster</i> protein interactions " skipBuilder="true">
              <query name="" model="genomic" view="Protein.proteinInteractions.experiment.publication.pubMedId Protein.proteinInteractions.shortName Protein.primaryAccession Protein.proteinInteractions.interactingProteins.primaryAccession" sortOrder="Protein.proteinInteractions.experiment.publication.pubMedId asc">
                <pathDescription pathString="Protein.proteinInteractions.interactingProteins" description="Interacting protein">
                </pathDescription>
                <pathDescription pathString="Protein.proteinInteractions" description="Protein interaction">
                </pathDescription>
                <pathDescription pathString="Protein.proteinInteractions.experiment.publication" description="Protein interaction experiment">
                </pathDescription>
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
            <im:querylink text="All <i>C. elegans</i> protein interactions " skipBuilder="true">
              <query name="" model="genomic" view="Protein.proteinInteractions.experiment.publication.pubMedId Protein.proteinInteractions.shortName Protein.primaryAccession Protein.proteinInteractions.interactingProteins.primaryAccession" sortOrder="Protein.proteinInteractions.experiment.publication.pubMedId asc">
                <pathDescription pathString="Protein.proteinInteractions.interactingProteins" description="Interacting protein">
                </pathDescription>
                <pathDescription pathString="Protein.proteinInteractions" description="Protein interaction">
                </pathDescription>
                <pathDescription pathString="Protein.proteinInteractions.experiment.publication" description="Protein interaction experiment">
                </pathDescription>
                <node path="Protein" type="Protein">
                </node>
                <node path="Protein.organism" type="Organism">
                </node>
                <node path="Protein.organism.name" type="String">
                  <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="A" extraValue="">
                  </constraint>
                </node>
              </query>
            </im:querylink>

          </li>

          <li>
            <im:querylink text="All <i>S. cerevisiae</i> protein interactions " skipBuilder="true">
              <query name="" model="genomic" view="Protein.proteinInteractions.experiment.publication.pubMedId Protein.proteinInteractions.shortName Protein.primaryAccession Protein.proteinInteractions.interactingProteins.primaryAccession" sortOrder="Protein.proteinInteractions.experiment.publication.pubMedId asc">
                <pathDescription pathString="Protein.proteinInteractions.interactingProteins" description="Interacting protein">
                </pathDescription>
                <pathDescription pathString="Protein.proteinInteractions" description="Protein interaction">
                </pathDescription>
                <pathDescription pathString="Protein.proteinInteractions.experiment.publication" description="Protein interaction experiment">
                </pathDescription>
                <node path="Protein" type="Protein">
                </node>
                <node path="Protein.organism" type="Organism">
                </node>
                <node path="Protein.organism.name" type="String">
                  <constraint op="=" value="Saccharomyces cerevisiae" description="" identifier="" code="A" extraValue="">
                  </constraint>
                </node>
              </query>
            </im:querylink>

          </li>

          <li>
            <im:querylink text="All <i>D. melanogaster</i> genetic interactions " skipBuilder="true">
              <query name="" model="genomic" view="Gene.geneticInteractions.experiment.publication.pubMedId Gene.geneticInteractions.shortName Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.geneticInteractions.interactingGenes.primaryIdentifier Gene.geneticInteractions.interactingGenes.secondaryIdentifier Gene.geneticInteractions.interactingGenes.symbol Gene.geneticInteractions.type Gene.geneticInteractions.geneRole" sortOrder="Gene.geneticInteractions.experiment.publication.pubMedId asc">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A" extraValue="">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i>C. elegans</i> genetic interactions " skipBuilder="true">
              <query name="" model="genomic" view="Gene.geneticInteractions.experiment.publication.pubMedId Gene.geneticInteractions.shortName Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.geneticInteractions.interactingGenes.primaryIdentifier Gene.geneticInteractions.interactingGenes.secondaryIdentifier Gene.geneticInteractions.interactingGenes.symbol Gene.geneticInteractions.type Gene.geneticInteractions.geneRole" sortOrder="Gene.geneticInteractions.experiment.publication.pubMedId asc">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="A" extraValue="">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i>S. cerevisiae</i> genetic interactions " skipBuilder="true">
              <query name="" model="genomic" view="Gene.geneticInteractions.experiment.publication.pubMedId Gene.geneticInteractions.shortName Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.geneticInteractions.interactingGenes.primaryIdentifier Gene.geneticInteractions.interactingGenes.secondaryIdentifier Gene.geneticInteractions.interactingGenes.symbol Gene.geneticInteractions.type Gene.geneticInteractions.geneRole" sortOrder="Gene.geneticInteractions.experiment.publication.pubMedId asc">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Saccharomyces cerevisiae" description="" identifier="" code="A" extraValue="">
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
