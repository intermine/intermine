<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- proteinSequenceDisplayer.jsp -->

<div id="sequence-feature-displayer" class="collection-table column-border-by-2">
    <c:set var="protein" value="${reportObject.object}"/>

    <h3>Sequence</h3>

    <table border="0" cellspacing="0">
          <c:choose>
            <c:when test="${!empty protein.length}">
              <tr>
                <td class="label">Length:</td>
                <td colspan="4">
                  <c:set var="interMineObject" value="${reportObject.object}" scope="request" />
                  <tiles:insert page="/model/sequenceShortDisplayerWithField.jsp">
                    <tiles:put name="expr" value="length" />
                    <tiles:put name="objectClass" value="${objectClass}" />
                  </tiles:insert>
                </td>
              </tr>
            </c:when>
            <c:otherwise>
              <i>Sequence information not available</i>
            </c:otherwise>
          </c:choose>
    </table>
</div>

<!-- /proteinSequenceDisplayer.jsp -->