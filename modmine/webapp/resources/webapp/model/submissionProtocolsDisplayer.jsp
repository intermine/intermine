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

<%--
<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function () {
        jQuery("button").click(function () { 
      jQuery("p").toggle();
        });
    })
</script>

<button>Toggle</button>
  <p>Hello</p>
  <p style="display: none">Good Bye</p>
--%>


<table cellspacing="0" width="100%">
<tr>
  <TD colspan=2 align="left" style="padding-bottom:10px">
<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function () {
        jQuery("#bro").click(function () { 
           jQuery("#submissionProtocols").toggle();
        });
    })
</script>

<div id="bro"> <h3>Browse metadata for this submission (click to toggle)<img src="images/undisclosed.gif">:</h3>

  <div id="submissionProtocols" style="display: block">

<p>
    <table cellpadding="0" cellspacing="0" border="0" class="results">
      
      <tr>
        <th>Step</th>
        <th colspan="2">Inputs</th>
        <th>Applied Protocol</th>
        <th colspan=2">Outputs</th>
      </tr>
       <c:set var="prevStep" value="0" />

      <tbody>
    <c:forEach var="row" items="${pagedResults.rows}" varStatus="status">

      <c:set var="rowClass">
        <c:choose>
          <c:when test="${status.count % 2 == 1}">odd</c:when>
          <c:otherwise>even</c:otherwise>
        </c:choose>
      </c:set>

      <c:forEach var="subRow" items="${row}" varStatus="multiRowStatus">
       <%--
       <tr class="<c:out value="${rowClass}"/>">
--%>

        <im:instanceof instanceofObject="${subRow[0]}" 
          instanceofClass="org.intermine.web.logic.results.flatouterjoins.MultiRowFirstValue" 
          instanceofVariable="isFirstValue"/>
        <c:if test="${isFirstValue == 'true'}">
         <c:set var="step" value="${subRow[0].value.field}" scope="request"/>
         </c:if>
                    <c:set var="stepClass">
                      <c:choose>
                       <c:when test="${step % 2 == 1}">stepE</c:when>
                       <c:otherwise>stepO</c:otherwise>
                      </c:choose>
                    </c:set>

       <tr class="<c:out value="${stepClass}${rowClass}"/>">
       <c:set var="output" value="true"/>

       <c:forEach var="column" items="${pagedResults.columns}" varStatus="status2">

          <im:instanceof instanceofObject="${subRow[column.index]}" instanceofClass="org.intermine.web.logic.results.flatouterjoins.MultiRowFirstValue" instanceofVariable="isFirstValue"/>
          <c:if test="${isFirstValue == 'true'}">
            <c:set var="resultElement" value="${subRow[column.index].value}" scope="request"/>

            <c:choose>
                <c:when test="${column.index == 0}">
                 <c:choose>
                   <c:when test="${resultElement.field != prevStep}">
                    <td rowspan="${subRow[column.index].rowspan}" >${resultElement.field}
                    </td>
                    <c:set var="prevStep" value="${resultElement.field}"/>
                  </c:when>
                  <c:otherwise>
                    <c:set var="output" value="true"/>
                    <td rowspan="${subRow[column.index].rowspan}" >${resultElement.field}
                    </td>
                  </c:otherwise>
                 </c:choose>
                </c:when>
            
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
                     rowspan="${subRow[column.index].rowspan}"  class="<c:out value="${stepClass}${rowClass}"/>"> 
              
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


</TD>
</tr>
</table>
</div>


</div>

