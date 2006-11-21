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

 <h4>
   <a href="javascript:toggleDiv('hiddenDiv1');">
    <img id='hiddenDiv1Toggle' src="images/undisclosed.gif"/>
    GO annotation in FlyMine ...
   </a>
 </h4>

<div id="hiddenDiv1" style="display:none;">
      <p>
      The Gene Ontology project provides a controlled vocabulary to describe 
      gene and gene product attributes in any organism.  The GO collaborators 
      are developing three structured, controlled vocabularies (ontologies) 
      that describe gene products in terms of their associated biological 
      processes, cellular components and molecular functions in a species-independent manner.
      </p>
<br/>
      <p>
      FlyMine has GO annotations for <i>Drosophila</i>,
      <i>C. elegans</i> and <i>A. gambiae</i>.  GO annotation for
      other organisms is also included and is accessible via orthologues.
      </p>      
        <ul>
         <li><i>D. melanogaster</i> - GO annotations for <i>D. melanogaster</i> gene products assigned by <a href="http://www.ebi.ac.uk/GOA/">FlyBase</a>, updated 26th March 2006.</li><br/>
         <li><i>A. gambiae</i> - GO annotations for <i>A. gambiae</i> gene products assigned by the <a href="http://www.ebi.ac.uk/GOA/">GO annotation@EBI</a> project, updated 4th March 2006.</li><br/>
         <li><i>C. elegans</i> - GO annotations for <i>C. elegans</i> gene products assigned by <a href="http://www.ebi.uniprot.org/index.shtml">UniProt</a>, updated 12th March 2006.</li><br/>
         <li><i>S. cerevisiae</i> - GO annotations for <I>S. cerevisiae</i> gene products assigned by <a href="http://www.yeastgenome.org/">SGD</a>, updated 19th April 2006.</li><br/>
         <li><i>M. musculus</i> - GO annotations for <I>M. musculus</i> gene products assigned by <a href="http://www.informatics.jax.org">MGI</a>, updated 15th April 2006.</li><br/>
         <li><i>R. norvegicus</i> - GO annotations for <i>R. norvegicus</i> gene products assigned by <a href="http://rgd.mcw.edu/">RGD</a>, updated 26th March 2006.</li><br/>
       </ul>
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
             <query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.goAnnotation.identifier Gene.goAnnotation.name">
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
           <im:querylink text="All gene/GO annotation pairs from <i>A. gambiae</i> " skipBuilder="true">
            <query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.annotations.identifier Gene.annotations.name">
             <node path="Gene" type="Gene">
              </node>
             <node path="Gene.organism" type="Organism">
              </node>
             <node path="Gene.organism.name" type="String">
             <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="A">
            </constraint>
           </node>
            <node path="Gene.annotations" type="GOAnnotation">
           </node>
          </query>
         </im:querylink>
        </li>

       </ul>
      </div>
    </td>
  </tr>
</table>
