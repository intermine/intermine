<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- bagDetails.jsp -->

<html:xhtml/>

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
//]]>-->
</script>
<script type="text/javascript" src="<html:rewrite page='/js/inlinetemplate.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/widget.js'/>"></script>

<%-- CSS framework --%>
<link rel="stylesheet" type="text/css" href="css/960gs.css" />

<div id="header_wrap">
  <div id="object_header" class="table">
    <a name="summary"></a>
    <div class="title">
      <h1 class="title">
          ${bag.type} list analysis: <strong>${bag.name}</strong> <a class="rename">rename</a>
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
                <a class="edit">edit</a>
              </c:when>
              <c:otherwise>
                <p><span class="text"></span></p> <a class="edit">set</a>
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
		            var t = jQuery('table.fields td.description div.text span.text').html();
		            if (t.length > 120) {
		                jQuery('table.fields td.description div.text p').html(
		                    '<span class="text">' + t.substring(0, 100) + '</span>' +
		                    '<span class="ellipsis">&hellip;</span> ' +
		                    '<span class="hidden">' + t.substring(100, t.length) + '</span>'
		                );
		                // toggler
		                jQuery('table.fields td.description div.text p').append('<a class="more">more</a>');
		            } else {
		                jQuery('table.fields td.description div.text p').html('<span class="text">' + t + '</span>');
		                // toggler
		                jQuery('table.fields td.description div.text a.more').remove();
		            }
		          }
		          moreDescription();

		          /** description edit toggler */
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
		                  jQuery('table.fields td.description div.text a.edit').html('Edit');
		                }
		                else {
		                  jQuery('table.fields td.description div.text a.edit').html('Set');
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
        <a name="list"><h2>List of ${bag.size}&nbsp;${bag.type}<c:if test="${bag.size != 1}">s</c:if>&nbsp;<div
        class="button">
            <div class="left"></div>
            <input type="button" value="+ Show">
            <div class="right"></div>
        </div>
    </h2></a>
    </div>

    <div class="clear"></div>

    <%-- convert to a different type & orthologues --%>
    <div class="box grid_4" id="convertList">
      <div class="feature convert">
        <h3 class="goog">Convert to a different type</h3>
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
    <div class="box grid_4" id="download">
      <div class="feature">
        <h3 class="goog">Download</h3>
        <c:set var="tableName" value="bag.${bag.name}" scope="request"/>
        <c:set var="pagedTable" value="${pagedResults}" scope="request"/>
        <tiles:get name="export.tile"/>
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

    <div class="clear"></div>

    <div id="ListArea" style="display:none;">

    <%-- list table --%>
    <div id="results" class="box grid_12">

      <%-- find in list & toolbox --%>
      <div class="toolbox">
        <div class="tool">
          <h5>Find in list</h5>
          <html:form styleId="findInListForm" action="/findInList">
            <input type="text" name="textToFind" id="textToFind"/>
            <input type="hidden" name="bagName" value="${bag.name}"/>
            <html:submit>Go</html:submit>
          </html:form>
        </div>
        <html:form action="/modifyBagDetailsAction" styleId="bagDetailsForm">
          <html:hidden property="bagName" value="${bag.name}"/>
          <div class="tool">
            <h5>Add records to another list</h5>
            <c:choose>
              <c:when test="${!empty PROFILE.savedBags && fn:length(PROFILE.savedBags) > 1}">
                <html:select property="existingBagName">
                  <c:forEach items="${PROFILE.savedBags}" var="entry">
                    <c:if test="${param.bagName != entry.key}">
                      <html:option value="${entry.key}">${entry.key} [${entry.value.type}]</html:option>
                    </c:if>
                  </c:forEach>
                </html:select>
                <input type="submit" name="addToBag" id="addToBag" value="Add" />
                  <script type="text/javascript" charset="utf-8">
                    jQuery('#addToBag').attr('disabled','disabled');
                  </script>
              </c:when>
              <c:otherwise>
                <em>Login to add records to another list.</em>
              </c:otherwise>
            </c:choose>
          </div>
          <div class="tool">
            <h5>Remove records from results</h5>
            <input type="submit" name="removeFromBag" id="removeFromBag" value="Remove selected from list" disabled="true" />
          </div>
        </html:form>
      </div>

      <div class="pagination">
        <tiles:insert name="paging.tile">
          <tiles:put name="resultsTable" beanName="pagedResults" />
            <tiles:put name="currentPage" value="bagDetails" />
            <tiles:put name="bag" beanName="bag" />
        </tiles:insert>
      </div>
	  <div class="clear"></div>
	  <div class="collection-table nowrap nomargin">
		  <div style="overflow-x:auto;">
		      <tiles:insert name="resultsTable.tile">
		        <tiles:put name="pagedResults" beanName="pagedResults" />
		          <tiles:put name="currentPage" value="bagDetails" />
		          <tiles:put name="bagName" value="${bag.name}" />
		          <tiles:put name="highlightId" value="${highlightId}"/>
		      </tiles:insert>
	      </div>

	      <div class="selected">
	        <span class="desc">Selected:</span>
	        <span id="selectedIdFields"></span>
	      </div>
      </div>
    </div>

    <script type="text/javascript">
    	(function() {
		      // will show/hide the results table and toolbox & change the link appropriately (text, ico)
		      function toggleResults() {
		        // expanding or contracting?
		        jQuery('#ListArea').toggle();

		        if (jQuery("#ListCategory h2 div.button").hasClass('active')) {
		          jQuery("#ListCategory h2 div.button input").attr('value', '+ Show');
		        } else {
		          jQuery("#ListCategory h2 div.button input").attr('value', '- Hide');
		        }

		        jQuery("#ListCategory h2 div.button").toggleClass('active');
		      }
		      // let us not forget that results will be shown on successful search and when paginating that requires synchronous call
		      <c:if test="${not empty param.gotoHighlighted || not empty param.page || not empty param.table}">
		        jQuery(document).ready(function() { toggleResults(); });
		      </c:if>

		      // shuffle "selected" around:
		      jQuery("#results b").not("table.results b").remove();
		      var s = jQuery.trim(jQuery("#results span#selectedIdFields").first().html());
		      jQuery("#results span#selectedIdFields").first().remove();
		      jQuery("#results span#selectedIdFields").html(s);

		      if (s.length == 0) { // hide when we have not selected anything before reload
		        jQuery('#results div.selected span.desc').hide();
		      }

		      // monitor checkboxes for changes
		      jQuery('#results input').each(function(index) {
		        jQuery(this).click(function() {
		          if (jQuery('#results div.selected span.desc').is(':visible')) {
		            if (jQuery('#results div.selected #selectedIdFields').html().length == 0) {
		                jQuery('#results div.selected span.desc').hide();
		            }
		          } else {
		            if (jQuery('#results div.selected #selectedIdFields').html().length > 0) {
		                jQuery('#results div.selected span.desc').show();
		            }
		          }
		        });
		      });

		      // apply highlight class to rows that were selected before reload
		      jQuery('#results input:checked').each(function(index) {
		        jQuery(this).parent().parent().find('td').not('th td').addClass('highlightCell');
		      });

		      // toggler event
		      jQuery('#ListCategory h2 div.button').click(function() {
		        toggleResults();
		      });
    	})();
    </script>

    <div class="clear"></div>

    </div>

    </div>

    <div class="clear"></div>

    <%-- widgets --%>
    <div id="WidgetsCategory" class="aspectBlock">
    <div class="box grid_12">
      <a name="widgets"><h2>Widgets displaying properties of '${bag.name}'</h2></a>
      <ol class="widgetList">
        <c:forEach items="${widgets}" var="widget">
          <li><a title="toggle widget" href="javascript:toggleWidget('widgetcontainer${widget.id}','togglelink${widget.id}')" id="togglelink${widget.id}" class="active">${widget.title}</a></li>
        </c:forEach>
      </ol>
    </div>

    <script language="javascript">
      function toggleWidget(widgetid,linkid) {
        jQuery('#'+widgetid).toggle();
        if(jQuery('#'+linkid).hasClass('active')) {
          jQuery('#'+linkid).removeClass('active');
          AjaxServices.saveToggleState(widgetid, false);
        } else {
          jQuery('#'+linkid).addClass('active');
          AjaxServices.saveToggleState(widgetid, true);
        }
      }
    </script>

    <link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/widget.css'/>"/>

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

    <div class="clear"></div>

    <div id="TemplatesCategory" class="aspectBlock">
    <div class="box grid_12">
        <a name="templates"><h2>Templates</h2></a>
    </div>

    <%-- templates --%>
    <div class="box grid_12">
      <div class="feature">
        <c:set var="templateIdPrefix" value="bagDetailsTemplate${bag.type}"/>
        <c:set value="${fn:length(CATEGORIES)}" var="aspectCount"/>
        <h3 class="goog">Template results for '${bag.name}' <span>(<a href="javascript:toggleAll(${aspectCount}, '${templateIdPrefix}', 'expand', null, true);">expand all <img src="images/disclosed.gif"/></a> / <a href="javascript:toggleAll(${aspectCount}, '${templateIdPrefix}', 'collapse', null, true);">collapse all <img src="images/undisclosed.gif"/></a>)</span></h3>
        <fmt:message key="bagDetails.templatesHelp">
            <fmt:param><img src="images/disclosed.gif"/> / <img src="images/undisclosed.gif"/></fmt:param>
        </fmt:message>
      </div>
    </div>

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
</c:otherwise>
</c:choose>

</div>