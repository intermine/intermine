<!-- resources.jsp -->
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>


<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">

<p> The following datasets might contain useful information for the (<i>Drosophila</i>) community:</p>


        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv1');">
              <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
              Insertions and deletions
            </a>
          </h4>

          <div id="hiddenDiv1" class="dataSetDescription">

    <p>
         <ul>
          <li><dt><a href="http://www.drosdel.org.uk" target="_new" title="DrosDel">DrosDel</a> is a collection
          of <i>P</i>-element insertions for generating custom chromosomal aberrations
          in <i>D. melanogaster</i>. The locations of the <i>P</i>-element insertions
          and the deletions that can be constructed from them have been loaded into
          FlyMine. Constructed deletions are tagged as available in FlyMine. Stocks
          can be ordered from the <a href="http://expbio.bio.u-szeged.hu/fly/index.php" target="_new">Szeged stock centre</a>.</dt></li>

          <li><dt>FlyMine has additional insertions from <a href="http://www.flybase.org/" target="_new">FlyBase</a> and
          from the <a href="http://drosophila.med.harvard.edu" target="_new">Exelixis</a> collection.</dt></li>
         </ul>


</p>
        </div>
      </dl>


        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv2');">
              <img id='hiddenDiv2Toggle' src="images/disclosed.gif"/>
              Affymetrix Probe Sets
            </a>
          </h4>

          <div id="hiddenDiv2" class="dataSetDescription">
    <p>

          <dt> Probesets from the <a href="http://www.affymetrix.com/" target="_new">Affymetrix</a> GeneChip <i>Drosophila</i> Genome 1.0 and 2.0 Arrays, a
           microarray tool for studying expression of <i>D. melanogaster</i> transcripts. Probeset locations and mapped genes (not transcripts) are downloaded from Ensembl. FlyMine only loads probesets that match to at least one gene. For more information on the probeset mappings, go to <a href="http://www.ensembl.org/Homo_sapiens/helpview?kw=microarray;ref=http%3A%2F%2Fwww.ensembl.org%2FMus_musculus%2Findex.html/" target="_new">Ensembl help</a>.  </dt>
    </br></p>

        </div>
       </dl>

        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv3');">
              <img id='hiddenDiv3Toggle' src="images/disclosed.gif"/>
              INDAC Microarray oligo set
            </a>
          </h4>

          <div id="hiddenDiv3" class="dataSetDescription">

      <p>
            <dt>The aim of INDAC is to produce a widely available and uniform set of array reagents so that microarray data collected from
            different studies may be more easily compared. On behalf of INDAC, the FlyChip group has designed a set of 65-69mer long
            oligonucleotides to release 4.1 of the D. melanogaster genome. Oligos were designed using a modified version of OligoArray2 and other
            post-processing steps (David Kreil, Debashis Rana, Gos Micklem unpublished).Synthesis of the set by Illumina began in April 2005. FlyMine
            will incorporate the results of these tests when available.</dt>

            <dt>Note: FlyMine curently stores the positions of the oligos relative to the transcript rather than to the chromosome.</dt>
      </br></p>
        </div>
      </dl>


        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv4');">
              <img id='hiddenDiv4Toggle' src="images/disclosed.gif"/>
              Whole genome tiling path
            </a>
          </h4>

          <div id="hiddenDiv4" class="dataSetDescription">


        <p>Note: From 7.0 of FlyMine the format for the tiling path
  span, PCR product and primer identifiers has changed.  Previously the
  identifier included the genomic location (e.g.
  span2L:1-7529_amplimer_1). However, in order for identifiers to remain
  consistent between genome sequence releases, this has now been changed
  to a numerical identifier (eg span2L:0000001_amplimer_1).  The old
  identifiers (with chromosome locations according to genome release
  4.0) can be found in the synonyms and can still be searched for using
  the quick search. The tiling path data has been re-mapped to
  genome sequence release 5.0 as of FlyMine release 7.0. </p>


        <p>Whole genome tiling path primers for ChIP-chip experiments
        in <i>D. melanogaster</i>.  The primers were designed to PCR-amplify 1000-1200
        bp overlapping fragments of the release 4.0 genome excluding the transposable
        elements.  The amplimers were designed in such a way that a subset of
        amplimers, the "promoter" amplimers, are positioned adjacent to the 5' end of
        annotated genes.  Rana and Micklem (unpublished).<br/></p>

        <p>The purpose of designing the tiling array in this way was to allow the "promoter"
        amplimers to be organised into separate plates to allow the printing of "promoter"
        only arrays.  In order to satisfy the need for these
        promoter amplimers and to attempt to cover the genome optimally, the
        genome was divided into "tiling path spans".  All spans started or
        ended with a gene translation start site (ATGs) and/or the start or
        end of transposons.  Spans covering transposons were excluded.  Each
        span was then treated as a separate design problem and the terminal
        amplimer of a span adjacent to a gene start was chosen to be a
        "promoter" amplimer.  Amplimers were typically in the size range
        800-1300bp so "promoter" amplimers cover approximately 1kb upstream of
        the translation start.  Tiling spans as such are probably not of great
        interest in their own right as they can include zero, one or two genes
        depending on relative orientation but are useful in order to
        understand the relationship between amplimers.<br/></p>

        <p>The Flymine GBrowse views below show the relationship between spans, PCR primers
        and PCR products.  The PCR products that overlap a promoter region are highlighted:</p>

        <div style="margin-left: 20px">
          <img style="border: 1px solid black" src="model/images/tiling_path_screen_shot.png" title="Tiling path" />
        </div>
        <p>A closer view:</p>
        <div style="margin-left: 20px">
          <img style="border: 1px solid black" src="model/images/tiling_path_screen_shot_zoom.png" title="Tiling path"/>
        </div>

     </div>
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
            <im:querylink text="All DrosDel deletions " skipBuilder="true">
