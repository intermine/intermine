<%@ include file="/shared/taglibs.jsp" %>

<!-- historyFavouritesView.jsp -->
<html:xhtml/>

  <im:body id="favourites">

    <p>
      <fmt:message key="history.favouritetemplates.help"/>
    </p>
  
    <c:choose>
      <c:when test="${empty favouriteTemplates}">
	<div class="altmessage">
	  None
    	</div>
      </c:when>
      <c:otherwise>

        <html:form action="/modifyTemplate">
        <table class="results history" cellspacing="0">
          <tr>
            <th>
              <input type="checkbox" id="selected_template"
                     onclick="selectColumnCheckbox(this.form, 'template')"/>
            </th>
            <th align="left" nowrap>
              <fmt:message key="history.namecolumnheader"/>
            </th>
            <th align="left" nowrap>
              <fmt:message key="history.descriptionheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="history.actionscolumnheader"/>
            </th>
          </tr>    
          <c:forEach items="${favouriteTemplates}" var="favouriteTemplate" varStatus="status">
            <tr>
              <td>
                <html:multibox property="selected" styleId="selected_template_${status.index}"
                               onclick="setDeleteDisabledness(this.form, 'template')">
                  <c:out value="${favouriteTemplate.name}"/>
                </html:multibox>
              </td>
              
              <c:choose>
                <c:when test="${!favouriteTemplate.valid}">
                  <td align="left" nowrap>
                    <html:link action="/templateProblems?name=${favouriteTemplate.key}&amp;scope=user" styleClass="brokenTmplLink">
                    <strike>${favouriteTemplate.name}</strike>
                    </html:link>
                  </td>
                </c:when>
                <c:otherwise>
                  <td>
                    <fmt:message var="linkTitle" key="templateList.run">
                      <fmt:param value="${favouriteTemplate.name}"/>
                    </fmt:message>
                      ${favouriteTemplate.name}
			        <c:if test="${IS_SUPERUSER}">
                      <c:set var="taggable" value="${favouriteTemplate}"/>
                      <tiles:insert name="inlineTagEditor.tile">
                        <tiles:put name="taggable" beanName="taggable"/>
                        <tiles:put name="vertical" value="true"/>
                        <tiles:put name="show" value="true"/>
                      </tiles:insert>
                    </c:if>
                  </td>
                </c:otherwise>
              </c:choose>
              
              <td>
                <c:choose>
                  <c:when test="${fn:length(favouriteTemplate.description) > 60}">
                    ${fn:substring(favouriteTemplate.description, 0, 60)}...
                  </c:when>
                  <c:otherwise>
                    ${favouriteTemplate.description}
                  </c:otherwise>
                </c:choose>
                &nbsp;
              </td>
              <td align="center" nowrap>
                <html:link action="/template?name=${favouriteTemplate.name}&amp;scope=all" 
                				titleKey="history.action.execute.hover">
                  <fmt:message key="history.action.execute"/>
                </html:link> |
                <html:link action="/exportTemplates?name=${favouriteTemplate.name}&amp;scope=all"
                				titleKey="history.action.export.hover">
                  <fmt:message key="history.action.export"/>
                </html:link>
              </td>
            </tr>
          </c:forEach>
        </table>
        <br/>
        <html:submit property="remove_favourite" disabled="true" styleId="remove_button" 
                     onclick="return confirmAction()">
          <fmt:message key="history.favourites.remove"/>
        </html:submit>
        <html:submit property="export" disabled="true" styleId="export_button">
          <fmt:message key="history.exportSelected"/>
        </html:submit>
        </html:form>
        <br/>
        </c:otherwise>
	</c:choose>
<!--    <c:if test="${IS_SUPERUSER}">
      <span class="smallnote">
        <c:if test="${!empty PROFILE.savedTemplates}">
          <html:link action="/exportTemplates?scope=user" titleKey="begin.exportTemplatesDesc">
            <fmt:message key="begin.exportTemplates"/>
          </html:link><br/>
        </c:if>
        <html:link action="/import" titleKey="begin.importTemplatesDesc">
          <fmt:message key="begin.importTemplates"/>
        </html:link>
      </span>
    </c:if> -->
  </im:body>
<!-- /historyFavouritesView.jsp -->
