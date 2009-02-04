<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- modelledStructure.jsp -->
<script type="text/javascript" src="<html:rewrite href='model/jmol/Jmol.js'/>" ></script>


<applet name="jmol" code="JmolApplet" archive="<html:rewrite href='model/jmol/JmolApplet0.jar'/>" width="350" height="350">
  <param name="progressbar" value="true"/>
  <param name="progresscolor" value="blue"/>
  <!--param name="load" value="http://localhost:8080/1417.atm"/-->
  <param name="load" value="<html:rewrite action='/getAttributeAsFile?object=${object.id}&field=atm'/>"/>
  <param name="script" value="select all; cpk off; wireframe off; backbone 0.3; color backbone structure"/>
</applet>
<!-- /modelledStructure.jsp -->
