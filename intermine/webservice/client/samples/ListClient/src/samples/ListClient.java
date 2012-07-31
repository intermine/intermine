package samples;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.lists.ItemList;
import org.intermine.webservice.client.results.Item;
import org.intermine.webservice.client.services.ListService;


/**
 * This program demonstrates the use of several InterMine list web-service features. Including:
 * <ul>
 * <li>Getting lists with a common member</li>
 * <li>Getting attributes of the lists</li>
 * <li>Iterating over the members of a list</li>
 * </ul>
 *
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 **/
public class ListClient
{
    private static final String serviceRootUrl = "http://www.flymine.org/query/service";
    private static final String format = "%-35s %-10d %s\n";
    private static final String headerFormat = "%-35s %-10s %s\n";

    /**
     * @param args command line arguments
     */
    @SuppressWarnings("serial")
    public static void main(String[] args) {

        // Construct a factory with access to lists
        ListService service = new ServiceFactory(serviceRootUrl).getListService();
        // Find lists which share a member.
        List<ItemList> result = service.getListsWithObject("FBgn0000606", "Gene");
        System.out.println("The following public lists contain the FBgn0000606 gene:");
        System.out.println("========================================================");
        System.out.printf(headerFormat, "NAME", "SIZE", "DESCRIPTION");
        System.out.println("--------------------------------------------------------");
        // Inspect lists by their meta-data properties:
        for (ItemList il: result) {
            System.out.printf(format, il.getName(), il.size(), il.getDescription());
        }

        Collections.sort(result, new Comparator<ItemList>() {
            public int compare(ItemList arg0, ItemList arg1) {
                return new Integer(arg0.size()).compareTo(new Integer(arg1.size()));
            }
        });

        ItemList smallest = result.get(0);

        // Iterate over the items in a list:
        System.out.println("\nFirst ten Items in the smallest list (" + smallest.getName() + "):");
        int c = 0;
        for (Item i: smallest) {
            System.out.print(i.getString("symbol") + " ");
            if (++c >= 10) break;
        }

        // Pick out one individual item by a property, or combination of properties:
        Item ttk = smallest.find(new HashMap<String, Object>() {{ put("symbol", "ttk"); }}).get(0);
        System.out.println("\n\nInformation about TramTrack:");
        System.out.println(ttk);
    }

}
