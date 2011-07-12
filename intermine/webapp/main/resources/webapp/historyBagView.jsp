<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- historyBagView.jsp -->
<html:xhtml/>

<script type="text/javascript" src="js/tablesort.js"></script>
<script type="text/javascript" src="js/historyBagView.js"></script>
<link rel="stylesheet" type="text/css" href="css/sorting.css"/>
<c:set var="type" value="bag"/>
<script type="text/javascript">
jQuery(document).ready(function() {
    //setTimeout("refreshSavedBagStatus()", 5000);
})
</script>
<im:body id="bagHistory">

  <p>
    <fmt:message key="history.savedbags.intro"/>
     <c:if test="${!PROFILE.loggedIn}">
      - <html:link action="/login?returnto=/mymine.do?subtab=lists"><fmt:message key="history.savedbags.login"/></html:link>&nbsp;&nbsp;
    </c:if>
  </p>
<br/>
  <c:choose>
    <c:when test="${empty PROFILE.savedBags}">
      <div class="altmessage">
        <fmt:message key="msgs.noResults"/>
      </div>
    </c:when>
    <c:otherwise>

    <table>
    <tr><td>

      <html:form action="/modifyBag">

        <table class="sortable-onload-2 rowstyle-alt no-arrow" cellspacing="0" id="bagTable">
          <tr>
            <th>
              <input type="checkbox" id="selected_bag"
                     onclick="selectColumnCheckbox(this.form, 'bag')">
            </th>
            <th align="left" nowrap class="sortable"><fmt:message key="query.savedbags.namecolumnheader"/></th>
            <th align="left" nowrap class="sortable"><fmt:message key="query.savedbags.descriptioncolumnheader"/></th>
            <th align="left" nowrap class="sortable"><fmt:message key="query.savedbags.typecolumnheader"/></th>
            <th align="right" nowrap class="sortable-numeric"><fmt:message key="query.savedbags.countcolumnheader"/></th>
            <th align="left" nowrap class="sortable"><fmt:message key="query.savedbags.datecreatedcolumnheader"/></th>
            <th align="left" nowrap class="sortable"><fmt:message key="query.savedbags.currentcolumnheader"/></th>
          </tr>
          <c:forEach items="${PROFILE.savedBags}" var="savedBag" varStatus="status">
            <tr>
              <td class="sorting" align="center">
                <html:multibox property="selectedBags" styleId="selected_bag_${status.index}">
                  <c:out value="${savedBag.key}"/>
                </html:multibox>
              </td>
              <td class="sorting">
                    <tiles:insert name="renamableElement.jsp">
                      <tiles:put name="name" value="${savedBag.value.name}"/>
                      <tiles:put name="type" value="${type}"/>
                      <tiles:put name="index" value="${status.count-1}"/>
                    </tiles:insert>

                    <tiles:insert name="setFavourite.tile">
                      <tiles:put name="name" value="${savedBag.value.name}"/>
                      <tiles:put name="type" value="${type}"/>
                    </tiles:insert>

                     <c:if test="${PROFILE.loggedIn}">
                          <c:set var="taggable" value="${savedBag.value}"/>
                         <tiles:insert name="inlineTagEditor.tile">
                           <tiles:put name="taggable" beanName="taggable"/>
                           <tiles:put name="vertical" value="true"/>
                           <tiles:put name="show" value="true"/>
                           <tiles:put name="onChangeCode" value="refreshTagSelect('mainSelect', 'bag')"/>
                         </tiles:insert>
                     </c:if>

              </td>
              <td class="sorting">
                <c:choose>
                  <c:when test="${fn:length(savedBag.value.description) > 100}">
                    ${fn:substring(savedBag.value.description, 0, 100)}...
                  </c:when>
                  <c:otherwise>
                    ${savedBag.value.description}
                  </c:otherwise>
                </c:choose>
                &nbsp;
              </td>

              <td class="sorting"><im:displaypath path="${savedBag.value.type}"/></td>
              <td class="sorting" align="right">
                <c:out value="${savedBag.value.size}"/>&nbsp;<c:choose>
                  <c:when test="${fn:endsWith(savedBag.value.class.name, 'InterMineIdBag')}">objects</c:when>
                  <c:otherwise>values</c:otherwise>
                </c:choose>
              </td>
              <td class="sorting"><im:dateDisplay date="${savedBag.value.dateCreated}"/></td>
              <td id="status_${savedBag.value.name}" class="sorting" align="right">
              <c:set var="status" value=""/>
              <c:forEach items="${SAVED_BAG_STATUS}" var="savedBagStatus">
                <c:if test="${savedBagStatus.key == savedBag.value.name}">
                <c:set var="status" value="${savedBagStatus.value}"/>
                <c:choose>
                <c:when test="${status == ''}"><fmt:message key="history.upgradingBag"/></c:when>
                <c:otherwise>
                  <c:if test="${status == 'CURRENT'}"><fmt:message key="history.currentBag"/></c:if>
                  <c:if test="${status == 'UPGRADING'}"><fmt:message key="history.upgradingBag"/></c:if>
                  <c:if test="${status == 'TO_UPGRADE'}"><html:link action="/bagUpgrade?bagName=${savedBag.value.name}&amp;bagType=${savedBag.value.type}"><fmt:message key="history.bagToUpgrade"/></html:link></c:if>
               </c:otherwise>
               </c:choose>
              </c:if>
              </c:forEach>
              </td>
            </tr>
          </c:forEach>
        </table>
        <br/>
        <c:if test="${fn:length(PROFILE.savedBags) >= 2}">
          New list name:
          <html:text property="newBagName" size="12"/>
            <input type="button" onclick="validateBagOperations('modifyBagForm', 'union')" value="Union"/>
            <input type="button" onclick="validateBagOperations('modifyBagForm', 'intersect')" value="Intersect"/>
           <input type="button" onclick="validateBagOperations('modifyBagForm', 'subtract')" value="Subtract"/>
        </c:if>
        <input type="button" onclick="validateBagOperations('modifyBagForm', 'delete')" value="Delete"/>
        <input type="button" onclick="validateBagOperations('modifyBagForm', 'copy')" value="Copy"/>

        <html:hidden property="pageName" value="MyMine"/>
        <html:hidden property="listsButton" value="" styleId="listsButton"/>
      </html:form>
      </td>
      </tr></table>
      <br/>

    </c:otherwise>
  </c:choose>

</im:body>



<!-- /historyBagView.jsp -->
