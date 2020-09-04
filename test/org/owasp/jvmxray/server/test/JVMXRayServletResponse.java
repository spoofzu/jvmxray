package org.owasp.jvmxray.server.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.owasp.jvmxray.exception.JVMXRayUnimplementedException;

public class JVMXRayServletResponse implements HttpServletResponse {

	private PrintWriter writer;
	private int sc;
	private StringWriter sw;

	public JVMXRayServletResponse( StringWriter sw ) {
		this.sw = sw;
		this.writer = new PrintWriter(sw);
	}

	@Override
	public String getCharacterEncoding() {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getCharacterEncoding(): Not implemented.");
	}

	@Override
	public String getContentType() {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getContentType(): Not implemented.");
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getOutputStream(): Not implemented.");
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return writer;
	}

	@Override
	public void setCharacterEncoding(String charset) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.setCharacterEncoding(String): Not implemented.");	
	}

	@Override
	public void setContentLength(int len) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.setContentLength(int): Not implemented.");	
	}

	@Override
	public void setContentLengthLong(long len) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.setContentLengthLong(long): Not implemented.");
	}

	@Override
	public void setContentType(String type) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getCharacterEncoding(String): Not implemented.");
	}

	@Override
	public void setBufferSize(int size) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getCharacterEncoding(int): Not implemented.");
	}

	@Override
	public int getBufferSize() {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getCharacterEncoding(): Not implemented.");
	}

	@Override
	public void flushBuffer() throws IOException {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.flushBuffer(): Not implemented.");
	}

	@Override
	public void resetBuffer() {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.resetBuffer(): Not implemented.");
	}

	@Override
	public boolean isCommitted() {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.isCommitted(): Not implemented.");
	}

	@Override
	public void reset() {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.reset(): Not implemented.");
	}

	@Override
	public void setLocale(Locale loc) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.setLocale(): Not implemented.");
	}

	@Override
	public Locale getLocale() {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getLocale(): Not implemented.");
	}

	@Override
	public void addCookie(Cookie cookie) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.addCookie(Cookie): Not implemented.");
	}

	@Override
	public boolean containsHeader(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.containsHeader(String): Not implemented.");
	}

	@Override
	public String encodeURL(String url) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getCharacterEncoding(String): Not implemented.");
	}

	@Override
	public String encodeRedirectURL(String url) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getCharacterEncoding(String): Not implemented.");
	}

	@Override
	public String encodeUrl(String url) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getCharacterEncoding(String): Not implemented.");
	}

	@Override
	public String encodeRedirectUrl(String url) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.encodeRedirectUrl(String): Not implemented.");
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.sendError(int, String): Not implemented.");	
	}

	@Override
	public void sendError(int sc) throws IOException {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.sendError(int): Not implemented.");
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.sendRedirect(String): Not implemented.");
	}

	@Override
	public void setDateHeader(String name, long date) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.setDateHeader(String, long): Not implemented.");	
	}

	@Override
	public void addDateHeader(String name, long date) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.addDateHeader(String, long): Not implemented.");
	}

	@Override
	public void setHeader(String name, String value) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.setHeader(String, String): Not implemented.");	
	}

	@Override
	public void addHeader(String name, String value) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.addHeader(String, String): Not implemented.");
	}

	@Override
	public void setIntHeader(String name, int value) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.setIntHeader(String, int): Not implemented.");	
	}

	@Override
	public void addIntHeader(String name, int value) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.addIntHeader(String, int): Not implemented.");	
	}

	@Override
	public void setStatus(int sc) {
		this.sc = sc;
	}

	@Override
	public void setStatus(int sc, String sm) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.setStatus(int, String): Not implemented.");	
	}

	@Override
	public int getStatus() {
		return sc;
	}

	@Override
	public String getHeader(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getHeader(String name): Not implemented.");
	}

	@Override
	public Collection<String> getHeaders(String name) {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getHeaders(String): Not implemented.");
	}

	@Override
	public Collection<String> getHeaderNames() {
		throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getHeaderNames(): Not implemented.");
	}

	
}
