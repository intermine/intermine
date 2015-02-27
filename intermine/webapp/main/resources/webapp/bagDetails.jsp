<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- bagDetails.jsp -->

<html:xhtml/>

<link rel="stylesheet" href="css/resultstables.css" type="text/css" />
<link rel="stylesheet" href="css/toolbar.css" type="text/css" media="screen" title="Toolbar Style" charset="utf-8"/>

<script type="text/javascript">
<!--//<![CDATA[
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
  var detailsType = 'bag';
  var webappUrl = "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}/";
  var service = webappUrl + "service/";
//]]>-->
</script>

<script type="text/javascript">
  <%-- the number of entries to show in References & Collections before switching to "show all" --%>
  var numberOfTableRowsToShow = '${object.numberOfTableRowsToShow}'; <%-- required on report.js --%>
  numberOfTableRowsToShow = (numberOfTableRowsToShow == '') ? 30 : parseInt(numberOfTableRowsToShow);
</script>
<script type="text/javascript" src="<html:rewrite page='/js/report.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/inlinetemplate.js'/>"></script>

<div class="body">
  <c:choose>
    <c:when test="${!empty bag}">

      <div class="heading results">
        <img src="images/icons/lists-64.png" alt="lists icon" style="width:32px" />
        <h1>
            <fmt:message key="bagDetails.title"/>
            <span style="font-size:0.9em;font-weight:normal">
                for <b>${bag.name}</b>
                (${bag.size}&nbsp;<c:out value="${imf:formatPathStr(bag.type, INTERMINE_API, WEBCONFIG)}s"/>)
            </span>
        </h1>
      </div>

      <script type="text/javascript" src="js/toolbar.js"></script>
      <script type="text/javascript" src="js/bag-details.js"></script>

      <%-- Fitting above large, rows below that. --%>
      <div class="grid medium-grid-fit grid-full">

        <div class="grid-cell"> <%-- Left column, auto resizing --%>
          <c:if test ="${bag.type ne 'Submission'}">

            <%-- CONTENTS --%>
            <div class="results">
              <%-- Table displaying bag elements --%>
              <tiles:insert name="resultsTable.tile">
                  <tiles:put name="currentPage" value="bagDetails" />
                  <tiles:put name="bagName" value="${bag.name}" />
                  <tiles:put name="highlightId" value="${highlightId}"/>
                  <tiles:put name="cssClass" value="bag-detail-table"/>
                  <tiles:put name="consumerContainer" value=".table-rhs-tools"/>
                  <tiles:put name="consumerBtnClass" value="btn-primary"/>
                  <tiles:put name="pageSize" value="10"/>
              </tiles:insert>
            </div>

          </c:if>

          <%-- LHS BagDisplayers --%>
          <c:if test="${!invalid}">
              <tiles:insert page="/bagDisplayers.jsp">
                  <tiles:put name="bag" beanName="bag"/>
                  <tiles:put name="showOnLeft" value="true"/>
              </tiles:insert>
          </c:if>

          <%-- DATE CREATED --%>
          <div>
            <small>Date Created:  <im:dateDisplay date="${bag.dateCreated}" /></small>
          </div>

        </div> <%-- End left column --%>

        <div class="grid-cell quarter"> <%-- RHS column, one-quarter width on large screens --%>

          <%-- The table can put some tools here, if it wants --%>
          <div class="table-rhs-tools"></div>

          <c:if test="${!invalid}">

            <%-- LIST CONVERSION --%>
            <div id="convertList" class="listtoolbox" align="left">
              <html:form action="/modifyBagDetailsAction">
              <html:hidden property="bagName" value="${bag.name}"/>
                <tiles:insert name="convertBag.tile">
                  <tiles:put name="bag" beanName="bag" />
                  <tiles:put name="idname" value="cp" />
                  <tiles:put name="orientation" value="h" />
                </tiles:insert>
              </html:form>
            </div>

            <%-- BagDisplayers --%>
            <div>
              <html:form action="/modifyBagDetailsAction">
              <html:hidden property="bagName" value="${bag.name}"/>
                <tiles:insert page="/bagDisplayers.jsp">
                <tiles:put name="bag" beanName="bag"/>
                <tiles:put name="showOnLeft" value="false"/>
                </tiles:insert>
              </html:form>

            </div>

            <%-- LINK OUTS --%>
            <div id="linkOuts" class="listtoolbox" align="left">
              <p>
                <tiles:insert name="attributeLinks.tile">
                    <tiles:put name="bag" beanName="bag" />
                </tiles:insert>
              </p>
            </div>

        </c:if> <%-- End if valid --%>

          <%-- Bag Description --%>
          <c:choose>
            <c:when test="${myBag == 'true'}">
                <div class="listtoolbox" id="bagDescriptionDiv" onclick="toggleDescription()">
                    <h3>
                        <img src="images/icons/description.png" title="Description of your list"/>
                        Description
                    </h3>
                    <c:choose>
                        <c:when test="${! empty bag.description}">
                          <p><c:out value="${bag.description}"/></p>
                        </c:when>
                        <c:otherwise>
                          <div id="emptyDesc"><fmt:message key="bagDetails.bagDescr"/></div>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="listtoolbox" id="bagDescriptionTextarea" style="display:none">
                    <h3>
                        <img src="images/icons/description.png" title="Description of your list"/>
                        Description
                    </h3>
                    <div>
                        <textarea id="textarea" placeholder="enter a description"><c:if test="${! empty bag.description}"><c:out value="${bag.description}" /></c:if></textarea>
                    </div>
                    <div>
                        <button onclick="cancelEditBagDescription()">
                          <fmt:message key="confirm.cancel"/>
                        </button>
                        <button onclick="saveBagDescription('${bag.name}'); return false;">
                          <fmt:message key="button.save"/>
                        </button>
                    </div>
                </div>
            </c:when>
            <c:when test="${! empty bag.description}">
                <div id="listtoolbox bagDescriptionDiv">
                    <b>Description:</b><c:out value="${bag.description}"/>
                </div>
            </c:when>
          </c:choose>

          <%-- TAGS --%>
          <div class="listtoolbox" id="listTags">
            <div class="grid">
                <div class="grid-cell cell-no-grow">
                    <h4>Tags</h4>
                </div>
                <c:choose>
                    <c:when test="${PROFILE.loggedIn}">
                    <div class="grid-cell">
                        <c:set var="taggable" value="${bag}"/>
                        <tiles:insert name="inlineTagEditor.tile">
                            <tiles:put name="taggable" beanName="taggable"/>
                            <tiles:put name="vertical" value="true"/>
                            <tiles:put name="show" value="true"/>
                        </tiles:insert>
                    </div>
                    <div class="grid-cell cell-no-grow">
                        <div class="inline-tag-editor">
                            <tiles:insert name="setFavourite.tile" >
                                <tiles:put name="name" value="${bag.name}"/>
                                <tiles:put name="type" value="bag"/>
                            </tiles:insert>
                        </div>
                    </div>
                    </c:when>
                    <c:otherwise>
                    <div class="grid-cell">
                        <div class="inline-tag-editor">
                        <fmt:message key="login.to.tag.lists"/>: <im:login/>
                        </div>
                    </div>
                    </c:otherwise>
                </c:choose>
            </div>
          </div>

          <%-- TODO: allow users to remove list items --%>

        </div> <%-- End RHS column --%>
        
      </div>

      <c:if test="${!invalid}">

        <link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/widget.css'/>"/>

        <!-- Widgets -->
        <div class="heading" style="clear:both;margin-top:15px">
            <a id="widgets">Widgets displaying properties of '${bag.name}'</a> &nbsp;
        </div>

        <div id="toggle-widgets">
          <p>Click to select widgets you would like to display:</p>
          <ol>
          <c:forEach items="${widgets}" var="widget">
            <li>
              <a class="widget-toggler" href="#" title="toggle ${widget.title}" data-widget="${widget.id}">
                <c:out value="${widget.title}"/>
              </a>
            </li>
          </c:forEach>
          </ol>
          <div style="clear:both"></div>
        </div>

        <c:forEach items="${widgets}" var="widget">
          <tiles:insert name="widget.tile">
            <tiles:put name="widget" beanName="widget"/>
            <tiles:put name="bag" beanName="bag"/>
            <tiles:put name="widget2extraAttrs" beanName="widget2extraAttrs" />
          </tiles:insert>
        </c:forEach>
        <div style="clear:both;"></div>
        <!-- /widgets -->

        <!-- LIST TEMPLATES -->
        <c:set var="templateIdPrefix" value="bagDetailsTemplate${bag.type}"/>
        <c:set value="${fn:length(CATEGORIES)}" var="aspectCount"/>

        <div class="heading">
          <a id="relatedTemplates">
            <fmt:message key="bags.templates.executed">
              <fmt:param>${bag.name}</fmt:param>
            </fmt:message>
          </a>
        </div>

        <div class="body">
            <fmt:message key="bagDetails.templatesHelp"/>

            <%-- Insert templates grouped by aspect --%>
            <c:forEach items="${CATEGORIES}" var="aspect" varStatus="status">
                <div id="${fn:replace(aspect, " ", "_")}Category" class="aspectBlock">
                    <tiles:insert name="reportAspect.tile">
                    <tiles:put name="placement" value="im:aspect:${aspect}"/>
                    <tiles:put name="trail" value="|bag.${bag.name}"/>
                    <tiles:put name="interMineIdBag" beanName="bag"/>
                    <tiles:put name="aspectId" value="${templateIdPrefix}${status.index}" />
                    <tiles:put name="opened" value="${status.index == 0}" />
                    </tiles:insert>
                </div>
            </c:forEach>
        </div>
        <!-- /LIST TEMPLATES -->

      </c:if>
    </c:when>

    <c:otherwise>
      <!--  No list found with this name -->
      <div class="bigmessage">
        <html:link action="/bag?subtab=view">View all lists</html:link>
      </div>
    </c:otherwise>

  </c:choose>
</div>  <!-- end .body -->
<!-- /bagDetails.jsp -->
