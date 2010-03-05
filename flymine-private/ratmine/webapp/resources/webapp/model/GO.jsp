<!-- GO.jsp -->
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">

 <h4>
   <a href="javascript:toggleDiv('hiddenDiv1');">
    <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
    GO annotation in RatMine ...
   </a>
 </h4>

<div id="hiddenDiv1" class="dataSetDescription">
      <p>
      The Gene Ontology project provides a controlled vocabulary to describe
      gene and gene product attributes in any organism.  The GO collaborators
      are developing three structured, controlled vocabularies (ontologies)
      that describe gene products in terms of their associated biological
      processes, cellular components and molecular functions in a species-independent manner.
      </p>
      <p>
      RatMine has GO annotations for <i>R. norvigicus</i>.
      </p>
        <ul>
         <li><i>R. norvegicus</i> - GO annotations for <i>R. norvegicus</i> gene products assigned by <a href="http://rgd.mcw.edu" target="_new">Rat Genome Database</a></li>
       </ul>
 </div>


 <h4>
   <a href="javascript:toggleDiv('hiddenDiv2');">
    <img id='hiddenDiv2Toggle' src="images/disclosed.gif"/>
    Evidence Codes ...
   </a>
 </h4>

<div id="hiddenDiv2"  class="dataSetDescription">

      <p> Every GO annotation indicates the type of evidence that
      supports it; these evidence codes correspond to broad categories
      of experimental or other support. The codes are listed below. For more
      information, go to <a href="http://www.geneontology.org/GO.evidence.shtml" target="_new">Guide to GO Evidence Codes</a>. </p>
    <p> EXP = Inferred from Experiment </p>
    <p> IDA = Inferred from Direct Assay </p>
    <p> IPI = Inferred from Physical Interaction </p>
    <p> IMP = Inferred from Mutant Phenotype </p>
    <p> IGI = Inferred from Genetic Interaction </p>
    <p> IEP = Inferred from Expression Pattern </p>
    <p> ISS = Inferred from Sequence or Structural Similarity </p>
    <p> ISO = Inferred from Sequence Orthology </p>
    <p> ISA = Inferred from Sequence Alignment </p>
    <p> ISM = Inferred from Sequence Model </p>
    <p> IGC = Inferred from Genomic Context </p>
    <p> RCA = inferred from Reviewed Computational Analysis </p>
    <p> TAS = Traceable Author Statement </p>
    <p> NAS = Non-traceable Author Statement </p>
    <p> IC = Inferred by Curator </p>
    <p> ND = No biological Data available </p>
    <p> IEA = Inferred from Electronic Annotation </p>
    <p> NR = Not Recorded </p>
     <br/>
    </div>



 <h4>
   <a href="javascript:toggleDiv('hiddenDiv3');">
    <img id='hiddenDiv3Toggle' src="images/disclosed.gif"/>
    Qualifiers ...
   </a>
 </h4>

<div id="hiddenDiv3"  class="dataSetDescription">

      <p> The Qualifier column is used for flags that modify the
      interpretation of an annotation. Allowable values are
      'contributes_to', 'colocalizes_with' and 'NOT'.
      'colocalizes_with' is used only with cellular component terms.
      'contributes_to' is used only with molecular function terms.
      'NOT' is used with terms from any of the three ontologies.
  </p>
     </div>
    </div>
   </td>



    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
        <ul>

         <li>
            <im:querylink text="All gene/GO annotation pairs from <i>Rattus Norvegicus</i>" skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.symbol Gene.name Gene.goAnnotation.ontologyTerm.name Gene.goAnnotation.ontologyTerm.description Gene.goAnnotation.ontologyTerm.identifier" sortOrder="Gene.primaryIdentifier asc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.goAnnotation" type="GOAnnotation">
  </node>
  <node path="Gene.goAnnotation.ontologyTerm" type="GOTerm">
  </node>
</query>
            </im:querylink>
        </li>

       </ul>
      </div>
    </td>
  </tr>
</table>
<!-- /GO.jsp -->