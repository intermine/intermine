<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<TABLE width="100%">
  <TR>
    <TD valign="top">
      <div class="heading2">
        Current Data
      </div>
      <div class="body">
        <P><B>High-throughput 2-hybrid protein interaction datasets.</B></P>
        <P>
          These datasets were downloaded in <A href="http://psidev.sourceforge.net/mi/xml/doc/user">PSI-MI format</A> from the <A href="http://www.ebi.ac.uk/intact/index.html">intAct</A> database:
       </P>
       <P><B><I>D. melanogaster</I></B></P>
       <DL>
         <DT>Giot et al (2003) Science 302 1727-1736 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14605208">PubMed: 14605208</A>).
         </DT>
         <DD>
           High throughput Gal4-based two-hybrid interaction data set.<BR>
           7048 proteins and 20,405 interactions.<BR>
         </DD>
         <DT>Stanyon et al (2004) Genome Biology 5: R96 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15575970">PubMed: 15575970</A>).
         </DT>
         <DD>
           High-throughput LexA-based two-hybrid system.<BR>  
           Focussed on proteins related to cell-cycle regulators.<BR>
           1,814 reproducible interactions among 488 proteins.<BR>
           28 interactions in common between this screen and the Giot et al screen described above.<BR>
         </DD>
       </DL>
       <P><B><I>C. elegans</I></B></P>
       <DL>
         <DT>
           Li et al (2004) Science 303 540-543 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14704431">PubMed: 14704431</A>)
         </DT>
         <DD>
           Total of 4049 interactions identified.
         </DD>
       </DL>
     </div>
    </TD>
    <TD width="30%" valign="top">
      <div class="heading2">
        Datasets
      </div>
      <div class="body">
        
        <ul>
          <li>
            <im:querylink text="All <i>D. melanogaster</i> protein interactions (browse)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction ProteinInteraction.bait ProteinInteraction.prey ProteinInteraction.evidence ProteinInteraction.evidence.analysis.publication">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.evidence" type="ExperimentalResult">
  </node>
  <node path="ProteinInteraction.bait" type="Protein">
  </node>
  <node path="ProteinInteraction.bait.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.bait.organism.name" type="String">
  </node>
  <node path="ProteinInteraction.bait.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster">
    </constraint>
  </node>
  <node path="ProteinInteraction.prey" type="Protein">
  </node>
  <node path="ProteinInteraction.prey.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.prey.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>D. melanogaster</i> protein interactions (for export/download)" skipBuilder="true">
              <query name="" model="genomic" view="ProteinInteraction.bait.identifier ProteinInteraction.bait.primaryAccession ProteinInteraction.prey.identifier ProteinInteraction.prey.primaryAccession ProteinInteraction.evidence.confidence">
                <node path="ProteinInteraction" type="ProteinInteraction">
                </node>
                <node path="ProteinInteraction.evidence" type="AnalysisResult">
                </node>
                <node path="ProteinInteraction.bait" type="Protein">
                </node>
                <node path="ProteinInteraction.bait.organism" type="Organism">
                </node>
                <node path="ProteinInteraction.bait.organism.name" type="String">
                </node>
                <node path="ProteinInteraction.bait.organism.shortName" type="String">
                  <constraint op="=" value="D. melanogaster">
                  </constraint>
                </node>
                <node path="ProteinInteraction.prey" type="Protein">
                </node>
                <node path="ProteinInteraction.prey.organism" type="Organism">
                </node>
                <node path="ProteinInteraction.prey.organism.shortName" type="String">
                  <constraint op="=" value="D. melanogaster">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="Giot et al, 2003 (for export/download)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.bait.identifier ProteinInteraction.bait.primaryAccession ProteinInteraction.prey.identifier ProteinInteraction.prey.primaryAccession ProteinInteraction.evidence.confidence">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.evidence" type="AnalysisResult">
  </node>
  <node path="ProteinInteraction.evidence.analysis" type="Analysis">
  </node>
  <node path="ProteinInteraction.evidence.analysis.publication" type="Publication">
  </node>
  <node path="ProteinInteraction.evidence.analysis.publication.pubMedId" type="String">
    <constraint op="=" value="14605208">
    </constraint>
  </node>
  <node path="ProteinInteraction.bait" type="Protein">
  </node>
  <node path="ProteinInteraction.bait.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.bait.organism.name" type="String">
  </node>
  <node path="ProteinInteraction.bait.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster">
    </constraint>
  </node>
  <node path="ProteinInteraction.prey" type="Protein">
  </node>
  <node path="ProteinInteraction.prey.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.prey.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="Stanyon et al, 2004 (for export/download)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.bait.identifier ProteinInteraction.bait.primaryAccession ProteinInteraction.prey.identifier ProteinInteraction.prey.primaryAccession">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.evidence" type="AnalysisResult">
  </node>
  <node path="ProteinInteraction.evidence.analysis" type="Analysis">
  </node>
  <node path="ProteinInteraction.evidence.analysis.publication" type="Publication">
  </node>
  <node path="ProteinInteraction.evidence.analysis.publication.pubMedId" type="String">
    <constraint op="=" value="15575970">
    </constraint>
  </node>
  <node path="ProteinInteraction.bait" type="Protein">
  </node>
  <node path="ProteinInteraction.bait.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.bait.organism.name" type="String">
  </node>
  <node path="ProteinInteraction.bait.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster">
    </constraint>
  </node>
  <node path="ProteinInteraction.prey" type="Protein">
  </node>
  <node path="ProteinInteraction.prey.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.prey.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>C. elegans</i> protein interactions (browse)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction ProteinInteraction.bait ProteinInteraction.prey ProteinInteraction.evidence.analysis.publication">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.evidence" type="ExperimentalResult">
  </node>
  <node path="ProteinInteraction.bait" type="Protein">
  </node>
  <node path="ProteinInteraction.bait.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.bait.organism.name" type="String">
  </node>
  <node path="ProteinInteraction.bait.organism.shortName" type="String">
    <constraint op="=" value="C. elegans">
    </constraint>
  </node>
  <node path="ProteinInteraction.prey" type="Protein">
  </node>
  <node path="ProteinInteraction.prey.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.prey.organism.shortName" type="String">
    <constraint op="=" value="C. elegans">
    </constraint>
  </node>
