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
		Genes in RatMine ...
		</a>
	  </h4>

	  <div id="hiddenDiv1" class="dataSetDescription">
      <p>
      A gene is the basic unit of heredity in a living organism. Genes hold the information to build and maintain their cells and pass genetic traits to offspring. According to Sequence Ontology a gene is "A region (or regions) that includes all of the sequence elements necessary to encode a functional transcript. A gene may include regulatory regions, transcribed regions and/or other functional sequence regions".
      </p>
      <p>
      RatMine has Genes for <i>R. norvigicus</i>.
      </p>
        <ul>
         <li><i>R. norvegicus</i> - Genes for <i>R. norvegicus</i> currated by <a href="http://rgd.mcw.edu" target="_new">Rat Genome Database</a></li>
		 <li><i>R. norvegicus</i> - Gene FASTA sequences from <a href="http://hgdownload.cse.ucsc.edu/goldenPath/rn4/bigZips/">UC Santa Cruz</a></li>
		 <li><i>R. norvegicus</i> - GFF3 for gene features provided by <a href="http://rgd.mcw.edu" target="_new">Rat Genome Database</a></li>
		</ul>
		</div>
		<!-- done hidden -->
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All <i>Rattus norvegicus</i> genes (browse)" skipBuilder="true">
              <query name="" model="genomic" view="Gene.secondaryIdentifier Gene.name Gene.primaryIdentifier Gene.symbol Gene.chromosome.identifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end">
                <node path="Gene" type="Gene">
                </node>
                <node path="Gene.organism" type="Organism">
                </node>
                <node path="Gene.organism.name" type="String">
                  <constraint op="=" value="Rattus norvegicus" description="" identifier="" code="A">
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
