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
     <i>D. melanogaster</i>  - Alleles and their phenotypes from FlyBase ...
   </a>
  </h4>

<div id="hiddenDiv1" class="dataSetDescription">
       <dl>

          <dt>Allele information downloaded from FlyBase: Data include
          a brief description of the allele class (e.g. "hypomorph" or
          "gain of function"), and its origin (e.g. the mutagen(s)
          used to induce the mutant allele or the type of in vitro
          mutagenesis method used to create it). </dt>

          <dt>Controlled vocabulary terms describe the overall
          phenotypic class of the mutant allele and the parts of the
          animal affected by it. If the mutant phenotype is observed
          in combination with another allele of the same gene, or a
          deficiency that affects the same gene, or if the allele is
          part of a system that requires a driver, these will be
          listed after the CV terms. </dt>

       </dl>
  <br/>
</div>

  <h4>
   <a href="javascript:toggleDiv('hiddenDiv2');">
    <img id='hiddenDiv2Toggle' src="images/disclosed.gif"/>
     <i>D. melanogaster</i>  - High-throughput cell-based RNAi screens from the RNAi Screening Center ...
   </a>
  </h4>

<div id="hiddenDiv2" class="dataSetDescription">
        <dl>
          <dt>Data from the <i>Drosophila</i> RNAi Screening Center
          (DRSC): Results show RNAi results, from primary screens,
          linked to genes via amplicons, with the corresponding
          publication. Results can be:</dt>
    <li><dt>Strong hit,</dt></li>
    <li><dt>Medium hit,</dt></li>
    <li><dt>Weak hit,</dt></li>
    <li><dt>Not a hit,</li></dt>
    <li><dt>Not screened.</li></dt>

    <dt> These designations are
          provided by the authors, except for "Not screened" which is
          annotated by the DRSC based on the screening plates that
          were used.
          </dt>
       </dl>
  <br/>

</div>

    <h4>
      <a href="javascript:toggleDiv('hiddenDiv3');">
        <img id='hiddenDiv3Toggle' src="images/disclosed.gif"/>
          <i>C. elegans</i>  - <i>In vivo</i> RNAi data from WormBase ...
      </a>
    </h4>

<div id="hiddenDiv3" class="dataSetDescription">

        <dl>
          <dt>Date from WormBase are downloaded using WormMart: Results show RNAi phenotype(s) linked to genes, with the corresponding publication. Every phenotype result has a flag for 'Observed': true/false. In some experiments, the authors noted a penetrance range for certain phenotypes.
          </dt>
        </dl>

      </div>
</div>

   <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">

          <ul>
            <li>
<im:querylink text="All <i>D. melanogaster</i> alleles linked to genes " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.symbol Gene.alleles.primaryIdentifier Gene.alleles.secondaryIdentifier Gene.alleles.mutagens.description" sortOrder="Gene.primaryIdentifier asc">
  <constraint path="Gene.organism.name" op="=" value="Drosophila melanogaster"/>
</query>
              </im:querylink>
            </li>

           <li>
<im:querylink text="All <i>D. melanogaster</i> alleles with phenotypes " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.symbol Gene.alleles.primaryIdentifier Gene.alleles.secondaryIdentifier Gene.alleles.mutagens.description Gene.alleles.alleleClass Gene.alleles.phenotypeAnnotations.annotationType Gene.alleles.phenotypeAnnotations.description" sortOrder="Gene.primaryIdentifier asc">
  <constraint path="Gene.organism.name" op="=" value="Drosophila melanogaster"/>
</query>
              </im:querylink>
            </li>

          <li>
             <im:querylink text="All <i>D. melanogaster</i> RNAi data " skipBuilder="true">
<query name="" model="genomic" view="Gene.rnaiResults.rnaiScreen.publication.pubMedId Gene.rnaiResults.pcrProduct.primaryIdentifier Gene.rnaiResults.result" sortOrder="Gene.rnaiResults.rnaiScreen.publication.pubMedId asc">
  <constraint path="Gene.rnaiResults" type="RNAiScreenHit"/>
  <constraint path="Gene.organism.name" op="=" value="Drosophila melanogaster"/>
</query>
              </im:querylink>
            </li>

            <li>
              <im:querylink text="All <i>C. elegans</i> RNAi data " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.rnaiResults.name Gene.rnaiResults.code Gene.rnaiResults.penetranceFrom Gene.rnaiResults.penetranceTo Gene.rnaiResults.publications.pubMedId" sortOrder="Gene.primaryIdentifier asc">
  <constraint path="Gene.organism.name" op="=" value="Caenorhabditis elegans"/>
  <constraint path="Gene.rnaiResults" type="RNAiPhenotype"/>
</query>
              </im:querylink>
            </li>
          </ul>

        </div>
      </td>
</table>
<!-- /RNAi.jsp -->