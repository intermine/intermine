<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<fmt:setBundle basename="model"/>

<!-- modelledStructure.jsp -->
<applet name="jmol" code="JmolApplet" archive="<html:rewrite href='model/JmolApplet.jar'/>" width="350" height="350">
  <param name="progressbar" value="true"/>
  <param name="progresscolor" value="blue"/>
  <!--param name="load" value="http://localhost:8080/1417.atm"/-->
  <param name="load" value="<html:rewrite action='/getAttributeAsFile?object=${object.id}&field=atm'/>"/>
  <param name="script" value="select all; cpk off; wireframe off; backbone 0.3; color backbone structure"/>
</applet>
<!-- /modelledStructure.jsp -->
