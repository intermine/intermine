<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>
<link rel="stylesheet" type="text/css" href="css/begin.css"/>

<table id="frontpagelayout" cellspacing="0" cellpadding="0">
  <tr><td>

      <div class="body">
        <span style="font-size:+2em;">
          <a href="what.xml">What is FlyMine?</a>
        </span>&nbsp;&nbsp;<span style="font-size:+1.4em">
          <a href="tour_1.html" target="_blank"  onclick="javascript:window.open('tour_1.html','_manual','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Take a tour!</a>
        </span>

        <p class="errors">
          This is release 8.0 of FlyMine.  See the <a href="release-notes.xml">release notes</a> to find out what's new.
        </p>
      </div>
    </td>
    <td>
      <p>
        <tiles:insert name="tipWrapper.tile"/>
      </p>
    </td>
  </tr>
  <tr>
    <td>

      <im:roundbox title="Templates" stylename="welcome" height="350">

        <p>
          <u>
            <fmt:message key="begin.trytemplates"/>
          </u>
        </p>
        <tiles:insert name="webSearchableList.tile">
          <!-- optional -->
          <tiles:put name="limit" value="5"/>
          <!-- bag or template? -->
          <tiles:put name="type" value="template"/>
          <!-- user or global -->
          <tiles:put name="scope" value="global"/>
          <tiles:put name="tags" value="im:public"/>
          <tiles:put name="showDescriptions" value="false"/>
          <tiles:put name="showSearchBox" value="false"/>
          <%--<tiles:put name="height" value="100"/>--%>
        </tiles:insert>
        <div align="center">
          <table>
            <tr>
              <td>
                <html:link action="/templates.do">
                  <img src="images/gotoTemplates.png" alt="Go To Template Page" style="float:left;"  >
                </html:link>
              </td>
              <td style="white-space:nowrap;">
                <ul>
                  <li>run templates</li>
                  <li>edit templates</li>
                  <li>import and export</li>
                </ul>
              </td>
            </tr>
          </table>
      </im:roundbox>
    </td>
    <td>
      <im:roundbox title="Lists" stylename="welcome" height="350">
        <p>
          <em>
            <fmt:message key="begin.bags"/>
          </em>
        </p>
        <p>
          <u>
            <fmt:message key="begin.trybags"/>
          </u>
        </p>
        <tiles:insert name="webSearchableList.tile">
          <tiles:put name="limit" value="3"/>
          <%-- bag or template? --%>
          <tiles:put name="type" value="bag"/>
          <%-- user or global --%>
          <tiles:put name="scope" value="global"/>
          <tiles:put name="tags" value="im:public"/>
          <tiles:put name="showSearchBox" value="false"/>
          <%--<tiles:put name="height" value="100"/>--%>
        </tiles:insert>
        <p>
          <u>
            <fmt:message key="begin.createbags"/>
          </u>
        </p>
        <div align="center">
          <table>
            <tr>
              <td>
                <html:link action="/bag.do">
                  <img  src="images/gotoLists.png" align="center" alt="Go To Bag Page" border="0">
                </html:link>
              </td>
              <td style="white-space:nowrap;">
                <ul style="text-align:left" >
                  <li>analyse list contents</li>
                  <li>create new lists</li>
                  <li>run operations on lists</li>
                  <li>import and export</li>
                </ul>
              </td>
            </tr>
          </table>
        </div>
      </im:roundbox>
    </td>
  </tr>
  <tr>
    <td>
      <im:roundbox title="Data" stylename="welcome" height="180">
        <p>
          <em>
            <fmt:message key="begin.data"/>
          </em>
        </p>
        <div align="center">
        		
        		
        <c:choose>
		    <c:when test="${!empty ASPECTS}">
		       <tiles:insert name="aspects.tile"/>
		    </c:when>
		    <c:otherwise>
		      <c:forEach items="${CATEGORIES}" var="category">
		        <c:if test="${!empty CATEGORY_CLASSES[category]}">
		          <div class="heading"><c:out value="${category}"/></div>
		          <div class="body">
		            <c:set var="classes" value="${CATEGORY_CLASSES[category]}"/>
		            <c:forEach items="${classes}" var="classname" varStatus="status">
		              <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&amp;className=${classname}" title="<c:out value="${classDescriptions[classname]}"/>">
		                ${classname}</a><c:if test="${!status.last}">,</c:if>
		            </c:forEach>
		            <c:if test="${!empty CATEGORY_TEMPLATES[category]}">
		              <br/><span class="smallnote"><fmt:message key="begin.or"/> <html:link action="/templates" paramId="category" paramName="category"><fmt:message key="begin.related.templates"/></html:link></span>
		            </c:if>
		          </div>
		          <im:vspacer height="5"/>
		        </c:if>
		      </c:forEach>
		    </c:otherwise>
		  </c:choose>
        
        
        
        
        
          <html:link action="/dataCategories.do">
            <img src="images/gotoData.png" alt="View All Datasets">
          </html:link>
        </div>
      </im:roundbox>
    </td>
    <td>
      <im:roundbox title="Query Builder" stylename="welcome" height="180">
        <p>
          <em>
            <fmt:message key="begin.querybuilder"/>
          </em>
        </p>
        <div align="center">
          <html:link action="/customQuery.do">
            <img src="images/gotoQB.png" alt="Go To Query Builder">
          </html:link>
        </div>
      </im:roundbox>
    </td>
  </tr>
</table>

<!-- /begin.jsp -->
