<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- TODO - remove this in favour of wsBagTable.jsp --%>


<!-- bagView.jsp -->
<script type="text/javascript">
<!--//<![CDATA[
  function selectColumnCheckbox(form, type) {
    var columnCheckBox = 'selected_' + type;
    var checked = document.getElementById(columnCheckBox).checked;
    with(form) {
      for(i=0;i < elements.length;i++) {
        thiselm = elements[i];
        var testString = columnCheckBox + '_';
        if(thiselm.id.indexOf(testString) != -1)
          thiselm.checked = checked;
      }
    }
  }
  function noenter() {
    return !(window.event && window.event.keyCode == 13);
  }
//]]>-->
</script>

<html:xhtml/>

<im:roundbox titleKey="bagspage.title" color="roundcorner" >
<h2><fmt:message key="bagspage.mybags.title"/></h2>
<p>
  <fmt:message key="bagspage.mybags.help"/>
</p>

<html:form action="/modifyBag">
  <tiles:insert name="wsBagTable.tile">
    <tiles:put name="limit" value="15"/>
    <tiles:put name="scope" value="user"/>
    <tiles:put name="makeCheckBoxes" value="true"/>
    <tiles:put name="showDescriptions" value="false"/>
  </tiles:insert>

  <c:if test="${fn:length(PROFILE.savedBags) >= 2}">
    <fmt:message key="history.savedbags.newbag"/>
    <html:text property="newBagName" size="12"/><br/>
    <html:submit property="union">
      <fmt:message key="history.union"/>
    </html:submit>
    <html:submit property="intersect">
      <fmt:message key="history.intersect"/>
    </html:submit>
    <html:submit property="subtract">
      <fmt:message key="history.subtract"/>
    </html:submit>
  </c:if>
  <c:if test="${fn:length(PROFILE.savedBags) > 0}">
    <div width="100%" align="right">
      <html:submit property="delete">
        <fmt:message key="history.delete"/>
      </html:submit>
    </div>
  </c:if>
  <br/>

  <h2><fmt:message key="bagspage.public.title"/></h2>
  <p>
    <fmt:message key="bagspage.public.help"/>
  </p>

  <tiles:insert name="wsBagTable.tile">
    <tiles:put name="limit" value="15"/>
    <tiles:put name="scope" value="global"/>
    <tiles:put name="makeCheckBoxes" value="true"/>
    <tiles:put name="showDescriptions" value="false"/>
    <tiles:put name="height" value="300"/>
  </tiles:insert>

  <fmt:message key="history.savedbags.newbag"/>
  <html:text property="newBagName" size="12"/><br/>
  <html:submit property="union">
    <fmt:message key="history.union"/>
  </html:submit>
  <html:submit property="intersect">
    <fmt:message key="history.intersect"/>
  </html:submit>
  <html:submit property="subtract">
    <fmt:message key="history.subtract"/>
  </html:submit>
</html:form>
</im:roundbox>
<!-- /bagView.jsp -->
