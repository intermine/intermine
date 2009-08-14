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

<c:set var="LIMIT" value="1"/>
<em>${LIMIT+1} most recent submissions for each project:</em>

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
	<c:forEach items="${subs}" var="item" varStatus="status">
		<c:if test="${status.count%2 eq 1}"><tr></c:if>
		<td>
    <c:forEach items="${item.key.organisms}" var="organism" varStatus="orgStatus">
    <c:if test="${organism.taxonId eq 7227}"> 
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/><br>
    </c:if>
    <c:if test="${organism.taxonId eq 6239}"> 
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/><br>
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

		</c:forEach> <%--organism --%>
		</td>
			<td><html:link
				href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>

<%--

<br>
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
        <hr>
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

--%>

</td>



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
 ${fn:replace(sub.title,"_", " ")}
<%-- ${sub.dCCid} --%>
    </html:link>
    </td>
<%--    <td> ${sub.experimentType }</td> --%>   
    <td><fmt:formatDate value="${sub.publicReleaseDate}" type="date" pattern="yyyy-MM-dd"/>
    </td>
      </tr>
</c:forEach>
</table>


      <c:forEach items="${counts}" var="nr">
        <c:if test="${nr.key.surnamePI eq item.key.surnamePI}">
          <c:set var="nrSubs" value="${nr.value}" />
        </c:if>
      </c:forEach> 
        <hr>
        <im:querylink text="All ${nrSubs} submissions" showArrow="true" skipBuilder="true">
            <query name="" model="genomic"
              view="Project.submissions.DCCid Project.submissions.title Project.submissions.design"
              sortOrder="Project.submissions.DCCid">
<%--
              view="Project.labs.submissions.title Project.labs.submissions.design Project.labs.submissions.factorName Project.labs.submissions.factorType Project.labs.submissions.description"
              sortOrder="Project.labs.submissions.title">
--%>

            <node path="Project" type="Project">
            </node>
            <node path="Project.surnamePI" type="String">
            <constraint op="=" value="${item.key.surnamePI}" description=""
              identifier="" code="A">
            </constraint>
            </node>
            </query>
          </im:querylink>



</c:otherwise>
</c:choose>
 </td>
 
</c:forEach>

<%--
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

--%>
</table>
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

