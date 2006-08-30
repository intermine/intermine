<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td>
      <div class="heading2">
        GO annotation in MilkMine
      </div>
      <div class="body">
        <DL>
          <P>
      The GO collaborators are developing three structured, controlled
      vocabularies (ontologies) that describe gene products in terms of
      their associated biological processes, cellular components and
      molecular functions in a species-independent manner.
<br>&nbsp<br>
      The GO annotations are 'attached' to
      genes, proteins and peptides and so each entity has a reference (link) to a list of its GO
      annotations (if available).
          </P>
          <DT><I>GO annotations</I></DT>
          <DD>
	    <ul>
             <li><I>H. sapiens</I> annotations assigned by <a href="http://www.ebi.ac.uk/GOA/">GO annotation@EBI</a>, updated 15th April 2006.</li>
             <li><I>B. taurus</I> annotations assigned by <a href="http://www.ebi.ac.uk/GOA/">GO annotation@EBI</a>, updated 15th April 2006.</li>
	     <li><I>M. musculus</I> annotations assigned by <a href="http://www.informatics.jax.org">MGI</a>, updated 15th April 2006.</li>
	    </ul>
          </DD>
        </DL>
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download MilkMine data
      </div>
      <div class="body">
        <ul>
          <li>
            All gene/GO annotation pairs from <i>H. sapiens</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Gene Gene.goAnnotation">
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
            <im:querylink text="(export)" skipBuilder="true">
<query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.goAnnotation.identifier Gene.goAnnotation.name Gene.goAnnotation.qualifier">
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
            All gene/GO annotation pairs from <i>B. taurus</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Gene Gene.goAnnotation">
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
            <im:querylink text="(export)" skipBuilder="true">
<query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.goAnnotation.identifier Gene.goAnnotation.name Gene.goAnnotation.qualifier">
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
            All gene/GO annotation pairs from <i>M. musculus</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Gene Gene.goAnnotation">
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
            <im:querylink text="(export)" skipBuilder="true">
<query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.goAnnotation.identifier Gene.goAnnotation.name Gene.goAnnotation.qualifier">
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
