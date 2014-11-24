package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * A mine requester that makes HTTP requests.
 * @author Alex Kalderimis
 *
 */
public class HttpRequester implements MineRequester
{

    private static final Logger LOG = Logger.getLogger(HttpRequester.class);

    private final int timeout;

    /**
     * Create an object that will make HTTP requests
     * @param timeoutInSeconds The number of seconds we will wait before timing out.
     */
    public HttpRequester(int timeoutInSeconds) {
        this.timeout = timeoutInSeconds;
    }

    @Override
    public BufferedReader requestURL(final String urlString, final ContentType contentType) {
        BufferedReader reader = null;
        OutputStreamWriter writer = null;
        // TODO: when all friendly mines support mimetype formats then we can remove this.
        String suffix = "?format=" + contentType.getFormat();
        try {
            URL url = new URL(StringUtils.substringBefore(urlString, "?") + suffix);
            URLConnection conn = url.openConnection();
            conn.addRequestProperty("Accept", contentType.getMimeType());
            conn.setConnectTimeout(timeout * 1000); // conn accepts millisecond timeout.
            if (urlString.contains("?")) {
                // POST
                String queryString  = StringUtils.substringAfter(urlString, "?");
                conn.setDoOutput(true);
                writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(queryString);
                writer.flush();
                LOG.info("FriendlyMine URL (POST) " + urlString);
            }
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to access " + urlString + " exception: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.error("Error sending POST request", e);
                }
            }
        }
        return reader;
    }

}
