<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- view.jsp -->
<html:xhtml/>



  
  <style>
 
.viewTable td.topLeft {
  border-right: 1px solid #bbb;
  border-bottom: 1px solid #aaa;
  font-style: italic;
  font-size: 11px;
}

.viewTable td {
  
}

.viewTable td.tab {
  border-top: 1px solid #bbb;
  border-right: 1px solid #bbb;
  spacing-right: 3px;
}

.viewTable td.selected {
  font-weight: bold;
  border-right: 1px solid #bbb;
  border-top: 1px solid #aaa;
}

.viewTable td.not-selected {
  border-bottom: 1px solid #aaa;
  color: #555;
}

.viewTable td.tab-space {
  border-bottom: 1px solid #aaa;
}

.viewTable td.viewLabelCell {
  border-top: 1px solid #bbb;
}

.viewTable td.viewTableView {
  border-bottom: 1px solid #999;
  border-right: 1px solid #999;
  border-left: 1px solid #999;
  padding-bottom: 0px;
  padding-right: 0px;
}

.viewTable td.viewTableView, td.viewLabelCell, td.selected {
  background-color: #f8f8f8;
}

.viewTable div.viewActionLinks {
  margin-top: -3px;
  margin-bottom: 3px;
}


.viewTable td.not-selected a:link {
}

.viewTable td.not-selected a:hover {
  color: #000;
  text-decoration: underline;
}
    
  </style>
  

<a name="showing"></a>

<div class="heading">
  <fmt:message key="view.notEmpty.description"/><im:helplink key="view.help.output"/>
</div>



<c:choose>
  <c:when test="${TEMPLATE_BUILD_STATE != null && !empty EDITING_VIEW}">
    <c:set var="viewList" value="${QUERY.alternativeViews[EDITING_VIEW]}" scope="request"/>
  </c:when>
  <c:otherwise>
    <c:set var="viewList" value="${QUERY.view}" scope="request"/>
  </c:otherwise>
</c:choose>

<div class="view">
  <div class="body">
        
<c:choose>
  <c:when test="${TEMPLATE_BUILD_STATE != null}">
    <c:if test="${fn:length(viewList) > 1}">
      <div>
        <fmt:message key="view.columnOrderingTip"/>
      </div>
    </c:if>
    <p>
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
    <p>
      <div style="clear:left">
        <html:form action="/viewAction">
          <html:submit property="action">
            <fmt:message key="view.showresults"/>
          </html:submit>
        </html:form>
      </div>
    </p>
  </c:if>
  
  </div>
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
