<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- footer.jsp -->
<table class="footer">
  <tr>
    <td align="left" width="10%">
      <table>
        <tr>
          <td align="center">
            <a href="http://www.wellcome.ac.uk">
              <img src="images/wellcome.gif" border="0" hspace="10" alt="Wellcome Trust Logo"/>
            </a>
          </td>
          <td>
            <fmt:message key="intermine.funding"/>
          </td>
        </tr>
      </table>
    </td>
    <td align="right" width="10%">
      <table cellpadding="10">
        <tr>
          <td>
            FlyMine<br/>Department of Genetics<br/>Downing Street<br/>Cambridge, CB2 3EH, UK
          </td>
          <td>
            Tel: +44 (0)1223 333377<br/>Fax: +44 (0)1223 333992<br/><a href="mailto:info[at]flymine.org">info[at]flymine.org</a>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<!-- /footer.jsp -->
