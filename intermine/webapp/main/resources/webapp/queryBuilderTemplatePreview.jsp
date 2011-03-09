<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- queryBuilderTemplatePreview.jsp -->

<c:if test="${EDITING_TEMPLATE != null || NEW_TEMPLATE != null}">
  <link rel="stylesheet" type="text/css" href="css/templatePreview.css"/>

  <imutil:disclosure id="template-preview" opened="true" styleClass="body">
  <imutil:disclosureBody style="align:center;">
      <div id="tmplPreview">
        <div class="previewTitle">
          <fmt:message key="templateBuilder.previewtitle"/>
        </div>
        <fmt:message key="templatePreview.sort.instructions"/>
        <tiles:insert name="template.tile">
          <tiles:put name="builder" value="yes"/>
        </tiles:insert>
      </div>
  </imutil:disclosureBody>
  </imutil:disclosure>

    <div align="center">
      <table border="0">
        <tr>
          <td>
            <form action="<html:rewrite action="/queryBuilderChange"/>" method="post">
              <input type="hidden" name="method" value="stopTemplateBuild"/>
              <input type="submit" value="Cancel template building" />
            </form>
          </td>
          <td>
            <c:if test="${PROFILE.loggedIn && !empty QUERY}">
                <fmt:message key="${NEW_TEMPLATE != null ?
                    'templateBuilder.save' : 'templateBuilder.update'}" var="saveLabel"/>
                <form method="link" action="<html:rewrite action="/createTemplate"/>">
                  <input type="submit" value="${saveLabel}"/>
                </form>
            </c:if>
          </td>
        </tr>
      </table>
      <br/>
    </div>

</c:if>

<!-- /queryBuilderTemplatePreview.jsp -->
