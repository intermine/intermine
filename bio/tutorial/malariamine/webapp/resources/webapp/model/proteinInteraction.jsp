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
        <P><B>High-throughput 2-hybrid interaction datasets.</B></P>
        <P>
          These datasets were downloaded in <A href="http://psidev.sourceforge.net/mi/xml/doc/user">PSI-MI format</A> from the <A href="http://www.ebi.ac.uk/intact">IntAct</A> database:

                         <im:querylink text="All <i>Plasmodium falciparum 3D7</i> interactions from IntAct " skipBuilder="true">
<query name="" model="genomic" view="Interaction.experiment.publication.pubMedId Interaction.experiment.name Interaction.experiment.interactionDetectionMethod.name Interaction.experiment.participantIdentificationMethod.name Interaction.gene.primaryIdentifier Interaction.role Interaction.interactingGenes.primaryIdentifier Interaction.type.name" sortOrder="Interaction.experiment.publication.pubMedId asc" constraintLogic="A and B">
  <node path="Interaction" type="Interaction">
  </node>
  <node path="Interaction.gene" type="Gene">
  </node>
  <node path="Interaction.gene.organism" type="Organism">
  </node>
  <node path="Interaction.gene.organism.name" type="String">
    <constraint op="=" value="Plasmodium falciparum 3D7" description="" taxonId="" code="A"></constraint>
  </node>
  <node path="Interaction.dataSets" type="DataSet">
  </node>
  <node path="Interaction.dataSets.dataSource" type="DataSource">
  </node>
  <node path="Interaction.dataSets.dataSource.name" type="String">
    <constraint op="=" value="IntAct" description="" identifier="" code="B"></constraint>
  </node>
</query>
            </im:querylink>

       </P>
      </div>
    </TD>
  </TR>
</TABLE>
