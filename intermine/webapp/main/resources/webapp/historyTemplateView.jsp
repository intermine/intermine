<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- replace this with wsTemplateTable --%>

<!-- historyTemplateView.jsp -->
<html:xhtml/>

<script type="text/javascript" src="js/tablesort.js"></script>
<link rel="stylesheet" type="text/css" href="css/sorting.css"/>

  <tiles:useAttribute id="type" name="type"/>

  <im:body id="savedTemplates">

    <script LANGUAGE="JavaScript">
      <!--//<![CDATA[
          function confirmAction() {
          return confirm("Do you really want to delete the selected templates?")
          }
          //]]>-->
    </script>

    <p>
      <fmt:message key="history.savedtemplates.intro"/>
    </p>

    <br/>

        <span class="smallnote">
      <html:link action="/import" titleKey="history.importTemplatesDesc">
        <fmt:message key="history.importTemplates"/>
      </html:link>
    </span>

     <br/> <br/>

    <%-- Choose the queries to display --%>
    <c:choose>
      <c:when test="${empty PROFILE.savedTemplates}">
        <div class="altmessage">
          <fmt:message key="msgs.noResults"/>
        </div>
      </c:when>
      <c:otherwise>

        <html:form action="/modifyTemplate">
        <table class="rowstyle-alt colstyle-alt" cellspacing="0">
          <tr>
            <th>
              <input type="checkbox" id="selected_template"
                     onclick="selectColumnCheckbox(this.form, 'template')"/>
            </th>
            <th align="left" class="sortable">
              <fmt:message key="history.namecolumnheader"/>
            </th>
            <th align="left" class="sortable">
              <fmt:message key="history.titleheader"/>
            </th>
            <th align="left" class="sortable">
              <fmt:message key="history.descriptionheader"/>
            </th>
            <th align="left" class="sortable">
              <fmt:message key="history.commentheader"/>
            </th>
            <c:if test="${IS_SUPERUSER}">
                <th align="left" class="sortable-numeric">
                  <fmt:message key="history.rankheader"/>
                </th>
                <th align="left" class="sortable-numeric">
                  <fmt:message key="history.counterheader"/>
                </th>
            </c:if>
            <th align="center">
              <fmt:message key="history.actionscolumnheader"/>
            </th>
          </tr>
          <c:forEach items="${PROFILE.savedTemplates}" var="savedTemplate" varStatus="status">
            <tr>
              <td align="center" style="border-right: 1px solid #C1DAD7; border-bottom: 1px solid #C1DAD7;">
                <html:multibox property="selected" styleId="selected_template_${status.index}"
                               onclick="setDeleteDisabledness(this.form, '${type}')">
                  <c:out value="${savedTemplate.key}"/>
                </html:multibox>
              </td>

              <c:choose>
                <c:when test="${!savedTemplate.value.valid}">
                  <td class="sorting" align="left">
                    <html:link action="/templateProblems?name=${savedTemplate.key}&amp;scope=user" styleClass="brokenTmplLink">
                    <strike>${savedTemplate.value.name}</strike>
                    </html:link>
                  </td>
                </c:when>
                <c:otherwise>
                  <td class="sorting">
                    <fmt:message var="linkTitle" key="templateList.run">
            <fmt:param value="${savedTemplate.value.name}"/>
              </fmt:message>
            ${savedTemplate.value.name}
                    <tiles:insert name="setFavourite.tile">
                      <tiles:put name="name" value="${savedTemplate.value.name}"/>
                      <tiles:put name="type" value="template"/>
                    </tiles:insert>
                    <c:if test="${IS_SUPERUSER}">
                      <c:set var="taggable" value="${savedTemplate.value}"/>
                      <tiles:insert name="inlineTagEditor.tile">
                        <tiles:put name="taggable" beanName="taggable"/>
                        <tiles:put name="vertical" value="true"/>
                        <tiles:put name="show" value="true"/>
                      </tiles:insert>
                    </c:if>
                  </td>
                </c:otherwise>
              </c:choose>

              <td class="sorting">
                ${savedTemplate.value.title}
              </td>
              <td class="sorting">
                <c:choose>
                  <c:when test="${fn:length(savedTemplate.value.description) > 60}">
                    ${fn:substring(savedTemplate.value.description, 0, 60)}...
                  </c:when>
                  <c:otherwise>
                    ${savedTemplate.value.description}
                  </c:otherwise>
                </c:choose>
                &nbsp;
              </td>
              <td class="sorting">
                <c:choose>
                  <c:when test="${fn:length(savedTemplate.value.comment) > 60}">
                    ${fn:substring(savedTemplate.value.comment, 0, 60)}...
                  </c:when>
                  <c:otherwise>
                    ${savedTemplate.value.comment}
                  </c:otherwise>
                </c:choose>
                &nbsp;
              </td>
               <c:if test="${IS_SUPERUSER}">
               <c:set var="minRank" value="minRank"/>
               <td class="sorting">
                   <c:if test="${!empty templateRank}">
                       <c:choose>
                       <c:when test="${!empty templateRank[savedTemplate.value.name]}">
                           ${templateRank[savedTemplate.value.name]}
                       </c:when>
                       <c:otherwise>${templateRank[minRank]}</c:otherwise>
                       </c:choose>
                   </c:if>
               </td>
               <td class="sorting">
                   <c:if test="${!empty templateCounter}">
                       <c:choose>
                       <c:when test="${!empty templateCounter[savedTemplate.value.name]}">
                           ${templateCounter[savedTemplate.value.name]}
                       </c:when>
                       <c:otherwise>0</c:otherwise>
                       </c:choose>
                   </c:if>
               </td>
               </c:if>
              <td class="sorting" align="center" nowrap>
              <%-- Disable Run & Edit for bad template queries --%>
              <c:choose>
                <c:when test="${!savedTemplate.value.valid}">
                   <span style="color:grey;"><fmt:message key="history.action.execute"/></span>
                </c:when>
                <c:otherwise>
                   <html:link action="/template?name=${savedTemplate.value.name}&amp;scope=user"
                             titleKey="history.action.execute.hover">
                      <fmt:message key="history.action.execute"/>
                   </html:link>
                </c:otherwise>
              </c:choose>

              |

              <c:choose>
                <c:when test="${!savedTemplate.value.valid}">
                      <span style="color:grey;"><fmt:message key="history.action.edit"/></span>
                </c:when>
                <c:otherwise>
                    <html:link action="/editTemplate?name=${savedTemplate.value.name}"
                            titleKey="history.action.edit.hover">
                      <fmt:message key="history.action.edit"/>
                    </html:link>
                </c:otherwise>
              </c:choose>

                |
                <html:link action="/exportTemplates?scope=user&amp;name=${savedTemplate.value.name}&amp;originalTemplate=true"
                        titleKey="history.action.export.hover">
                  <fmt:message key="history.action.export"/>
                </html:link>
                <%-- these links need to be fixed, they take a very long time to load --%>
                <c:if test="${IS_SUPERUSER && savedTemplate.value.valid}">
                  <tiles:insert name="precomputeTemplate.tile">
                    <tiles:put name="templateName" value="${savedTemplate.value.name}"/>
                    <tiles:put name="precomputedTemplateMap" beanName="precomputedTemplateMap" />
                  </tiles:insert>
                  <tiles:insert name="summariseTemplate.tile">
                    <tiles:put name="templateName" value="${savedTemplate.value.name}"/>
                    <tiles:put name="summarisedTemplateMap" beanName="summarisedTemplateMap" />
                  </tiles:insert>
                </c:if>
              </td>
            </tr>
          </c:forEach>
        </table>
        <br/>
        <html:submit property="delete" disabled="true" styleId="delete_button"
                     onclick="return confirmAction()">
          <fmt:message key="history.delete"/>
        </html:submit>
        <html:submit property="export" disabled="true" styleId="export_button">
          <fmt:message key="history.exportSelected"/>
        </html:submit>
        <html:hidden property="pageName" value="MyMine"/>
        </html:form>
        <br/>
      </c:otherwise>
    </c:choose>
    <span class="smallnote">
      <html:link action="/import" titleKey="history.importTemplatesDesc">
        <fmt:message key="history.importTemplates"/>
      </html:link>
    </span>
    
<script type="text/javascript">
(function() {
	jQuery(window).load(function(){
		<%-- sort templates by a remembered column --%>
		var order = im.getCookie("mymine.templates.order");
		if (order && parseInt(order)) {
			fdTableSort.jsWrapper(jQuery("form#modifyTemplateForm table").attr("id"), order);
		}
		
		<%-- callback saving sort order of tables into a cookie --%>
		window.sortCompleteCallback = function() {
			var table = jQuery("form#modifyTemplateForm table");
			var th = table.find("th.forwardSort");
			if (!jQuery(th).exists()) {
				th = table.find("th.reverseSort");
			}
			im.setCookie("mymine.templates.order", th.attr("class").replace(/[^0-9.]/g, ""));
		};
	});
})();
</script>  
    
  </im:body>

<!-- /historyTemplateView.jsp -->
