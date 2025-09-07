package org.jvmxray.platform.client.mcp.bridge;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.InputStream;

/**
 * Unit tests for McpStdioToSocketBridge
 */
public class McpStdioToSocketBridgeTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @Before
    public void setUp() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testHelpOption() {
        // Test that help option works by creating a bridge instance and testing argument parsing
        McpStdioToSocketBridge bridge = new McpStdioToSocketBridge();
        
        // Use reflection to test the private parseArguments method
        try {
            java.lang.reflect.Method parseMethod = McpStdioToSocketBridge.class.getDeclaredMethod("parseArguments", String[].class);
            parseMethod.setAccessible(true);
            
            String[] args = {"--help"};
            Boolean result = (Boolean) parseMethod.invoke(bridge, (Object) args);
            
            // Help option should return false (indicating program should exit)
            assertFalse("Help option should return false", result);
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testMissingApiKey() {
        // Test that missing API key is handled properly
        McpStdioToSocketBridge bridge = new McpStdioToSocketBridge();
        
        try {
            java.lang.reflect.Method parseMethod = McpStdioToSocketBridge.class.getDeclaredMethod("parseArguments", String[].class);
            parseMethod.setAccessible(true);
            
            String[] args = {"--host", "localhost", "--port", "9000"};
            Boolean result = (Boolean) parseMethod.invoke(bridge, (Object) args);
            
            // Missing API key should return false (indicating program should exit)
            assertFalse("Missing API key should return false", result);
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidPort() {
        // Test that invalid port is handled properly
        McpStdioToSocketBridge bridge = new McpStdioToSocketBridge();
        
        try {
            java.lang.reflect.Method parseMethod = McpStdioToSocketBridge.class.getDeclaredMethod("parseArguments", String[].class);
            parseMethod.setAccessible(true);
            
            String[] args = {"--port", "invalid", "--api-key", "test-key"};
            Boolean result = (Boolean) parseMethod.invoke(bridge, (Object) args);
            
            // Invalid port should return false (indicating program should exit)
            assertFalse("Invalid port should return false", result);
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testValidArguments() {
        // Test that valid arguments are parsed correctly
        McpStdioToSocketBridge bridge = new McpStdioToSocketBridge();
        
        try {
            java.lang.reflect.Method parseMethod = McpStdioToSocketBridge.class.getDeclaredMethod("parseArguments", String[].class);
            parseMethod.setAccessible(true);
            
            String[] args = {"--host", "testhost", "--port", "8080", "--api-key", "test-key", "--debug", "/tmp/debug.log"};
            Boolean result = (Boolean) parseMethod.invoke(bridge, (Object) args);
            
            // Valid arguments should return true (indicating program should continue)
            assertTrue("Valid arguments should return true", result);
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}
