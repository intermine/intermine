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
    GO annotation in FlyMine ...
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
      FlyMine has GO annotations for <i>Drosophila</i>,
      <i>C. elegans</i> and <i>A. gambiae</i>.  GO annotation for
      other organisms is also included and is accessible via orthologues.
      </p>
        <ul>
         <li><i>D. melanogaster</i> - GO annotations for <i>D. melanogaster</i> gene products assigned by <a href="http://www.flybase.org" target="_new">FlyBase</a></li><br/>
         <li><i>A. gambiae</i> - GO annotations for <i>A. gambiae</i> gene products assigned by the <a href="http://www.ebi.ac.uk/GOA/" target="_new">GO annotation@EBI</a> project.</li><br/>
         <li><i>C. elegans</i> - GO annotations for <i>C. elegans</i> gene products assigned by <a href="http://www.wormbase.org" target="_new">WormBase</a>.</li><br/>
         <li><i>S. cerevisiae</i> - GO annotations for <i>S. cerevisiae</i> gene products assigned by <a href="http://www.yeastgenome.org/" target="_new">SGD</a>.</li><br/>
         <li><i>M. musculus</i> - GO annotations for <i>M. musculus</i> gene products assigned by <a href="http://www.informatics.jax.org" target="_new">MGI</a>.</li><br/>
         <li>Protein Domains - GO annotations for InterPro protein domains gene  assigned by <a href="http://www.ebi.ac.uk/interpro/" target="_new">InterPro</a>.</li><br/>
       </ul>
      </div>


 <h4>
   <a href="javascript:toggleDiv('hiddenDiv2');">
    <img id='hiddenDiv2Toggle' src="images/disclosed.gif"/>
    GO Evidence Codes ...
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
    GO Qualifiers ...
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
            <im:querylink text="All gene/GO annotation pairs from <i>D. melanogaster</i> " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.goAnnotation.ontologyTerm.name Gene.goAnnotation.ontologyTerm.identifier Gene.goAnnotation.ontologyTerm.namespace" sortOrder="Gene.primaryIdentifier asc">
  <constraint path="Gene.organism.shortName" op="=" value="D. melanogaster"/>
</query>
</im:querylink>
         </li>

         <li>
           <im:querylink text="All gene/GO annotation pairs from <i>A. gambiae</i> " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.goAnnotation.ontologyTerm.name Gene.goAnnotation.ontologyTerm.identifier Gene.goAnnotation.ontologyTerm.namespace" sortOrder="Gene.primaryIdentifier asc">
  <constraint path="Gene.organism.shortName" op="=" value="A. gambiae"/>
</query>
</im:querylink>
        </li>

        <li>
         <im:querylink text="All GO Term identifiers with names and descriptions " skipBuilder="true">
<query name="" model="genomic" view="GOTerm.identifier GOTerm.namespace GOTerm.name GOTerm.description" sortOrder="GOTerm.identifier asc">
</query>
         </im:querylink>
        </li>

       </ul>
      </div>
    </td>
  </tr>
</table>
<!-- /GO.jsp -->
