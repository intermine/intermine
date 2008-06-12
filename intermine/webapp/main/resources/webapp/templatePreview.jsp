<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- templatePreview.jsp -->

<c:if test="${TEMPLATE_BUILD_STATE != null}">
  <link rel="stylesheet" type="text/css" href="css/templatePreview.css"/>

  <imutil:disclosure id="template-preview" opened="true" styleClass="body">
        Template preview
  <imutil:disclosureBody style="align:center;">
      <div id="tmplPreview">
        <div class="previewTitle">
          <fmt:message key="templateBuilder.previewtitle"/>
        </div>
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
            <form action="<html:rewrite action="/mainChange"/>" method="post">
              <input type="hidden" name="method" value="stopTemplateBuild"/>
              <input type="submit" value="Cancel template building" />
            </form>
          </td>
          <td>
            <c:if test="${!empty PROFILE.username}">
              <c:if test="${!empty QUERY}">
                <fmt:message key="${TEMPLATE_BUILD_STATE.updatingTemplate == null ?
                    'templateBuilder.save' : 'templateBuilder.update'}" var="saveLabel"/>
                <form method="link" action="<html:rewrite action="/createTemplate"/>">
                  <input type="submit" value="${saveLabel}"/>
                </form>
              </c:if>
            </c:if>
          </td>
        </tr>
      </table>
      <br/>
    </div>

</c:if>

<!-- /templatePreview.jsp -->
