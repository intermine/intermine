<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td>
      <div class="heading2">
        GO annotation in FlyMine
      </div>
      <div class="body">
        <DL>
          <P>
      The GO collaborators are developing three structured, controlled
      vocabularies (ontologies) that describe gene products in terms of
      their associated biological processes, cellular components and
      molecular functions in a species-independent manner.
          </P>
      GO annotations for <I>Drosophila</I>, <I>C. elegans</I> and <I>A. gambiae</I> have
      been loaded into FlyMine.  GO annotation for other organisms is also included and
      is accessible via orthologues.
          </P>
          <DT><I>D. melanogaster</I></DT>
          <DD>
            GO annotations for <I>D. melanogaster</I> gene products assigned by FlyBase, updated 26th March 2006.
          </DD>
          <DT><I>A. gambiae</I></DT>
          <DD>
            GO annotations for <I>A. gambiae</I> gene products assigned by the <a href="http://www.ebi.ac.uk/GOA/" target="_new">GO annotation@EBI</a> project, updated 4th March 2006.<BR/>
          </DD>
          <DT><I>C. elegans</I></DT>
          <DD>
            GO annotations for <I>C. elegans</I> gene products assigned by <a href="www.wormbase.org" target="_new">WormBase</a>, updated 12th March 2006.<BR/>
          </DD>
          <DT><I>S. cerevisiae</I></DT>
          <DD>
            GO annotations for <I>S. cerevisiae</I> gene products assigned by <a href="http://www.yeastgenome.org/" target="_new">SGD</a>, updated 19th April 2006.<BR/>
          </DD>
          <DT><I>M. musculus</I></DT>
          <DD>
            GO annotations for <I>M. musculus</I> gene products assigned by <a href="http://www.informatics.jax.org" target="_new">MGI</a>, updated 15th April 2006.<BR/>
          </DD>
          <DT><I>R. norvegicus</I></DT>
          <DD>
            GO annotations for <I>R. norvegicus</I> gene products assigned by <a href="http://rgd.mcw.edu/" target="_new">RGD</a>, updated 26th March 2006.<BR/>
          </DD>
        </DL>
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
        <ul>
          <li>
            All gene/GO annotation pairs from <i>D. melanogaster</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Gene Gene.goAnnotation">
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
            <im:querylink text="(export)" skipBuilder="true">
<query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.goAnnotation.identifier Gene.goAnnotation.name Gene.goAnnotation.qualifier">
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
        </ul>
        <ul>
          <li>
            All gene/GO annotation pairs from <i>A. gambiae</i> 
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Gene Gene.annotations">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.name" type="String">
    <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="Gene.annotations" type="Annotation">
  </node>
  <node path="Gene.annotations.property" type="GOTerm">
  </node>
</query>
            </im:querylink>
            <im:querylink text="(export)" skipBuilder="true">
<query name="" model="genomic" view="Gene.identifier Gene.organismDbId Gene.symbol Gene.annotations.identifier Gene.annotations.name Gene.annotations.qualifier">
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
