package org.jvmxray.platform.shared.log.logback.codec;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XRCSVEncoderTest {

    private XRCSVEncoder newStartedEncoder(LoggerContext context) {
        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%msg");
        layout.start();

        XRCSVEncoder encoder = new XRCSVEncoder();
        encoder.setContext(context);
        encoder.setLayout(layout);
        encoder.start();
        return encoder;
    }

    @Test
    public void encodesCoreFieldsAsCsv() {
        LoggerContext context = new LoggerContext();
        Logger logger = context.getLogger("test.logger");
        XRCSVEncoder encoder = newStartedEncoder(context);

        LoggingEvent event = new LoggingEvent(
                "org.jvmxray.Fqcn", logger, Level.INFO, "hello world", null, null);
        event.setThreadName("main");
        event.setTimeStamp(1234567890L);
        // A hand-built LoggingEvent has no MDC map; real logback always sets one.
        // Without this, encode() NPEs iterating a null MDC map.
        event.setMDCPropertyMap(Collections.emptyMap());

        String csv = new String(encoder.encode(event));
        // CSV column order: timestamp,level,thread,logger,message,exception,<mdc...>
        // Columns 0-3 precede the message and contain no commas, so split(",") is safe for them.
        String[] cols = csv.split(",");

        assertEquals("1234567890", cols[0]);
        assertEquals("INFO", cols[1]);
        assertEquals("main", cols[2]);
        assertEquals("test.logger", cols[3]);
        assertTrue("CSV should end with newline", csv.endsWith("\n"));
    }

    @Test
    public void includesExceptionMessageWhenThrowablePresent() {
        LoggerContext context = new LoggerContext();
        Logger logger = context.getLogger("test.logger");
        XRCSVEncoder encoder = newStartedEncoder(context);

        LoggingEvent event = new LoggingEvent(
                "org.jvmxray.Fqcn", logger, Level.ERROR, "boom occurred",
                new RuntimeException("kaboom"), null);
        event.setThreadName("worker-1");
        event.setTimeStamp(42L);
        event.setMDCPropertyMap(Collections.emptyMap());

        String csv = new String(encoder.encode(event));

        // Column order: timestamp,level,thread,logger,message,exception(,mdc...).
        // With no MDC, the exception field is the last column, so the line ends with it.
        // This exercises the JSON exception branch (the only path that reads
        // node.has("exception") + the exception value back out).
        assertTrue("prefix", csv.startsWith("42,ERROR,worker-1,test.logger,"));
        assertTrue("exception column should carry the throwable message",
                csv.endsWith("kaboom\n"));
    }
}
