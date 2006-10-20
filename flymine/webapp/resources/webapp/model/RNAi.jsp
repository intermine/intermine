<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
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
    <img id='hiddenDiv1Toggle' src="images/undisclosed.gif"/>
     <i>D. melanogaster</i>  - High-throughput cell-based RNAi screens from the RNAi Screening Center ...
   </a>
  </h4>

<div id="hiddenDiv1" style="display:none;">

        <dl>
          <dt>
            Agaisse et al (2005) Science 309:1248-1251 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16020693">PubMed: 16020693</a>).
          </dt>
          <dd>
            Genome-wide RNAi screen for host factors required for intracellular bacterial infection.
          </dd>
          <dt>
           Baeg et al (2005) Genes Dev 19:1861-1870 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16055650">PubMed: 16055650</a>).
          </dt>
          <dd>
           Genome-wide RNAi analysis of JAK/STAT signaling components in <i>Drosophila</i>.
          </dd>
          <dt>
           Boutros et al (2004) Science 303:832-835 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14764878">PubMed: 14764878</a>).
          </dt>
          <dd>
           Genome-wide RNAi analysis of growth and viability in <i>Drosophila</i> cells.
          </dd>
          <dt>
           DasGupta et al (2005) Science 308:826-833 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15817814">PubMed: 15817814</a>).
          </dt>
          <dd>
           Functional genomic analysis of the Wnt-wingless signaling pathway.
         </dd>
          <dt>
           Eggert et al (2004) PLoS Biol 2:e379 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15547975">PubMed: 15547975</a>).
          </dt>
          <dd>
           Parallel chemical genetic and genome-wide RNAi screens identify cytokinesis inhibitors and targets.
          </dd>
          <dt>
           Philips et al (2005) (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16020694">PubMed: 16020694</a>).
          </dt>
          <dd>
           <i>Drosophila</i> RNAi screen reveals CD36 family member required for mycobacterial infection.
          </dd>
          <dt>
           Vig et al (2006) Science 312:1220-1223 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16645049">PubMed: 16645049</a>).
          </dt>
          <dd>
           CRACM1 is a plasma membrane protein essential for store-operated Ca2+ entry.
          </dd>

          <p>
          Note: Because of the issue of off-target effect (OTE) (see <a href="http://www.flyrnai.org/RNAi_OTE.html">Matter Arising: Issues of off-targets in Drosophila RNAi screens</A>), FlyMine only contains results from the DRSC curated lists for each screen. Any gene that was targeted by a dsRNA with predicted OTEs is omitted from that list.  Furthermore, for each result, FlyMine shows the number of potential off-targets based on sequence matches (numOffTargets) - at least one length of 19 bp or more with matching sequence of 19 bp or more of this amplicon - as well as the maximum continuous sequence overlap of potential off-target(s) (maxOffTargetOverlaps).
          </p>

        </dl>

  <br/>

</div>

    <h4>   
      <a href="javascript:toggleDiv('hiddenDiv2');">
        <img id='hiddenDiv2Toggle' src="images/undisclosed.gif"/>
          <i>C. elegans</i>  - RNAi data from <i>in vivo</i> experiments ...
      </a>
    </h4>

<div id="hiddenDiv2" style="display:none;">

        <dl>
          <dt>
            Fraser et al (2000) Nature 408:325-330 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=11099033">PubMed: 11099033</a>).
          </dt>
          <dd>
            Functional genomic analysis of <i>C. elegans</i> chromosome 1 by systematic RNA interference.
          </dd>
          <dt>
            Kamath et al (2003) Nature 421:231-237 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=12529635">PubMed: 12529635</a>).
          </dt>
          <dd>
            Systematic functional analysis of the <i>Caenorhabditis elegans</i> genome using RNAi.
          </dd>
          <dt>
            Simmer et al (2003) Plos Biology 1:77-84 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=14551910">PubMed: 14551910</a>).
          </dt>
          <dd>
            Genome-wide RNAi of <i>C. elegans</i> using the hypersensitive rrf-3 strain reveals novel gene functions.
          </dd>
        </dl>

      </div>
</div>

      <td width="30%" valign="top">
        <div class="heading2">
          Bulk download
        </div>
        <div class="body">
          
          <ul>
            <li>
              <im:querylink text="All <i>C. elegans</i> RNAi phenotype data (browse)" skipBuilder="true">
                <query name="" model="genomic" view="Gene Gene.phenotypes">
                  <node path="Gene" type="Gene">
                  </node>
                  <node path="Gene.organism" type="Organism">
                  </node>
                  <node path="Gene.organism.shortName" type="String">
                    <constraint op="=" value="C. elegans">
                    </constraint>
                  </node>
                </query>
              </im:querylink>
            </li>
            <li>
              <im:querylink text="All <i>C. elegans</i> RNAi phenotype data (for export/download)" skipBuilder="true">
                <query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.phenotypes.name Gene.phenotypes.RNAiCode Gene.phenotypes.identifier">
                  <node path="Gene" type="Gene">
                  </node>
                  <node path="Gene.organism" type="Organism">
                  </node>
                  <node path="Gene.organism.shortName" type="String">
                    <constraint op="=" value="C. elegans">
                    </constraint>
                  </node>
                </query>
              </im:querylink>
            </li>
            <li>
              <im:querylink text="Kamath et al, 2003 (browse)" skipBuilder="true">
                <query name="" model="genomic" view="Gene Gene.annotations.property">
                  <node path="Gene" type="Gene">
                  </node>
                  <node path="Gene.organism" type="Organism">
                  </node>
                  <node path="Gene.organism.shortName" type="String">
                    <constraint op="=" value="C. elegans">
                    </constraint>
                  </node>
                  <node path="Gene.annotations" type="Annotation">
                  </node>
                  <node path="Gene.annotations.evidence" type="ExperimentalResult">
                  </node>
                  <node path="Gene.annotations.evidence.analysis" type="Analysis">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication" type="Publication">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication.pubMedId" type="String">
                    <constraint op="=" value="12529635">
                    </constraint>
                  </node>
                  <node path="Gene.annotations.property" type="Phenotype">
                  </node>
                </query>        
              </im:querylink>
            </li>
            <li>
              <im:querylink text="Kamath et al, 2003 (for export/download)" skipBuilder="true">
                <query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.annotations.property.name Gene.annotations.property.RNAiCode Gene.annotations.property.identifier">
                  <node path="Gene" type="Gene">
                  </node>
                  <node path="Gene.organism" type="Organism">
                  </node>
                  <node path="Gene.organism.shortName" type="String">
                    <constraint op="=" value="C. elegans">
                    </constraint>
                  </node>
                  <node path="Gene.annotations" type="Annotation">
                  </node>
                  <node path="Gene.annotations.evidence" type="ExperimentalResult">
                  </node>
                  <node path="Gene.annotations.evidence.analysis" type="Analysis">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication" type="Publication">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication.pubMedId" type="String">
                    <constraint op="=" value="12529635">
                    </constraint>
                  </node>
                  <node path="Gene.annotations.property" type="Phenotype">
                  </node>
                </query>
              </im:querylink>
            </li>
            <li>
              <im:querylink text="Fraser et al, 2000 (browse)" skipBuilder="true">
                <query name="" model="genomic" view="Gene Gene.annotations.property">
                  <node path="Gene" type="Gene">
                  </node>
                  <node path="Gene.organism" type="Organism">
                  </node>
                  <node path="Gene.organism.shortName" type="String">
                    <constraint op="=" value="C. elegans">
                    </constraint>
                  </node>
                  <node path="Gene.annotations" type="Annotation">
                  </node>
                  <node path="Gene.annotations.evidence" type="ExperimentalResult">
                  </node>
                  <node path="Gene.annotations.evidence.analysis" type="Analysis">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication" type="Publication">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication.pubMedId" type="String">
                    <constraint op="=" value="11099033">
                    </constraint>
                  </node>
                  <node path="Gene.annotations.property" type="Phenotype">
                  </node>
                </query>
              </im:querylink>
            </li>
            <li>
              <im:querylink text="Fraser et al, 2000 (for export/download)" skipBuilder="true">
                <query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.annotations.property.name Gene.annotations.property.RNAiCode Gene.annotations.property.identifier">
                  <node path="Gene" type="Gene">
                  </node>
                  <node path="Gene.organism" type="Organism">
                  </node>
                  <node path="Gene.organism.shortName" type="String">
                    <constraint op="=" value="C. elegans">
                    </constraint>
                  </node>
                  <node path="Gene.annotations" type="Annotation">
                  </node>
                  <node path="Gene.annotations.evidence" type="ExperimentalResult">
                  </node>
                  <node path="Gene.annotations.evidence.analysis" type="Analysis">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication" type="Publication">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication.pubMedId" type="String">
                    <constraint op="=" value="11099033">
                    </constraint>
                  </node>
                  <node path="Gene.annotations.property" type="Phenotype">
                  </node>
                </query>        
              </im:querylink>
            </li>
            <li>
              <im:querylink text="Simmer et al, 2003 (browse)" skipBuilder="true">
                <query name="" model="genomic" view="Gene Gene.annotations.property">
                  <node path="Gene" type="Gene">
                  </node>
                  <node path="Gene.organism" type="Organism">
                  </node>
                  <node path="Gene.organism.shortName" type="String">
                    <constraint op="=" value="C. elegans">
                    </constraint>
                  </node>
                  <node path="Gene.annotations" type="Annotation">
                  </node>
                  <node path="Gene.annotations.evidence" type="ExperimentalResult">
                  </node>
                  <node path="Gene.annotations.evidence.analysis" type="Analysis">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication" type="Publication">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication.pubMedId" type="String">
                    <constraint op="=" value="14551910">
                    </constraint>
                  </node>
                  <node path="Gene.annotations.property" type="Phenotype">
                  </node>
                </query>        
              </im:querylink>
            </li>
            <li>
              <im:querylink text="Simmer et al, 2003 (for export/download)" skipBuilder="true">
                <query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.annotations.property.name Gene.annotations.property.RNAiCode Gene.annotations.property.identifier">
                  <node path="Gene" type="Gene">
                  </node>
                  <node path="Gene.organism" type="Organism">
                  </node>
                  <node path="Gene.organism.shortName" type="String">
                    <constraint op="=" value="C. elegans">
                    </constraint>
                  </node>
                  <node path="Gene.annotations" type="Annotation">
                  </node>
                  <node path="Gene.annotations.evidence" type="ExperimentalResult">
                  </node>
                  <node path="Gene.annotations.evidence.analysis" type="Analysis">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication" type="Publication">
                  </node>
                  <node path="Gene.annotations.evidence.analysis.publication.pubMedId" type="String">
                    <constraint op="=" value="14551910">
                    </constraint>
                  </node>
                  <node path="Gene.annotations.property" type="Phenotype">
                  </node>
                </query>        
              </im:querylink>
            </li>
            <li>
              <im:querylink text="All phenotype identifiers, names and RNAi codes" skipBuilder="true">
                <query name="" model="genomic" view="Phenotype.identifier Phenotype.name Phenotype.RNAiCode">
                </query>
              </im:querylink>
            </li>
          </ul>
        </div>
      </TD>
  </TR>
</TABLE>
