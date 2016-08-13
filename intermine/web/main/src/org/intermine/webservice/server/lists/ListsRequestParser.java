package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Request processor for ListsService that process request, validates it and returns
 * parsed input as a parameter object.
 * @author Jakub Kulaviak
 **/
public class ListsRequestParser
{
    private HttpServletRequest request;

    /**
     * Name of parameter with intermine id of object.
     */
    private static final String MINE_ID_PARAMETER = "id";
    /**
     * Name of parameter with id of object. It can be for example primaryIdentifier,
     * secondaryIdentifier. Intermine id is obtained by lookup of this id.
     */
    private static final String PUBLIC_ID_PARAMETER = "publicId";

    /**
     * Name of parameter with object type.
     */
    public static final String TYPE_PARAMETER = "type";

    /**
     * ListsRequestProcessor constructor.
     * @param request request
     */
    public ListsRequestParser(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns parameters of web service as a parameter object.
     * @return ListsService input
     */
    public ListsServiceInput getInput() {
        ListsServiceInput ret = new ListsServiceInput();

        String publicId = request.getParameter(PUBLIC_ID_PARAMETER);
        String mineId = request.getParameter(MINE_ID_PARAMETER);
        if ((publicId == null && mineId == null) || (publicId != null && mineId != null)) {
            throw new BadRequestException("invalid parameters: " + MINE_ID_PARAMETER + " or "
                    + PUBLIC_ID_PARAMETER + " are required.");
        }

        if (publicId != null) {
            ret.setPublicId(publicId);
            String  type = request.getParameter(TYPE_PARAMETER);
            if (type != null) {
                ret.setType(type);
            } else {
                throw new BadRequestException("missing parameter: " + TYPE_PARAMETER);
            }
        } else {
            try {
                ret.setMineId(Integer.parseInt(mineId));
            } catch (Throwable t) {
                throw new BadRequestException("invalid parameter: " + MINE_ID_PARAMETER);
            }
        }
        ret.setExtraValue(request.getParameter("extraValue"));
        return ret;
    }
}
