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

<p> The following datasets might contain useful information for the (<i>Drosophila</i>) community:</p>


        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv1');">
              <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
              Insertions and deletions
            </a>
          </h4>

          <div id="hiddenDiv1" class="dataSetDescription">


         <ul>
          <li><dt><a href="http://www.drosdel.org.uk">DrosDel</a> is a collection
          of <i>P</i>-element insertions for generating custom chromosomal aberrations
          in <i>D. melanogaster</i>. The locations of the <i>P</i>-element insertions
          and the deletions that can be constructed from them have been loaded into
          FlyMine. Constructed deletions are tagged as available in FlyMine. Stocks 
          can be ordered from the <a href="http://expbio.bio.u-szeged.hu/fly/index.php" target="_new">Szeged stock centre</a>.</dt></li>

          <li><dt>FlyMine has additional insertions from <a href="http://www.flybase.org/" target="_new">FlyBase</a> and 
          from the <a href="http://drosophila.med.harvard.edu" target="_new">Exelixis</a> collection.</dt></li>
         </ul>

	<dt>Note: The DrosDel data has been re-mapped to
	genome sequence release 5.0 as of FlyMine release 7.0,
	however, coordinates for the Exelixis set are still to genomce
	sequence release 4.0.  These will be updated to release 5.0 in the
	next release of FlyMine.</dt>

        </div>
      </dl> 


        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv2');">
              <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
              Affymetrix Probe Sets
            </a>
          </h4>

          <div id="hiddenDiv2" class="dataSetDescription">

          <dt> Probe sets from the <a href="http://www.affymetrix.com/" target="_new">Affymetrix</a> GeneChip <i>Drosophila</i> Genome 2.0 Array, a
           microarray tool for studying expression of <i>D. melanogaster</i> transcripts.Comprised of 18,880 probe sets for the analysis 
           of over 18,500 transcripts. Sequences used in the design of the GeneChip <i>Drosophila</i> Genome 2.0 Array were selected 
           from Flybase version 3.1.</dt>


        </div>
       </dl>   

        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv3');">
              <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
              INDAC Microarray oligo set
            </a>
          </h4>

          <div id="hiddenDiv3" class="dataSetDescription">

            <dt>The aim of INDAC is to produce a widely available and uniform set of array reagents so that microarray data collected from 
            different studies may be more easily compared. On behalf of INDAC, the FlyChip group has designed a set of 65-69mer long
            oligonucleotides to release 4.1 of the D. melanogaster genome. Oligos were designed using a modified version of OligoArray2 and other
            post-processing steps (David Kreil, Debashis Rana, Gos Micklem unpublished).Synthesis of the set by Illumina began in April 2005. FlyMine 
            will incorporate the results of these tests when available.</dt>

            <dt>Note: FlyMine curently stores the positions of the oligos relative to the transcript rather than to the chromosome.</dt>

        </div>
      </dl> 


        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv4');">
              <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
              Whole genome tiling path
            </a>
          </h4>

          <div id="hiddenDiv4" class="dataSetDescription">


        <p>Note: In release 7.0 of FlyMine the format for the tiling path
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
          <img style="border: 1px solid black" src="model/images/tiling_path_screen_shot.png"/>
        </div>
        <p>A closer view:</p>
        <div style="margin-left: 20px">
          <img style="border: 1px solid black" src="model/images/tiling_path_screen_shot_zoom.png"/>
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
              <query name="" model="genomic" view="ArtificialDeletion.identifier ArtificialDeletion.available ArtificialDeletion.chromosome.identifier ArtificialDeletion.chromosomeLocation.start ArtificialDeletion.chromosomeLocation.end">
                <node path="ArtificialDeletion" type="ArtificialDeletion">
                </node>
                <node path="ArtificialDeletion.organism" type="Organism">
                </node>
                <node path="ArtificialDeletion.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All available DrosDel deletions " skipBuilder="true">
              <query name="" model="genomic" view="ArtificialDeletion.identifier ArtificialDeletion.chromosome.identifier ArtificialDeletion.chromosomeLocation.start ArtificialDeletion.chromosomeLocation.end">
                <node path="ArtificialDeletion" type="ArtificialDeletion">
                </node>
                <node path="ArtificialDeletion.organism" type="Organism">
                </node>
                <node path="ArtificialDeletion.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
                  </constraint>
                  <node path="ArtificialDeletion.available" type="Boolean">
                    <constraint op="=" value="true" description="" identifier="" code="B">
                    </constraint>
                  </node>
               </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All DrosDel insertions " skipBuilder="true">
            <query name="" model="genomic" view="TransposableElementInsertionSite.identifier TransposableElementInsertionSite.type TransposableElementInsertionSite.subType TransposableElementInsertionSite.chromosome.identifier TransposableElementInsertionSite.chromosomeLocation.start TransposableElementInsertionSite.chromosomeLocation.end" constraintLogic="A and B">
             <node path="TransposableElementInsertionSite" type="TransposableElementInsertionSite">
              </node>
             <node path="TransposableElementInsertionSite.organism" type="Organism">
              </node>
             <node path="TransposableElementInsertionSite.organism.name" type="String">
               <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
               </constraint>
              </node>
             <node path="TransposableElementInsertionSite.evidence" type="DataSet">
              </node>
             <node path="TransposableElementInsertionSite.evidence.title" type="String">
              <constraint op="LIKE" value="%DrosDel%" description="" identifier="" code="B">
              </constraint>
             </node>
            </query>
           </im:querylink>
          </li>

        <li>
            <im:querylink text="All P-element insertions (including Exelixis and DrosDel)" skipBuilder="true">
              <query name="" model="genomic" view="TransposableElementInsertionSite.identifier TransposableElementInsertionSite.type TransposableElementInsertionSite.subType TransposableElementInsertionSite.chromosome.identifier TransposableElementInsertionSite.chromosomeLocation.start TransposableElementInsertionSite.chromosomeLocation.end">
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
             <im:querylink text="All Affymetrix probes from the GeneChip <i>Drosophila</i> Genome 2.0 Array " skipBuilder="true">
              <query name="" model="genomic" view="ProbeSet.identifier ProbeSet.length ProbeSet.isControl">
               </query>
             </im:querylink>
            </li>


          <li>
            <im:querylink text="All INDAC microarray oligos with their length and tm and the identifier of the associated transcript " skipBuilder="true">
              <query name="" model="genomic" view="MicroarrayOligo.identifier MicroarrayOligo.length MicroarrayOligo.tm MicroarrayOligo.transcript.identifier">
              </query>
            </im:querylink>

          </li>


          <li>
            <im:querylink text="All tiling path PCR product identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="PCRProduct.identifier PCRProduct.promoter PCRProduct.chromosomeLocation.start PCRProduct.chromosomeLocation.end PCRProduct.chromosome.identifier"/>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All tiling path primer identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="Primer.identifier Primer.chromosomeLocation.start Primer.chromosomeLocation.end Primer.chromosome.identifier"/>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All tiling path forward primer identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="ForwardPrimer.identifier ForwardPrimer.chromosomeLocation.start ForwardPrimer.chromosomeLocation.end ForwardPrimer.chromosome.identifier"/>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All tiling path reverse primer identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="ReversePrimer.identifier ReversePrimer.chromosomeLocation.start ReversePrimer.chromosomeLocation.end ReversePrimer.chromosome.identifier"/>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All tiling path span identifiers and chromosomal locations " skipBuilder="true">
              <query name="" model="genomic" view="TilingPathSpan.identifier TilingPathSpan.chromosomeLocation.start TilingPathSpan.chromosomeLocation.end TilingPathSpan.chromosome.identifier"/>
            </im:querylink>
          </li>

        </ul>
      </div>
    </td>
  </tr>
</table>
