<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Major genomes
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Dataset links
      </div>
    </td>
  </tr>
  <tr>
    <td>
      <div class="body">
        <p>
          <a href="/">FlyMine</a> is a resource aimed at the Drosophila and
          Anopheles research communities hence the focus is on those organisms.
        <dl>
          <dt><i>Drosophila melanogaster</i> - <a href="http://www.flybase.org"><html:img src="model/FlyBase_logo_mini.png"/></a></dt>
          <dd>
            Release 4.2 genome annotation from <A href="http://flybase.bio.indiana.edu"> 
            FlyBase</a>.
          </dd>
          <dt><i>Anopheles gambiae</i> str. PEST - <a href="http://www.ensembl.org/Anopheles_gambiae"><html:img src="model/ensembl_logo_mini.png"/></a></dt>
          <dd>
            Release 2b genome annotations from <a href="http://www.ensembl.org/Anopheles_gambiae/index.html">Ensembl</a>.
          </dd>
        </dl>
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All Drosophila genes (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene">
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
            <im:querylink text="All Drosophila gene identifiers, chromosome positions and chromosome identifiers (for export)" skipBuilder="true">
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
            <im:querylink text="All Anopheles genes (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene">
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
            <im:querylink text="All Anopheles gene identifiers, chromosome positions and chromosome identifiers (for export)" skipBuilder="true">
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
        </ul>
      </div>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <div class="heading2">
        Minor datasets
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Dataset links
      </div>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <div class="body">
        <p>
          More limited information is available
          for <i>C. elegans</i>, <i>S. cerevisiae</i> and others for
          comparison purposes.
        </p>
        <dl>
          <dt><i>Caenorhabditis elegans</i></dt>
          <dd>
            Genome information from <a href="www.wormbase.org">WormBase</a>,
            protein interaction data from 
            <a href="http://www.ebi.ac.uk/intact/">intAct</a>, orthologues and
            paralogues from <a href="http://inparanoid.cgb.ki.se">Inparanoid</a>, GO
            terms, Uniprot and Interpro data
          </dd>
        </dl>
      </div>
    </td>
    <td width="4%" valign="top">
      <div class="body">
        <ul>
          <li>
            <im:querylink text="Caenorhabditis elegans genes (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="Caenorhabditis elegans gene identifiers, chromosome positions and chromosome identifiers (for export)" skipBuilder="true">
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Caenorhabditis elegans" description="" identifier="" code="A">
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
