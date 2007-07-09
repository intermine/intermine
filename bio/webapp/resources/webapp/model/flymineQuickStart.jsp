<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- flymineQuickStart.jsp -->
<html:xhtml/>

<img src="model/quickStart.png" class="aspectPageIcon" align="left"/>
<div class="body aspectIntro">
  Welcome to the FlyMine quick start guide.  This page exists to give you an
  overview of what is possible using the FlyMine system.  More detail is
  available by following the links below and in the
  <html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/index.html">full manual</html:link>.
</div>

<div style="clear:both;"/>

<table>
  <tr>
    <td valign="top">
      <div class="heading2">
        Browse data and searching for name and identifiers
      </div>
      <div class="body">
        All entries in FlyMine have a summary page includes data
        from all aspects.  Some examples:
        <im:querylink text="the 'eve' gene" skipBuilder="true">
          <query name="" model="genomic" view="Synonym.subject">
            <node path="Synonym" type="Synonym">
            </node>
            <node path="Synonym.value" type="String">
              <constraint op="=" value="eve">
              </constraint>
            </node>
          </query>
        </im:querylink>
        and the its
        <im:querylink text="corresponding protein" skipBuilder="true">
          <query name="" model="genomic" view="Synonym.subject">
            <node path="Synonym" type="Synonym">
            </node>
            <node path="Synonym.value" type="String">
              <constraint op="=" value="eve_drome">
              </constraint>
            </node>
          </query>
        </im:querylink>
        Enter any identifier or symbol in the Quick Search box on the front page to
        get started.

        <c:set var="helpUrl"
               value="${WEB_PROPERTIES['project.helpLocation']}/manual/manualQuickStartBrowsing.html"/>
        <c:set var="browseTemplateName" value="${WEB_PROPERTIES['begin.browse.template']}"/>

        <tiles:insert name="browse.tile">
          <tiles:put name="prompt" value="Alternatively try it here"/>
          <tiles:put name="templateName" value="${browseTemplateName}"/>
        </tiles:insert>
        <br/>
        <p class="smallnote">
          <fmt:message key="begin.browse.help.message"/>
          <html:link href="${helpUrl}"><fmt:message key="begin.link.help"/></html:link>]
        </p>

      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Template queries
      </div>
      <div class="body">
        FlyMine has a powerful query interface that can run
        arbitrary queries on the integrated database.  A library of pre-defined
        ('template') queries that simply require filling in a form is available.
        Find lists of templates on each aspect page or using the Template Search
        box:
  <html:form action="/templateSearch" method="get">
    <fmt:message key="templateSearch.search.label"/>
    <html:text property="queryString" size="40" styleId="queryString"/>
    <html:select property="type">
      <html:option key="templateSearch.form.global" value="global"/>
      <html:option key="templateSearch.form.user" value="user"/>
      <html:option key="templateSearch.form.all" value="ALL"/>
    </html:select>
    <html:submit><fmt:message key="templateSearch.form.submit"/></html:submit>
    <br/>
  </html:form>
      </div>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <div class="heading2">
        Operate on lists of data
      </div>
      <div class="body">
        All queries in FlyMine can be performed on lists of data.
        For example, templates that operate on a single gene identifier can be
        run with a list of gene identifiers you have created yourself.
    </td>
    <td valign="top">
      <div class="heading2">
        Design your own query
      </div>
      <div class="body">
        Advanced users can use a flexible query interface to construct their
        own queries.  You can save queries you have created for future use and
        create your own templates.  See the user manual and tutorials for more
        information.
      </div>
    </td>
  </tr>
</table>

<!-- /flymineQuickStart.jsp -->
