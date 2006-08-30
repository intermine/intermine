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
          <li><I>Homo sapiens</I> (Human)</li>
          <li><I>Pan troglodytes</I> (Chimpanzee)</li>
          <li><I>Mus musculus</I> (Mouse)</li>
          <li><I>Rattus norvegicus</I> (Rat)</li>
          <li><I>Canis familiaris</I> (Dog)</li>
        </ul>
      </div>
    </TD>

    <TD width="45%" valign="top">
      <div class="heading2">
       Bulk download MilkMine data
      </div>
      <div class="body">
        <ul>
          <li>
            Orthologues: <i>P. troglodytes</i> vs <i>H. sapiens</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Orthologue.object Orthologue.subject Orthologue" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.object" type="Gene">
  </node>
  <node path="Orthologue.object.organism" type="Organism">
  </node>
  <node path="Orthologue.object.organism.taxonId" type="String">
    <constraint op="=" value="9598" code="A">
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
          <li>
            Orthologues: <i>R. norvegicus</i> vs <i>H. sapiens</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Orthologue.object Orthologue.subject Orthologue" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.object" type="Gene">
  </node>
  <node path="Orthologue.object.organism" type="Organism">
  </node>
  <node path="Orthologue.object.organism.taxonId" type="String">
    <constraint op="=" value="10116" code="A">
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
          <li>
            Orthologues: <i>C. familiaris</i> vs <i>H. sapiens</i>
            <im:querylink text="(browse)" skipBuilder="true">
<query name="" model="genomic" view="Orthologue.object Orthologue.subject Orthologue" constraintLogic="A and B">
  <node path="Orthologue" type="Orthologue">
  </node>
  <node path="Orthologue.object" type="Gene">
  </node>
  <node path="Orthologue.object.organism" type="Organism">
  </node>
  <node path="Orthologue.object.organism.taxonId" type="String">
    <constraint op="=" value="9615" code="A">
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
 

