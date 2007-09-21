<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<p style="color:#e00;">
    Note: Due to recent format changes in the PSI data format confidence scores may be missing, this will be corrected shortly (12/5/06)
</p>
<TABLE width="100%">
  <TR>
    <TD valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <P><B>High-throughput 2-hybrid protein interaction datasets.</B></P>
        <P>
          These datasets were downloaded in <A href="http://psidev.sourceforge.net/mi/xml/doc/user" target="_new">PSI-MI format</A> from the <A href="http://www.ebi.ac.uk/intact/index.html" target="_new">intAct</A> database:
       </P>
       <P><B><I>D. melanogaster</I></B></P>
       <DL>
         <DT>Giot et al (2003) Science 302 1727-1736 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14605208" target="_new">PubMed: 14605208</A>).
         </DT>
         <DD>
           High throughput Gal4-based two-hybrid interaction data set.<BR>
           7048 proteins and 20,405 interactions.<BR>
         </DD>
         <DT>Stanyon et al (2004) Genome Biology 5: R96 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15575970" target="_new">PubMed: 15575970</A>).
         </DT>
         <DD>
           High-throughput LexA-based two-hybrid system.<BR>  
           Focussed on proteins related to cell-cycle regulators.<BR>
           1,814 reproducible interactions among 488 proteins.<BR>
           28 interactions in common between this screen and the Giot et al screen described above.<BR>
         </DD>
         <DT>Formstecher E. et al (2005) Genome Research 15 (3): 376-84.   (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15710747" target="_new">PubMed: 15710747</A>).
         </DT>
         <DD>
High-throughput yeast two-hybrid dataset.<BR>
More than 2300 protein-protein interactions were identified, of which 710 are of high confidence.<BR>
         </DD>
         <DT>In addition a number of protein interactions and complexes from smaller scale experiments are available.
             <im:querylink text=" D. melanogaster experiment list" skipBuilder="true">
                 <query name="" model="genomic" view="ProteinInteractionExperiment ProteinInteractionExperiment.publication.pubMedId ProteinInteractionExperiment.publication.firstAuthor ProteinInteractionExperiment.publication.year">
                     <node path="ProteinInteractionExperiment" type="ProteinInteractionExperiment"></node>
                     <node path="ProteinInteractionExperiment.hostOrganism" type="String"></node>
                     <node path="ProteinInteractionExperiment.interactions" type="ProteinInteraction"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins" type="Protein"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins.organism" type="Organism"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins.organism.name" type="String">
                         <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A"></constraint>
                     </node>
                 </query>
             </im:querylink>
         </DT>
       </DL>
       <P><B><I>C. elegans</I></B></P>
       <DL>
         <DT>
           Li et al (2004) Science 303 540-543 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14704431">PubMed: 14704431</A>)
         </DT>
         <DD>
           Total of 4049 interactions identified.
         </DD>
            <DT>In addition a number of protein interactions and complexes from smaller scale experiments are available.
                <im:querylink text=" C. elegans experiment list" skipBuilder="true">
                    <query name="" model="genomic" view="ProteinInteractionExperiment ProteinInteractionExperiment.publication.pubMedId ProteinInteractionExperiment.publication.firstAuthor ProteinInteractionExperiment.publication.year">
                        <node path="ProteinInteractionExperiment" type="ProteinInteractionExperiment"></node>
                        <node path="ProteinInteractionExperiment.hostOrganism" type="String"></node>
                        <node path="ProteinInteractionExperiment.interactions" type="ProteinInteraction"></node>
                        <node path="ProteinInteractionExperiment.interactions.proteins" type="Protein"></node>
                        <node path="ProteinInteractionExperiment.interactions.proteins.organism" type="Organism"></node>
                        <node path="ProteinInteractionExperiment.interactions.proteins.organism.name" type="String">
                            <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="A"></constraint>
                        </node>
                    </query>
                </im:querylink>
            </DT>
         </DL>
       <P><B><I>S. cerevisiae</I></B></P>
       <DL>
         <DT>
           Uetz P. et al (2000) Nature 403: 623-7 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=10688190">PubMed: 10688190</A>)
         </DT>
         <DD>
           High throughput Gal4-based two-hybrid interaction data set.<BR>
           957 putative interactions involving 1,004 S. cerevisiae proteins.<BR>
         </DD>
         <DT>
          Ito T. et al (2000) Proc Natl Acad Sci USA 97(3): 1143-7  (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=10655498">PubMed: 10655498</A>)
         </DT>
         <DD>
           High throughput Gal4-based two-hybrid interaction data set.<BR>
           957 putative interactions involving 1,004 S. cerevisiae proteins.<BR>
         </DD>
         <DT>
          Ito T. et al (2001) Proc Natl Acad Sci USA 98(8): 4569-74  (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=11283351">PubMed: 11283351</A>)
         </DT>
         <DD>
           High throughput two-hybrid interaction data set.<BR>
           4,549 two-hybrid interactions among 3,278 proteins.<BR>
         </DD>
         <DT>
          Ho Y. et al (2002) Nature 415: 180-3 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=11805837">PubMed: 11805837</A>)
         </DT>
         <DD>
           High-throughput mass spectrometric protein complex identification. <BR>
           Detected 3,617 associated proteins covering 25% of the yeast proteome.<BR>
         </DD>
         <DT>
          Gavin AC. et al (2002) Nature 415: 141-7 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=11805826">PubMed: 11805826</A>)
         </DT>
         <DD>
           Large-scale tandem-affinity purification (TAP) and mass spectrometry to characterize multiprotein complexes in Saccharomyces cerevisiae. <BR>
          Identified 232 distinct multiprotein complexes.<BR>                   
         </DD>
         <DT>
           In addition a number of protein interactions and complexes from smaller scale experiments are available.
             <im:querylink text=" S. cerevisiae experiment list" skipBuilder="true">
                 <query name="" model="genomic" view="ProteinInteractionExperiment ProteinInteractionExperiment.publication.pubMedId ProteinInteractionExperiment.publication.firstAuthor ProteinInteractionExperiment.publication.year">
                     <node path="ProteinInteractionExperiment" type="ProteinInteractionExperiment"></node>
                     <node path="ProteinInteractionExperiment.hostOrganism" type="String"></node>
                     <node path="ProteinInteractionExperiment.interactions" type="ProteinInteraction"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins" type="Protein"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins.organism" type="Organism"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins.organism.name" type="String">
                         <constraint op="=" value="Saccharomyces cerevisiae" description="" identifier="" code="A"></constraint>
                     </node>
                 </query>
             </im:querylink>
         </DT>
       </DL>
     </div>
    </TD>
    <TD width="40%" valign="top">
      <div class="heading2">
       Bulk download
      </div>
      <div class="body">
        
        <ul>
          <li>
            <im:querylink text="All <i>D. melanogaster</i> protein interactions (browse)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction ProteinInteraction.interactors.role ProteinInteraction.interactors.protein ProteinInteraction.interactors.interaction.interactors.role ProteinInteraction.interactors.interaction.interactors.protein ProteinInteraction.interactors.interaction.experiment.publication ProteinInteraction.evidence.confidence ProteinInteraction.evidence.confidenceDesc" constraintLogic="A and B and C and D and E">
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
            <im:querylink text="All <i>D. melanogaster</i> protein interactions (for export/download)" skipBuilder="true">
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
            <im:querylink text="Giot et al, 2003 (for export/download)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.shortName ProteinInteraction.interactors.role ProteinInteraction.interactors.protein.identifier ProteinInteraction.interactors.protein.primaryAccession ProteinInteraction.interactors.interaction.interactors.role ProteinInteraction.interactors.interaction.interactors.protein.identifier ProteinInteraction.interactors.interaction.interactors.protein.primaryAccession ProteinInteraction.evidence.confidence ProteinInteraction.evidence.confidenceDesc ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" constraintLogic="A and B and E">
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
  <node path="ProteinInteraction.interactors.interaction.experiment" type="ProteinInteractionExperiment">
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment.publication" type="Publication">
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" type="String">
    <constraint op="=" value="14605208" description="" identifier="" code="E">
    </constraint>
  </node>
  <node path="ProteinInteraction.evidence" type="AnalysisResult">
  </node>
</query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="Stanyon et al, 2004 (for export/download)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.shortName ProteinInteraction.interactors.role ProteinInteraction.interactors.protein.identifier ProteinInteraction.interactors.protein.primaryAccession ProteinInteraction.interactors.interaction.interactors.role ProteinInteraction.interactors.interaction.interactors.protein.identifier ProteinInteraction.interactors.interaction.interactors.protein.primaryAccession ProteinInteraction.evidence.confidence ProteinInteraction.evidence.confidenceDesc ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" constraintLogic="A and B and E">
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
  <node path="ProteinInteraction.interactors.interaction.experiment" type="ProteinInteractionExperiment">
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment.publication" type="Publication">
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" type="String">
    <constraint op="=" value="15575970" description="" identifier="" code="E">
    </constraint>
  </node>
  <node path="ProteinInteraction.evidence" type="AnalysisResult">
  </node>
</query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>C. elegans</i> protein interactions (browse)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction ProteinInteraction.interactors.role ProteinInteraction.interactors.protein ProteinInteraction.interactors.interaction.interactors.role ProteinInteraction.interactors.interaction.interactors.protein ProteinInteraction.interactors.interaction.experiment.publication ProteinInteraction.evidence.confidence ProteinInteraction.evidence.confidenceDesc" constraintLogic="A and B and C and D">
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
            <im:querylink text="All <i>C. elegans</i> protein interactions (for export/download)" skipBuilder="true">
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
            <im:querylink text="Li et al, 2004 (for export/download)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.shortName ProteinInteraction.interactors.role ProteinInteraction.interactors.protein.identifier ProteinInteraction.interactors.protein.primaryAccession ProteinInteraction.interactors.interaction.interactors.role ProteinInteraction.interactors.interaction.interactors.protein.identifier ProteinInteraction.interactors.interaction.interactors.protein.primaryAccession ProteinInteraction.evidence.confidence ProteinInteraction.evidence.confidenceDesc ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" constraintLogic="A and B and E">
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
  <node path="ProteinInteraction.interactors.interaction.experiment" type="ProteinInteractionExperiment">
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment.publication" type="Publication">
  </node>
  <node path="ProteinInteraction.interactors.interaction.experiment.publication.pubMedId" type="String">
    <constraint op="=" value="14704431" description="" identifier="" code="E">
    </constraint>
  </node>
  <node path="ProteinInteraction.evidence" type="AnalysisResult">
  </node>
</query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </TD>
  </TR>
</TABLE>
