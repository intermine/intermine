package org.intermine.webservice.server.lists;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.BadRequestException;

public class ListAppendService extends ListUploadService {

    public static final String USAGE =
          "List Append Service\n"
        + "===================\n"
        + "Append items to a list\n"
        + "Parameters:\n"
        + "name: the name of the list to append items to\n\n"
        + "Content: text/plain - list of ids\n";

    public ListAppendService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        if (!this.isAuthenticated()) {
            throw new BadRequestException("Not authenticated.\n" + USAGE);
        }
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);
        Properties webProperties
            = (Properties) session.getServletContext().getAttribute(Constants.WEB_PROPERTIES);;

        String name = request.getParameter("name");
        String extraFieldValue = request.getParameter("extraValue");

        if (StringUtils.isEmpty(name)) {
            throw new BadRequestException("Name is blank." + USAGE);
        }

        output.setHeaderAttributes(getHeaderAttributes());

        String bagUploadDelims =
            (String) webProperties.get("list.upload.delimiters") + " ";
        StrMatcher matcher = StrMatcher.charSetMatcher(bagUploadDelims);
        if (!"text/plain".equals(request.getContentType())) {
            throw new BadRequestException("Bad content type - " + request.getContentType() + USAGE);
        }
        BufferedReader r = request.getReader();

        Set<String> ids = new LinkedHashSet<String>();
        Set<String> unmatchedIds = new HashSet<String>();

        InterMineBag bag = profile.getSavedBags().get(name);
        if (bag == null) {
            throw new BadRequestException(name + " is not a list you have access to");
        }
        String line;
        while ((line = r.readLine()) != null) {
            StrTokenizer st = new StrTokenizer(line, matcher, StrMatcher.doubleQuoteMatcher());
            while (st.hasNext()) {
                String token = st.nextToken();
                ids.add(token);
            }
            if (ids.size() >= BAG_QUERY_MAX_BATCH_SIZE) {
                addIdsToList(ids, bag, bag.getType(), extraFieldValue, unmatchedIds);
                ids.clear();
            }
        }
        if (ids.size() > 0) {
            addIdsToList(ids, bag, bag.getType(), extraFieldValue, unmatchedIds);
        }
        for (Iterator<String> i = unmatchedIds.iterator(); i.hasNext();) {
            List<String> row = new ArrayList<String>(Arrays.asList(i.next()));
            if (i.hasNext()) {
                row.add("");
            }
            output.addResultItem(row);
        }

    }

}
