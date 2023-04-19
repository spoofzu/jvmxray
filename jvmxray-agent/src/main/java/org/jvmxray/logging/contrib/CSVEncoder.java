package org.jvmxray.logging.contrib;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

/**
 * Encoder for CSVFiles ensures any data with commas is properly
 * escaped prior to logging.  It's intended to help cleanup
 * machine logs for better parsing.
 * @author Milton Smith
 */
public class CSVEncoder extends LayoutWrappingEncoder {

    private static final char CSV_DELIMITER = ',';
    private static Charset charset = Charset.forName("UTF-8");

    public CSVEncoder() {
        super();
    }

    public byte[] encode(ILoggingEvent event) {
        String formattedMessage = Arrays.toString(super.encode(event));
        return (toCsv(formattedMessage, toJson(event, event.getMDCPropertyMap()), event) + "\n").getBytes(charset);
    }

    private String toCsv(String formattedMessage, JsonNode node, ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append(node.get("timestamp").asLong()).append(CSV_DELIMITER);
        builder.append(node.get("level").asText()).append(CSV_DELIMITER);
        builder.append(node.get("thread").asText()).append(CSV_DELIMITER);
        builder.append(node.get("logger").asText()).append(CSV_DELIMITER);
        builder.append(formattedMessage.replace(",", "\\,")).append(CSV_DELIMITER); // use formattedMessage here
        if (node.has("exception")) {
            builder.append(node.get("exception").asText().replace(",", "\\,")).append(CSV_DELIMITER);
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

    private JsonNode toJson(ILoggingEvent event, Map<String, String> mdcProperties) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("timestamp", event.getTimeStamp());
        node.put("level", event.getLevel().toString());
        node.put("thread", event.getThreadName());
        node.put("logger", event.getLoggerName());
        node.put("message", event.getFormattedMessage());
        if (event.getThrowableProxy() != null) {
            node.put("exception", event.getThrowableProxy().getMessage());
        }
        if (mdcProperties != null) {
            for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                node.put(entry.getKey(), entry.getValue());
            }
        }
        return node;
    }


}
