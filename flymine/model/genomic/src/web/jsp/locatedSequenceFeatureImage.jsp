<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<fmt:setBundle basename="model"/>

<!-- locatedSequenceFeatureImage.jsp -->
<html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.source]}?name=${cld.unqualifiedName}:FlyMineInternalID_${object.id};width=800">
  <div>
    <html:img src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.source']}?name=${cld.unqualifiedName}:FlyMineInternalID_${object.id};width=400"/>
  </div>
  <div>
    <fmt:message key="locatedSequenceFeature.GBrowse.message"/>
  </div>
</html:link>
<!-- /locatedSequenceFeatureImage.jsp -->
