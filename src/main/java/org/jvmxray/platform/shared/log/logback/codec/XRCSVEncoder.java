package org.jvmxray.platform.shared.log.logback.codec;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.google.gson.JsonObject;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

/**
 * Encoder for CSVFiles ensures any data with commas is properly
 * escaped prior to logging.  It's intended to help cleanup
 * machine logs for better parsing.
 * @author Milton Smith
 */
public class XRCSVEncoder extends LayoutWrappingEncoder {

    private static final char CSV_DELIMITER = ',';
    private static Charset charset = Charset.forName("UTF-8");

    public XRCSVEncoder() {
        super();
    }

    public byte[] encode(ILoggingEvent event) {
        String formattedMessage = Arrays.toString(super.encode(event));
        return (toCsv(formattedMessage, toJson(event, event.getMDCPropertyMap()), event) + "\n").getBytes(charset);
    }

    private String toCsv(String formattedMessage, JsonObject node, ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append(node.get("timestamp").getAsLong()).append(CSV_DELIMITER);
        builder.append(node.get("level").getAsString()).append(CSV_DELIMITER);
        builder.append(node.get("thread").getAsString()).append(CSV_DELIMITER);
        builder.append(node.get("logger").getAsString()).append(CSV_DELIMITER);
        builder.append(formattedMessage.replace(",", "\\,")).append(CSV_DELIMITER); // use formattedMessage here
        if (node.has("exception")) {
            builder.append(node.get("exception").getAsString().replace(",", "\\,")).append(CSV_DELIMITER);
        } else {
            builder.append(CSV_DELIMITER);
        }
        // add MDC values to CSV
        for (Map.Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            builder.append(value.replace(",", "\\,")).append(CSV_DELIMITER);
        }
        builder.setLength(builder.length() - 1); // remove last delimiter
        return builder.toString();
    }

    private JsonObject toJson(ILoggingEvent event, Map<String, String> mdcProperties) {
        JsonObject node = new JsonObject();
        node.addProperty("timestamp", event.getTimeStamp());
        node.addProperty("level", event.getLevel().toString());
        node.addProperty("thread", event.getThreadName());
        node.addProperty("logger", event.getLoggerName());
        node.addProperty("message", event.getFormattedMessage());
        if (event.getThrowableProxy() != null) {
            node.addProperty("exception", event.getThrowableProxy().getMessage());
        }
        if (mdcProperties != null) {
            for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                node.addProperty(entry.getKey(), entry.getValue());
            }
        }
        return node;
    }


}
