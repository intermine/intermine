package org.intermine.api.mines;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class HttpRequester implements MineRequester {

    private static final Logger LOG = Logger.getLogger(HttpRequester.class);

    private final int timeout;

    public HttpRequester(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public BufferedReader requestURL(String urlString, ContentType contentType) {
        BufferedReader reader = null;
        OutputStreamWriter writer = null;
        try {
            URL url = new URL(StringUtils.substringBefore(urlString, "?"));
            URLConnection conn = url.openConnection();
            conn.addRequestProperty("Accept", contentType.getMimeType());
            conn.setConnectTimeout(timeout);
            if (urlString.contains("?")) {
                // POST
                String queryString  = StringUtils.substringAfter(urlString, "?");
                conn.setDoOutput(true);
                writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(queryString);
                writer.flush();
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                LOG.info("FriendlyMine URL (POST) " + urlString);
            }
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            return reader;
        } catch (Exception e) {
            LOG.info("Unable to access " + urlString + " exception: " + e.getMessage());
            return null;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.error("Error sending POST request", e);
                }
            }
        }
    }

}