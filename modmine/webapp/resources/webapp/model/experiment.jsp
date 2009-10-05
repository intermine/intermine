<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>


<style type="text/css">

.dbsources table.features {
  clear:left;
  font-size: small;
  border: none;
}

.dbsources table.features td {
  white-space:nowrap;
  padding: 3px; 
  border-left:1px solid;
  border-right: none;
  border-bottom: none;
  border-top:none;
}

.dbsources table.features .firstrow {
  white-space:nowrap;
  padding: 3px; 
  border-top:none;
}

.dbsources table.features .firstcolumn {
  white-space: nowrap;
  padding: 3px;
  border-left: none;
}



</style>


<tiles:importAttribute />


<html:xhtml />


<div class="body">

<c:forEach items="${experiments}" var="exp"  varStatus="status">

  <im:boxarea title="${exp.name}" stylename="gradientbox">

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
  
  <td>experiment: <b><c:out value="${exp.name}"/></b></td>
  <td>project: <b><c:out value="${exp.projectName}"></c:out></b></td>
  <td>PI: <b><c:out value="${exp.pi}"></c:out></b></td>
  </tr>
  
  
  <tr>
  <td colspan="4"><c:out value="${exp.description}"></c:out></td>
  </tr>
  </table>

  <em>
  <c:choose>
    <c:when test="${exp.submissionCount == 0}">
      There are no submissions for this experiment:
    </c:when>
    <c:when test="${exp.submissionCount == 1}">
      There is <c:out value="${exp.submissionCount}"></c:out> submission for this experiment:
    </c:when>
    <c:otherwise>
      There are <c:out value="${exp.submissionCount}"></c:out> submissions for this experiment:   
    </c:otherwise>
    
  </c:choose>
  </em>
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

  
  <c:forEach items="${exp.submissionsAndFeatureCounts}" var="subCounts">
    
    <c:set var="sub" value="${subCounts.key}"></c:set>
    <tr>
      <td><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${subCounts.key.id}"><c:out value="${sub.dCCid}"></c:out></html:link></td>
      <td><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${subCounts.key.id}"><c:out value="${sub.title}"></c:out></html:link></td>
      <td><c:out value="${sub.experimentType}"></c:out></td>
      <td><fmt:formatDate value="${sub.publicReleaseDate}" type="date"/></td>
      <c:forEach items="${exp.factorTypes}" var="factorType">
        <td>
          <c:forEach items="${sub.experimentalFactors}" var="factor" varStatus="status">
            <c:if test="${factor.type == factorType}">
              <c:choose>
                <c:when test="${factor.property != null}">
                  <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${factor.property.id}">
                  <c:out value="${factor.name}"/>
                  </html:link>
                </c:when>
                <c:otherwise>
                  <c:out value="${factor.name}"/>          
                </c:otherwise>
              </c:choose>
            </c:if>      
          </c:forEach>
        </td>


      </c:forEach>
    <%--</tr>
   
    <tr>--%>     
      <td colspan="${exp.factorCount + 5}">
      <table cellpadding="0" cellspacing="0" border="0" class="features">
      <c:forEach items="${subCounts.value}" var="fc" varStatus="rowNumber">
        
        <c:set var="class" value=""/>
        <c:if test="${rowNumber.first}">
          <c:set var="class" value="firstrow"/>
        </c:if>
        <tr>                 
          <td class="firstcolumn ${class}">${fc.key}</td>
          <td class="${class}">${fc.value}</td>
          <td class="${class}" align="right">
              <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">VIEW RESULTS</html:link>
          </td>
          <td class="${class}" align="right">
              export: 
               <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${sub.dCCid}&feature=${fc.key}&format=tab">TAB</html:link>
               <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${sub.dCCid}&feature=${fc.key}&format=csv">CSV</html:link>
               <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${sub.dCCid}&feature=${fc.key}&format=gff3">GFF3</html:link>
          </td>
          <td class="${class}" align="right">
            create list
          </td>
      </tr>
    </c:forEach>
    </table>
        </td>

    <%--</tr>--%>

  </c:forEach>

  <tr>
  <td colspan="${exp.factorCount + 5}">
  <br/>
  <p>
    ALL FEATURES GENERATED BY THIS EXPERIMENT:
  </p>
  <br/>
          <table cellpadding="0" cellspacing="0" border="0">
      <%--<tr>
        <th>Feature type</th>
        <th>Count</th>
        <th>View data</th>
        <th colspan="3">Export</th>
      </tr>--%>
      <c:forEach items="${exp.featureCounts}" var="fc" varStatus="status">
          <tr>
            <td>${fc.key}</td>
            <td>${fc.value}</td>                     
            <td align="right">
              view:
              <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=results&experiment=${exp.name}&feature=${fc.key}">VIEW RESULTS</html:link>
            </td>
            <td align="right">
              export: 
               <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.key}&format=tab">TAB</html:link>
            
            </td>
            <td align="right">
              <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.key}&format=csv">CSV</html:link>
           
            </td>
            <td align="right">
             <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.key}&format=gff3">GFF3</html:link>
           
            </td>
            <td>
            create list
            </td>
          </tr>
      </c:forEach>
      <!-- end submission loop -->
    </table>
  </td>
  </tr>

  </table>

  </im:boxarea>

</c:forEach>
</div>

