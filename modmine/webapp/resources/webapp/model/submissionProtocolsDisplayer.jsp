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

<style type="text/css">
div#submissionProtocols h3 {
  color: black;
  margin-bottom: 20px;
}
</style>

<div class="body">
  <div id="submissionProtocols">
    <p>
    
    <h3>Browse metadata for this submission:</h3>
    
    <table cellpadding="0" cellspacing="0" border="0" class="results">
      
      <tr>
        <th>Step</th>
        <th colspan="2">Inputs</th>
        <th>Applied Protocol</th>
        <th colspan=2">Outputs</th>
      </tr>

      <tbody>
    <c:forEach var="row" items="${pagedResults.rows}" varStatus="status">

      <c:set var="rowClass">
        <c:choose>
          <c:when test="${status.count % 2 == 1}">odd</c:when>
          <c:otherwise>even</c:otherwise>
        </c:choose>
      </c:set>

      <c:forEach var="subRow" items="${row}" varStatus="multiRowStatus">
       <tr class="<c:out value="${rowClass}"/>">

       <c:set var="output" value="true"/>

       <c:forEach var="column" items="${pagedResults.columns}" varStatus="status2">

          <im:instanceof instanceofObject="${subRow[column.index]}" instanceofClass="org.intermine.web.logic.results.flatouterjoins.MultiRowFirstValue" instanceofVariable="isFirstValue"/>
          <c:if test="${isFirstValue == 'true'}">
            <c:set var="resultElement" value="${subRow[column.index].value}" scope="request"/>

            <c:choose>
              <c:when test="${column.index == 1  || column.index == 5}">
            
                <c:if test="${fn:startsWith(resultElement.field,'Anonymous Datum')}">
                  
                  <td colspan="2" rowspan="${subRow[column.index].rowspan}" >
                    <c:choose>
                      <c:when test="${column.index == 1}">
                        <i><c:out value="-- from previous Step"/></i>
                      </c:when>
                      <c:otherwise>
                        <i><c:out value="--> next Step"/></i>                     
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <c:set var="output" value="false"/>
                </c:if>            
              </c:when>
              <c:otherwise>
              
                <c:if test="${column.index == 4}">
                  <c:set var="output" value="true"/>
                </c:if>     
                                   
                <c:if test="${output}">
                  <td id="cell,${status2.index},${status.index},${subRow[column.index].value.type}"
                     rowspan="${subRow[column.index].rowspan}"> 
              
                   <c:choose>
                    <c:when test="${fn:startsWith(fn:trim(resultElement.field), 'http://')}">
                      <a href="${resultElement.field}" class="value extlink">
                        <c:set var="elements" value="${fn:split(resultElement.field,'/')}"/>
                        <c:out value="${elements[fn:length(elements) - 1]}"/>
                      </a>
                    </c:when>
                    <c:otherwise>
                      <tiles:insert name="objectView.tile" />
                  </c:otherwise>
                  </c:choose>
                  </td>
                </c:if>
              </c:otherwise>
            </c:choose>
          </c:if>
        </c:forEach>
        </tr>
      </c:forEach>
    </c:forEach>
    </tbody>

    </table>
    <br/>
  </div>
</div>
