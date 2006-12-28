<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- view.jsp -->
<html:xhtml/>

<a name="showing"></a>

<div class="heading">
  <fmt:message key="view.notEmpty.description"/><im:manualLink section="manualPageQB.shtml#manualOutputListQB"/>
</div>

<c:choose>
  <c:when test="${TEMPLATE_BUILD_STATE != null && !empty EDITING_VIEW}">
    <c:set var="viewList" value="${QUERY.alternativeViews[EDITING_VIEW]}" scope="request"/>
  </c:when>
  <c:otherwise>
    <c:set var="viewList" value="${QUERY.view}" scope="request"/>
  </c:otherwise>
</c:choose>

<div class="body">
  
  <c:if test="${fn:length(viewList) > 1}">
    <noscript>
      <div>
        <fmt:message key="view.columnOrderingTip"/>
      </div>
    </noscript>
    <script type="text/javascript">
    <!--
    document.write('<p><fmt:message key="view.columnOrderingTip.jscript"/></p>');
    // -->
    </script>
  </c:if>
  
	<c:choose>
	  <c:when test="${TEMPLATE_BUILD_STATE != null}">
	    <table cellpadding="5" cellspacing="0" border="0" class="viewTable" width="100%">
	      <tr>
	        <td class="topLeft" nowrap>
	          <fmt:message key="view.outputLists"/>
	        </td>
	        <c:choose>
	          <c:when test="${!empty EDITING_VIEW || param.action=='newView'}">
	            <td class="tab not-selected"
	                onmouseover="this.style.backgroundColor='#fbfbfb'"
	                onmouseout="this.style.backgroundColor='#fff'"
	                nowrap>
	              <html:link action="/mainChange?method=selectDefaultView">
	                <fmt:message key="view.defaultOutput"/>
	              </html:link>
	            </td>
	          </c:when>
	          <c:otherwise>
	            <td class="tab selected" nowrap>
	              <fmt:message key="view.defaultOutput"/>
	            </td>
	          </c:otherwise>
	        </c:choose>
	        <c:forEach var="entry" items="${QUERY.alternativeViews}">
	          <c:choose>
	            <c:when test="${empty EDITING_VIEW || EDITING_VIEW != entry.key || param.action=='newView'}">
	              <td class="tab not-selected"
	                onmouseover="this.style.backgroundColor='#fbfbfb'"
	                onmouseout="this.style.backgroundColor='#fff'"
	                nowrap>
	                <html:link action="/mainChange?method=selectView&amp;name=${entry.key}">
	                  ${entry.key}
	                </html:link>
	              </td>
	            </c:when>
	            <c:otherwise>
	              <td class="tab selected" nowrap>
	                <c:choose>
	                  <c:when test="${param.action=='renameView' && param.name==entry.key}">
	                    <script type="text/javascript">
	                    <!--
	                    window.onload = function() {
	                      document.getElementById("renameView").focus();
	                    }
	                    // -->
	                    </script>
	                    <form action="<html:rewrite action="/mainChange"/>" method="POST">
	                    <input type="text" name="newName" value="${entry.key}" size="10" id="renameView"/>
	                    <input type="hidden" name="oldName" value="${entry.key}"/>
	                    <input type="hidden" name="method" value="renameView"/>
	                    <input type="submit" name="rename" value="Rename"/>
	                    </form>
	                  </c:when>
	                  <c:otherwise>
	                    ${entry.key}
	                  </c:otherwise>
	                </c:choose>
	              </td>
	            </c:otherwise>
	          </c:choose>
	        </c:forEach>
	        <td width="99%" class="tab-space" align="right">
	          &nbsp;
	          <html:link action="/query?action=newView">
	            <fmt:message key="view.new"/>
	          </html:link>
	          <c:if test="${!empty EDITING_VIEW}">
	            |
	            <html:link action="/query?action=renameView&amp;name=${EDITING_VIEW}">
	              <fmt:message key="view.rename"/>
	            </html:link> |
	            <html:link action="/mainChange?method=deleteView&amp;name=${EDITING_VIEW}">
	               <fmt:message key="view.delete"/>
	            </html:link>
	          </c:if>
	        </td>
	      </tr>
	      <tr>
	        <td class="viewTableView" colspan="${fn:length(QUERY.alternativeViews)+3}">
	          <c:choose>
	            <c:when test="${param.action=='newView'}">
	                    <script type="text/javascript">
	                    <!--
	                    window.onload = function() {
	                      document.getElementById("newViewName").focus();
	                    }
	                    // -->
	                    </script>
	              <form action="<html:rewrite action="/mainChange"/>" method="POST">
	                <div class="body">
	                <p>
	                  <fmt:message key="view.newViewPrompt"/>
	                  <input type="hidden" name="method" value="newView"/>
	                  <input type="text" name="name" value="" size="12" id="newViewName"/>
	                  <input type="submit" value="<fmt:message key="view.createButton"/>"/>
	                </p>
	                </div>
	              </form>
	            </c:when>
	            <c:when test="${empty viewList}">
	              <div class="body">
	                <p><i><fmt:message key="view.empty.description"/></i>&nbsp;</p>
	              </div>
	            </c:when>
	            <c:otherwise>
	              <tiles:insert page="/viewLine.jsp"/>
	            </c:otherwise>
	          </c:choose>
	        </td>
	      </tr>
	    </table>
	  </c:when>
	  <c:when test="${empty viewList}">
	    <div class="body">
	      <p><i><fmt:message key="view.empty.description"/></i>&nbsp;</p>
	    </div>
	  </c:when>
	  <c:otherwise>
	    <tiles:insert page="/viewLine.jsp"/>
	  </c:otherwise>
	</c:choose>
    
  <c:if test="${!empty viewList}">
    <div style="clear:left; margin-bottom: 18px">
       <p>
       <html:form action="/viewAction">
         <html:submit property="action">
           <fmt:message key="view.showresults"/>
         </html:submit>
       </html:form>
       </p>
     </div>
   
     <script type="text/javascript">
     <!--
       var previousOrder = '';
       
       Sortable.create('viewDivs', {
         tag:'div', constraint:'horizontal', overlap:'horizontal', onUpdate:function() {
           reorderOnServer();
         }
       });
       
       recordCurrentOrder();
       
       function recordCurrentOrder() {
         previousOrder = Sortable.serialize('viewDivs');
         previousOrder = previousOrder.replace(/viewDivs/g, 'oldOrder');
       }
       
       /**
        * Send the previous order and the new order to the server.
        */
       function reorderOnServer() {
         var newOrder = Sortable.serialize('viewDivs');
         //$('ser').innerHTML=newOrder;
         new Ajax.Request('<html:rewrite action="/viewChange"/>', {
           parameters:'method=reorder&'+previousOrder+'&'+newOrder,
           asynchronous:true
         });
         recordCurrentOrder();
       }
     //-->
     </script>
     
  </c:if>

</div>
    
    

<c:if test="${!empty PROFILE.username && TEMPLATE_BUILD_STATE == null}">
  <div align="center">
    <p>
      <form action="<html:rewrite action="/mainChange"/>" method="post">
        <input type="hidden" name="method" value="startTemplateBuild"/>
        <input type="submit" value="Start building a template query" />
      </form>
    </p>
  </div>
</c:if>


<!-- /view.jsp -->
