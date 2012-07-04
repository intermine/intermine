<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- queryBuilderSummary.jsp -->

<html:xhtml/>
<script>
  function showInModel(path) {
	var nodes  = path.split('.');
	if (nodes.length > 3) {
		jQuery('<div/>', {
			'class': 'loading',
			'style': 'top:'
		}).appendTo(jQuery("#queryBuilderBrowser"));
	}
	
    <%-- 'collapse' all expanded --%>
    jQuery("div.browserline").each(function(index) {
    	if (index > 0 && !jQuery(this).hasClass('indent-1')) {
    		jQuery(this).remove();
    	} else {
    		var img = jQuery(this).find('img.toggle');
    		if (img.attr('src') != 'images/plus-disabled.gif') {
    			img.attr('src', 'images/plus.gif');
    		}
    	}
    });
    
    <%-- these nodes will need to be expanded --%>
    var result = [];
    for (var i = 1; i < nodes.length; i++) {
        var node = [];
        for (var j = 0; j <= i; j++) {
        	node.push(nodes[j]);
        }
        result.push(node.join('.'));
    };
    deQueue();
    
    <%-- enqueue toggling mainly to switch the toggler image in time --%>
    function deQueue() {
    	var path  	= result.splice(0, 1)[0],
			id    	= path.replace(/\./g, "\\."),
			image 	= jQuery("img#img_" + id + '.toggle'),
			browser = jQuery("#browserbody");
	    jQuery.get('<html:rewrite action="/queryBuilderChange"/>' + '?method=ajaxExpand&path=' + (path),
	      	function(data) {
	  	        jQuery("div#" + id).after(data);
	  	    	image.attr('src', 'images/minus.gif');
	  	    	if (result.length > 0) {
	  	    		deQueue();
	  	    	} else {
	  	    		browser.animate({
	  	    			scrollTop: jQuery("div#" + id).offset().top - browser.height() + browser.scrollTop()
	  	    			}, 'fast', function() {
	  	    				jQuery("#queryBuilderBrowser").find('div.loading').remove();
	  	    				jQuery("div#" + id).highlight();
	  	    			}
	  	    		);
	  	    	}
	      	}
	    );
    }
    
    return false;
  }

  function editConstraint(path, code, displayPath) {
    displayPath = displayPath || path;
    new Ajax.Updater('queryBuilderConstraint', '<html:rewrite action="/queryBuilderChange"/>',
      {parameters:'method=ajaxEditConstraint&code='+code,
       asynchronous:true, evalScripts:true,
      onSuccess: function() {
        new Ajax.Updater('query-builder-summary', '<html:rewrite action="/queryBuilderChange"/>',
          {parameters:'method=ajaxRenderPaths', asynchronous:true, evalScripts:true, onSuccess: function() {
          new Boxy(jQuery('#constraint'), {title: "Constraint for " + displayPath, modal: true, unloadOnHide: true})
          }
        });
      }
    });
    return false;
  }

  function editTemplateConstraint(path, code, displayPath ) {
    displayPath = displayPath || path;
    new Ajax.Updater('queryBuilderConstraint', '<html:rewrite action="/queryBuilderChange"/>',
      {parameters:'method=ajaxEditTemplateConstraint&code='+code,
       asynchronous:true, evalScripts:true,
      onSuccess: function() {
        new Ajax.Updater('query-builder-summary', '<html:rewrite action="/queryBuilderChange"/>',
          {parameters:'method=ajaxRenderPaths', asynchronous:true, evalScripts:true, onSuccess: function() {
             new Boxy(jQuery('#constraint'), {title: "Constraint for " + displayPath, modal: true, unloadOnHide: true})
          }
        });
      }
    });
    return false;
  }

  function editSwitchableConstraint(path, code, displayPath) {
    displayPath = displayPath || path;
      new Ajax.Updater('queryBuilderConstraint', '<html:rewrite action="/queryBuilderChange"/>',
        {parameters:'method=ajaxEditSwitchableConstraint&code='+code,
         asynchronous:true, evalScripts:true,
        onSuccess: function() {
          new Ajax.Updater('query-builder-summary', '<html:rewrite action="/queryBuilderChange"/>',
            {parameters:'method=ajaxRenderPaths', asynchronous:true, evalScripts:true, onSuccess: function() {
               new Boxy(jQuery('#constraint'), {title: "Constraint for " + displayPath, modal: true});
            }
          });
        }
      });
      return false;
    }

  function editJoinStyle(path, displayPath) {
    displayPath = displayPath || path;
    new Ajax.Updater('queryBuilderConstraint', '<html:rewrite action="/queryBuilderChange"/>',
      {parameters:'method=ajaxEditJoinStyle&path='+path,
       asynchronous:true, evalScripts:true,
      onSuccess: function() {
        new Ajax.Updater('query-builder-summary', '<html:rewrite action="/queryBuilderChange"/>',
          {parameters:'method=ajaxRenderPaths', asynchronous:true, evalScripts:true, onSuccess: function() {
            new Boxy(jQuery('#constraint'), {title: "Switch Join Style " + displayPath, modal: true});
          }
        });
      }
    });
    return false;
  }
  //-->
