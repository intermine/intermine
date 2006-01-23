<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table>
  <tr>
    <td valign="top">
      <div class="heading2">
        Current Data
      </div>
      <div class="body">
        <dt>
          <dt><i>D. melanogaster</i></dt>
          <dd>
            Release 4.2 genome annotation from <A href="http://flybase.bio.indiana.edu">FlyBase</a>.
          </dd>
          <dt><i>A. gambiae</i></dt>
          <dd>
            Release 2b genome annotations from <a href="http://www.ensembl.org/Anopheles_gambiae/index.html">Ensembl</a>.
          </dd>
        </dt>
      </div>
    </td>
    <td width="30%" valign="top">
      <div class="heading2">
        Datasets
      </div>
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
</table>
