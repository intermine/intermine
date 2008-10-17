<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<TABLE width="100%">
  <TR>
    <TD valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <p>
          Orthologue and paralogue relationships calculated by <A
          href="http://inparanoid.cgb.ki.se/index.html">InParanoid</A> (latest
          calculated 16th April 2005) between the following organisms:
        </p>
        <ul>
          <li><I>Plasmodium falciparum 3D7</I></li>
          <li><I>Schizosaccharomyces pombe</I></li>
        </ul>
      </div>
    </TD>

    <TD width="45%" valign="top">
      <div class="heading2">
       Bulk download
      </div>
      <div class="body">
        <ul>
          <li>
                    <p><im:querylink text="Show all pairs of organisms linked by orthologues" skipBuilder="true">
            <query name="" model="genomic" view="Homologue.gene.organism.shortName Homologue.homologue.organism.shortName"><node path="Homologue" type="Homologue"></node></query>
          </im:querylink></p>
          </li>
        </ul>
      </div>
    </TD>
  </TR>
</TABLE>


