<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
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
        Bulk download MilkMine data
      </div>
    </td>
  </tr>
  <tr>
    <td>
      <div class="body">
        <p>
          <a href="http://bifx1.bio.ed.ac.uk:8080/query/begin.do">MilkMine</a> contains several genomes, with annotation, from Ensembl:
        </p>
        <ul>
          <li>
            <a href="http://www.ensembl.org/Homo_sapiens/">
              <html:img src="model/ensembl_logo_mini.png"/>
              Ensembl <i>H. sapiens</i> Release 36 genome annotation</a>
          </li>
          <li>
            <a href="http://www.ensembl.org/Bos_taurus/">
              <html:img src="model/ensembl_logo_mini.png"/>
              Ensembl <i>B. taurus</i> Release 2.0 genome annotation</a>
          </li>
          <li>
            <a href="http://www.ensembl.org/Mus_musculus/">
              <html:img src="model/ensembl_logo_mini.png"/>
              Ensembl <i>M. musculus</i> Release 36 genome annotation</a>
          </li>
        </ul>
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All <i>H. sapiens</i> genes (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Homo sapiens" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>H. sapiens</i> gene identifiers, chromosome positions and chromosome identifiers (for export)" skipBuilder="true">
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Homo sapiens" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
	  <li>
            <im:querylink text="All <i>B. taurus</i> genes (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Bos taurus" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>B. taurus</i> gene identifiers, chromosome positions and chromosome identifiers (for export)" skipBuilder="true">
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Bos taurus" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
	  <li>
            <im:querylink text="All <i>M. musculus</i> genes (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Mus musculus" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>
          <li>
            <im:querylink text="All <i>M. musculus</i> gene identifiers, chromosome positions and chromosome identifiers (for export)" skipBuilder="true">
              <query name="" model="genomic" view="Gene.identifier Gene.name Gene.organismDbId Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Mus musculus" description="" identifier="" code="A">
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
