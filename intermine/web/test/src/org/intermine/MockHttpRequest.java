package org.intermine;

import java.io.BufferedReader;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Mock for testing methods that handle requests.
 *
 * @author Alex Kalderimis
 */
public class MockHttpRequest implements HttpServletRequest
{
    private final Map<String, String[]> headers, parameters;
    private final String method;

    private static final SimpleDateFormat dateFormat
        = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    public MockHttpRequest(String method,
            Map<String, String[]> headers,
            Map<String, String[]> parameters) {
        if (headers == null || parameters == null || method == null) {
            throw new NullPointerException();
        }
        this.method = method;
        this.headers = headers;
        this.parameters = parameters;
    }

    @Override
    public String getAuthType() {
        throw new UnmockedException();
    }

    @Override
    public long getDateHeader(String name) {
        String val = getHeader(name);
        if (val != null) {
            try {
                return dateFormat.parse(val).getTime();
            } catch (ParseException e) {
                // Ignore.
            }
        }
        return 0L;
    }

    @Override
    public String getHeader(String name) {
        for (String k: headers.keySet()) {
            if (k.equalsIgnoreCase(name)) {
                return headers.get(k)[0];
            }
        }
        return null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        Vector<String> v = new Vector<String>();
        for (String k: headers.keySet()) {
            if (k.equalsIgnoreCase(name)) {
                v.addAll(Arrays.asList(headers.get(k)));
            }
        }
        return v.elements();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Vector<String> v = new Vector<String>(headers.keySet());
        return v.elements();
    }

    @Override
    public int getIntHeader(String name) {
        return Integer.valueOf(getHeader(name));
    }

    @Override
    public String getMethod() {
        return method;
    }

    private String pathInfo = null;

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return getPathInfo();
    }

    @Override
    public String getContextPath() {
        throw new UnmockedException();
    }

    @Override
    public String getQueryString() {
        throw new UnmockedException();
    }

    @Override
    public String getRemoteUser() {
        throw new UnmockedException();
    }
    @Override
    public String getRequestedSessionId() {
        throw new UnmockedException();
    }

    @Override
    public String getRequestURI() {
        throw new UnmockedException();
    }

    @Override
    public StringBuffer getRequestURL() {
        throw new UnmockedException();
    }

    @Override
    public String getServletPath() {
        throw new UnmockedException();
    }

    @Override
    public HttpSession getSession() {
        throw new UnmockedException();
    }

    @Override
    public HttpSession getSession(boolean create) {
        throw new UnmockedException();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        throw new UnmockedException();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        throw new UnmockedException();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new UnmockedException();
    }


    @Override
    public boolean isUserInRole(String role) {
        throw new UnmockedException();
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnmockedException();
    }

    // From ServletRequest

    @Override
    public Object getAttribute(String name) {
        throw new UnmockedException();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        throw new UnmockedException();
    }

    @Override
    public String getContentType() {
        throw new UnmockedException();
    }

    @Override
    public ServletInputStream getInputStream() {
        throw new UnmockedException();
    }

    @Override
    public String getLocalAddr() {
        throw new UnmockedException();
    }

    @Override
    public Locale getLocale() {
        throw new UnmockedException();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new UnmockedException();
    }

    @Override
    public String getLocalName() {
        throw new UnmockedException();
    }

    @Override
    public int getLocalPort() {
        throw new UnmockedException();
    }

    @Override
    public String getParameter(String name) {
        String[] all = parameters.get(name);
        if (all != null) {
            return all[0];
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        Vector<String> v = new Vector<String>(parameters.keySet());
        return v.elements();
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public String getProtocol() {
        throw new UnmockedException();
    }

    @Override
    public BufferedReader getReader() {
        throw new UnmockedException();
    }

    @Override
    public String getRealPath(String path) {
        throw new UnmockedException();
    }

    @Override
    public String getRemoteAddr() {
        throw new UnmockedException();
    }

    @Override
    public String getRemoteHost() {
        throw new UnmockedException();
    }

    @Override
    public int getRemotePort() {
        throw new UnmockedException();
    }

    @Override
    public String getScheme() {
        throw new UnmockedException();
    }

    @Override
    public String getServerName() {
        throw new UnmockedException();
    }

    @Override
    public int getServerPort() {
        throw new UnmockedException();
    }


    @Override
    public void setCharacterEncoding(String name) {
        throw new UnmockedException();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new UnmockedException();
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
        throw new UnmockedException();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new UnmockedException();
    }

    @Override
    public Cookie[] getCookies() {
        throw new UnmockedException();
    }


    @Override
    public boolean isSecure() {
        throw new UnmockedException();
    }

    @Override
    public void removeAttribute(String arg0) {
        throw new UnmockedException();

    }


    @Override
    public String getCharacterEncoding() {
        throw new UnmockedException();

    }

    @Override
    public int getContentLength() {
        throw new UnmockedException();
    }


    public static class UnmockedException extends RuntimeException
    {
        private static final long serialVersionUID = 962046232556269912L;

        public UnmockedException() {
            super("No implementation provided for " + new Exception().getStackTrace()[1].getMethodName());
        }
    }


}
