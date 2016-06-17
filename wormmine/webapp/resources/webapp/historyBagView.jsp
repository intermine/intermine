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
<c:set var="index" value="0"/>
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
      - <a href="${WEB_PROPERTIES['webapp.baseurl']}/google"><fmt:message key="history.savedbags.login"/></a>&nbsp;&nbsp;
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
   <c:if test="${statusSavedBag.key == 'CURRENT'}">
                <div id="historyBagDiv">
   </c:if>
    <div class="${statusSavedBag.key} status-table" <c:if test="${empty(statusSavedBag.value) || statusSavedBag.key == 'NOT_CURRENT' || statusSavedBag.key == 'UPGRADING'}">style="display:none;"</c:if>>
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

      <table class="bag-table sortable-onload-2 rowstyle-alt colstyle-alt no-arrow">
        <thead>
            <tr>
              <th style="display:none;"></th>
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
            <c:set var="index" value="${index+1}"/>
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
                         <tiles:insert name="shareBag.tile">
                           <tiles:put name="bagName" value="${savedBag.value.name}"/>
                           <tiles:put name="isBagValid" value="true"/>
                           <tiles:put name="id" value="${index}"/>
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
                  <c:when test="${fn:endsWith(savedBag.value.class.name, 'InterMineIdBag')}">object</c:when>
                  <c:otherwise>value</c:otherwise>
                </c:choose>
                <c:if test="${savedBag.value.size != 1}">s</c:if>
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
             <input type="button" onclick="showAsymmetricDirection()" value="Asymmetric Difference"/>
          </c:if>
          <input type="button" onclick="validateBagOperations('modifyBagForm', 'delete')" value="Delete"/>
          <input type="button" onclick="validateBagOperations('modifyBagForm', 'copy')" value="Copy"/>
          <br>
          <div id="directionDiv">
             <input type="radio" name="asymmetricDirection" onClick="submitAsymOperation1();">
             List <span id="listA1"></span> minus <span id="listB1"></span>
             </input>
             <input type="radio" name="asymmetricDirection" onClick="submitAsymOperation2();">
             List <span id="listB2"></span> minus <span id="listA2"></span>
             </input>
          </div>
          <html:hidden property="pageName" value="MyMine"/>
          <html:hidden property="listsButton" value="" styleId="listsButton"/>
          <html:hidden property="listLeft" value="" styleId="listLeft"/>
          <html:hidden property="listRight" value="" styleId="listRight"/>
        </c:if>

        </div>
      <c:if test="${statusSavedBag.key == 'CURRENT'}">
                </div>
      </c:if>
      </c:forEach>
      </html:form>

    </c:otherwise>
  </c:choose>
  <c:if test="${! empty PROFILE.invalidBags}">
    <h2>
      <fmt:message key="bags.invalid.intro"/>
    </h2>
    <p>
    <fmt:message key="bags.invalid.explanation"/>
    </p>
      <html:form action="/triageBag">
        <table class="bag-table sortable-onload-2 rowstyle-alt colstyle-alt no-arrow">

         <thead>
           <tr>
             <th style="display:none;"></th>
             <th>
               <input type="checkbox" id="selected_bag" onclick="selectColumnCheckbox(this.form, 'bag')">
             </th>
             <th align="left" nowrap class="sortable">
               <fmt:message key="query.savedbags.namecolumnheader"/>
             </th>
             <th align="left" nowrap class="sortable">
               <fmt:message key="query.savedbags.descriptioncolumnheader"/>
             </th>
             <th align="left" nowrap class="sortable">
               <fmt:message key="query.savedbags.typecolumnheader"/>
             </th>
             <th align="right" nowrap class="sortable-numeric">
               <fmt:message key="query.savedbags.countcolumnheader"/>
             </th>
             <th align="left" nowrap class="sortable">
               <fmt:message key="query.savedbags.datecreatedcolumnheader"/>
             </th>
           </tr>
         </thead>

         <c:forEach items="${PROFILE.invalidBags}" var="bagEntry" varStatus="status">
           <c:set var="index" value="${index+1}"/>
           <c:set var="bag" value="${bagEntry.value}"/>
           <tr>
             <td class="list-name" style="display:none;">${bag.name}</td> <%-- ID cell --%>
             <td class="sorting" align="center">                          <%-- Selection cell --%>
                <html:multibox property="selectedBags" styleId="selected_bag_${status.index}">
                  <c:out value="${bag.name}"/>
                </html:multibox>
              </td>
              <td class="sorting">                                        <%-- Name cell --%>
                <tiles:insert name="renamableElement.jsp">
                  <tiles:put name="name" value="${bag.name}"/>
                  <tiles:put name="type" value="bag"/>
                  <tiles:put name="state" value="NOT_CURRENT"/>
                  <tiles:put name="index" value="${status.count-1}"/>
                </tiles:insert>
                
                <tiles:insert name="setFavourite.tile">
                  <tiles:put name="name" value="${bag.name}"/>
                  <tiles:put name="type" value="bag"/>
                </tiles:insert>

                <c:if test="${PROFILE.loggedIn}">
                  <c:set var="taggable" value="${bag}"/>
                  <tiles:insert name="inlineTagEditor.tile">
                    <tiles:put name="taggable" beanName="taggable"/>
                    <tiles:put name="vertical" value="true"/>
                    <tiles:put name="show" value="true"/>"
                    <tiles:put name="onChangeCode" value="refreshTagSelect('mainSelect', 'bag')"/>
                  </tiles:insert>
                  <tiles:insert name="shareBag.tile">
                           <tiles:put name="bagName" value="${bag.name}"/>
                           <tiles:put name="isBagValid" value="false"/>
                           <tiles:put name="id" value="${index}"/>
                   </tiles:insert>
                </c:if>

              </td>
              <td class="sorting">                                        <%-- Description --%>
                <c:choose>
                  <c:when test="${fn:length(bag.description) > 100}">
                    ${fn:substring(bag.description, 0, 100)}...
                  </c:when>
                  <c:otherwise>
                    ${bag.description}
                  </c:otherwise>
                </c:choose>
                &nbsp;
              </td>
              <td class="sorting">                                         <%-- Type --%>
                <tiles:insert name="renamableElement.jsp">
                  <tiles:put name="name" value="${bag.name}"/>
                  <tiles:put name="type" value="invalid.bag.type"/>
                  <tiles:put name="index" value="${status.count-1}"/>
                  <tiles:put name="currentValue" value="${bag.type}"/>
                </tiles:insert>
              </td>
              <td id="size_${bag.name}" class="sorting" align="right">     <%-- Size --%>
                <c:out value="${bag.size}"/>&nbsp;value<c:if test="${bag.size != 1}">s</c:if>
              </td>
              <td class="sorting">                                         <%-- Date --%>
                  <im:dateDisplay date="${bag.dateCreated}"/>
              </td>
         </c:forEach>
        </table>

        <input type="button" onclick="validateBagOperations('triageBagForm', 'delete')" value="Delete"/>
        <input type="button" onclick="validateBagOperations('triageBagForm', 'export')" value="Export"/>

        <html:hidden name="newBagName" property="newBagName" value="__DUMMY-VALUE__"/>
        <html:hidden property="pageName" value="MyMine"/>
        <html:hidden property="listsButton" value="" styleId="listsButton"/>
      </html:form>
  </c:if>