<query name="" model="genomic" view="ChromosomalDeletion.primaryIdentifier ChromosomalDeletion.available ChromosomalDeletion.chromosome.primaryIdentifier ChromosomalDeletion.chromosomeLocation.start ChromosomalDeletion.chromosomeLocation.end ChromosomalDeletion.element1.primaryIdentifier ChromosomalDeletion.element2.primaryIdentifier" sortOrder="ChromosomalDeletion.primaryIdentifier asc">
  <constraint path="ChromosomalDeletion.organism.name" op="=" value="Drosophila melanogaster"/>
</query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All available DrosDel deletions " skipBuilder="true">
<query name="" model="genomic" view="ChromosomalDeletion.primaryIdentifier ChromosomalDeletion.available ChromosomalDeletion.chromosome.primaryIdentifier ChromosomalDeletion.chromosomeLocation.start ChromosomalDeletion.chromosomeLocation.end ChromosomalDeletion.element1.primaryIdentifier ChromosomalDeletion.element2.primaryIdentifier" sortOrder="ChromosomalDeletion.primaryIdentifier asc" constraintLogic="A and B">
  <constraint path="ChromosomalDeletion.organism.name" code="A" op="=" value="Drosophila melanogaster"/>
  <constraint path="ChromosomalDeletion.available" code="B" op="=" value="true"/>
</query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All DrosDel insertions " skipBuilder="true">
<query name="" model="genomic" view="TransposableElementInsertionSite.primaryIdentifier TransposableElementInsertionSite.name TransposableElementInsertionSite.type TransposableElementInsertionSite.subType TransposableElementInsertionSite.chromosome.primaryIdentifier TransposableElementInsertionSite.chromosomeLocation.start TransposableElementInsertionSite.chromosomeLocation.end" sortOrder="TransposableElementInsertionSite.primaryIdentifier asc" constraintLogic="A and B">
  <node path="TransposableElementInsertionSite" type="TransposableElementInsertionSite">
  </node>
  <node path="TransposableElementInsertionSite.organism" type="Organism">
  </node>
  <node path="TransposableElementInsertionSite.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="TransposableElementInsertionSite.dataSets" type="DataSet">
  </node>
  <node path="TransposableElementInsertionSite.dataSets.name" type="String">
    <constraint op="LIKE" value="*DrosDel*" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
           </im:querylink>
          </li>

        <li>
            <im:querylink text="All <i>D. melanogaster</i> transposon insertions (including Exelixis and DrosDel)" skipBuilder="true">
<query name="" model="genomic" view="TransposableElementInsertionSite.primaryIdentifier TransposableElementInsertionSite.name TransposableElementInsertionSite.type TransposableElementInsertionSite.subType TransposableElementInsertionSite.chromosome.primaryIdentifier TransposableElementInsertionSite.chromosomeLocation.start TransposableElementInsertionSite.chromosomeLocation.end" sortOrder="TransposableElementInsertionSite.primaryIdentifier asc" constraintLogic="A and B">
  <constraint path="TransposableElementInsertionSite.organism.name" code="A" op="=" value="Drosophila melanogaster"/>
  <constraint path="TransposableElementInsertionSite.dataSets.name" code="B" op="=" value="DrosDel P-element and Deletion collection"/>
</query>
          </im:querylink>
          </li>

          <li>
      <im:querylink text="All mapped <i>D. melanogaster</i> transposon insertions (including Exelixis and DrosDel)" skipBuilder="true">
