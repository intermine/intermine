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

	<p>
	Note: The DrosDel data has been re-mapped to
	genome sequence release 5.0 as of FlyMine release 7.0,
	however, coordinates for the Exelixis set are still to genomce
	sequence release 4.0.  These will be updated to release 5.0 in the
	next release of FlyMine.
	</p>

        <p>
          <a href="http://www.drosdel.org.uk">DrosDel</a> is a collection
          of <i>P</i>-element insertions for generating custom chromosomal aberrations
          in <i>D. melanogaster</i>.  The locations of the <i>P</i>-element insertions
          and the deletions that can be constructed from them have been loaded into
          FlyMine.  Constructed deletions are tagged as available in FlyMine. Stocks 
          can be ordered from the <a href="http://expbio.bio.u-szeged.hu/fly/index.php">Szeged stock centre</a>.
        </p>
        <p>
          The DrosDel collection has been reported in Ryder et al (2004)
          Genetics 167: 797-813
          (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15238529">PubMed: 15238529</a>) 
          - The DrosDel collection: a set of P-element insertions for generating custom chromosomal aberrations in <i>Drosophila melanogaster</i>.
        </p>

        <p>FlyMine has additional insertions from <a href="http://www.flybase.org/">FlyBase</a> and from the <a href="http://drosophila.med.harvard.edu">Exelixis</a> collection.</p>

      </div>
    </td>
    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
        <ul>

          <li>
            <im:querylink text="All DrosDel deletions " skipBuilder="true">
              <query name="" model="genomic" view="ArtificialDeletion.identifier ArtificialDeletion.available ArtificialDeletion.chromosome.identifier ArtificialDeletion.chromosomeLocation.start ArtificialDeletion.chromosomeLocation.end">
                <node path="ArtificialDeletion" type="ArtificialDeletion">
                </node>
                <node path="ArtificialDeletion.organism" type="Organism">
                </node>
                <node path="ArtificialDeletion.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All available DrosDel deletions " skipBuilder="true">
              <query name="" model="genomic" view="ArtificialDeletion.identifier ArtificialDeletion.chromosome.identifier ArtificialDeletion.chromosomeLocation.start ArtificialDeletion.chromosomeLocation.end">
                <node path="ArtificialDeletion" type="ArtificialDeletion">
                </node>
                <node path="ArtificialDeletion.organism" type="Organism">
                </node>
                <node path="ArtificialDeletion.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
                  </constraint>
                  <node path="ArtificialDeletion.available" type="Boolean">
                    <constraint op="=" value="true" description="" identifier="" code="B">
                    </constraint>
                  </node>
               </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All DrosDel insertions " skipBuilder="true">
            <query name="" model="genomic" view="TransposableElementInsertionSite.identifier TransposableElementInsertionSite.type TransposableElementInsertionSite.subType TransposableElementInsertionSite.chromosome.identifier TransposableElementInsertionSite.chromosomeLocation.start TransposableElementInsertionSite.chromosomeLocation.end" constraintLogic="A and B">
             <node path="TransposableElementInsertionSite" type="TransposableElementInsertionSite">
              </node>
             <node path="TransposableElementInsertionSite.organism" type="Organism">
              </node>
             <node path="TransposableElementInsertionSite.organism.name" type="String">
               <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
               </constraint>
              </node>
             <node path="TransposableElementInsertionSite.evidence" type="DataSet">
              </node>
             <node path="TransposableElementInsertionSite.evidence.title" type="String">
              <constraint op="LIKE" value="%DrosDel%" description="" identifier="" code="B">
              </constraint>
             </node>
            </query>
           </im:querylink>
          </li>

        <li>
            <im:querylink text="All P-element insertions (including Exelixis and DrosDel)" skipBuilder="true">
              <query name="" model="genomic" view="TransposableElementInsertionSite.identifier TransposableElementInsertionSite.type TransposableElementInsertionSite.subType TransposableElementInsertionSite.chromosome.identifier TransposableElementInsertionSite.chromosomeLocation.start TransposableElementInsertionSite.chromosomeLocation.end">
                <node path="TransposableElementInsertionSite" type="TransposableElementInsertionSite">
                </node>
                <node path="TransposableElementInsertionSite.organism" type="Organism">
                </node>
                <node path="TransposableElementInsertionSite.organism.name" type="String">
                  <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
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
