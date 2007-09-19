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
            <img id='hiddenDiv1Toggle' src="images/undisclosed.gif" title="Click here to view the datasets" />
            Major data sets ...
          </a>
        </h4>

        <div id="hiddenDiv1" style="display:none;" class="dataSetDescription">
          <p>
            <a href="/">FlyMine</a> is a resource aimed at the <i>Drosophila</i> and
            <i>Anopheles</i> research communities hence the focus is on those organisms.
          </p>
         <ul>
         <li>
            <dt>
              <i>Drosophila melanogaster</i> - Genome annotation release 5.1 from <a href="http://www.flybase.org" target="_new">
                <html:img src="images/FlyBase_logo_mini.png" title="Click here to view FlyBase's website" /> </a>.
            </dt></dl>
          <li>
            <dt>
              <i>Drosophila pseudoobscura</i> - Genome annotation release 2.0 from
              <a href="http://www.flybase.org" target="_new">
                <html:img src="images/FlyBase_logo_mini.png" title="Click here to view FlyBase's website"/></a>.
            </dt></li>
        <li>
          <dt>
            <i>Anopheles gambiae</i> str. PEST -  Genome annotation release AgamP3 from
            <a href="http://www.ensembl.org/Anopheles_gambiae" target="_new">
              <html:img src="images/ensembl_logo_mini.png" title="Click here to view EnSembl's website" />
            </a>.
          </dt></li>
         <li>
          <dt>
            <i>Apis mellifera</i> - Genome annotation release 2 from
            <a href="http://www.ensembl.org/Apis_mellifera" target="_new">
              <html:img src="images/ensembl_logo_mini.png" title="Click here to view EnSembl's website"/>
            </a>.
          </dt></li>
        </ul>
        </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv2');">
            <img id='hiddenDiv2Toggle' src="images/undisclosed.gif" title="Click here to view more datasets"/>
            Minor data sets ...
          </a>
        </h4>

        <div id="hiddenDiv2" style="display:none;" class="dataSetDescription">

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
            <img id='hiddenDiv3Toggle' src="images/undisclosed.gif" title="Click here to view more datasets"/>
            EST data sets ...
          </a>
        </h4>

        <div id="hiddenDiv3" style="display:none;" class="dataSetDescription">
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
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
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
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
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
            <im:querylink text="All <i>Anopheles gambiae </i> str. PEST gene identifiers and chromosomal positions " skipBuilder="true">
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i>Apis mellifera</i> gene identifiers and chromosomal positions " skipBuilder="true">
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Apis mellifera" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i>A. gambiae</i> EST clusters from Imperial College"
 skipBuilder="true">
              <query name="" model="genomic" view="ESTCluster.identifier ESTCluster.length ESTCluster.chromosome.identifier ESTCluster.chromosomeLocation.start ESTCluster.chromosomeLocation.end">
                <pathDescription pathString="ESTCluster.chromosomeLocation" description="Chromosome location">
                </pathDescription>
                <pathDescription pathString="ESTCluster.chromosome" description="Chromosome">
                </pathDescription>
                <pathDescription pathString="ESTCluster" description="EST cluster">
                </pathDescription>
                <node path="ESTCluster" type="ESTCluster">
                </node>
                <node path="ESTCluster.organism" type="Organism">
                </node>
                <node path="ESTCluster.organism.name" type="String">
                  <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="A">
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
