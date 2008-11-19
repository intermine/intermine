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

<c:set var="LIMIT" value="2"/>
<em>${LIMIT+1} most recent submissions for each project:</em>

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
	<c:forEach items="${subs}" var="item" varStatus="status">
		<c:if test="${status.count%2 eq 1}"><tr></c:if>
		<td>
    <c:forEach items="${item.key.organisms}" var="organism" varStatus="orgStatus">
    <c:if test="${organism.taxonId eq 7227}"> 
        <img border="0" class="arrow" src="images/f_vs.png" title="fly"/><br>
    </c:if>
    <c:if test="${organism.taxonId eq 6239}"> 
        <img border="0" class="arrow" src="images/w_vs.png" title="worm"/><br>
    </c:if>

<%--
<c:choose>
		<c:when test="${organism.taxonId eq 7227}">
		${organism.taxonId}<img border="0" class="arrow" src="images/f_vs.png" title="fly"/><br>
		</c:when>
		<c:when  test="${organism.taxonId eq 6239}">
    ${organism.taxonId}<img border="0" class="arrow" src="images/w_vs.png" title="worm"/><br>		
		</c:when>
</c:choose>			
--%>

		</c:forEach>
		</td>
			<td><html:link
				href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>

<td>
<c:choose>
<c:when test="${empty item.value}">
 -
</c:when>
<c:otherwise>
<table cellpadding="0" cellspacing="0" border="0" class="internal">
<c:forEach items="${item.value}" var="sub" varStatus="subStatus" end="${LIMIT}">
<tr>
      <td><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.id}">
 ${sub.title}
    </html:link>
    </td>    
    <td><fmt:formatDate value="${sub.publicReleaseDate}" type="date" pattern="yyyy-MM-dd"/>
    </td>
    
      </tr>
</c:forEach>
</table>
</c:otherwise>
</c:choose>
 </td>


     <td>
      <c:forEach items="${counts}" var="nr">
        <c:if test="${nr.key.surnamePI eq item.key.surnamePI}">
          <c:set var="nrSubs" value="${nr.value}" />
        </c:if>
      </c:forEach> 
      <c:choose>
        <c:when test="${nrSubs eq 0}">
        -
        </c:when>
        <c:when test="${nrSubs gt 0}">
				<im:querylink text="${nrSubs} submissions" showArrow="true" skipBuilder="true">
            <query name="" model="genomic"
              view="Project.labs.submissions.title Project.labs.submissions.design Project.labs.submissions.factorName Project.labs.submissions.factorType Project.labs.submissions.description"
              sortOrder="Project.labs.submissions.title">
            <node path="Project" type="Project">
            </node>
            <node path="Project.surnamePI" type="String">
            <constraint op="=" value="${item.key.surnamePI}" description=""
              identifier="" code="A">
            </constraint>
            </node>
            </query>
          </im:querylink>
        </c:when>
      </c:choose>
     
     
     
  </c:forEach>

</table>
</div>

