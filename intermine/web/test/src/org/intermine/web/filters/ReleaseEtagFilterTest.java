package org.intermine.web.filters;

import static org.easymock.EasyMock.*;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.WebConfig;
import org.junit.Before;
import org.junit.Test;

public class ReleaseEtagFilterTest {

    private ReleaseEtagFilter filter;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private FilterChain chain;
    private Properties webProperties;
    private String release;

    @Before
    public void setup() {
        webProperties = new Properties();
        webProperties.setProperty("project.releaseVersion", "testing");
        release = "testing-" + String.valueOf(org.intermine.web.logic.Constants.WEB_SERVICE_VERSION);
        InterMineContext.initilise(
                createMock(InterMineAPI.class),
                webProperties,
                createMock(WebConfig.class));
        req = createMock(HttpServletRequest.class);
        resp = createMock(HttpServletResponse.class);
        chain = createMock(FilterChain.class);
    }

    @Test
    public void testCacheMiss() throws IOException, ServletException {
        filter = new ReleaseEtagFilter();
        expect(req.getHeader("If-None-Match")).andReturn("foo");
        expect(req.getDateHeader("If-Modified-Since")).andReturn(-1L);
        resp.setHeader("ETag", release);
        resp.setHeader("Cache-Control", "public,max-age=600");
        resp.setDateHeader("Last-Modified", ReleaseEtagFilter.START_UP.getTime());

        replay(req);
        replay(resp);
        filter.doFilter(req, resp, chain);
    }

    @Test
    public void testCacheHit() throws IOException, ServletException {
        filter = new ReleaseEtagFilter();
        expect(req.getHeader("If-None-Match")).andReturn(release);
        expect(req.getDateHeader("If-Modified-Since")).andReturn(-1L);
        resp.setStatus(304);

        replay(req);
        replay(resp);
        filter.doFilter(req, resp, chain);
    }

    @Test
    public void testCacheHitGzip() throws IOException, ServletException {
        filter = new ReleaseEtagFilter();
        expect(req.getHeader("If-None-Match")).andReturn(release + "-gzip");
        expect(req.getDateHeader("If-Modified-Since")).andReturn(-1L);
        resp.setStatus(304);

        replay(req);
        replay(resp);
        filter.doFilter(req, resp, chain);
    }
}
