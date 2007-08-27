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
       Tiling Path data for <i>D. melanogaster</i> in FlyMine ...
     </a>
   </h4>

<div id="hiddenDiv1" style="display:none;" class="dataSetDescription">
	
		<p>
		Note: In release 7.0 of FlyMine the format for the tiling path
	span, PCR product and primer identifiers has changed.  Previously the
	identifier included the genomic location (eg
	span2L:1-7529_amplimer_1). However, in order for identifiers to remain
	consistent between genome sequence releases, this has now been changed
	to a numerical identifier (eg span2L:0000001_amplimer_1).  The old
	identifiers (with chromosome locations according to genome release
	4.0) can be found in the synonyms and can still be searched for using
	the quick search. The tiling path data has been re-mapped to
	genome sequence release 5.0 as of FlyMine release 7.0. </p>


           <p>
            Whole genome tiling path primers for ChIP-chip experiments
            in <i>D. melanogaster</i>.  The primers were designed to PCR-amplify 1000-1200
            bp overlapping fragments of the release 4.0 genome excluding the transposable
            elements.  The amplimers were designed in such a way that a subset of
            amplimers, the "promoter" amplimers, are positioned adjacent to the 5' end of
            annotated genes.  Rana and Micklem (unpublished).<br/>
           </p>
           <p>
            The purpose of designing the tiling array in this way was to allow the "promoter" 
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
            understand the relationship between amplimers.</br>
           </p>
           <p>
            The Flymine GBrowse views below show the relationship between spans, PCR primers 
            and PCR products.  The PCR products that overlap a promoter region are highlighted:
           </p>
        <div style="margin-left: 20px">
          <img style="border: 1px solid black" src="model/tiling_path_screen_shot.png"/>
        </div>
        <p>A closer view:</p>
        <div style="margin-left: 20px">
          <img style="border: 1px solid black" src="model/tiling_path_screen_shot_zoom.png"/>
        </div>
        </div>
      </div>
    </td>


    <td valign="top" width="40%">
      <div class="heading2">
        Bulk download
      </div>

      <div class="body">
        <ul>
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