<query name="" model="genomic" view="TransposableElementInsertionSite.primaryIdentifier TransposableElementInsertionSite.secondaryIdentifier TransposableElementInsertionSite.chromosome.primaryIdentifier TransposableElementInsertionSite.chromosomeLocation.start TransposableElementInsertionSite.chromosomeLocation.end TransposableElementInsertionSite.cytoLocation" sortOrder="TransposableElementInsertionSite.primaryIdentifier asc">
  <node path="TransposableElementInsertionSite" type="TransposableElementInsertionSite">
  </node>
  <node path="TransposableElementInsertionSite.organism" type="Organism">
  </node>
  <node path="TransposableElementInsertionSite.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
          </im:querylink>
          </li>


           <li>
             <im:querylink text="Affymetrix probesets from the GeneChip <i>Drosophila</i> Genome Array with their locations" skipBuilder="true">
<query name="" model="genomic" view="ProbeSet.primaryIdentifier ProbeSet.locations.locatedOn.primaryIdentifier ProbeSet.locations.start ProbeSet.locations.end" sortOrder="ProbeSet.primaryIdentifier asc">
  <constraint path="ProbeSet.dataSets.name" op="=" value="Affymetrix array: DrosGenome1"/>
</query>
           </im:querylink>
          </li>

          <li>
             <im:querylink text="Affymetrix probesets from the GeneChip <i>Drosophila</i> Genome Array with the genes they map to" skipBuilder="true">
<query name="" model="genomic" view="ProbeSet.primaryIdentifier ProbeSet.genes.primaryIdentifier ProbeSet.genes.symbol" sortOrder="ProbeSet.primaryIdentifier asc">
  <constraint path="ProbeSet.dataSets.name" op="=" value="Affymetrix array: DrosGenome1"/>
</query>
             </im:querylink>
            </li>

          <li>
             <im:querylink text="Affymetrix probesets from the GeneChip <i>Drosophila</i> 2 Array with their locations" skipBuilder="true">
<query name="" model="genomic" view="ProbeSet.primaryIdentifier ProbeSet.locations.locatedOn.primaryIdentifier ProbeSet.locations.start ProbeSet.locations.end" sortOrder="ProbeSet.primaryIdentifier asc">
  <constraint path="ProbeSet.dataSets.name" op="=" value="Affymetrix array: Drosophila_2"/>
</query>
             </im:querylink>
            </li>

     <li>
             <im:querylink text="Affymetrix probesets from the GeneChip <i>Drosophila</i> 2 Array with the genes they map to" skipBuilder="true">
<query name="" model="genomic" view="ProbeSet.primaryIdentifier ProbeSet.genes.primaryIdentifier ProbeSet.genes.symbol" sortOrder="ProbeSet.primaryIdentifier asc">
  <constraint path="ProbeSet.dataSets.name" op="=" value="Affymetrix array: Drosophila_2"/>
</query>
             </im:querylink>
            </li>

          <li>
            <im:querylink text="All INDAC microarray oligos with their length and tm and the identifier of the associated transcript " skipBuilder="true">
              <query name="" model="genomic" view="MicroarrayOligo.primaryIdentifier MicroarrayOligo.length MicroarrayOligo.tm MicroarrayOligo.transcript.primaryIdentifier">
              </query>
            </im:querylink>

          </li>


          <li>
            <im:querylink text="All tiling path PCR product identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="PCRProduct.primaryIdentifier PCRProduct.promoter PCRProduct.chromosomeLocation.start PCRProduct.chromosomeLocation.end PCRProduct.chromosome.primaryIdentifier"/>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All tiling path primer identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="Primer.primaryIdentifier Primer.chromosomeLocation.start Primer.chromosomeLocation.end Primer.chromosome.primaryIdentifier"/>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All tiling path forward primer identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="ForwardPrimer.primaryIdentifier ForwardPrimer.chromosomeLocation.start ForwardPrimer.chromosomeLocation.end ForwardPrimer.chromosome.primaryIdentifier"/>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All tiling path reverse primer identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="ReversePrimer.primaryIdentifier ReversePrimer.chromosomeLocation.start ReversePrimer.chromosomeLocation.end ReversePrimer.chromosome.primaryIdentifier"/>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All tiling path span identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="TilingPathSpan.primaryIdentifier TilingPathSpan.chromosomeLocation.start TilingPathSpan.chromosomeLocation.end TilingPathSpan.chromosome.primaryIdentifier"/>
            </im:querylink>
          </li>

        </ul>
      </div>
    </td>
  </tr>
</table>
<!-- /resources.jsp -->