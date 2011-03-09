package samples;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.ListService;


/**
 * The ListClient is an example of list client fetching all public lists containing FBgn0000606 gene
 * from InterMine web service. It demonstrates using of InterMine list web service.
 * 
 * NOTE: The list will change probably in next FlyMine versions and it is possible, that
 * there won't be any result. In this case please download newer version of samples or 
 * modify it properly.
 *
 * @author Jakub Kulaviak
 **/
public class ListClient
{
    private static String serviceRootUrl = "http://localhost:8080/query/service";
    
    /**
     * @param args command line arguments
     */
    public static void main(String[] args) {
        
        ListService service = new ServiceFactory(serviceRootUrl, "ListClient").getListService();
        List<String> result = service.getPublicListsWithObject("FBgn0000606", "Gene");
        System.out.println("Following public lists contain FBgn0000606 gene: ");
        for (String row : result) {
            System.out.println(row);
        }
    }

}
