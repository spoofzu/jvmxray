package org.jvmxray.collector.microcontainer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.jvmxray.agent.exception.JVMXRayUnimplementedException;

/**
 * Specialized Servlet Micro-container for easy IDE project testing.  It's not a complete
 * or performant implementation.  It's not recommended for production or any serious
 * work.
 * @author Milton Smith
 */
public class JVMXRayServletResponse implements HttpServletResponse {

    private PrintWriter writer;
    private int sc;
    private String content_type="text/html";

    public JVMXRayServletResponse( StringWriter sw ) {
        this.writer = new PrintWriter(sw);
    }

    @Override
    public String getCharacterEncoding() {
        throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getCharacterEncoding(): Not implemented.");
    }

    @Override
    public String getContentType() {
        return content_type;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        throw new JVMXRayUnimplementedException("JVMXRayServletResponse.getOutputStream(): Not implemented.");
    }

    @Override
    public PrintWriter getWriter(){
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
        content_type = type;
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
    public void flushBuffer() {
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
    public void sendError(int sc, String msg) {
        throw new JVMXRayUnimplementedException("JVMXRayServletResponse.sendError(int, String): Not implemented.");
    }

    @Override
    public void sendError(int sc) {
        throw new JVMXRayUnimplementedException("JVMXRayServletResponse.sendError(int): Not implemented.");
    }

    @Override
    public void sendRedirect(String location) {
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
