<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current Data
      </div>
      <div class="body">
        <dl>
          <dt><i>D. melanogaster</i></dt>
          <dd>
            Whole genome tiling path primers for ChIP-Chip experiments
            in <i>D. melanogaster</i>.  The primers were designed to PCR-amplify 1000-1200
            bp overlapping fragments of the release 4.0 genome excluding the transposable
            elements.  The amplimers were designed in such a way that a subset of
            amplimers, the "promoter" amplimers, are positioned adjacent to the 5' end of
            annotated genes.  Rana and Micklem (unpublished). 
          </dd>
        </dl>
        <p>
          These Flymine GBrowse view show the relationship between spans, PCR
          products and primers.  The PCR products that overlap a promoter
          region are highlighted:
        </p>
        <div style="margin-left: 20px">
          <img style="border: 1px solid black" src="model/tiling_path_screen_shot.png"/>
        </div>
        <p>A closer view:</p>
        <div style="margin-left: 20px">
          <img style="border: 1px solid black" src="model/tiling_path_screen_shot_zoom.png"/>
        </div>
      </div>
    </td>
    <td valign="top" width="40%">
      <div class="heading2">
        Current data sets
      </div>

      <div class="body">
        <ul>
          <li>
            <im:querylink text="Tiling path PCR products (browse)" skipBuilder="true">
              <query name="" model="genomic" view="PCRProduct"/>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="Tiling path PCR products identifier and
                                chromosome locations (for export/download)" skipBuilder="true">
              <query name="" model="genomic" view="PCRProduct.identifier PCRProduct.promoter PCRProduct.chromosomeLocation.start PCRProduct.chromosomeLocation.end PCRProduct.chromosome.identifier"/>
            </im:querylink>
          </li>
        </ul>
        <ul>
          <li>
            <im:querylink text="All tiling path primers (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Primer"/>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All tiling path primer identifiers and
                                chromosome locations (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Primer.identifier Primer.chromosomeLocation.start Primer.chromosomeLocation.end Primer.chromosome.identifier"/>
            </im:querylink>
          </li>
        </ul>
        <ul>
          <li>
            <im:querylink text="All tiling path forward primers (browse)" skipBuilder="true">
              <query name="" model="genomic" view="ForwardPrimer"/>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All tiling path forward primer identifiers and
                                chromosome locations (for export/download)" skipBuilder="true">
              <query name="" model="genomic" view="ForwardPrimer.identifier ForwardPrimer.chromosomeLocation.start ForwardPrimer.chromosomeLocation.end ForwardPrimer.chromosome.identifier"/>
            </im:querylink>
          </li>
        </ul>
        <ul>
          <li>
            <im:querylink text="All tiling path reverse primers (browse)" skipBuilder="true">
              <query name="" model="genomic" view="ReversePrimer"/>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All tiling path reverse primer identifiers and
                                chromosome locations (for export/download)" skipBuilder="true">
              <query name="" model="genomic" view="ReversePrimer.identifier ReversePrimer.chromosomeLocation.start ReversePrimer.chromosomeLocation.end ReversePrimer.chromosome.identifier"/>
            </im:querylink>
          </li>
        </ul>
        <ul>
          <li>
            <im:querylink text="All tiling path spans (browse)" skipBuilder="true">
              <query name="" model="genomic" view="TilingPathSpan"/>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All tiling path span identifiers and
                                chromosome locations (for export/download)" skipBuilder="true">
              <query name="" model="genomic" view="TilingPathSpan.identifier TilingPathSpan.chromosomeLocation.start TilingPathSpan.chromosomeLocation.end TilingPathSpan.chromosome.identifier"/>
            </im:querylink>
          </li>
        </ul>
      </div>
    </td>
  </tr>
</table>
