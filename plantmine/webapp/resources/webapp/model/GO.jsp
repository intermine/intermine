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
          <DT><I>Plasmodium falciparum 3D7</I></DT>
          <DD>
            GO annotations for <I>Plasmodium falciparum 3D7</I> gene products assigned by <a href="http://www.informatics.jax.org">MGI</a>, updated 15th April 2006.<BR/>
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
            All gene/GO annotation pairs from <i>Plasmodium falciparum 3D7</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Gene Gene.goAnnotation">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.name" type="String">
    <constraint op="=" value="Plasmodium falciparum 3D7" description="" identifier="" code="A">
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
    <constraint op="=" value="Plasmodium falciparum 3D7" description="" identifier="" code="A">
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
