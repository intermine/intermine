<%@ tag body-content="empty"  %>

<%@ attribute name="scope" required="true" %>
<%@ attribute name="interMineBag" required="true" type="org.intermine.web.logic.bag.InterMineBag" %>
<%@ attribute name="desc" required="false" type="java.lang.String" %>

<%@ include file="/shared/taglibs.jsp" %>

<html:link action="/bagDetails?bagName=${interMineBag.name}&amp;type=${scope}"
           title="${linkTitle}">
  <span class="templateTitle">${!empty desc ? desc : interMineBag.title}</span>
</html:link>
