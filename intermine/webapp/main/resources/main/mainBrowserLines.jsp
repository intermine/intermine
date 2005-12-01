<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- mainBrowserLines.jsp -->

  <html:xhtml/>

  <script>
    function toggle(id, path) {
      if ($(id).innerHTML=='') {
        new Ajax.Updater(id, '<html:rewrite action="/mainChange"/>',
          {parameters:'method=expand&path='+path, asynchronous:true});
        $('img_'+path).src='images/minus.gif';
      } else {
        // still need to call mainChange
        $('img_'+path).src='images/plus.gif';
        path = path.substring(0, path.lastIndexOf('.'));
        new Ajax.Request('<html:rewrite action="/mainChange"/>',
          {parameters:'method=collapse&path='+path, asynchronous:true});
        $(id).innerHTML='';
      }
    }
    
    function addConstraint(path) {
      new Ajax.Updater('mainConstraint', '<html:rewrite action="/mainChange"/>',
        {parameters:'method=newConstraint&path='+path, asynchronous:true, evalScripts:true});
    }
  </script>

  <c:set var="indent" value="0"/>

  <c:forEach var="node" items="${nodes}">
    <%-- This hideous stuff surrounds branches of the statically
      rendered tree with the right div ids. This kind of thing
      would be a lot easier if we were rendering a real tree
      rather than just a list of Nodes.. --%>
    <c:if test="${!noTreeIds && node.indentation > indent}">
      <div id="${previousNodePath}">
    </c:if>
    <c:if test="${!noTreeIds && node.indentation < indent}">
      </div>
    </c:if>
    <c:set var="indent" value="${node.indentation}"/>
    <c:set var="node" value="${node}" scope="request"/>
    <!--browser line ${node.path} indent ${node.indentation}-->
    <tiles:insert page="/mainBrowserLine.jsp"/>
    <c:set var="previousNodePath" value="${node.path}"/>
  </c:forEach>
    <%-- see above --%>
    <c:if test="!noTreeIds">
      </div>
    </c:if>
    
<!-- /mainBrowserLines.jsp -->
