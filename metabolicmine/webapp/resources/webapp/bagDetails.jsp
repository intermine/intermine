<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- bagDetails.jsp -->

<div class="body">

<c:choose>
<c:when test="${!empty bag}">

<script type="text/javascript">
  <%-- the number of entries to show in References & Collections before switching to "show all" --%>
  var numberOfTableRowsToShow = '${object.numberOfTableRowsToShow}'; <%-- required on report.js --%>
  numberOfTableRowsToShow = (numberOfTableRowsToShow == '') ? 30 : parseInt(numberOfTableRowsToShow);
</script>
<script type="text/javascript" src="js/report.js"></script>

<script type="text/javascript">
<!--//<![CDATA[
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
  var detailsType = 'bag';
  var webappUrl = "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/";
  var service = webappUrl + "service/";
//]]>-->
</script>
<script type="text/javascript" src="<html:rewrite page='/js/inlinetemplate.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/intermine.api.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/intermine.widgets.js'/>"></script>

<%-- CSS framework --%>
<link rel="stylesheet" type="text/css" href="css/960gs.css" />

<div id="header_wrap">
  <div id="object_header" class="table">
    <a name="summary"></a>
    <div class="title">
      <h1 class="title">
          ${bag.type} list analysis: <strong>${bag.name}</strong> <c:if test="${!isBagPublic}"><a class="rename">rename</a></c:if>
      </h1>
      <div class="form" style="display:none;">
        <input type="text" class="text" value="${bag.name}" />
        <input type="button" class="save" value="Save" />
      </div>
    </div>
    <script type="text/javascript">
      <%-- show form --%>
      (function() {
        jQuery('div.title a').click(function() {
          jQuery('div.title div.form').show();
          jQuery('div.title h1').hide();
        });

        <%-- current name --%>
        var currentName = '${bag.name}';

        <%-- save name --%>
        function saveName() {
          var newName = jQuery('div.title input.text').attr('value').replace(/^\s*|\s*$/g,"");
          AjaxServices.rename(currentName, 'bag', newName, function(result) {
              if (result.indexOf("Invalid name") == 0) {
                  // invalid name
                  jQuery('#error_msg.errors').prepend(result).show();
                  jQuery('div.title div.form').hide();
                  jQuery('div.title h1').show();
              } else  if (result.indexOf("already exists") > -1 && result.indexOf("<i>") == 0) {
                  // name already exists
                  jQuery('#error_msg.errors').prepend(result.replace(/<\/?[^>]+>/gi, "")).show();
                  jQuery('div.title div.form').hide();
                  jQuery('div.title h1').show();
              } else {
                  // OK
                  if (currentName != result) { // we will need to redirect
                    var urlObj = jQuery(location);
                    var url = jQuery(location).attr('href');
                    // replace the list name in "bagName/name=" before next "&"
                    var a = url.indexOf("name=");
                    if (a > -1) {
                      var z = url.substring(a, url.length).indexOf("&");
                      // redirect
                      urlObj.attr('href', url.substring(0, a + 5) + result.replace(/ /g,"+") + url.substring(a + z, url.length));
                    } else {
                      a = url.indexOf("bagName=");
                      // redirect
                      urlObj.attr('href', url.substring(0, a + 8) + result.replace(/ /g,"+"));
                    }
                  } else { // no name change
                    jQuery('div.title div.form').hide();
                    jQuery('div.title h1').show();
                  }
              }
          });
        }
        jQuery('div.title input.save').click(function() {
          saveName();
        });
        jQuery('div.title input.text').bind('keyup', function(e) {
          if((e.keyCode ? e.keyCode : e.which) == 13) {
            saveName();
          }
        });
      })();
    </script>

    <%-- tags --%>
    <c:if test="${PROFILE.loggedIn}">
      <div class="tags">
        <c:set var="taggable" value="${bag}"/>
        <tiles:insert name="inlineTagEditor.tile">
          <tiles:put name="taggable" beanName="taggable"/>
          <tiles:put name="vertical" value="true"/>
          <tiles:put name="show" value="true"/>
        </tiles:insert>
      </div>
    </c:if>

    <%-- meta --%>
    <table class="fields">
      <tr>
        <td class="nobr slim">Results: ${bag.size}&nbsp;${bag.type}<c:if test="${bag.size != 1}">s</c:if></td>
        <td class="nobr slim">Created: <fmt:formatDate dateStyle="full" timeStyle="full" value="${bag.dateCreated}" /></td>
        <td class="description">
          Description:
          <div class="text">
            <c:choose>
              <c:when test="${! empty bag.description}">
                <p><span class="text"><c:out value="${bag.description}" escapeXml="false" /></span></p>
                <c:if test="${!isBagPublic}"><a class="edit">edit</a></c:if>
              </c:when>
              <c:otherwise>
                <p><span class="text"></span></p> <c:if test="${!isBagPublic}"><a class="edit">set</a></c:if>
              </c:otherwise>
            </c:choose>
          </div>

          <div class="form" style="display:none;">
            <c:set var="bagDescriptionLine" value='${fn:replace(bag.description, "<br/>", "")}' />
            <input type="text" class="text" value="${bagDescriptionLine}" />
            <input type="button" class="save" value="Save" />
          </div>
        </td>
        <script type="text/javascript">
          (function() {
              /** show more */
              function moreDescription() {
                var e = jQuery('table.fields td.description div.text span.text');
                var t = e.html();
                e.remove();
                if (t.length > 120) {
                    jQuery('table.fields td.description div.text p').html('')
                    .append(jQuery('<span/>', {
                        'class': 'text',
                        'text': t.substring(0, 100)
                    }))
                    .append(jQuery('<span/>', {
                        'class': 'ellipsis',
                        'html': '&hellip;'
                    }))
                    .append(jQuery('<span/>', {
                        'class': 'hidden',
                        'text': t.substring(100, t.length)
                    }))
                    .append(jQuery('<a/>', {
                        'class': 'more',
                        'text': 'more'
                    }));
                } else {
                  jQuery('<span/>', {
                      'class': 'text',
                      'text': t
                  }).appendTo('table.fields td.description div.text p');

                    // toggler
                    jQuery('table.fields td.description div.text a.more').remove();
                }
              }
              moreDescription();

              /** description show more toggler */
              jQuery('table.fields td.description a.more').live('click', function() {
                jQuery(this).parent().find('span.text').html(
                    jQuery(this).parent().find('span.text').html() + jQuery(this).parent().find('span.hidden').html()
                );
                jQuery(this).parent().find('span').not('span.text').remove();
                jQuery(this).remove();
              });

              /** description edit toggler */
              jQuery('table.fields td.description a.edit').click(function() {
                // hide the div
                jQuery('table.fields td.description div.text').hide();
                // show the form
                jQuery('table.fields td.description div.form').show();
                // focus, make it usable
                jQuery('table.fields td.description input.text').focus();
              });

              /** Save description */
              function saveDescription() {
                var newDescription = jQuery('table.fields td.description input.text').val().replace(/[\n\r]+/g, "\n\r<br/>");
                AjaxServices.saveBagDescription('${bag.name}', newDescription, function(returnedDescription){
                    // populate the value
                    jQuery('table.fields td.description div.text span.text').html(returnedDescription);
                    // switch the visibility
                    jQuery('table.fields td.description div.text').show();
                    jQuery('table.fields td.description div.form').hide();

                    // set the link name based on whether we've cleared or not
                    if (returnedDescription.length > 0) {
                      jQuery('table.fields td.description div.text a.edit').html('edit');
                    }
                    else {
                      jQuery('table.fields td.description div.text a.edit').html('set');
                    }

                    moreDescription();
                });
              }
              jQuery('table.fields td.description input.text').bind('keyup', function(e) {
                 if((e.keyCode ? e.keyCode : e.which) == 13) {
                   saveDescription();
                 }
              });
              jQuery('table.fields td.description input.save').click(function() {
                 saveDescription();
              });
          })();
        </script>
      </tr>
    </table>

    <div class="clear"></div>

  </div>

