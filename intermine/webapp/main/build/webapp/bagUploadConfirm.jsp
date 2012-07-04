<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- bagUploadConfirm.jsp -->
<html:xhtml/>

<div style="display:none">
<%-- cache --%>
<img src="images/progress/green-btn-center-hover.png" />
<img src="images/progress/green-btn-left-hover.png" />
<img src="images/progress/green-btn-right-hover.png" />
</div>

<html:form action="/bagUploadConfirm" focus="newBagName" method="post" enctype="multipart/form-data">

<div id="sidebar">
  <div class="wrap">
  <c:choose>
  <c:when test="${!empty buildNewBag}">
    <div id="bigGreen" class='button <c:if test="${matchCount == 0}">inactive</c:if>'>
      <div class="left"></div><input id="saveList" type="button" name="confirmBagUpload"
          value='Save a list of ${matchCount}&nbsp;${bagUploadConfirmForm.bagType}<c:if test="${matchCount != 1}">s</c:if>'
          onclick="if (!updateMatchIDs()) return false;jQuery('#bigGreen').addClass('clicked');validateBagName('bagUploadConfirmForm');"/><div class="right"></div>
    </div>
    </c:when>
    <c:otherwise>
    <input type="hidden" name="upgradeBagName" value="${bagName}"/>
    <div id="bigGreen" class='button <c:if test="${matchCount == 0}">inactive</c:if>'>
      <div class="left"></div><input id="saveList" type="button" name="confirmBagUpload"
          value='Upgrade a list of ${matchCount}&nbsp;${bagUploadConfirmForm.bagType}<c:if test="${matchCount != 1}">s</c:if>'
          onclick="updateMatchIDs();submit();"/><div class="right"></div>
    </div>
    </c:otherwise>
   </c:choose>
    <div style="clear:both;"></div>
    <c:if test="${!empty duplicates || ! empty lowQualityMatches || ! empty convertedObjects}">
      <p id="furtherMatches" class="hl">There are further matches provided below.</p>
    </c:if>
  </div>

  <div style="clear:both;"></div>

  <h2>In your list</h2>
  <ul>
    <li class="added">${matchCount}&nbsp;${bagUploadConfirmForm.bagType}<c:if test="${matchCount != 1}">s</c:if></li>
    <c:if test="${! empty lowQualityMatches}">
      <li class="lowQ"><a>Add</a> ${fn:length(lowQualityMatches)} Synonym match<c:if test="${fn:length(lowQualityMatches) > 1}">es</c:if></li>
      <script type="text/javascript">
        jQuery('#sidebar ul li.lowQ a').click(function(e) { addAll('lowQ', '${flatLowQualityMatches}'); });
      </script>
    </c:if>
    <c:if test="${! empty duplicates}">
      <li class="duplicate"><a>Add</a> ${fn:length(duplicates)} Duplicate match<c:if test="${fn:length(duplicates) != 1}">es</c:if></li>
      <script type="text/javascript">
        jQuery('#sidebar ul li.duplicate a').click(function(e) { addAll('duplicate', '${flatDuplicate}'); });
      </script>
    </c:if>
    <c:if test="${! empty convertedObjects}">
      <li class="converted"><a>Add</a> ${fn:length(convertedObjects)} Converted type<c:if test="${fn:length(convertedObjects) != 1}">s</c:if></li>
      <script type="text/javascript">
        jQuery('#sidebar ul li.converted a').click(function(e) { addAll('converted', '${flatConverted}'); });
      </script>
    </c:if>
  </ul>
</div>

<html:hidden property="matchIDs" styleId="matchIDs" />
<html:hidden property="bagType"/>

<script type="text/javascript" src="js/baguploadconfirm.js"></script>

