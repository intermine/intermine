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
          <img id='hiddenDiv1Toggle' src="images/undisclosed.gif"/>
            <i>D. melanogaster</i> - High-throughput 2-hybrid protein interaction datasets ...
        </a>
       </h4>

<div id="hiddenDiv1" style="display:none;">
       
       <dl>
         <dt>Giot et al (2003) Science 302: 1727-1736 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14605208">PubMed: 14605208</a>) 
             - A protein interaction map of <i>Drosophila melanogaster</i>.
         </dt>
         <dd>
           High throughput Gal4-based two-hybrid interaction data set.<br/>
           7048 proteins and 20,405 interactions.<br/>
         </dd>

         <dt>Stanyon et al (2004) Genome Biology 5: R96 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15575970">PubMed: 15575970</a>) 
             - A <i>Drosophila</i> protein-interaction map centered on cell-cycle regulators.
         </dt>
         <dd>
           High-throughput LexA-based two-hybrid system.<br/>
           1,814 reproducible interactions among 488 proteins.<br/>
           28 interactions in common between this screen and the Giot et al screen described above.<br/>
         </dd>

         <dt>Formstecher E. et al (2005) Genome Research 15: 376-384 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15710747">PubMed: 15710747</a>) 
             - Protein interaction mapping: a <i>Drosophila</i> case study.
         </dt>
         <dd>
           High-throughput yeast two-hybrid dataset.<br/>
           More than 2300 protein-protein interactions were identified, of which 710 are of high confidence.<br/>
         </dd>
         <dt>In addition a number of protein interactions and complexes from smaller scale experiments are available:
             <im:querylink text=" <i>D. melanogaster</i> experiment list" skipBuilder="true">
                 <query name="" model="genomic" view="Protein.publications.title Protein.publications.pubMedId Protein.publications.firstAuthor Protein.publications.year">
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
         </dt>
       </dl>

</div>

      <h4>
        <a href="javascript:toggleDiv('hiddenDiv2');">
          <img id='hiddenDiv2Toggle' src="images/undisclosed.gif"/>
         <i>C. elegans</i> - High-throughput 2-hybrid protein interaction datasets ...
      </a>
     </h4>

<div id="hiddenDiv2" style="display:none;">

       <dl>
         <dt>
           Li et al (2004) Science 303: 540-543 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14704431">PubMed: 14704431</a>)
           - A map of the interactome network of the metazoan <i>C. elegans</i>.
         </dt>
         <dd>
           Total of 4049 interactions identified.
         </dd>
            <dt>In addition a number of protein interactions and complexes from smaller scale experiments are available:
                <im:querylink text=" <i>C. elegans</i> experiment list" skipBuilder="true">
                    <query name="" model="genomic" view="Protein.publications.title Protein.publications.pubMedId Protein.publications.firstAuthor Protein.publications.year">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.name" type="String">
    <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
                </im:querylink>
            </dt>
         </dl>
</div>

      <h4>
        <a href="javascript:toggleDiv('hiddenDiv3');">
          <img id='hiddenDiv3Toggle' src="images/undisclosed.gif"/>
           <i>S. cerevisiae</i> - High-throughput 2-hybrid protein interaction datasets ...
      </a>
     </h4>

