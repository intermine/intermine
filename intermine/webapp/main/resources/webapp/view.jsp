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

<div class="body">
<h3>Columns to Display</h3>
  <c:if test="${fn:length(viewStrings) > 1}">
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
    <c:when test="${empty viewStrings}">
      <div class="body">
	<p><i><fmt:message key="view.empty.description"/></i>&nbsp;</p>
      </div>
    </c:when>
    <c:otherwise>
      <tiles:insert page="/viewLine.jsp"/>
    </c:otherwise>
  </c:choose>


<br clear="all"><br>
<h3>Sort Data By</h3>
				<!-- sort by -->
  				<c:choose>
    				<c:when test="${empty sortOrderStrings}">
				      <div class="body">
						<p><i><fmt:message key="sortOrder.empty.description"/></i>&nbsp;</p>
				      </div>
				    </c:when>
			    <c:otherwise>
			    	<div class="sortorderBorder">
				      <tiles:insert page="/sortOrderLine.jsp"/>
				    </div>
			    </c:otherwise>
			  </c:choose>







  <c:if test="${!empty viewStrings}">
  <br clear="all"><br>
  
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
         tag:'div', dropOnEmpty:true,  constraint:'horizontal', overlap:'horizontal', onUpdate:function() {
           reorderOnServer();
         }
       });

       Sortable.create('sortOrderDivs', {
         tag:'div', dropOnEmpty:true, constraint:'horizontal', overlap:'horizontal', onUpdate:function() {
           updateSortOrder();
         }
       });


       recordCurrentOrder();

       function recordCurrentOrder() {
         previousOrder = Sortable.serialize('viewDivs');
         previousOrder = previousOrder.replace(/viewDivs/g, 'oldOrder');
         
         //previousSortOrder = Sortable.serialize('sortOrderDivs');
         //previousSortOrder = previousSortOrder.replace(/sortOrderDivs/g, 'oldSortOrder');
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

	  /* called from viewLine.jsp for now */
      function updateSortOrder(pathString) {
         new Ajax.Request('<html:rewrite action="/sortOrderChange"/>', {
           parameters:'method=addToSortOrder&pathString='+pathString,
           asynchronous:true
         });
         // replace . with > 
         s = new String(pathString);
         s = s.replace(/\./g," > ");
         document.getElementById('querySortOrder').innerHTML = s;
         //recordCurrentOrder();        
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
