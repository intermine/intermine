<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- historyBagView.jsp -->
<html:xhtml/>

<script type="text/javascript" src="js/tablesort.js"></script>
<script type="text/javascript" src="js/historyBagView.js"></script>
<link rel="stylesheet" type="text/css" href="css/sorting.css"/>
<c:set var="type" value="bag"/>

<script type="text/javascript">
if (!im.bagWorks) {
    im.bagWorks = {};
    <%-- config JS bag works here --%>
    im.bagWorks.upgradingMessage = 'Your lists are being automatically upgraded, please wait.';
    im.bagWorks.timeout = 1000;
} else if (typeof im.bagWorks != 'object') {
    throw new Error("im.bagWorks already exists");
}
</script>

<im:body id="bagHistory">

  <h1>
    <fmt:message key="history.savedbags.intro"/>
     <c:if test="${!PROFILE.loggedIn}">
      - <html:link action="/login?returnto=/mymine.do?subtab=lists"><fmt:message key="history.savedbags.login"/></html:link>&nbsp;&nbsp;
    </c:if>
  </h1>

  <c:choose>
    <c:when test="${empty PROFILE.savedBags}">
      <div class="altmessage">
        <fmt:message key="msgs.noResults"/>
      </div>
    </c:when>
    <c:otherwise>

	<html:form action="/modifyBag">
	<c:forEach items="${PROFILE.savedBagsByStatus}" var="statusSavedBag">
		
		<div class="${statusSavedBag.key} status-table" <c:if test="${empty(statusSavedBag.value) || statusSavedBag.key == 'NOT_CURRENT'}">style="display:none;"</c:if>>
		<c:choose>
			<c:when test="${statusSavedBag.key == 'TO_UPGRADE'}">
				<h2>Lists to upgrade</h2>
				<p>You need to manually upgrade the following lists in order to use them.</p>
			</c:when>
			<c:when test="${statusSavedBag.key == 'NOT_CURRENT' && fn:length(statusSavedBag.value) > 0}">
				<script type="text/javascript">
				(function() {
					<%-- lists are being upgraded (should be anyways) --%>
					jQuery('div.topBar.errors').clone().addClass('loading').show().text(im.bagWorks.upgradingMessage)
					.appendTo(jQuery('div.topBar.errors').parent());
					im.bagWorks.notCurrentLists = new Array();
					<c:forEach items="${statusSavedBag.value}" var="list">
					im.bagWorks.notCurrentLists.push('${list.value.name}');
					</c:forEach>
				})();
				</script>
			</c:when>
		</c:choose>
		
    	<table class="bag-table sortable-onload-2 rowstyle-alt no-arrow">
    	  <thead>
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
	            <c:if test="${statusSavedBag.key == 'TO_UPGRADE'}">
	            	<th align="left" nowrap class="upgrade">Upgrade list</th>
	            </c:if>
	          </tr>
          </thead>
          <tbody>
          <c:forEach items="${statusSavedBag.value}" var="savedBag" varStatus="status">
            <tr>
              <td class="list-name" style="display:none;">${savedBag.value.name}</td>
              <td class="sorting" align="center">
                <html:multibox property="selectedBags" styleId="selected_bag_${status.index}">
                  <c:out value="${savedBag.key}"/>
                </html:multibox>
              </td>
              <td class="sorting">
                    <tiles:insert name="renamableElement.jsp">
                      <tiles:put name="name" value="${savedBag.value.name}"/>
                      <tiles:put name="type" value="${type}"/>
                      <tiles:put name="state" value="${savedBag.value.state}"/>
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
              <td id="size_${savedBag.value.name}" class="sorting" align="right">
                <c:out value="${savedBag.value.size}"/>&nbsp;<c:choose>
                  <c:when test="${fn:endsWith(savedBag.value.class.name, 'InterMineIdBag')}">objects</c:when>
                  <c:otherwise>values</c:otherwise>
                </c:choose>
              </td>
              <td class="sorting"><im:dateDisplay date="${savedBag.value.dateCreated}"/></td>
              
              <%-- to upgrade link --%>
              <c:if test="${savedBag.value.state == 'TO_UPGRADE'}">
              <td class="upgrade">
              	<str:encodeUrl var="nameForURL">${savedBag.value.name}</str:encodeUrl>
                <html:link title="Upgrade this list" action="/bagUpgrade?bagName=${nameForURL}" styleClass="bagToUpgrade"></html:link>
              </td>
              </c:if>
              
            </tr>
          </c:forEach>
          </tbody>
        </table>
        
        <%-- list works --%>
        <c:if test="${statusSavedBag.key == 'CURRENT'}">
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
        </c:if>
        
        </div>
        
      </c:forEach>
      </html:form>

    </c:otherwise>
  </c:choose>
