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
              <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
              <i>D. melanogaster</i>  - <i>Cis</i>-regulatory modules (CRMs) ...
            </a>
          </h4>

          <div id="hiddenDiv1" class="dataSetDescription">


            <ul><li><dt>
              CRMs from the <a href="http://redfly.ccr.buffalo.edu" target="_new"> REDfly database</a>.
            </dt></li></ul>

            <ul><li><dt>
              Enhancers annotated by
              <a href="http://flybase.bio.indiana.edu" target="_new">FlyBase</a>.
            </dt></li></ul>

          </div>

          <h4>
            <a href="javascript:toggleDiv('hiddenDiv2');">
              <img id='hiddenDiv2Toggle' src="images/disclosed.gif" title="Click here to view the binding sites" />
              <i>D. melanogaster</i>  - Binding sites ...
            </a>
          </h4>

          <div id="hiddenDiv2" class="dataSetDescription">

          <ul><li><dt>
             Transcription factor binding sites from the <a href="http://redfly.ccr.buffalo.edu" target="_new"> REDfly database </a>.
          </dt></li></ul>
  
       <ul><li><dt>
            Predicted regulatory motifs and functional sites ("motif instances") on genome sequences from the <a href="http://servlet.sanger.ac.uk/tiffin/" target="_new">Tiffin database</a>.
       </dt></li></ul>

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
            <im:querylink text="REDfly <i>cis</i>-regulatory modules " skipBuilder="true">
<query name="" model="genomic" view="CRM.primaryIdentifier CRM.length CRM.chromosome.primaryIdentifier CRM.chromosomeLocation.start CRM.chromosomeLocation.end CRM.elementEvidence" sortOrder="CRM.primaryIdentifier asc" constraintLogic="A and B">
  <node path="CRM" type="CRM">
  </node>
  <node path="CRM.evidence" type="DataSet">
  </node>
  <node path="CRM.evidence.title" type="String">
    <constraint op="=" value="REDfly Drosophila transcriptional cis-regulatory modules" description="" identifier="" code="B" extraValue="">
    </constraint>
  </node>
  <node path="CRM.organism" type="Organism">
    <constraint op="LOOKUP" value="Drosophila melanogaster" description="" identifier="" code="A" extraValue="">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>


          <li>
            <im:querylink text="REDfly transcription factor binding sites " skipBuilder="true">

<query name="" model="genomic" view="TFBindingSite.primaryIdentifier TFBindingSite.length TFBindingSite.chromosomeLocation.start TFBindingSite.chromosomeLocation.end" sortOrder="TFBindingSite.primaryIdentifier asc" constraintLogic="A and B">
  <node path="TFBindingSite" type="TFBindingSite">
  </node>
  <node path="TFBindingSite.organism" type="Organism">
    <constraint op="LOOKUP" value="Drosophila melanogaster" description="" identifier="" code="B" extraValue="">
    </constraint>
  </node>
  <node path="TFBindingSite.evidence" type="DataSet">
  </node>
  <node path="TFBindingSite.evidence.title" type="String">
    <constraint op="=" value="REDfly Drosophila transcription factor binding sites" description="" identifier="" code="A" extraValue="">
    </constraint>
  </node>
</query>
          </im:querylink>
          </li>

          <li>
            <im:querylink text="FlyBase enhancers " skipBuilder="true">
<query name="" model="genomic" view="Enhancer.primaryIdentifier Enhancer.length Enhancer.chromosome.primaryIdentifier Enhancer.chromosomeLocation.start Enhancer.chromosomeLocation.end" sortOrder="Enhancer.primaryIdentifier asc">
  <node path="Enhancer" type="Enhancer">
  </node>
  <node path="Enhancer.organism" type="Organism">
    <constraint op="LOOKUP" value="Drosophila melanogaster" description="" identifier="" code="A" extraValue="">
    </constraint>
  </node>
</query>
          </im:querylink>
          </li>



          <li>
            <im:querylink text="Transcription factor binding sites predicted by Tiffin"
                          skipBuilder="true">
<query name="" model="genomic" view="TFBindingSite.motif.primaryIdentifier TFBindingSite.primaryIdentifier TFBindingSite.chromosome.primaryIdentifier TFBindingSite.chromosomeLocation.start TFBindingSite.chromosomeLocation.end" sortOrder="TFBindingSite.motif.primaryIdentifier asc">
  <pathDescription pathString="TFBindingSite.chromosome" description="Chromosome">
  </pathDescription>
  <pathDescription pathString="TFBindingSite" description="Binding site">
  </pathDescription>
  <pathDescription pathString="TFBindingSite.chromosomeLocation" description="Chromosome location">
  </pathDescription>
  <pathDescription pathString="TFBindingSite.motif" description="Motif">
  </pathDescription>
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

         <li>
            <im:querylink text="Tiffin motifs associated with expression terms"
                          skipBuilder="true">
<query name="" model="genomic" view="Motif.primaryIdentifier Motif.expressionTerms.name" sortOrder="Motif.primaryIdentifier asc">
</query>
           </im:querylink>
          </li>
        </ul>
      </div>
    </td>

  </tr>
</table>
