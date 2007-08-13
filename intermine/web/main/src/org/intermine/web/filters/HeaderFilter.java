package org.intermine.web.filters;

import java.util.Enumeration;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * A Filter that modifies response headers of outgoing pages.
 * @author Kim Rutherford
 */
public class HeaderFilter implements Filter {
    private FilterConfig fc;

    /**
     * Do the filtering.
     * {@inheritDoc}
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
    throws IOException, ServletException {
        HttpServletResponse response =(HttpServletResponse) res;

        Enumeration<String> e = fc.getInitParameterNames();
        while (e.hasMoreElements()) {
            String headerName = e.nextElement();
            response.addHeader(headerName, fc.getInitParameter(headerName));
        }

        chain.doFilter(req, response);
    }

    /**
     * Initialise this Filter.
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) {
        this.fc = filterConfig;
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
       // empty
    }
}
