<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Data sets
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download
      </div>
    </td>
  </tr>
  <tr>
  <c:set var="flagships">Glycine max|soybean|G. max,Setaria italica|foxtail millet|S. italica,Setaria italica early-release|foxtail millet|S. italica early-release,Populus trichocarpa|western poplar|P. trichocarpa,Physcomitrella patens|moss|P. patens,Physcomitrella patens early-release|moss|P. patens early-release,Chlamydomonas reinhardtii|green algae|C. reinhardtii,Brachypodium distachyon|purple false brome|B. distachyon,Brachypodium distachyon early-release|purple false brome|B. distachyon early-release,Brachypodium stacei|Brachypodium stacei|B. stacei early-release,,Panicum virgatum|switchgrass|P. virgatum,Sorghum bicolor|cereal grass|S. bicolor,Sorghum bicolor early-release|cereal grass|S. bicolor early-release</c:set>

  <c:set var="others">Amborella trichopoda|amborella|A. trichopoda early-release,Amaranthus hypochondriacus|grain amaranth|A. hypochondriacus early-release|Aquilegia coerulea|Colorado blue columbine|A. coerulea,Arabidopsis halleri|Arabidopsis halleri|A. halleri early-release,Arabidopsis lyrata|Lyrate rockcress|A. lyrata,Arabidopsis thaliana|thale cress|A. thaliana,Boechera stricta|Drummond's rockcress|B. stricta,Brassica rapa FPsc|turnip mustard|B. rapa FPsc,Capsella grandiflora|Capsella grandiflora|C. grandiflora,Capsella rubella|red shepherd's purse|C. rubella,Carica papaya|papaya|C. papaya,Citrus clementina|clementine|C. clementina,Citrus sinensis|sweet orange|C. sinensis,Coccomyxa subellipsoidea C-169|Coccomyxa subellipsoidea C-169|C. subellipsoidea C-169,Cucumis sativus|cucumber|C. sativus,Eucalyptus grandis|eucalyptus|E. grandis,Eucalyptus grandis early-release|eucalyptus|E. grandis early-release,Eutrema salsugineum|salt cress|E. salsugineum,Fragaria vesca|strawberry|F. vesca,Gossypium raimondii|cotton|G. raimondii,Kalanchoe marnieriana|Kalanchoe marnieriana|K. marnieriana early-release,Linum usitatissimum|flax|L. usitatissimum,Malus domestica|apple|M. domestica,Manihot esculenta|cassava|M. esculenta,Manihot esculenta early-release|cassava|M. esculenta early-release,Medicago truncatula|barrel medic|M. truncatula,Micromonas pusilla CCMP1545|Micromonas pusilla CCMP1545|M. pusilla CCMP1545,Micromonas sp. RCC299|Micromonas sp. RCC299|M. sp. RCC299,Mimulus guttatus|monkey flower|M. guttatus,Musa acuminata|banana|M. acuminata early-release,Oryza sativa|rice|O. sativa,Ostreococcus lucimarinus|Ostreococcus lucimarinus|O. lucimarinus,Panicum hallii|Hall's panicgrass|P. hallii early-release,Phaseolus vulgaris|common bean|P. vulgaris,Prunus persica|peach|P. persica,Prunus persica early-release|peach|P. persica early-release,Ricinus communis|castor bean plant|R. communis,Salix purpurea|shrub willow|S. purpurea,Selaginella moellendorffii|spikemoss|S. moellendorffii,Setaria viridis|Setaria viridis|S. viridis early-release,Solanum lycopersicum|tomato|S. lycopersicum,Solanum tuberosum|potato|S. tuberosum,Sphagnum fallax|Sphagnum fallax|S. fallax early-release,Spirodela polyrhiza|greater duckweed|S. polyrhiza early-release,Theobroma cacao|cocoa bean|T. cacao,Triticum aestivum|wheat|T. aestivum early-release,Vitis vinifera|grape vine|V. vinifera,Volvox carteri|volvox|V. carteri,Zea mays|maize|Z. mays</c:set>

    <td>
      <div class="body">
        <p>
          Phytomine contains genomic data from the JGI flagship genomes:
          <ul>
           <c:forTokens items="${flagships}" delims="," var="flagship">

             <c:set var="field" value="${fn:split(flagship,'|')}" />
             <li>
               <em>${field[0]}</em>  - ${field[1]}
            <im:querylink text="Query for all gene identifiers." skipBuilder="true">
                <query name="" model="genomic" view="Gene.primaryIdentifier Gene.organism.shortName Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
                  <constraint path="Gene.organism.shortName" op="=" value="${field[2]}"/>
                </query>
            </im:querylink>
             </li>
           </c:forTokens>
        </ul>

        In addition, we have also included genomes from collaborators and the
        model organism community:
        <ul>

           <c:forTokens items="${others}" delims="," var="other">

             <c:set var="field" value="${fn:split(other,'|')}" />
             <li>
               <em>${field[0]}</em>  - ${field[1]}
            <im:querylink text="Query for all gene identifiers." skipBuilder="true">
                <query name="" model="genomic" view="Gene.primaryIdentifier Gene.organism.shortName Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
                  <constraint path="Gene.organism.shortName" op="=" value="${field[2]}"/>
                </query>
            </im:querylink>
             </li>
           </c:forTokens>
         </ul>
      </div>
    </td>

    <td width="40%" valign="top">
      <div class="body">
      Bulk data files for all organisms in Phytozome are available for download from the 
      <a href="http://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=PhytozomeV10">
      JGI Download Portal </a>.

      </div>
    </td>
  </tr>
</table>