<script type="text/javascript">
(function() {
	<%-- attach handler to select all in a table --%>
	jQuery('table.bag-table th input[type="checkbox"]').click(function() {
		jQuery(this).closest('table').find('tr td input[type="checkbox"]').attr('checked', jQuery(this).is(':checked'));
	});
	
	<%-- poll for lists that are "current" or "to upgrade" --%>
    if (im.bagWorks.notCurrentLists) {
    	
    	function remainingLists() {
			if (!jQuery('div.topBar.errors.loading div.count').exists()) {
				jQuery('<div/>', {
					'class': 'count',
					'style': 'display:inline; margin-left:5px;',
				}).appendTo('div.topBar.errors.loading');
			}
			if (im.bagWorks.notCurrentLists.length > 0) {
				jQuery('div.topBar.errors.loading div.count').html(function() {
					return '<strong>' + im.bagWorks.notCurrentLists.length + '</strong> ' +
					((im.bagWorks.notCurrentLists.length > 1) ? 'lists' : 'list') + ' remaining.';
				});
			} else {
				jQuery('div.topBar.errors.loading').remove();
			}
    	}
    	remainingLists();
    	
    	jQuery('<h2/>', {
    		'text': 'Your current lists'
    	}).prependTo(jQuery('div.CURRENT.status-table'));
    	
		im.bagWorks.timeout = window.setInterval(function() {		    
			AjaxServices.getSavedBagStatus(function(json) {
		    	var jSONObject = jQuery.parseJSON(json);
	            jQuery.each(jSONObject, function(i) {
	            	var list = jSONObject[i],
	            		j	 = im.bagWorks.notCurrentLists.indexOf(list['name']);
	            	if (j >= 0) {
	            		jQuery('div.status-table.NOT_CURRENT table tbody tr').each(function(i) {
	            			if (jQuery(this).find('td.list-name').text() == list['name']) {
	            				jQuery('div.status-table.' + list['status']).show();
	            				var newList = jQuery(this).clone();
	            				
	            				if (list['status'] == 'TO_UPGRADE') {
	            					jQuery('<td/>', {
	            						'class': 'upgrade'
	            					})
	            					.append(jQuery('<a/>', {
	            						'href': '<html:rewrite action="/bagUpgrade"/>' + '?bagName=' + newList.find('td.list-name').text().replace(/ /g, '+'),
	            						'class': 'bagToUpgrade'
	            					}))
	            					.appendTo(newList);
	            				}
	            				
	            				newList.prependTo(jQuery('div.status-table.' + list['status'] + ' table tbody'))
	            				.highlight();
	            			}
	            		});
	            		im.bagWorks.notCurrentLists.splice(j, 1);
	            		remainingLists();
	            	}
	            });
	            <%-- shall we poll any further? --%>
	            if (im.bagWorks.notCurrentLists.length == 0) window.clearInterval(im.bagWorks.timeout);
		    });
		}, im.bagWorks.timeout);
    }
})();
</script>
</im:body>

<!-- /historyBagView.jsp -->