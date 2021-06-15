package org.jvmxray.collector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.jvmxray.exception.JVMXRayUnimplementedException;

public class JVMXRayServletRequest implements HttpServletRequest {

	private final Socket socket;
	private ServletInputStream in;
    private String httpMethod;
    private String httpQueryString;
	private String content;

	public JVMXRayServletRequest(Socket socket, ServletInputStream in, String content, String httpMethod, String httpQueryString) throws IOException {
	    this.socket = socket;
		this.in = in;
		this.content = content;
	    this.httpMethod = httpMethod;
	    this.httpQueryString = httpQueryString;
	}

	@Override
	public Object getAttribute(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getAttribute(String): Not implemented.");
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getAttributeNames(): Not implemented.");
	}

	@Override
	public String getCharacterEncoding() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getCharacterEncoding(): Not implemented.");
	}

	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.setCharacterEncoding(String): Not implemented.");
	}

	@Override
	public int getContentLength() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getContentLength(): Not implemented.");
	}

	@Override
	public long getContentLengthLong() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getContentLengthLong(): Not implemented.");
	}

	@Override
	public String getContentType() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getContentType(): Not implemented.");
	}

	// used
	@Override
	public ServletInputStream getInputStream() throws IOException {
		return in;
	}

	@Override
	public String getParameter(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getParameter(String): Not implemented.");
	}

	@Override
	public Enumeration<String> getParameterNames() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getParameterNames(String): Not implemented.");
	}

	@Override
	public String[] getParameterValues(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getParameterValues(String): Not implemented.");
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getParameterMap(): Not implemented.");

	}

	@Override
	public String getProtocol() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getProtocol(): Not implemented.");
	}

	@Override
	public String getScheme() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getScheme(): Not implemented.");
	}

	@Override
	public String getServerName() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getServerName(): Not implemented.");
	}

	@Override
	public int getServerPort() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getServerPort(): Not implemented.");
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getReader(): Not implemented.");
	}

	@Override
	public String getRemoteAddr() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRemoteAddr(): Not implemented.");
	}

	@Override
	public String getRemoteHost() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRemoteHost(): Not implemented.");
	}

	@Override
	public void setAttribute(String name, Object o) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.setAttribute(String, Object): Not implemented.");
	}

	@Override
	public void removeAttribute(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.removeAttribute(String): Not implemented.");
	}

	@Override
	public Locale getLocale() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getLocale(): Not implemented.");
	}

	@Override
	public Enumeration<Locale> getLocales() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getLocales(): Not implemented.");
	}

	@Override
	public boolean isSecure() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.isSecure(): Not implemented.");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRequestDispatcher(String): Not implemented.");
	}

	@Override
	public String getRealPath(String path) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRealPath(String): Not implemented.");
	}

	@Override
	public int getRemotePort() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRemotePort(): Not implemented.");
	}

	@Override
	public String getLocalName() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getLocalName(): Not implemented.");
	}

	@Override
	public String getLocalAddr() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getLocalAddr(): Not implemented.");
	}

	@Override
	public int getLocalPort() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getLocalPort(): Not implemented.");
	}

	@Override
	public ServletContext getServletContext() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getServletContext(): Not implemented.");
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.startAsync(): Not implemented.");
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
			throws IllegalStateException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.startAsync(ServletRequest, ServletResponse): Not implemented.");
	}

	@Override
	public boolean isAsyncStarted() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.isAsyncStarted(): Not implemented.");
	}

	@Override
	public boolean isAsyncSupported() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.isAsyncSupported(): Not implemented.");
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getAsyncContext(): Not implemented.");
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getDispatcherType(): Not implemented.");
	}

	@Override
	public String getAuthType() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getAuthType(): Not implemented.");
	}

	@Override
	public Cookie[] getCookies() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getCookies(): Not implemented.");
	}

	@Override
	public long getDateHeader(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getDateHeader(String): Not implemented.");
	}

	@Override
	public String getHeader(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getHeader(String): Not implemented.");
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getHeaders(String): Not implemented.");
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getHeaderNames(): Not implemented.");
	}

	@Override
	public int getIntHeader(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getIntHeader(String): Not implemented.");
	}

	// used 
	@Override
	public String getMethod() {
		return httpMethod;
	}

	@Override
	public String getPathInfo() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getPathInfo(): Not implemented.");
	}

	@Override
	public String getPathTranslated() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getPathTranslated(): Not implemented.");
	}

	@Override
	public String getContextPath() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getContextPath(): Not implemented.");
	}

	// used
	@Override
	public String getQueryString() {
		return httpQueryString;
	}

	@Override
	public String getRemoteUser() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRemoteUser(): Not implemented.");
	}

	@Override
	public boolean isUserInRole(String role) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.isUserInRole(String): Not implemented.");
	}

	@Override
	public Principal getUserPrincipal() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getUserPrincipal(): Not implemented.");
	}

	@Override
	public String getRequestedSessionId() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRequestedSessionId(): Not implemented.");
	}

	@Override
	public String getRequestURI() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRequestURI(): Not implemented.");
	}

	@Override
	public StringBuffer getRequestURL() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getRequestURL(): Not implemented.");
	}

	@Override
	public String getServletPath() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getServletPath(): Not implemented.");
	}

	@Override
	public HttpSession getSession(boolean create) {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getSession(boolean): Not implemented.");
	}

	@Override
	public HttpSession getSession() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getSession(): Not implemented.");
	}

	@Override
	public String changeSessionId() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.changeSessionId(): Not implemented.");
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.isRequestedSessionIdValid(): Not implemented.");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.isRequestedSessionIdFromCookie(): Not implemented.");
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.isRequestedSessionIdFromURL(): Not implemented.");
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.isRequestedSessionIdFromUrl(): Not implemented.");
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.authenticate(HttpServletResponse): Not implemented.");
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.login(String, String): Not implemented.");
	}

	@Override
	public void logout() throws ServletException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.logout(): Not implemented.");
		
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getParts(): Not implemented.");
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.getPart(String): Not implemented.");
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		throw new JVMXRayUnimplementedException("JVMXRayServletRequest.upgrade(Class<T>): Not implemented.");
	}



}