</div>

<div id="content">

  <%-- menu --%>
  <div id="menu-target">&nbsp;</div>
  <div id="toc-menu-wrap">
    <tiles:insert name="bagDetailsMenu.jsp" />
  </div>
  <div id="fixed-menu">
    <tiles:insert name="bagDetailsMenu.jsp" />
  </div>
  <script type="text/javascript">
    (function() {
      jQuery('#fixed-menu').hide(); // hide for IE7
      jQuery(window).scroll(function() {
        // transition fix
        if (jQuery('#menu-target').isInView('partial')) {
          jQuery('#fixed-menu').hide();
        } else {
          jQuery('#fixed-menu').show();
        }
      });
    })();
  </script>

  <%-- main content --%>
  <div id="content" class="container_12">

    <div id="ListCategory" class="aspectBlock">
    <div class="box grid_4">
        <a name="list"><h2>List of ${bag.size}&nbsp;${bag.type}<c:if test="${bag.size != 1}">s</c:if>
    </h2></a>
    </div>

    <div class="clear"></div>

  <%-- show expanded table immediately? --%>
  <c:choose>
    <c:when test="${not empty param.gotoHighlighted || not empty param.page || not empty param.table}">
      <script type="text/javascript">var showTable = true;</script>
    </c:when>
    <c:otherwise>
      <script type="text/javascript">
        if (window.location.hash == '#hasHighlighted') var showTable = true;
      </script>
    </c:otherwise>
  </c:choose>

  <div id="ListArea">

    <%-- list table --%>
    <div id="results" class="box grid_12">

    <div id="list-table" class="collection-table nowrap results">
      <div style="overflow-x:auto;">
          <tiles:insert name="resultsTable.tile">
            <tiles:put name="pagedResults" beanName="pagedResults" />
              <tiles:put name="currentPage" value="bagDetails" />
              <tiles:put name="bagName" value="${bag.name}" />
              <tiles:put name="highlightId" value="${highlightId}"/>
          </tiles:insert>
        </div>
      </div>
    </div>

    <div class="clear"></div>

    </div>

  <div class="clear"></div>

  <div class="grid_8">

      <%-- convert to a different type & orthologues --%>
      <div class="box grid_6" id="convertList">
        <div class="feature convert">
          <html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
          <html:hidden property="bagName" value="${bag.name}"/>
            <tiles:insert name="convertBag.tile">
              <tiles:put name="bag" beanName="bag" />
              <tiles:put name="idname" value="cp" />
              <tiles:put name="orientation" value="h" />
            </tiles:insert>
          </html:form>
        </div>
      </div>

      <%-- download list --%>
      <div class="box grid_6" id="download">
        <div class="feature">
          <h3 class="goog">Download</h3>
          <c:set var="tableName" value="bag.${bag.name}" scope="request"/>
          <c:set var="pagedTable" value="${pagedResults}" scope="request"/>
          <tiles:get name="export.tile"/>
        </div>
      </div>
    </div>

    <%-- external links --%>
    <div class="box grid_4" id="externalLinks">
      <div class="feature">
      <html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
        <html:hidden property="bagName" value="${bag.name}"/>
        <tiles:insert page="/bagDisplayers.jsp">
          <tiles:put name="bag" beanName="bag"/>
          <tiles:put name="showOnLeft" value="false"/>
        </tiles:insert>
      </html:form>
      </div>
    </div>

    </div>

    <div class="clear"></div>

    <%-- widgets --%>
    <div id="WidgetsCategory" class="aspectBlock">
    <div class="box grid_12">
      <a name="widgets"><h2>Widgets displaying properties of '${bag.name}'</h2></a>
      <ol class="widgetList">
        <c:forEach items="${widgets}" var="widget">
          <li><a title="toggle widget" name="${widget.id}" class="active">${widget.title}</a></li>
        </c:forEach>
      </ol>
    </div>

    <script type="text/javascript">
    (function() {
        <%-- widget toggler --%>
        jQuery('ol.widgetList li a').each(function() {
            jQuery(this).click(function() {
                jQuery(this).toggleClass('active');
                var widgetName = jQuery(this).attr('name');
                var widget = jQuery('#' + widgetName + '-widget').toggle();
                AjaxServices.saveToggleState('widgetcontainer' + widgetName, widget.is(":visible"));
                });
            });
        })();
    </script>

    <link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/widget.css'/>"/>
    <script type="text/javascript">
        window.widgets = new intermine.widgets(window.service, "${token}");
    </script>
    <c:forEach items="${widgets}" var="widget">
    <div class="box">
      <div class="feature">
        <tiles:insert name="widget.tile">
          <tiles:put name="widget" beanName="widget"/>
          <tiles:put name="bag" beanName="bag"/>
          <tiles:put name="widget2extraAttrs" beanName="widget2extraAttrs" />
        </tiles:insert>
      </div>
    </div>
    </c:forEach>

    </div>

    <div class="clear"></div>

    <div id="TemplatesCategory" class="aspectBlock">
    <div class="box grid_12">
        <a name="templates"><h2>Template results for '${bag.name}'</h2></a>
    </div>

    <div class="box grid_12">
        <%-- Each aspect --%>
        <c:forEach items="${CATEGORIES}" var="aspect" varStatus="status">
          <tiles:insert name="reportAspect.tile">
            <tiles:put name="placement" value="im:aspect:${aspect}"/>
            <tiles:put name="trail" value="|bag.${bag.name}"/>
            <tiles:put name="interMineIdBag" beanName="bag"/>
            <tiles:put name="aspectId" value="${templateIdPrefix}${status.index}" />
            <tiles:put name="opened" value="${status.index == 0}" />
          </tiles:insert>
        </c:forEach>
    </div>
  </div>
</div>

</c:when>
<c:otherwise>

  <script type="text/javascript">
    (function() {
      jQuery(document).ready(function() {
        // parse the message because the messaging is eeeu
        var m = jQuery(jQuery('#error_msg.topBar.errors').html()).text().replace('Hide', '').trim();
        // append link to all lists
        var l = ', <html:link styleClass="inline" action="/bag?subtab=view">view all lists</html:link> instead.';
        jQuery('#error_msg.topBar.errors').html(jQuery('#error_msg.topBar.errors').html().replace(m, m+l));
      });
    })();
  </script>

<!--  No list found with this name -->
<div class="bigmessage">
 <br />
 <html:link action="/bag?subtab=view">View all lists</html:link>
</div>

</c:otherwise>
</c:choose>

</div>