<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<TABLE width="100%">
  <TR>
    <TD valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <p>
          Orthologue and paralogue relationships calculated by <A
          href="http://inparanoid.cgb.ki.se/index.html">InParanoid</A> (latest
          calculated 16th April 2005) between the following organisms:
        </p>
        <ul>
          <li><I>M. musculus</I></li>
          <li><I>H. sapiens</I></li>
        </ul>
      </div>
    </TD>

    <TD width="45%" valign="top">
      <div class="heading2">
       Bulk download
      </div>
      <div class="body">
        <ul>
          <li>
            Orthologues: <i>M. musculus</i> vs <i>H. sapiens</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Orthologue.object Orthologue.subject Orthologue" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.object" type="Gene">
  </node>
  <node path="Orthologue.object.organism" type="Organism">
  </node>
  <node path="Orthologue.object.organism.taxonId" type="String">
    <constraint op="=" value="10090" code="A">
    </constraint>
  </node>
  <node path="Orthologue.subject" type="Gene">
  </node>
  <node path="Orthologue.subject.organism" type="Organism">
  </node>
  <node path="Orthologue.subject.organism.taxonId" type="String">
    <constraint op="=" value="9606" code="B">
    </constraint>
  </node>
</query>
</im:querylink>
          </li>
        </ul>
      </div>
    </TD>
  </TR>
</TABLE>
 

