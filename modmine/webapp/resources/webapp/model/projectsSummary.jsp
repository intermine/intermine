<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
	prefix="str"%>


<tiles:importAttribute />


<html:xhtml />

<div class="body">

<c:forEach items="${experiments}" var="proj">

  <h3>${proj.key}</h3>

 <c:forEach items="${proj.value}" var="exp"  varStatus="status">
  <table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr><td>
    <c:forEach items="${exp.organisms}" var="organism" varStatus="orgStatus">
      <c:if test="${organism eq 'D. melanogaster'}"> 
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/><br>
      </c:if>
      <c:if test="${organism eq 'C. elegans'}"> 
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/><br>
      </c:if>
    </c:forEach> 
  </td>
  
  <td>experiment: <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${exp.name}">${exp.name}</html:link>
  <td>project: <b><c:out value="${exp.projectName}"></c:out></b></td>
  <td>PI: <b><c:out value="${exp.pi}"></c:out></b></td>
  </tr>
  
  
  <tr>
  <%--<td colspan="4"><c:out value="${exp.description}"></c:out></td>--%>
  </tr>
  </table>

</c:forEach>
</c:forEach>
</div>

<!-- links to all subs -->

<table cellspacing="4"><tr>

<td>    
<im:querylink text="Fly" showArrow="true" skipBuilder="true">
  <query name="" model="genomic"
    view="Submission.title Submission.DCCid Submission.design Submission.factorName Submission.publicReleaseDate"
    sortOrder="Submission.title">
  <node path="Submission" type="Submission">
  </node>
  <node path="Submission.organism" type="Organism">
  <constraint op="LOOKUP" value="Drosophila melanogaster" description=""
    identifier="" code="A" extraValue="">
  </constraint>
  </node>
  </query>
</im:querylink>
    </td>

<td>
<im:querylink text="Worm" showArrow="true" skipBuilder="true">
  <query name="" model="genomic"
    view="Submission.title Submission.DCCid Submission.design Submission.factorName Submission.publicReleaseDate"
    sortOrder="Submission.title">
  <node path="Submission" type="Submission">
  </node>
  <node path="Submission.organism" type="Organism">
  <constraint op="LOOKUP" value="Caenorhabditis elegans" description=""
    identifier="" code="A" extraValue="">
  </constraint>
  </node>
  </query>
</im:querylink>
</td>

<td>    
<im:querylink text="All submissions" showArrow="true" skipBuilder="true">
  <query name="" model="genomic"
    view="Submission.title Submission.DCCid Submission.design Submission.factorName Submission.publicReleaseDate"
    sortOrder="Submission.title">
  <node path="Submission" type="Submission">
  </node>
  </query>
</im:querylink>
    </td>



<%--
<td>
<html:link
          href="/${WEB_PROPERTIES['webapp.path']}/submissions.do"> All submissions <img border="0" class="arrow" src="images/right-arrow.gif" title="-&gt;"/>
    </html:link>
<br>
</td>
--%>
  </tr></table>

