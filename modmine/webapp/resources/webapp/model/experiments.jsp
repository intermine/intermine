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
<em>All modENCODE experiments and their submissions</em>

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  
  <c:forEach items="${subs}" var="item"  varStatus="status">
    <c:if test="${status.count%2 eq 1}"><tr></c:if>

<td>
    <c:forEach items="${item.key.project.organisms}" var="organism" varStatus="orgStatus">
    <c:if test="${organism.taxonId eq 7227}"> 
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/><br>
    </c:if>
    <c:if test="${organism.taxonId eq 6239}"> 
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/><br>
    </c:if>
      </c:forEach> 
</td>

    <td>

     <c:choose>
      <c:when test="${fn:contains(item.key.name,'&oldid')}">
       <c:set var="displayName" value="${fn:substringBefore(item.key.name,'&oldid=')}"></c:set> 
      </c:when>
      <c:when test="${fn:contains(item.key.name,'%E2%80%99')}">
       <c:set var="displayName" value="${fn:replace(item.key.name,'%E2%80%99','&#039;')}"></c:set> 
      </c:when>
      <c:otherwise>
       <c:set var="displayName" value="${item.key.name}"></c:set> 
      </c:otherwise>
     </c:choose>
    
    <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${item.key.id}">
${displayName}
    </html:link>


<hr>
<html:link href="${item.key.project.url}"> ${item.key.project.surnamePI}
</html:link>

&nbsp;&nbsp;&nbsp;&nbsp;

      <c:forEach items="${counts}" var="nr">
        <c:if test="${nr.key.name eq item.key.name}">
          <c:set var="nrSubs" value="${nr.value}" />
        </c:if>
      </c:forEach> 

          <im:querylink text="${nrSubs} submissions " showArrow="true" skipBuilder="true">
<query name="" model="genomic" view="Experiment.submissions.DCCid Experiment.submissions.title Experiment.submissions:experimentalFactors.name Experiment.submissions:experimentalFactors.type">
  <node path="Experiment" type="Experiment">
  </node>
  <node path="Experiment.submissions" type="Submission">
  </node>
  <node path="Experiment.name" type="String">
    <constraint op="=" value="${item.key.name}" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
          </im:querylink>

<%-- does not work..
<span style="float:rigth">
||
</span>
--%>


</td>      
  </c:forEach>

  </tr>


</table>
</div>

<%-- 
      <c:choose>
        <c:when test="${nrSubs eq 0}">
        -
        </c:when>
        <c:when test="${nrSubs gt 0}">
        </c:when>
      </c:choose>

--%>