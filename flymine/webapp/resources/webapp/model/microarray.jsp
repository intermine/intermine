<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>


<div class="heading2">
Current data
</div>

<div class="body">
<h4>
  <a href="javascript:toggleDiv('hiddenDiv1');">
    <img id='hiddenDiv1Toggle' src="images/undisclosed.gif"/>
     <i>D. melanogaster</i>  - Microarray-based gene expression data from FlyAtlas ...
  </a>
</h4>

<div id="hiddenDiv1" style="display:none;">

<dt>An affymetrix microarray-based atlas of gene expression in the adult <i>Drosophila</i> fly from <a href="http://www.flyatlas.org/">FlyAtlas</a>.</dt>
<dd>
This approach has proven valuable in understanding the function of the Malpighian tubule and was reported in Wang et al (2004) Genome Biol 5(9):R69 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=15345053">PubMed: 15345053</a>) - Function-informed transcriptome analysis of <i>Drosophila</i> renal tubule.
</dd>

<br/> 

</div>

<h4>
  <a href="javascript:toggleDiv('hiddenDiv2');">
    <img id='hiddenDiv2Toggle' src="images/undisclosed.gif"/>
      <i>D. melanogaster</i>  - Experiments performed in the FlyChip facility and submitted to ArrayExpress ...
 </a>
</h4>

<div id="hiddenDiv2" style="display:none;"> 

<p>
Bate M - Identification of genes regulated by synaptic transmission in embryos - ArrayExpress: <a href="http://www.ebi.ac.uk/arrayexpress/query/result?queryFor=Experiment&eAccession=E-FLYC-2">E-FLYC-2</a>
</p>

<p>
Papafotiou G - Identification of changes in gene expression due to Wolbachia infection - ArrayExpress: <a href="http://www.ebi.ac.uk/arrayexpress/query/result?queryFor=Experiment&eAccession=E-FLYC-3">E-FLYC-3</a></DT> 
</p>

<p>
Whitfield W - Identification of aberrant gene expression in the presence of mutant CP190 protein - ArrayExpress: <a href="http://www.ebi.ac.uk/arrayexpress/query/result?queryFor=Experiment&eAccession=E-FLYC-1">E-FLYC-1</a>
</p>

<br/> 

</div>

<h4>
  <a href="javascript:toggleDiv('hiddenDiv3');">
    <img id='hiddenDiv3Toggle' src="images/undisclosed.gif"/>
     <i>D. melanogaster</i>  - Gene expression data from ArrayExpress ...
   </a>
</h4>

<div id="hiddenDiv3" style="display:none;"> 

<dt>
Arbeitman et al (2002) Science 297:2270-2275 (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=12351791">PubMed: 12351791</a>) - Gene Expression During the Life Cycle of <i>Drosophila melanogaster</i> - ArrayExpress: <a href="http://www.ebi.ac.uk/arrayexpress/query/result?queryFor=Experiment&eAccession=E-FLYC-6">E-FLYC-6</a>
</dt>
<dd>
Arbeitman et al reported gene expression patterns for nearly one third of all <i>Drosophila</i> genes during a complete time course of development.  Graphs are displayed on summary pages for each gene involved in the experiment showing Log 2 exression ratio for 67 time points across life stages.  For more infomation refer to the <a href="http://genome.med.yale.edu/Lifecycle/">White Lab</a> page at Yale.
</dd>

<i>An example graph showing expression of the gene 'big brain'.</i>
<br/>
<img style="border: 1px solid black" src="model/big_brain_expression.png"/>
</div>

</div>