</script>

<div class="heading currentTitle">
  <fmt:message key="query.currentquery"/>
</div>
<div class="body">
<br/>
<c:choose>
  <c:when test="${empty summaryPaths}">
    <div class="smallnote altmessage"><fmt:message key="query.empty"/></div>
  </c:when>
  <c:otherwise>
    <c:forEach var="path" items="${summaryPaths}">
      <div>
        <div style="white-space: nowrap">
          <div>
            <c:if test="${path.indentation > 0}">
              <c:forEach begin="1" end="${path.indentation}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </c:forEach>
            </c:if>
            <im:viewableSpan path="${path.pathString}" viewPaths="${viewPaths}" test="${!empty path.fieldName}" idPrefix="query">
              <span class="attributeField"><c:out value="${imf:formatField(path.path, WEBCONFIG)}"/></span>
            </im:viewableSpan>
            <span class="type">
              <c:if test="${!path.attribute}">
                <fmt:message var="changePath" key="query.changePath">
                    <fmt:param value="${path.type}"/>
                </fmt:message>
                <im:viewableSpan path="${path.pathString}" viewPaths="${viewPaths}" test="${empty path.fieldName}" idPrefix="query">
                  <html:link action="/queryBuilderChange?method=changePath&amp;path=${path.pathString}"
                   onclick="return showInModel('${path.pathString}');"
                   title="${changePath}"><span class="type"><c:out value="${imf:formatPathStr(path.type, INTERMINE_API, WEBCONFIG)}"/></span></html:link>
                </im:viewableSpan><c:if test="${path.collection}">&nbsp;<fmt:message key="query.collection"/></c:if>
              </c:if>
            </span>
            <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
              <fmt:param value="${path.friendlyName}"/>
            </fmt:message>
            <c:if test="${!path.locked}">
              <fmt:message key="query.removeNodeTitle" var="removeNodeTitle">
                <fmt:param value="${path.friendlyName}"/>
              </fmt:message>
              <c:choose>
                <%-- View only --%>
                <c:when test="${(empty path.constraints) && (!empty viewPaths[path.pathString])}">
                  <html:link action="/queryBuilderViewChange?method=removeFromView&amp;path=${path.pathString}"
                           title="${removeNodeTitle}">
                    <img border="0" src="images/cross.gif" width="13" height="13"
                       title="${removeNodeTitle}"/>
                  </html:link>
                </c:when>
                <%-- Constraint --%>
                <c:otherwise>
                  <html:link action="/queryBuilderChange?method=removeNode&amp;path=${path.pathString}"
                           title="${removeNodeTitle}">
                    <c:if test="${path.indentation != 0}">
                      <img border="0" src="images/cross.gif" width="13" height="13"
                         title="${removeNodeTitle}"/>
                  </c:if>
                  </html:link>
                </c:otherwise>
              </c:choose>
            </c:if>
            <c:if test="${path.locked}">
              <img border="0" src="images/discross.gif" width="13" height="13"
                   title="x" title="<fmt:message key="query.disabledRemoveNodeTitle"/>"/>
            </c:if>
            <c:if test="${!path.forcedInnerJoin}">
              <c:choose>
                <c:when test="${QUERY.outerMap[path.pathString] && !path.attribute && !empty path.parent}">
                  <fmt:message key="query.editConstraintTitle" var="editConstraintTitle"/>
                  <html:link action="/queryBuilderChange?method=editJoinStyle&amp;path=${path.pathString}"
                        onclick="return editJoinStyle('${path.pathString}', '${imf:formatPathStr(path.pathString, INTERMINE_API, WEBCONFIG)}');"
                         title="${editConstraintTitle}">
                    <img border="0" src="images/join_outer.png" width="13" height="13"
                     title="Outer join"/>
                  </html:link>
                </c:when>
                <c:when test="${!QUERY.outerMap[path.pathString] && !path.attribute && !empty path.parent}">
                  <fmt:message key="query.editConstraintTitle" var="editConstraintTitle"/>
                  <html:link action="/queryBuilderChange?method=editJoinStyle&amp;path=${path.pathString}"
                        onclick="return editJoinStyle('${path.pathString}', '${imf:formatPathStr(path.pathString, INTERMINE_API, WEBCONFIG)}');"
                         title="${editConstraintTitle}">
                    <img border="0" src="images/join_inner.png" width="13" height="13"
                     title="Inner join"/>
                  </html:link>
                </c:when>
              </c:choose>
            </c:if>
          </div>
          <c:if test="${path.subclass != null}">
            <div>
              <c:forEach begin="0" end="${path.indentation}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </c:forEach>
              <fmt:message key="query.subclassConstraint" var="msg"/>
              <span class="constraint">
                <c:out value="${msg}"/>&nbsp;
                <c:out value="${imf:formatPathStr(path.subclass, INTERMINE_API, WEBCONFIG)}"/>
              </span>
              <fmt:message key="query.removeConstraintTitle" var="removeConstraintTitle"/>
              <html:link action="/queryBuilderChange?method=removeSubclass&amp;path=${path.pathString}" title="${removeConstraintTitle}">
                <img border="0" src="images/cross.gif" width="13" height="13" title="${removeConstraintTitle}"/>
              </html:link>
            </div>
          </c:if>
          <c:forEach var="constraint" items="${path.constraints}" varStatus="status">
              <div>
                <c:forEach begin="0" end="${path.indentation}">
                  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                </c:forEach>
                <span class="constraint">
                  <im:displayableOpName opName="${constraint.op}" valueType="${path.type}"/>
                      <c:out value=" ${constraint.value}"/>
                </span>
                <fmt:message key="query.removeConstraintTitle" var="removeConstraintTitle"/>
                <html:link action="/queryBuilderChange?method=removeConstraint&amp;code=${constraint.code}"
                           title="${removeConstraintTitle}">
                  <img border="0" src="images/cross.gif" width="13" height="13"
                       title="Remove this constraint"/>
                </html:link>&nbsp;<fmt:message key="query.editConstraintTitle" var="editConstraintTitle"/><html:link action="/queryBuilderChange?method=editConstraint&amp;code=${constraint.code}"
                    onclick="return editConstraint('${path.pathString}', '${constraint.code}', '${imf:formatPathStr(path.pathString, INTERMINE_API, WEBCONFIG)}')"
                           title="${editConstraintTitle}">
                  <img border="0" src="images/edit.gif" width="13" height="13"
                       title="Edit this constraint"/>
                </html:link>
                <c:if test="${EDITING_TEMPLATE != null || NEW_TEMPLATE != null}">
                  <c:choose>
                    <c:when test="${constraint.validEditableConstraintType}">
                      <html:link action="/queryBuilderChange?method=editTemplateConstraint&amp;code=${constraint.code}"
                                 titleKey="templateBuilder.editTemplateConstraint.linktitle" onclick="return editTemplateConstraint('${path.pathString}', '${constraint.code}', '${imf:formatPathStr(path.pathString, INTERMINE_API, WEBCONFIG)}')" >
                        <c:choose>
                          <c:when test="${constraint.editableInTemplate}">
                            <img border="0" src="images/unlocked.gif" width="13" height="13" title="Unlocked"/>
                          </c:when>
                          <c:otherwise>
                            <img border="0" src="images/locked.gif" width="13" height="13" title="Locked"/>
                          </c:otherwise>
                        </c:choose>
                      </html:link>
                    </c:when>
                    <c:otherwise>
                      <img border="0" src="images/locked-disabled.gif" width="13" height="13" title="Locked"
                        title="<fmt:message key="templateBuilder.constraintNotEditable"/>"/>
                    </c:otherwise>
                  </c:choose>
                  <c:if test="${constraint.editableInTemplate}">
                    <c:choose>
                      <c:when test="${constraint.switchable == 'locked'}">
                        <span class="optionalConstraint">required</span>
                      </c:when>
                      <c:otherwise>
                        <c:choose>
                          <c:when test="${constraint.switchable == 'on'}">
                            <span class="optionalConstraint">optional: <strong>ON</strong></span>
                          </c:when>
                          <c:otherwise>
                            <span class="optionalConstraint">optional: <strong>OFF</strong></span>
                          </c:otherwise>
                        </c:choose>
                      </c:otherwise>
                    </c:choose>
                  </c:if>
                </c:if>

                <c:if test="${!empty constraint.code}">
                  (<b>${constraint.code}</b>)
                </c:if>
              </div>
              <c:if test="${EDITING_TEMPLATE != null && constraint.editableInTemplate}">
                <div>
                  <c:forEach begin="0" end="${path.indentation+1}">
                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                  </c:forEach>
                  <span class="constraintDesc">
                    <c:choose>
                      <c:when test="${empty constraint.description}">
                        &lt;no label&gt;
                      </c:when>
                      <c:when test="${fn:length(constraint.description) > 30}">
                        ${fn:substring(constraint.description, 0, 30)}...
                      </c:when>
                      <c:otherwise>
                        ${constraint.description}
                      </c:otherwise>
                    </c:choose>
                    </span>
                </div>
              </c:if>
          </c:forEach>
        </div>
      </div>
    </c:forEach>
  </c:otherwise>
</c:choose>
<tiles:insert page="/queryBuilderConstraintLogic.jsp"/>
</div>

<!-- /queryBuilderSummary.jsp -->
