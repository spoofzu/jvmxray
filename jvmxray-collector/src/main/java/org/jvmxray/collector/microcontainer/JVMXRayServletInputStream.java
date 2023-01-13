package org.jvmxray.collector.microcontainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.jvmxray.agent.exception.JVMXRayUnimplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized Servlet Micro-container for easy IDE project testing.  It's not a complete
 * or performant implementation.  It's not recommended for production or any serious
 * work.
 * @author Milton Smith
 */
public class JVMXRayServletInputStream extends ServletInputStream {

    /** Get logger instance. */
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.collector.micrcontainer.JVMXRayServletInputStream");

    private InputStream in;
    public JVMXRayServletInputStream( ByteArrayInputStream in) throws IOException {
        this.in = in;
    }
    @Override
    public boolean isFinished() {
        return !isReady();
    }
    @Override
    public boolean isReady() {
        boolean result = false;
        try {
            result = in.available() > 0;
        } catch (IOException e) {
            logger.error("Error testing stream for available data.",e);
        }
        return result;
    }
    @Override
    public void setReadListener(ReadListener readListener) {
        throw new JVMXRayUnimplementedException("JVMXRayServletInputStream.setReadListener(): Not implemented.");
    }
    @Override
    public int read() throws IOException {
        return in.read();
    }
}

