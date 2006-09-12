<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<p style="color:#e00;">
    Note: Due to recent format changes in the PSI data format confidence scores may be missing, this will be corrected shortly (12/5/06)
</p>
<TABLE width="100%">
  <TR>
    <TD valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <P><B>High-throughput 2-hybrid protein interaction datasets.</B></P>
        <P>
          These datasets were downloaded in <A href="http://psidev.sourceforge.net/mi/xml/doc/user">PSI-MI format</A> from the <A href="http://www.ebi.ac.uk/intact/index.html">intAct</A> database:

             <im:querylink text="Arabidopsis thaliana experiment list" skipBuilder="true">
                 <query name="" model="genomic" view="ProteinInteractionExperiment ProteinInteractionExperiment.publication.pubMedId ProteinInteractionExperiment.publication.firstAuthor ProteinInteractionExperiment.publication.year">
                     <node path="ProteinInteractionExperiment" type="ProteinInteractionExperiment"></node>
                     <node path="ProteinInteractionExperiment.hostOrganism" type="String"></node>
                     <node path="ProteinInteractionExperiment.interactions" type="ProteinInteraction"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins" type="Protein"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins.organism" type="Organism"></node>
                     <node path="ProteinInteractionExperiment.interactions.proteins.organism.name" type="String">
                         <constraint op="=" value="3702" description="" taxonId="" code="A"></constraint>
                     </node>
                 </query>
             </im:querylink>
       </P>
      </div>
    </TD>
  </TR>
</TABLE>
