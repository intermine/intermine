<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- orthologueMsg -->

<c:if test="${! empty externalids}">
   <c:set var="externalIdList" value="${fn:split(externalids,',')}"/>
   Found ${bag.size} orthologues in ${addparameter} for ${fn:length(externalIdList)} ${bag.type}s
   <im:querylink text="[view orthologue mapping]" skipBuilder="true">
   <query name="" model="genomic" view="Gene.identifier Gene.orthologues.orthologue.identifier Gene.orthologues.inParanoidScore" sortOrder="Gene.identifier asc" constraintLogic="A and B and C">
     <node path="Gene" type="Gene">
       <constraint op="LOOKUP" value="${externalids}" description="" identifier="" code="A">
       </constraint>
     </node>
     <node path="Gene.orthologues" type="Orthologue">
     </node>
     <node path="Gene.orthologues.type" type="String">
       <constraint op="=" value="main" description="" identifier="" code="B">
       </constraint>
     </node>
     <node path="Gene.orthologues.orthologue" type="Gene">
     </node>
     <node path="Gene.orthologues.orthologue.organism" type="Organism">
     </node>
     <node path="Gene.orthologues.orthologue.organism.shortName" type="String">
       <constraint op="=" value="${addparameter}" description="" identifier="" code="C">
       </constraint>
     </node>
   </query>
   </im:querylink>
</c:if>
   
<!-- /orthologueMsg -->
