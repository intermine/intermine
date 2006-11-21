<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- historyBagView.jsp -->
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

<h2>My bags</h2>
  <p>
    <fmt:message key="history.savedbags.help"/>
  </p>

  <c:choose>
    <c:when test="${empty PROFILE.savedBags}">
      <div class="altmessage">
        None
      </div>
    </c:when>
    <c:otherwise>

      <html:form action="/modifyBag">
        
        <table class="results history" cellspacing="0">
          <tr>
            <th>
              <input type="checkbox" id="selected_bag"
                     onclick="selectColumnCheckbox(this.form, 'bag')">
            </th>
            <th align="left" colspan="2" nowrap>
              <fmt:message key="query.savedbags.namecolumnheader"/>
            </th>
            <th align="right" nowrap>
              <fmt:message key="query.savedbags.countcolumnheader"/>
            </th>
          </tr>
          <c:forEach items="${PROFILE.savedBags}" var="savedBag" varStatus="status">
            <tr>
              <td>
                <html:multibox property="selectedBags" styleId="selected_bag_${status.index}">
                  <c:out value="${savedBag.key}" escapeXml="false"/>
                </html:multibox>
              </td>

              <tiles:insert name="renamableElement.jsp">
                <tiles:put name="name" value="${savedBag.key}"/>
                <tiles:put name="type" value="bag"/>
                <tiles:put name="index" value="${status.index}"/>
              </tiles:insert>
              
              <td align="right">
                <c:out value="${savedBag.value.size}"/>
                <c:choose>
                  <c:when test="${fn:endsWith(savedBag.value.class.name, 'InterMineIdBag')}">
                    <c:if test="${savedBag.value.size == 1}">
                      object
                    </c:if>
                    <c:if test="${savedBag.value.size != 1}">
                      objects
                    </c:if>
                  </c:when>
                  <c:otherwise>
                    <c:if test="${savedBag.value.size == 1}">
                      value
                    </c:if>
                    <c:if test="${savedBag.value.size != 1}">
                      values
                    </c:if>
                  </c:otherwise>
                </c:choose>
              </td>
            </tr>
          </c:forEach>
        </table>
        <br/>
        <c:if test="${fn:length(PROFILE.savedBags) >= 2}">
          New bag name:
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
        <html:submit property="delete">
          <fmt:message key="history.delete"/>
        </html:submit>
      </html:form>
      <br/>

    </c:otherwise>
  </c:choose>

  
<!-- /historyBagView.jsp -->
