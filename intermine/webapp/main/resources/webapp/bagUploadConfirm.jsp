<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- bagUploadConfirm.jsp -->
<html:xhtml/>

<html:form action="/bagUploadConfirm" focus="newBagName" method="post" enctype="multipart/form-data">
  <input type="hidden" name="matchIDs" id="matchIDs">
  <input type="hidden" name="bagType" value="${bagType}">

  <div class="body">
    <!-- title -->
    <h1 class="title">Verifying identifiers</h1>

    <!-- progress -->
    <!--
    <div id="list-progress">
        <div class="gray"><strong>1</strong> <span>Upload list of identifiers</span></div
        ><div class="gray-to-white">&nbsp;</div
        ><div class="white"><strong>2</strong> <span>Verify identifier matches</span></div><div class="white-to-gray">&nbsp;</div
        ><div class="gray"><img src="images/icons/lists-16.png" alt="list" />
          <span>List analysis</span>
        </div>
    </div>
    <div class="clear">&nbsp;</div>
    -->
    
    <!-- choose name -->
    <div id="chooseName" style="display:none">
      <h2>Choose a name for the list</h2>
      <div style="clear:both;"></div>
      <div class="formik">
        <input id="newBagName" type="text" name="newBagName" value="${bagName}">
        <span>(e.g. Smith 2013)</span>
      </div>
      <div style="clear:both;"></div>
    </div>
    
    <!-- upgrading a list scenario -->
    <c:if test="${empty buildNewBag}">
      <input type="hidden" name="upgradeBagName" value="${bagName}"/>
    </c:if>

    <!-- additional matches -->
    <div id="additionalMatches" class="body" style="display:none">
      <div class="oneline">
        <h2>Add additional matches</h2>
        <div id="iframe"></div>
      </div>
      <div style="clear:both;"></div>
    </div>
  </div>

</html:form>

<style>
iframe { border:0; width: 100%; }
</style>

<script type="text/javascript" src="js/bagUploadConfirm.js"></script>
<script type="text/javascript">
jQuery(function () {
    // js-ify java-land data.
    var upgrading = false, paths = {js: [], css: []}, jobId, elem;
    <c:if test="${empty buildNewBag}">upgrading = true;</c:if>
    <c:set var="section" value="component-400"/>
    <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">      
    paths["${res.type}"].push("${res.url}");
    </c:forEach>
    jobId = "${jobUid}";
    elem = "#iframe";
    var extraFilter = "${bagExtraFilter}";
    var bagType = "${bagType}";
    // Point here.
    var root = window.location.protocol + "//" + window.location.host + "/${WEB_PROPERTIES['webapp.path']}";
    BagUpload.confirm(elem, jobId, root, upgrading, paths, extraFilter, bagType);
});
</script>
<!-- /bagUploadConfirm.jsp -->