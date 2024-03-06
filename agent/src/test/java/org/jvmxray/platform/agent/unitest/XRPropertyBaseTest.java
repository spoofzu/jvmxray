package org.jvmxray.platform.agent.unitest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvmxray.platform.shared.property.XRPropertyFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class XRPropertyBaseTest {

    private static final String PROPERTYFILE_HEADER = "JVMXRay UnitTest Properties";
    private XRUnitTestProperties propertyTest;
    private File tempDir;

    public static void main(String[] args) {
        XRPropertyBaseTest test = new XRPropertyBaseTest();
        try {
            // Initialize resources & call each test.
            test.setUp();
            test.testSetAndGetProperties();
            test.testIsModifiedAfterSetProperty();
            test.testSaveProperties();
            // Clean up resources
            test.tearDown();
            System.out.println("All tests finished successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Test failed: " + e.getMessage());
        }
    }

    @Before
    public void setUp() throws Exception {
        // Create a temporary directory for testing
        tempDir = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!tempDir.delete()) {
            throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath());
        }
        if (!tempDir.mkdir()) {
            throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
        }
        // Required to setup fake jvmxray home and logging.
        System.setProperty("jvmxray.base",tempDir.getAbsolutePath());
        XRPropertyFactory.init();
        // Testup the properties test.
        propertyTest = new XRUnitTestProperties(tempDir.toPath());
        propertyTest.init();
    }

    @After
    public void tearDown() {
        // Cleanup the temporary directory
        tempDir.delete();
    }

    @Test
    public void testSetAndGetProperties() {
        String testKey = "testKey";
        String testValue = "testValue";
        propertyTest.setProperty(testKey, testValue);
        assertEquals(testValue, propertyTest.getProperty(testKey));
    }

    @Test
    public void testSetAndGetIntProperties() {
        String testKey = "testKey";
        int testValue = 1;
        propertyTest.setIntProperty(testKey,testValue);
        assertEquals(testValue,propertyTest.getIntProperty(testKey));
    }

    @Test
    public void testSetAndGetLongProperties() {
        String testKey = "testKey";
        long testValue = 1;
        propertyTest.setLongProperty(testKey,testValue);
        assertEquals(testValue,propertyTest.getLongProperty(testKey));
    }

    @Test
    public void testIsModifiedAfterSetProperty() throws IOException {
        String testKey = "anotherTestKey";
        String testValue = "anotherTestValue";
        // Make sure property file is saved to disk or isModified()
        // returns true.
        propertyTest.saveProperties(PROPERTYFILE_HEADER);
        assertFalse(propertyTest.isModified());
        propertyTest.setProperty(testKey, testValue);
        assertTrue(propertyTest.isModified());
    }

    // Additional test methods for getIntProperty, getLongProperty, etc.

    @Test
    public void testSaveProperties() throws IOException {
        String testKey = "saveTestKey";
        String testValue = "saveTestValue";
        propertyTest.setProperty(testKey, testValue);
        propertyTest.saveProperties("JVMXRay UnitTest Properties");
        XRUnitTestProperties reloadedProperties = new XRUnitTestProperties(tempDir.toPath());
        reloadedProperties.init();
        assertEquals(testValue, reloadedProperties.getProperty(testKey));
    }
}

