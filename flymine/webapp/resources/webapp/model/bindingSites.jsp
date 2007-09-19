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
        <dl>
          <h4>
            <a href="javascript:toggleDiv('hiddenDiv1');">
              <img id='hiddenDiv1Toggle' src="images/undisclosed.gif"/>
              <i>D. melanogaster</i>  - Regulatory elements ...
            </a>
          </h4>

          <div id="hiddenDiv1" style="display:none;" class="dataSetDescription">


            <ul><li><dt>
              Transcriptional  <i>cis</i>-regulatory modules (CRMs) for <i>D. melanogaster</i>
              from the <a href="http://redfly.ccr.buffalo.edu" target="_new"> REDfly database </a>.
            </dt>


            <dt>
              Note: The REDfly data has been re-mapped to
              genome sequence release 5.0 as of FlyMine release 7.0.
            </dt></li></ul>

            <ul><li><dt>
              Regulatory elements for <i>D. melanogaster</i> annotated by
              <a href="http://flybase.bio.indiana.edu" target="_new">FlyBase</a>.
            </dt></li></ul>

          </div>

          <h4>
            <a href="javascript:toggleDiv('hiddenDiv2');">
              <img id='hiddenDiv2Toggle' src="images/undisclosed.gif" title="Click here to view the binding sites" />
              <i>D. melanogaster</i>  - Binding sites ...
            </a>
          </h4>

          <div id="hiddenDiv2" style="display:none;" class="dataSetDescription">

          <ul><li>
            <dt>DNase I footprints for <i>D. melanogaster</i> from the
              <a href="http://www.flyreg.org" target="_new"> DNase I footprint database (V2.0)</a>.</dt>
             <dt>Note: The FlyReg data has been re-mapped to genome sequence release 5.0 as of FlyMine release 7.0.</dt></li></ul>

          <ul><li>
            <dt><a href="http://servlet.sanger.ac.uk/tiffin/" target="_new">Tiffin</a> is a database of predicted regulatory motifs and predicted functional sites ("motif instances") on genome sequences.</dt>
            <dt>Note: The Tiffin-1.2 data has been re-mapped to genome sequence release 5.0 as of FlyMine release 7.0.</dt></li></ul>


          </div>
        </dl>
      </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
        <ul>

          <li>
            <im:querylink text="REDfly regulatory regions " skipBuilder="true">
              <query name="" model="genomic" view="TFmodule.identifier TFmodule.length TFmodule.chromosome.identifier TFmodule.chromosomeLocation.start TFmodule.chromosomeLocation.end TFmodule.gene.identifier TFmodule.elementEvidence">
                <node path="TFmodule" type="TFmodule">
                </node>
                <node path="TFmodule.evidence" type="DataSet">
                </node>
                <node path="TFmodule.evidence.title" type="String">
                  <constraint op="LIKE" value="%REDfly%" description="" identifier="" editable="true" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="FlyBase regulatory regions " skipBuilder="true">
              <query name="" model="genomic" view="RegulatoryRegion.identifier RegulatoryRegion.length RegulatoryRegion.chromosome.identifier RegulatoryRegion.chromosomeLocation.start RegulatoryRegion.chromosomeLocation.end">
                <node path="RegulatoryRegion" type="RegulatoryRegion">
                </node>
                <node path="RegulatoryRegion.evidence" type="DataSet">
                </node>
                <node path="RegulatoryRegion.evidence.title" type="String">
                  <constraint op="LIKE" value="FlyBase%" description="" identifier="" code="A">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="FlyReg transcription factor binding sites " skipBuilder="true">
              <query name="" model="genomic" view="TFBindingSite.identifier TFBindingSite.length TFBindingSite.gene.identifier TFBindingSite.factor.identifier TFBindingSite.chromosome.identifier TFBindingSite.chromosomeLocation.start TFBindingSite.chromosomeLocation.end">
                <node path="TFBindingSite" type="TFBindingSite">
                </node>
                <node path="TFBindingSite.evidence" type="DataSet">
                </node>
                <node path="TFBindingSite.evidence.title" type="String">
                  <constraint op="=" value="FlyReg data set">
                  </constraint>
                </node>
              </query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="Transcription factor binding sites predicted by Tiffin"
                          skipBuilder="true">
              <query name="" model="genomic" view="TFBindingSite.identifier TFBindingSite.length TFBindingSite.chromosome.identifier TFBindingSite.chromosomeLocation.start TFBindingSite.chromosomeLocation.end TFBindingSite.motif.identifier">
                <pathDescription pathString="TFBindingSite.motif" description="Motif"/>
                <pathDescription pathString="TFBindingSite.chromosomeLocation" description="Chromosome location"/>
                <pathDescription pathString="TFBindingSite.chromosome" description="Chromosome"/>
                <pathDescription pathString="TFBindingSite" description="Binding site"/>
                <node path="TFBindingSite" type="TFBindingSite">
                </node>
                <node path="TFBindingSite.evidence" type="DataSet">
                </node>
                <node path="TFBindingSite.evidence.title" type="String">
                  <constraint op="=" value="Tiffin" description="" identifier="" code="A">
                  </constraint>
                </node>
                <node path="TFBindingSite.organism" type="Organism">
                </node>
                <node path="TFBindingSite.organism.name" type="String">
                </node>
              </query>
            </im:querylink>
          </li>

        </ul>
      </div>
    </td>
  </tr>
</table>
