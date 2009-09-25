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

<c:forEach items="${experiments}" var="exp"  varStatus="status">

  <im:boxarea title="${exp.name}" stylename="gradientbox">

  <table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr><td>
    <c:forEach items="${exp.organisms}" var="organism" varStatus="orgStatus">
      <c:if test="${organism eq 7227}"> 
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/><br>
      </c:if>
      <c:if test="${organism eq 6239}"> 
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/><br>
      </c:if>
    </c:forEach> 
  </td>
  
  <td>experiment: <b><c:out value="${exp.name}"/></b></td>
  <td>project: <b><c:out value="${exp.projectName}"></c:out></b></td>
  <td>PI: <b><c:out value="${exp.pi}"></c:out></b></td>
  </tr>
  
  
  <tr>
  <td colspan="4"><c:out value="${exp.description}"></c:out></td>
  </tr>
  </table>

  <em>There are <c:out value="${exp.submissionCount}"></c:out> submissions for this experiment:</em>
  <table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr>
  <td>DCC id</td>
  <td>name</td>
  <td>type</td>
  <td>date</td>
  <c:forEach items="${exp.factorTypes}" var="factor">
    <td><c:out value="${factor}"></c:out></td>
  </c:forEach>
  <td>features</td>
  </tr>

  <c:forEach items="${exp.submissions}" var="sub">
    <tr>
    <td><c:out value="${sub.dCCid}"></c:out></td>
    <td><c:out value="${sub.title}"></c:out></td>
    <td><c:out value="${sub.experimentType}"></c:out></td>
    <td><fmt:formatDate value="${sub.publicReleaseDate}" type="date"/></td>
    <c:forEach items="${exp.factorTypes}" var="factorType">
      <td>
      <c:forEach items="${sub.experimentalFactors}" var="factor">
        <c:if test="${factor.type == factorType}">
          <c:out value="${factor.name}"/>
        </c:if>      
      </c:forEach>
      </td>
    </c:forEach>
    <td>FEATURES</td>
    </tr>

  </c:forEach>

  </table>

  </im:boxarea>

</c:forEach>
</div>

