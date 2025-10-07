package org.jvmxray.service.ai.processor;

import org.jvmxray.platform.shared.property.PropertyBase;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for stage processors in the JVMXRay data pipeline.
 * Each processor is responsible for moving and enriching data between stages.
 *
 * <p>Data Pipeline Stages:</p>
 * <ul>
 *   <li>STAGE0: Raw events written by agents</li>
 *   <li>STAGE1: Parsed events with normalized keypairs</li>
 *   <li>STAGE2: Enriched data with intelligence (CVE analysis, metadata, etc.)</li>
 * </ul>
 *
 * @since 0.0.1
 * @author JVMXRay AI Service Team
 */
public interface StageProcessor {

    /**
     * Gets the human-readable name of this processor.
     *
     * @return Processor name (e.g., "Stage0Processor", "Stage1Processor")
     */
    String getProcessorName();

    /**
     * Processes a batch of records from the source stage to the destination stage.
     *
     * @param connection Database connection (may be in transaction)
     * @param batchSize Maximum number of records to process in this batch
     * @return Number of records successfully processed
     * @throws SQLException if database operation fails
     */
    int processBatch(Connection connection, int batchSize) throws SQLException;

    /**
     * Initializes the processor with configuration properties.
     * Called once during AI service startup.
     *
     * @param properties Component properties for configuration
     * @throws Exception if initialization fails
     */
    void initialize(PropertyBase properties) throws Exception;

    /**
     * Checks if this processor is enabled in configuration.
     *
     * @return true if processor should be executed, false to skip
     */
    boolean isEnabled();
}
