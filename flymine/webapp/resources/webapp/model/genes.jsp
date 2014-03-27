<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- genomics.jsp -->
<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download
      </div>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <div class="body">

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv1');">
            <img id='hiddenDiv1Toggle' src="images/disclosed.gif" title="Click here to view the datasets" />
            Major data sets ...
          </a>
        </h4>

        <div id="hiddenDiv1" class="dataSetDescription">
          <p>
            <a href="/">FlyMine</a> is a resource aimed at the <i>Drosophila</i> and
            <i>Anopheles</i> research communities hence the focus is on those organisms.
          </p>
         <ul>
         <li>
            <dt>
              <i>Drosophila</i> - Genome annotation for D. melanogaster (R5.23), D. ananassae (R1.3), D. erecta (R1.3), D. grimshawi (R1.3), D. mojavensis (R1.3), D. persimilis (R1.3), D. pseudoobscura pseudoobscura (R2.6), D. sechellia (R1.3), D. simulans (R1.3), D. virilis (R1.2), D. willistoni (R1.3) and D. yakuba (R1.3) from <a href="http://www.flybase.org" target="_new">
                <html:img src="model/images/FlyBase_logo_mini.png" title="Click here to view FlyBase's website"/></a>. Only mapped genes are loaded for D. melanogaster.
            </dt></li>
        <li>
          <dt>
            <i>Anopheles gambiae</i> -  Genome annotation release AgamP3.4 from
            <a href="http://www.ensembl.org/Anopheles_gambiae" target="_new">
              <html:img src="model/images/ensembl_logo_mini.png" title="Click here to view EnSembl's website" />
            </a>.
          </dt></li>
        </ul>
        </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv2');">
            <img id='hiddenDiv2Toggle' src="images/disclosed.gif" title="Click here to view more datasets"/>
            Minor data sets ...
          </a>
        </h4>

        <div id="hiddenDiv2" class="dataSetDescription">

          <p>
            More limited information is available
            for <i>C. elegans</i>, <i>S. cerevisiae</i> and others for
            comparison purposes.
          </p>

            <ul><li><i>Caenorhabditis elegans</i> - Genome information from <a href="http://www.wormbase.org" target="_new">WormBase</a>
            </li></ul>

        </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv3');">
            <img id='hiddenDiv3Toggle' src="images/disclosed.gif" title="Click here to view more datasets"/>
            EST data sets ...
          </a>
        </h4>

        <div id="hiddenDiv3" class="dataSetDescription">
          <dl>
            <ul><li><i>Anopheles gambiae</i> - Clustered EST data set version 8.0 from the
              <a href="http://web.bioinformatics.ic.ac.uk/vectorbase/AnoEST.v8/index.php/" target="_new">Imperial College London Centre for Bioinformatics</a>.
            </dt>
          </li></ul>
        </div>
      </div>
   </td>

    <td width="40%" valign="top">
      <div class="body">
        <ul>

          <li>
            <im:querylink text="All <i>Drosophila melanogaster</i> gene identifiers and chromosomal positions " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i> Drosophila pseudoobscura</i> gene identifiers and chromosomal positions " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.name" type="String">
    <constraint op="=" value="Drosophila pseudoobscura" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i>Anopheles gambiae</i> gene identifiers and chromosomal positions " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.name" type="String">
    <constraint op="=" value="Anopheles gambiae" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>


          <li>
            <im:querylink text="All <i>Anopheles gambiae</i> EST clusters and chromosomal positions" skipBuilder="true">
              <query name="" model="genomic" view="OverlappingESTSet.primaryIdentifier OverlappingESTSet.length OverlappingESTSet.chromosome.primaryIdentifier OverlappingESTSet.chromosomeLocation.start OverlappingESTSet.chromosomeLocation.end">
                <pathDescription pathString="OverlappingESTSet.chromosomeLocation" description="Chromosome location">
                </pathDescription>
                <pathDescription pathString="OverlappingESTSet.chromosome" description="Chromosome">
                </pathDescription>
                <pathDescription pathString="OverlappingESTSet" description="EST cluster">
                </pathDescription>
                <node path="OverlappingESTSet" type="OverlappingESTSet">
                </node>
                <node path="OverlappingESTSet.organism" type="Organism">
                </node>
                <node path="OverlappingESTSet.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>


        </ul>
      </div>
    </td>
  </tr>

</table>
<!-- /genomics.jsp -->