<script type="text/javascript">
(function() {
  jQuery(document).ready(function() {
        jQuery("#directionDiv").hide();
  });
  
  jQuery(window).load(function(){
    <%-- sort bags by a remembered column --%>
    var order = im.getCookie("mymine.lists.order");
    if (order && parseInt(order)) {
      <%-- traverse all tables and call them custom sort --%>
      jQuery('table.bag-table').each(function() {
        fdTableSort.jsWrapper(jQuery(this).attr("id"), order);
      });
    }

    <%-- callback saving sort order of tables into a cookie --%>
    jQuery('table.bag-table').each(function() {
      var id = jQuery(this).attr("id"),
        that = this;
      window["sortCompleteCallback" + id] = function() {
        var th = jQuery(that).find("th.forwardSort");
        if (!jQuery(th).exists()) {
          th = jQuery(that).find("th.reverseSort");
        }
        im.setCookie("mymine.lists.order", th.attr("class").replace(/[^0-9.]/g, ""));
      };
    });
  });

  jQuery("#historyBagDiv input[name='selectedBags']").click(function() {
      hideDirectionDiv();
      var checked = jQuery("#historyBagDiv input[name='selectedBags']:checked");
      var selected = checked.length;
      if (selected > 1 ) {
        jQuery("#listA1").html(checked[0].value);
        jQuery("#listB1").html(checked[1].value);
        jQuery("#listA2").html(checked[0].value);
        jQuery("#listB2").html(checked[1].value);
      }
  });
  <%-- ##### --%>

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

                      if (list['status'] == 'TO_UPGRADE' && !newList.find('td.upgrade').exists()) {
                        jQuery('<td/>', {
                          'class': 'upgrade'
                        })
                        .append(jQuery('<a/>', {
                          'href': '<html:rewrite action="/bagUpgrade"/>' + '?bagName=' + newList.find('td.list-name').text().replace(/ /g, '+'),
                          'class': 'bagToUpgrade'
                        }))
                        .appendTo(newList);
                      } else if (list['status'] == 'CURRENT') {
                        var listSize = list['size'];
                        if (listSize != undefined) {
                          newList.find('td[id^="size"]').text(function() {
                            return listSize + " value" + ((listSize != 1) ? 's' : '');
                          });
                        }
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

function showAsymmetricDirection() {
    var checked = jQuery("#historyBagDiv input[name='selectedBags']:checked");
    var selected = checked.length;
    if (selected > 1 ) {
      showDirectionDiv();
    }
}
function submitAsymOperation1() {
    jQuery("#listsButton").val("asymmetricdifference");
    jQuery("#listLeft").val(jQuery("#listA1").html());
    jQuery("#listRight").val(jQuery("#listB1").html());
    validateBagOperations('modifyBagForm', 'asymmetricdifference');
  }
  function submitAsymOperation2() {
    jQuery("#listsButton").val("asymmetricdifference");
    jQuery("#listLeft").val(jQuery("#listB2").html());
    jQuery("#listRight").val(jQuery("#listA2").html());
    validateBagOperations('modifyBagForm', 'asymmetricdifference');
  }

</script>
</im:body>

<!-- /historyBagView.jsp -->
