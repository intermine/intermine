<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <p>
          The aim of <A href="http://www.indac.net" target="_new">INDAC</A> is to produce a widely
          available and uniform set of array reagents so that microarray data collected
          from different studies may be more easily compared. On behalf of INDAC, the
          <A href="http://www.flychip.org.uk" target="_new">FlyChip group</A> has designed a set
          of 65-69mer long oligonucleotides to release 4.1 of the <I>D. melanogaster</I>
          genome. Oligos were designed using a modified version of 
          <A href="http://berry.engin.umich.edu/oligoarray2" target="_new">OligoArray2</A> and other
          post-processing steps (David Kreil, Debashis Rana, Gos Micklem unpublished).
        </p>
        <p>
          Synthesis of the set by <A href="http://www.illumina.com" target="_new">Illumina</A>
          began in April 2005.  FlyMine will incorporate the results of these tests
          when available.
        </p>
        <p>
          Note: FlyMine curently stores the positions of the oligos relative to
          the transcript rather than to the chromosome.
        </p>
      </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
         <ul>
      
          <li>
            <im:querylink text="All INDAC microarray oligos with their length and tm and the identifier of the associated transcript " skipBuilder="true">
              <query name="" model="genomic" view="MicroarrayOligo.identifier MicroarrayOligo.length MicroarrayOligo.tm MicroarrayOligo.transcript.identifier">
              </query>
            </im:querylink>

          </li>
         </ul>
      </div>
    </td>
  </tr>
</table>
