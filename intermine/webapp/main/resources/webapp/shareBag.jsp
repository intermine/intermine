<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- shareBag.jsp -->

<html:xhtml/>
<tiles:importAttribute name="bagName" ignore="true"/>
<tiles:importAttribute name="id" ignore="true"/>
<c:if test="${empty bagIsPublic}">
<div>
<span id="shareBag-${id}">
  <span id="sharingUsers-${id}" class="current-tags">
    <script type="text/javascript">
     <c:forEach items="${currentSharingUsers}" var="user" varStatus="status">
        addUserSpan('${id}', '${user}', '${bagName}');
     </c:forEach>
</script>
  </span>
  <span id="addLink-${id}">
    <a class="addTagLink" onclick="startSharingBag('${id}')" >Share with users</a>
  </span>
  <span id="shareEdit-${id}" style="display:none; white-space:nowrap">
      <br/>
    <input type="text" style="border: 1px solid #888; padding: 2px; font-size: 10px" size="18" id="userValue-${id}" name="user"/>
    <input type="button" style="font-size: 10px" value="Add" onclick="addUser('${id}', '${bagName}')"/>
    <input type="button" style="font-size: 10px" value="Done" onclick="stopSharingBag('${id}')"/>
  </span>
</span>


</div>
</c:if>
<!-- /shareBag.jsp -->
