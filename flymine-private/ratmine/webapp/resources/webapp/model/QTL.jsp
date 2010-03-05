<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
    <td>
      <div class="body">
        <!--insert hidden here -->
	  <h4>
		<a href="javascript:toggleDiv('hiddenDiv1');">
		<img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
		QTLs in RatMine ...
		</a>
	  </h4>

	  <div id="hiddenDiv1" class="dataSetDescription">
      <p>
      A Quantitiave Trait Loci are stretches of DNA that are associated with an inherited trait. According to Sequence Ontology a QTL is "a polymorphic locus which contains alleles that differentially affect the expression of a continuously distributed phenotypic trait. Usually it is a marker described by statistical association to quantitative variation in the particular phenotypic trait that is thought to be controlled by the cumulative action of alleles at multiple loci".
      </p>
      <p>
      RatMine has QTLs for <i>R. norvigicus</i>.
      </p>
        <ul>
         <li><i>R. norvegicus</i> - QTLs for <i>R. norvegicus</i> currated by <a href="http://rgd.mcw.edu" target="_new">Rat Genome Database</a></li>
		 <li><i>R. norvegicus</i> - QTL FASTA sequences from <a href="http://hgdownload.cse.ucsc.edu/goldenPath/rn4/bigZips/">UC Santa Cruz</a></li>
		 <li><i>R. norvegicus</i> - GFF3 for QTL features provided by <a href="http://rgd.mcw.edu" target="_new">Rat Genome Database</a></li>
		</ul>
		</div>
		<!-- done hidden -->
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All <i>Rattus norvegicus</i> QTLs (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Qtl.primaryIdentifier Qtl.symbol Qtl.trait Qtl.lod Qtl.pValue Qtl:chromosome.primaryIdentifier Qtl:chromosomeLocation.start Qtl:chromosomeLocation.end" sortOrder="Qtl.primaryIdentifier asc">
					<node path="Qtl" type="Qtl">
					</node>
					<node path="Qtl:chromosome" type="Chromosome">
					</node>
				</query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </td>
  </tr>
</table>
