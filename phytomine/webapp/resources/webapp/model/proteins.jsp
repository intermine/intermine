<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top" rowspan="2">
      <div class="heading2">
        Data sets
      </div>

  <c:set var="flagships">Glycine max|soybean|G. max,Setaria italica|foxtail millet|S. italica,Populus trichocarpa|black cottonwood|P. trichocarpa,Physcomitrella patens||P. patens,Chlamydomonas reinhardtii||C. reinhardtii,Panicum virgatum|switchgrass|P. virgatum,Sorghum bicolor|sorghum|S. bicolor</c:set>

  <c:set var="others">Aquilegia coerulea||A. coerulea,Arabidopsis thaliana|thale cress|A. thaliana,Arabidopsis lyrata|lyrate rockcress|A. lyrata,Boechera stricta||B. stricta,Brachypodium distachyon||B. distachyon,Brassica rapa|field mustard|B. rapa,Capsella grandiflora||C. grandiflora,Capsella rubella||C. rubella,Carica papaya|papaya|C. papaya,Citrus sinensis|sweet orange|C. sinensis,Citrus clementina||C. clementina,Coccomyxa subellipsoidea C-169||C. subellipsoidea C-169,Cucumis sativus|cucumber|C. sativus,Eucalyptus grandis|rose gum|E. grandis,Eutrema salsugineum||E. salsugineum,Fragaria vesca|wild strawberry|F. vesca,Gossypium raimondii||G. raimondii,Linum usitatissimum|flax|L. usitatissimum,Malus domestica|apple|M. domestica,Manihot esculenta|cassava|M. esculenta,Medicago truncatula|barrel medic|M. truncatula,Micromonas pusilla CCMP1545||M. pusilla CCMP1545,Micromonas sp. RCC299||M. sp. RCC299,Mimulus guttatus|spotted monkey flower|M. guttatus,Oryza sativa|rice|O. sativa,Ostreococcus lucimarinus CCE9901||O. lucimarinus CCE9901,Phaseolus vulgaris|string bean|P. vulgaris,Prunus persica|peach|P. persica,Ricinus communis|castor bean|R. communis,Selaginella moellendorffii||S. moellendorffii,Solanum tuberosum|potato|S. tuberosum,Solanum lycopersicum|tomato|S. lycopersicum,Theobroma cacao|cacao|T. cacao,Vitis vinifera|wine grape|V. vinifera,Volvox carteri||V. carteri,Zea mays|maize|Z. mays</c:set>

      <div class="body">
        <p>
          Phytomine contains annotated proteome data from the JGI flagship genomes:
          <ul>
           <c:forTokens items="${flagships}" delims="," var="flagship">

             <c:set var="field" value="${fn:split(flagship,'|')}" />
             <li>
               <em>${field[0]}</em>  - ${field[1]}
            <im:querylink text="Query for all proteins." skipBuilder="true">
                <query name="" model="genomic" view="Protein.primaryIdentifier Protein.organism.shortName Protein.sequence.residues" sortOrder="Protein.primaryIdentifier asc">
                  <constraint path="Protein.organism.shortName" op="=" value="${field[2]}"/>
                </query>
            </im:querylink>
             </li>
           </c:forTokens>
        </ul>

        As well as the proteomes of other JGI and model organism genomes
        <ul>

           <c:forTokens items="${others}" delims="," var="other">

             <c:set var="field" value="${fn:split(other,'|')}" />
             <li>
               <em>${field[0]}</em>  - ${field[1]}
            <im:querylink text="Query for all proteins." skipBuilder="true">
                <query name="" model="genomic" view="Protein.primaryIdentifier Protein.organism.shortName Protein.sequence.residues" sortOrder="Protein.primaryIdentifier asc">
                  <constraint path="Protein.organism.shortName" op="=" value="${field[2]}"/>
                </query>
            </im:querylink>
             </li>
           </c:forTokens>
         </ul>
      </div>
    </td>

    <td valign="top" width="40%">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
      Bulk data files for all organisms in Phytozome are available for download from the 
      <a href="http://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=PhytozomeV10">
      JGI Download Portal </a>.

      </div>
    </td>
  </tr>
</table>
