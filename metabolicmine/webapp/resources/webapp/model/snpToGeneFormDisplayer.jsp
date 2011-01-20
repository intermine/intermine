<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<div id="nearbyGenes" class="bochs">

  <div class="inner">

  <h4>Find nearby Genes</h4>

  <html:form action="/snpToGeneAction" method="post" enctype="multipart/form-data" >

    <html:hidden property="bagName" value="${bag.name}" />

    <html:select styleId="typeSelector" property="distance">
      <c:forEach var="option" items="${distanceTypes}">
        <html:option value="${option.key}">${option.value}</html:option>
      </c:forEach>
    </html:select>

    <html:select styleId="typeSelector" property="direction">
      <c:forEach var="option" items="${directionTypes}">
        <html:option value="${option.key}">${option.value}</html:option>
      </c:forEach>
    </html:select>

    <%-- changing the <submit> text will break functionality! --%>
    <html:submit styleId="submitBag" property="action">Result</html:submit>
    - or -
    <div id="listButton"><html:submit styleId="submitBag" property="action">List</html:submit></div>

  </html:form>

  </div>

</div>