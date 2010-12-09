<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<div class="bochs">

  <h4>Find nearby Genes</h4>

  <html:form action="/snpToGeneAction" method="post" enctype="multipart/form-data" >

    <html:hidden property="bagName" value="${bag.name}" />

    <html:select styleId="typeSelector" property="distance">
      <html:option value="0.5kb">.5kb</html:option>
      <html:option value="1.0kb">1kb</html:option>
      <html:option value="2.0kb">2kb</html:option>
      <html:option value="5.0kb">5kb</html:option>
      <html:option value="10.0kb">10kb</html:option>
    </html:select>

    <html:select styleId="typeSelector" property="direction">
      <html:option value="upstream">upstream</html:option>
      <html:option value="downstream">downstream</html:option>
      <html:option value="bothways">both ways</html:option>
    </html:select>

    <html:submit styleId="submitBag" property="action">Result</html:submit>
    - or -
    <html:submit styleId="submitBag" property="action">List</html:submit>

  </html:form>

</div>