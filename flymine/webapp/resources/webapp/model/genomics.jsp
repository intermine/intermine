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
            <img id='hiddenDiv1Toggle' src="images/undisclosed.gif"/>
            Major data sets ...
          </a>
        </h4>

        <div id="hiddenDiv1" style="display:none;">
          <p>
            <a href="/">FlyMine</a> is a resource aimed at the <i>Drosophila</i> and
            <i>Anopheles</i> research communities hence the focus is on those organisms.
          </p>
          <dl>
            <dt>
              <i>Drosophila melanogaster</i>
            </dt>
            <dd>
              Release 5.1 genome annotation from <a href="http://www.flybase.org">
                <html:img src="model/FlyBase_logo_mini.png"/> </a>.
            </dd>
            <dt>
              <i>Drosophila pseudoobscura</i>
            </dt>
            <dd>
              Release 2.0 genome annotations from
              <a href="http://www.flybase.org">
                <html:img src="model/FlyBase_logo_mini.png"/>
              </a>.
            </dd>
          </dl>
          <dt>
            <i>Anopheles gambiae</i> str. PEST
          </dt> 
          <dd>
            Release 37.3 genome annotations from
            <a href="http://www.ensembl.org/Anopheles_gambiae">
              <html:img src="model/ensembl_logo_mini.png"/>
            </a>.
          </dd>
          <dt>
            <i>Apis mellifera</i>
          </dt>
          <dd>
            Release 37.2d genome annotations from
            <a href="http://www.ensembl.org/Apis_mellifera">
              <html:img src="model/ensembl_logo_mini.png"/>
            </a>.
          </dd>

        </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv2');">
            <img id='hiddenDiv2Toggle' src="images/undisclosed.gif"/>
            Minor data sets ...
          </a>
        </h4>

        <div id="hiddenDiv2" style="display:none;">

          <p>
            More limited information is available
            for <i>C. elegans</i>, <i>S. cerevisiae</i> and others for
            comparison purposes.
          </p>
          <dl>
            <dt><i>Caenorhabditis elegans</i></dt>
            <dd>
              Genome information from <a href="http://www.wormbase.org">WormBase</a>,
              protein interaction data from 
              <a href="http://www.ebi.ac.uk/intact/">IntAct</a>, orthologues and
              paralogues from <a href="http://inparanoid.cgb.ki.se">Inparanoid</a>, GO
              terms, Uniprot and Interpro data.
            </dd>
          </dl>
        </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv3');">
            <img id='hiddenDiv3Toggle' src="images/undisclosed.gif"/>
            EST data sets ...
          </a>
        </h4>

        <div id="hiddenDiv3" style="display:none;">
          <dl>
            <dt><i>A. gambiae</i> EST data set version 8.0</dt>
            <dd>
              Clustered EST data set from the 
              <a href="http://agambiae.vectorbase.org/SequenceData/EST/">Imperial College London Centre for Bioinformatics</a>.
            </dd>
          </dl>
        </div>
      </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv3');">
            <img id='hiddenDiv3Toggle' src="images/undisclosed.gif"/>
            Pathway data ...
          </a>
        </h4>

        <div id="hiddenDiv3" style="display:none;">
          <dl>
            <dt>Pathway data for <i>D. melanogaster</i></dt>
            <dd>
              Pathway data from <a href="http://www.genome.jp/kegg/">KEGG</a> for <i>D. melanogaster</i> genes.</dt>
            </dd>
          </dl>
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
