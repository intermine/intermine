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

          From the <a href="http://superfly.ucsd.edu/homophila/">Homophila web site</a>:
        </p>
        <blockquote>
          "Homophila utilizes the sequence information of human disease genes from the
          NCBI OMIM (Online Mendelian Inheritance in Man) database in order to determine
          if sequence homologs of these genes exist in the current <i>Drosophila</i> sequence
          database (FlyBase). Sequences are compared using NCBI's BLAST program. The
          database is updated weekly and can be searched by human disease, gene name,
          OMIM number, title, subtitle and/or allelic variant descriptions."
        </blockquote>

        <p>
          Homophila is described in these papers:
        </p>
        <ul>
          <li>
            Reiter LT, Potocki L, Chien S, Gribskov M, Bier E. Genome Res. 2001
            Jun;11(6):1114-25. "A Systematic Analysis of Human Disease-Associated
            Gene Sequences In Drosophila melanogaster" (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=11381037&query_hl=2">PubMed:
              11381037</A>)
          </li>
          <li>
            Chien S, Reiter LT, Bier E, Gribskov M., Nucleic Acids Res. 2002 Jan
            1;30(1):149-51.
            "Homophila: human disease gene cognates in Drosophila" (<a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=11752278&query_hl=2">PubMed:
              11752278</A>)
          </li>
        </ul>
        <p>
          See also:
          <a href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=OMIM">Online
          Mendelian Inheritance in Man<sup><font size="-2">TM</font></sup></a>
        </p>
      </div>
    </TD>
    <TD width="30%" valign="top">
      <div class="heading2">
       Bulk download
      </div>
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All diseases from the Homophila data set" 
                          skipBuilder="true">
              <query name="" model="genomic" view="Disease.omimId Disease.description">
              </query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </TD>
  </TR>
</TABLE>
