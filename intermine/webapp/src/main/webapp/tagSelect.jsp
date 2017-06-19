<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- tagSelect.jsp -->

<%--Tile usage:
    call refreshTagSelect(selectId, type) function if you want select to be reloaded --%>

<%-- type parameter - is type of objects for which you want to display tags like 'bag', 'template' --%>
<tiles:importAttribute name="type" ignore="false" />
<tiles:importAttribute name="selectId" ignore="false" />
<%--
    onChangeFunction parameter - is name of function that you want to be called when the select is changed,
        you must define this function with exactly one parameter - values of new select
--%>
<tiles:importAttribute name="onChangeFunction" ignore="true" />
<tiles:importAttribute name="disabled" ignore="true" />
<%-- tags - if defined, than select options are rendered by tile else javascript code is inserted that makes AJAX call --%>
<tiles:importAttribute name="tags" ignore="true" />
<tiles:importAttribute name="title" ignore="true" />

<c:if test="${empty title}">
    <c:set var="title" value="-- filter by a tag --"></c:set>
</c:if>

<c:choose>
  <c:when test="${!empty onChangeFunction}">
    <select id="${selectId}" onchange="javascript:callOnChangeFunction('${selectId}', '${onChangeFunction}')">
  </c:when>
  <c:otherwise>
    <select id="${selectId}">
  </c:otherwise>
</c:choose>
<c:choose>
    <c:when test="${!empty tags}">
        <option value="">${title}</option>
        <c:forEach items="${tags}" var="tag">
            <option value="${tag}" />${tag}
        </c:forEach>
        </select>
    </c:when>
    <c:otherwise>
        </select>
        <c:choose>
        <c:when test="${!empty type}">
          <script type="text/javascript">
               displayTagSelect("${title}", "${selectId}", "${type}");
          </script>
        </c:when>
        <c:otherwise>
         <!--  type is empty!! This is not good -->
        </c:otherwise>
       </c:choose>
    </c:otherwise>
</c:choose>

<c:if test="${!empty disabled}">
  <script type="text/javascript">
    document.getElementById("${selectId}").disabled = true;
  </script>
</c:if>

<!-- /tagSelect.jsp -->
