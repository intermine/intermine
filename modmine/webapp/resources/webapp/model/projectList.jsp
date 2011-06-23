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

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <c:forEach items="${labs}" var="item" varStatus="status">
    <c:if test="${status.count%2 eq 1}"><tr></c:if>
      <td><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>


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
          <im:querylink text="${nrSubs}" skipBuilder="true">
            <query name="" model="genomic"
              view="Project.labs.submissions.title Project.labs.submissions.design Project.labs.submissions.description"
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
