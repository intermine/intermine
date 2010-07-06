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

<tiles:insert name="keywordSearch.tile"/>

Search Term: <c:out value="${searchTerm}"/>

<div>

<c:if test="${empty displayMax}"><c:out value="Matching submissions: ${fn:length(submissions)}"/></c:if>
<c:if test="${!empty displayMax}">Matching submissions: more than <c:out value="${displayMax}" /> (only the top <c:out value="${displayMax}" /> matches are displayed)</c:if>
<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
<tr>
    <th>DCC id</th>
    <th>Organism</th>
    <th>Group</th>
    <th>Name</th>
    <th>Date</th>
    <th>Details</th>
    <th>Search score</th>
</tr>
<c:forEach items="${submissions}" var="subResult">
  <c:set var="sub" value="${subResult.key}"/>
  <tr>
      <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.id}"><c:out value="${sub.dCCid}"></c:out></html:link></td>
      <td>
      <c:if test="${sub.organism.genus eq 'Drosophila'}"> 
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/>
                        <c:set var="fly" value="1" />
      </c:if>
      <c:if test="${sub.organism.genus eq 'Caenorhabditis'}">  
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/>
                        <c:set var="worm" value="1" />
                    </c:if>
      </td>
      <td>PI: <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.project.id}"><c:out value="${sub.project.surnamePI}"/></html:link><br/>
          Lab: <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.lab.id}"><c:out value="${sub.lab.surname}"/></html:link><br/>
      </td>
      <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.id}"><c:out value="${sub.title}"></c:out></html:link></td>
      <td><fmt:formatDate value="${sub.publicReleaseDate}" type="date"/></td>
      <td>
        <c:set var="isPrimer" value="0"/>          
        <c:forEach items="${sub.properties}" var="prop" varStatus="status">
         <c:choose>
          <c:when test="${fn:contains(prop,'primer')}">
          <c:set var="isPrimer" value="${isPrimer + 1}"/>
          </c:when>
          </c:choose>
        <c:choose>
        <c:when test="${isPrimer <= 5 || !fn:contains(prop,'primer')}">
          <c:out value="${prop.type}: "/>
          <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${prop.id}">
          <c:out value="${prop.name}"/></html:link><br/>
        </c:when>
        <c:when test="${isPrimer > 5 && status.last}">
        ...<br></br>
        <im:querylink text="all ${isPrimer} ${prop.type}s" showArrow="true" skipBuilder="true" 
                  title="View all ${isPrimer} ${prop.type}s factors of submission ${sub.dCCid}">

<query name="" model="genomic" view="SubmissionProperty.name SubmissionProperty.type" sortOrder="SubmissionProperty.type asc" constraintLogic="A and B">
  <node path="SubmissionProperty" type="SubmissionProperty">
  </node>
  <node path="SubmissionProperty.submissions" type="Submission">
    <constraint op="LOOKUP" value="${sub.dCCid}" description="" identifier="" code="A" extraValue="">
    </constraint>
  </node>
  <node path="SubmissionProperty.type" type="String">
    <constraint op="=" value="${prop.type}" description="" identifier="" code="B" extraValue="">
    </constraint>
  </node>
</query>

                  </im:querylink>
        
        </c:when>
        </c:choose>
        </c:forEach>
      </td>
      
      <td><img height="10" width="${subResult.value * 5}" src="images/heat${subResult.value}.gif" alt="${subResult.value}" title="${subResult.value}"/></td>
</tr>
</c:forEach>
</table>



</div>

</div>