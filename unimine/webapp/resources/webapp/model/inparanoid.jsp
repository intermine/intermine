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
          <li><I>Plasmodium falciparum 3D7</I></li>
          <li><I>Schizosaccharomyces pombe</I></li>
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
            Orthologues: <i>Plasmodium falciparum 3D7</i> vs <i>Schizosaccharomyces pombe</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Orthologue.object Orthologue.subject Orthologue" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.object" type="Gene">
  </node>
  <node path="Orthologue.object.organism" type="Organism">
  </node>
  <node path="Orthologue.object.organism.taxonId" type="Integer">
    <constraint op="=" value="36329" code="A">
    </constraint>
  </node>
  <node path="Orthologue.subject" type="Gene">
  </node>
  <node path="Orthologue.subject.organism" type="Organism">
  </node>
  <node path="Orthologue.subject.organism.taxonId" type="Integer">
    <constraint op="=" value="4896" code="B">
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
 