</query>
        </im:querylink>
      </li>
          <li>
            <im:querylink text="All <i>C. elegans</i> protein interactions (for export/download)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.bait.identifier ProteinInteraction.bait.primaryAccession ProteinInteraction.prey.identifier ProteinInteraction.prey.primaryAccession">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.bait" type="Protein">
  </node>
  <node path="ProteinInteraction.bait.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.bait.organism.name" type="String">
  </node>
  <node path="ProteinInteraction.bait.organism.shortName" type="String">
    <constraint op="=" value="C. elegans">
    </constraint>
  </node>
  <node path="ProteinInteraction.prey" type="Protein">
  </node>
  <node path="ProteinInteraction.prey.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.prey.organism.shortName" type="String">
    <constraint op="=" value="C. elegans">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="Li et al, 2004 (for export/download)" skipBuilder="true">
<query name="" model="genomic" view="ProteinInteraction.bait.identifier ProteinInteraction.bait.primaryAccession ProteinInteraction.prey.identifier ProteinInteraction.prey.primaryAccession">
  <node path="ProteinInteraction" type="ProteinInteraction">
  </node>
  <node path="ProteinInteraction.bait" type="Protein">
  </node>
  <node path="ProteinInteraction.bait.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.bait.organism.name" type="String">
  </node>
  <node path="ProteinInteraction.bait.organism.shortName" type="String">
    <constraint op="=" value="C. elegans">
    </constraint>
  </node>
  <node path="ProteinInteraction.prey" type="Protein">
  </node>
  <node path="ProteinInteraction.prey.organism" type="Organism">
  </node>
  <node path="ProteinInteraction.prey.organism.shortName" type="String">
    <constraint op="=" value="C. elegans">
    </constraint>
  </node>
  <node path="ProteinInteraction.evidence" type="ExperimentalResult">
  </node>
  <node path="ProteinInteraction.evidence.analysis" type="Analysis">
  </node>
  <node path="ProteinInteraction.evidence.analysis.publication" type="Publication">
  </node>
  <node path="ProteinInteraction.evidence.analysis.publication.title" type="String">
  </node>
  <node path="ProteinInteraction.evidence.analysis.publication.pubMedId" type="String">
    <constraint op="=" value="14704431">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </TD>
  </TR>
</TABLE>
