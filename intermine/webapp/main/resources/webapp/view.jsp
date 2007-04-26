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

	<h3><fmt:message key="view.heading"/></h3>

	<div>
        <fmt:message key="view.instructions"/>
  		<c:if test="${fn:length(viewStrings) > 1}">
	    <noscript>
    	    <fmt:message key="view.intro"/>
	    </noscript>
    	<script type="text/javascript">
	       <!--
    	   document.write('<fmt:message key="view.intro.jscript"/>');
	       // -->
	    </script>
		</c:if>
    </div>
<br>      

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


<br clear="left"><br>


<c:if test="${fn:length(viewStrings) > 0}">

	<h3><fmt:message key="sortOrder.heading"/></h3>

	<div>
		<fmt:message key="sortOrder.instructions"/>
	</div>

<br>

	<!-- sort by -->
    <c:if test="${!empty viewStrings}">
		<div class="sortorderBorder">
	    	 <tiles:insert page="/sortOrderLine.jsp"/>
	   	</div>
	</c:if>

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
       updateSortImgs("${sortByIndex}");

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

	  // change from ascending to descending sort, or vice versa 
      function reverseSortDirection() {
        var img = document.getElementById('sortImg').src;
      	var newDirection;
      	if (img.match('desc.gif')) {
      		newDirection = 'asc';
        } else {
        	newDirection = 'desc';
        }
         new Ajax.Request('<html:rewrite action="/sortOrderChange"/>', {
           parameters:'method=changeDirection&direction='+newDirection,
           asynchronous:true
         });
         document.getElementById('sortImg').src = 'images/' + newDirection + '.gif';
       }

	  // called from viewElement.jsp
      function updateSortOrder(pathString, index) {
         new Ajax.Request('<html:rewrite action="/sortOrderChange"/>', {
           parameters:'method=addToSortOrder&pathString='+pathString,
           asynchronous:true
         });
         // replace . with > 
         s = new String(pathString);
         s = s.replace(/\./g," > ");
         document.getElementById('querySortOrder').innerHTML = s;
	     updateSortImgs(index);
       }
       
       // enable all imgs, disable the one the user just selected 
       function updateSortImgs(index) {
       	for (i=0;true;i++) {
       		if (!document.getElementById("btn_" + i)) return;
    		var b = document.getElementById("btn_" + i);
    		if(i==index) {
				disable(b);
			} else {
				enable(b);
			}
		}
       }
       
       function disable(b) {
		b.src = "images/sort-disabled.gif";
		b.disabled = true;
       }
	   function enable(b) {
		b.src = "images/sort.gif";
		b.disabled = false;
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
