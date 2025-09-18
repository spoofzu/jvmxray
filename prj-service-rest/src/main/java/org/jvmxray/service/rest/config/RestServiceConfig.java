package org.jvmxray.service.rest.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.jvmxray.service.rest.filter.ApiKeyAuthenticationFilter;

import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Spring Boot configuration for the REST Service.
 * Configures database connections, CORS, and filters.
 *
 * @author Milton Smith
 */
@Configuration
public class RestServiceConfig implements WebMvcConfigurer {

    private static final Logger logger = Logger.getLogger(RestServiceConfig.class.getName());

    @Value("${rest.service.database.url:jdbc:sqlite:.jvmxray/common/data/jvmxray-test.db}")
    private String databaseUrl;

    @Value("${rest.service.max.page.size:1000}")
    private int maxPageSize;

    @Value("${rest.service.default.page.size:100}")
    private int defaultPageSize;

    @Value("${rest.service.max.result.size:100000}")
    private int maxResultSize;

    /**
     * Configure HikariCP connection pool for SQLite database.
     *
     * @return DataSource configured for SQLite
     */
    @Bean
    public DataSource dataSource() {
        // Ensure SQLite driver is registered
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }


        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseUrl);
        config.setMaximumPoolSize(1); // SQLite works best with single connection
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("RestService-Pool");

        // SQLite-specific settings for better concurrent access
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "30000");
        config.addDataSourceProperty("cache_size", "-2000"); // 2MB cache

        logger.info("Configured database connection pool for: " + databaseUrl);
        return new HikariDataSource(config);
    }

    /**
     * Configure API key authentication filter.
     *
     * @param dataSource Database connection pool
     * @return Filter registration bean for API key authentication
     */
    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyFilter(DataSource dataSource) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter();
        filter.setDataSource(dataSource);

        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);

        logger.info("Configured API key authentication filter");
        return registrationBean;
    }

    /**
     * Configure CORS to allow cross-origin requests.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Total-Count", "X-Page-Number", "X-Page-Size")
                .maxAge(3600);

        logger.info("Configured CORS mappings");
    }

    /**
     * Get the maximum page size for paginated queries.
     *
     * @return Maximum page size
     */
    public int getMaxPageSize() {
        return maxPageSize;
    }

    /**
     * Get the default page size for paginated queries.
     *
     * @return Default page size
     */
    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    /**
     * Get the maximum result size for queries.
     *
     * @return Maximum result size
     */
    public int getMaxResultSize() {
        return maxResultSize;
    }
}