<div id="hiddenDiv3" style="display:none;">

       <dl>
         <dt>
           Uetz P. et al (2000) Nature 403: 623-7 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=10688190">PubMed: 10688190</A>)
           - A comprehensive analysis of protein-protein interactions in <i>Saccharomyces cerevisiae</i>.
         </dt>
         <dd>
           High throughput Gal4-based two-hybrid interaction data set.<BR>
           957 putative interactions involving 1,004 S. cerevisiae proteins.<BR>
         </dd>
         <dt>
          Ito T. et al (2000) Proc Natl Acad Sci USA 97(3): 1143-7  (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=10655498">PubMed: 10655498</a>)
          - Toward a protein-protein interaction map of the budding yeast: A comprehensive system to examine two-hybrid interactions in all possible combinations between the yeast proteins.
         </dt>
         <dd>
           High throughput Gal4-based two-hybrid interaction data set.<BR>
           957 putative interactions involving 1,004 S. cerevisiae proteins.<BR>
         </dd>
         <dt>
          Ito T. et al (2001) Proc Natl Acad Sci USA 98(8): 4569-74  (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=11283351">PubMed: 11283351</a>)
          - A comprehensive two-hybrid analysis to explore the yeast protein interactome.
         </dt>
         <dd>
           High throughput two-hybrid interaction data set.<br/> 
           4,549 two-hybrid interactions among 3,278 proteins.<br/>
         </dd>
         <dt>
          Ho Y. et al (2002) Nature 415: 180-3 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=11805837">PubMed: 11805837</a>)
          - Systematic identification of protein complexes in <i>Saccharomyces</i> cerevisiae by mass spectrometry. 
         </dt>
         <dd>
          High-throughput mass spectrometric protein complex identification.<br/>
          Detected 3,617 associated proteins covering 25% of the yeast proteome.<br/>
         </dd>
         <dt>
          Gavin AC. et al (2002) Nature 415: 141-7 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=11805826">PubMed: 11805826</a>)
          - Functional organization of the yeast proteome by systematic analysis of protein complexes.
         </dt>
         <dd>
          Large-scale tandem-affinity purification (TAP) and mass spectrometry to characterize multiprotein complexes in <i>Saccharomyces cerevisiae</i>.<br/> 
          Identified 232 distinct multiprotein complexes.<br/>                   
         </dd>
         <dt>
           In addition a number of protein interactions and complexes from smaller scale experiments are available:
             <im:querylink text=" <i>S. cerevisiae</i> experiment list" skipBuilder="true">
                 <query name="" model="genomic" view="Protein.publications.title Protein.publications.pubMedId Protein.publications.firstAuthor Protein.publications.year">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.name" type="String">
    <constraint op="=" value="Saccharomyces cerevisiae" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
             </im:querylink>
         </dt>
       </dl>
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
            <im:querylink text="All <i>D. melanogaster</i> protein interactions " skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.shortName ProteinInteraction.interactors.role ProteinInteraction.interactors.protein.identifier ProteinInteraction.interactors.protein.primaryAccession ProteinInteraction.interactors.interaction.interactors.role ProteinInteraction.interactors.interaction.interactors.protein.identifier ProteinInteraction.interactors.interaction.interactors.protein.primaryAccession ProteinInteraction.evidence.confidence ProteinInteraction.evidence.confidenceDesc ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" constraintLogic="A and B and C and D and E">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.interactors" type="ProteinInteractor">
  </node>
  <node path="ProteinInteraction.interactors.role" type="String">
    <constraint op="=" value="bait" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.interaction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors" type="ProteinInteractor">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.role" type="String">
    <constraint op="=" value="prey" description="" identifier="" code="B">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.protein" type="Protein">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.protein.organism" type="Organism">
    <constraint op="=" value="ProteinInteraction.interactors.protein.organism" code="D">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment" type="ProteinInteractionExperiment">
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment.publication" type="Publication">
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" type="String">
    <constraint op="!=" value="11196647" description="" identifier="" code="E">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.protein" type="Protein">
  </node>
  <node path="ProteinInteraction.interactors.protein.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.interactors.protein.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster" description="" identifier="" code="C">
    </constraint>
  </node>
  <node path="ProteinInteraction.evidence" type="AnalysisResult">
  </node>
</query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i>C. elegans</i> protein interactions " skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.shortName ProteinInteraction.interactors.role ProteinInteraction.interactors.protein.identifier ProteinInteraction.interactors.protein.primaryAccession ProteinInteraction.interactors.interaction.interactors.role ProteinInteraction.interactors.interaction.interactors.protein.identifier ProteinInteraction.interactors.interaction.interactors.protein.primaryAccession ProteinInteraction.evidence.confidence ProteinInteraction.evidence.confidenceDesc ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" constraintLogic="A and B and C and D">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.interactors" type="ProteinInteractor">
  </node>
  <node path="ProteinInteraction.interactors.role" type="String">
    <constraint op="=" value="bait" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.interaction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors" type="ProteinInteractor">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.role" type="String">
    <constraint op="=" value="prey" description="" identifier="" code="B">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.protein" type="Protein">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.protein.organism" type="Organism">
    <constraint op="=" value="ProteinInteraction.interactors.protein.organism" code="D">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.protein" type="Protein">
  </node>
  <node path="ProteinInteraction.interactors.protein.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.interactors.protein.organism.shortName" type="String">
    <constraint op="=" value="C. elegans" description="" identifier="" code="C">
    </constraint>
  </node>
  <node path="ProteinInteraction.evidence" type="AnalysisResult">
  </node>
</query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i>C. cerevisiae</i> protein interactions " skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.shortName ProteinInteraction.interactors.role ProteinInteraction.interactors.protein.identifier ProteinInteraction.interactors.protein.primaryAccession ProteinInteraction.interactors.interaction.interactors.role ProteinInteraction.interactors.interaction.interactors.protein.identifier ProteinInteraction.interactors.interaction.interactors.protein.primaryAccession ProteinInteraction.evidence.confidence ProteinInteraction.evidence.confidenceDesc ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" constraintLogic="A and B and C and D">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.interactors" type="ProteinInteractor">
  </node>
  <node path="ProteinInteraction.interactors.role" type="String">
    <constraint op="=" value="bait" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.interaction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors" type="ProteinInteractor">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.role" type="String">
    <constraint op="=" value="prey" description="" identifier="" code="B">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.protein" type="Protein">
  </node>
  <node path="ProteinInteraction.interactors.interaction.interactors.protein.organism" type="Organism">
    <constraint op="=" value="ProteinInteraction.interactors.protein.organism" code="D">
    </constraint>
  </node>
  <node path="ProteinInteraction.interactors.protein" type="Protein">
  </node>
  <node path="ProteinInteraction.interactors.protein.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.interactors.protein.organism.shortName" type="String">
    <constraint op="=" value="C. cerevisiae" description="" identifier="" code="C">
    </constraint>
  </node>
  <node path="ProteinInteraction.evidence" type="AnalysisResult">
  </node>
</query>
            </im:querylink>
          </li>
         </ul>

      </div>
    </td>
  </tr>
</table>