<c:set var="totalIdCount" value="${fn:length(duplicates) + fn:length(lowQualityMatches) + fn:length(convertedObjects) + matchCount + fn:length(unresolved)}"/>
<div class="body">

    <c:choose>
      <c:when test="${(totalIdCount - fn:length(unresolved)) > 0}">
        <h1>Before we show you the results...</h1>
      </c:when>
      <c:otherwise>
        <h1>There are no matches</h1>
      </c:otherwise>
    </c:choose>

    <div id="list-progress">
        <div class="gray"><strong>1</strong> <span>Upload list of identifiers</span></div
        ><div class="gray-to-white">&nbsp;</div
        ><div class="white"><strong>2</strong> <span>Verify identifier matches</span></div><div class="white-to-gray">&nbsp;</div
        ><div class="gray"><img src="images/icons/lists-16.png" alt="list" /> <span
        <c:if test="${(totalIdCount - fn:length(unresolved)) == 0}">style="text-decoration: line-through;"</c:if>>List analysis</span></div>
    </div>
    <div class="clear">&nbsp;</div>

    <c:if test="${(totalIdCount - fn:length(unresolved)) > 0}">
      <div id="chooseName">
      <h2><c:if test="${!empty duplicates || ! empty lowQualityMatches || ! empty convertedObjects}">a) </c:if>Choose a name for the list</h2>
      <div style="clear:both;"></div>
      <div class="formik">
      <input id="newBagName" type="text" name="newBagName" value="${bagName}">
      <script type="text/javascript">
      (function() {
          var extraFilter = ("${bagExtraFilter}") ? "${bagExtraFilter}" : "all ${bagUploadConfirmForm.extraFieldValue}s".toLowerCase();

          <%-- on keypress --%>
          jQuery('input#newBagName').keypress(function(e) {
            var code = (e.keyCode ? e.keyCode : e.which);
            if (code == 13) { <%-- Enter --%>
              validateBagName('bagUploadConfirmForm');
              e.preventDefault();
            }
          });
          if (jQuery('input#newBagName').val().length == 0) {
            <%-- if we do not have a name of the list generate one from user's time --%>
            var t = new Date();
            var m = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
            jQuery('input#newBagName').val("${bagUploadConfirmForm.bagType} list for " + extraFilter + " " + t.getDate() + " " + m[t.getMonth()] + " " + t.getFullYear() + " " + t.getHours() + "." + t.getMinutes());
          }
      })();
      </script>
        <span>(e.g. Smith 2009)</span>
      </div>
      <div style="clear:both"></div>
    </div>
    </c:if>

  </div>
  <c:if test="${!empty duplicates || ! empty lowQualityMatches || ! empty convertedObjects}">

    <c:set var="totalRowCount" value="0" scope="request" />

    <div id="additionalMatches" class="body">
    <div class="oneline">
      <h2>b) Add additional matches</h2>
      <p class="inline h2"><b><span id="addAllLink" onclick="addAll('all','${jsArray}');" class="fakelink">Add all</span> |
      <span id="removeAllLink" onclick="removeAll('all','${jsArray}');">Remove all</span></b></p>
    </div>
    <div style="clear:both;"></div>

        <br/>

    <p><fmt:message key="bagUploadConfirm.issuesHelp">
         <fmt:param value="${bagUploadConfirmForm.bagType}"/>
       </fmt:message></p>
    <c:if test="${! empty lowQualityMatches}">
    <br/>
      <p>
        <h3>Synonym matches</h3>
    </p>
      <p>
        <c:set var="issueMap" value="${lowQualityMatches}"/>
        <tiles:insert name="bagUploadConfirmIssue.tile">
          <tiles:put name="message" value="${message}"/>
          <tiles:put name="issueMap" beanName="issueMap"/>
          <tiles:put name="issueType" value="lowQ"/>
          <tiles:put name="messageKey" value="bagUploadConfirm.lowQ"/>
          <tiles:put name="arrayName" value="${flatLowQualityMatches}"/>
        </tiles:insert>
    </p>
    </c:if>
    <c:if test="${! empty duplicates}">
      <br/>
      <p>
        <h3>Duplicates</h3>
      </p>
    <p>
        <c:set var="issueMap" value="${duplicates}"/>
        <tiles:insert name="bagUploadConfirmIssue.tile">
          <tiles:put name="message" value="${message}"/>
          <tiles:put name="issueMap" beanName="issueMap"/>
          <tiles:put name="issueType" value="duplicate"/>
          <tiles:put name="messageKey" value="bagUploadConfirm.duplicatesHeader"/>
          <tiles:put name="arrayName" value="${flatDuplicate}"/>
        </tiles:insert>
    </p>
    </c:if>

    <c:if test="${!empty convertedObjects}">
    <br/>
      <p>
        <h3><fmt:message key="bagUploadConfirm.convertedHeader"/></h3>
  </p>
      <p>
        <c:set var="issueMap" value="${convertedObjects}"/>
        <tiles:insert name="bagUploadConfirmIssue.tile">
          <tiles:put name="message" value="${message}"/>
          <tiles:put name="issueMap" beanName="issueMap"/>
          <tiles:put name="issueType" value="converted"/>
          <tiles:put name="messageKey" value="bagUploadConfirm.converted"/>
          <tiles:put name="messageParam" value="${bagUploadConfirmForm.bagType}"/>
          <tiles:put name="arrayName" value="${flatConverted}"/>
        </tiles:insert>
      </p>
    </c:if>
  </div>
  </c:if>

  <div style="clear:both;"></div>

  <c:if test="${fn:length(unresolved) > 0}">
    <div class="heading">
      <fmt:message key="bagUploadConfirm.unresolvedDesc"/>
    </div>
    <div class="body">
      <input type="button" onclick="history.back();" id="goBack" value='<fmt:message key="bagUploadConfirm.goBack"/>' />
      <p>
        <fmt:message key="bagUploadConfirm.unresolved">
          <fmt:param value="${fn:length(unresolved)}"/>
          <fmt:param value="${bagUploadConfirmForm.bagType}"/>
          <fmt:param value="${bagUploadConfirmForm.extraFieldValue}"/>
        </fmt:message>
      </p>
      <ul class="unresolvedIdentifiers">
        <c:forEach items="${unresolved}" var="unresolvedIdentifer">
          <li><c:out value="${unresolvedIdentifer.key}" escapeXml="true" /></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>
</html:form>

<script type="text/javascript">
  var matchCount = ${matchCount};
  var totalCount = ${matchCount + totalRowCount};

  var listType = "${bagUploadConfirmForm.bagType}";
  var furtherMatchesText = "There are further matches provided below.";
  initForm("${buildNewBag}");
  checkIfAlreadyInTheBag();
</script>

<!-- /bagUploadConfirm.jsp -->
