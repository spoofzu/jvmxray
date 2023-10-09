package org.jvmxray.graphql.app;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jvmxray.graphql.controller.JVMXRayCassandraConnectionFactory;
import org.jvmxray.graphql.resolver.EventResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "JVMXRayGraphQLServer", value = "/graphql/*")
public class JVMXRayGraphQLServer extends HttpServlet {

    private static Logger logger = LoggerFactory.getLogger("org.jvmxray.graphql.app.JVMXRayGraphQLServer");
    private GraphQL graphQL;
    private CqlSession session;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            initCassandra();
            initGraphQL();
        } catch(Throwable t) {
            logger.error("Server init() error. emsg="+t.getMessage(),t);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        session.close();
    }

    private void initCassandra() throws IOException {
        JVMXRayCassandraConnectionFactory controller = JVMXRayCassandraConnectionFactory.getInstance();
        String basePath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String fqfn = basePath + "app.properties";
        Properties appProps = new Properties();
        appProps.load(new FileInputStream(fqfn));
        String userid = appProps.getProperty("userid");
        //TODO: password in clear. secure file or encrypt field.
        String password = appProps.getProperty("password");
        String node = appProps.getProperty("node");
        String sport = appProps.getProperty("port");
        int port = Integer.parseInt(sport);
        String datacenter = appProps.getProperty("datacenter");
        controller.setConnectionMeta(userid,password, node,port,datacenter);
        controller.connect();
        session = controller.getSession();

    }

    private void initGraphQL() {
        // Schema
        SchemaParser schemaParser = new SchemaParser();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(getClass().getClassLoader().getResourceAsStream("schema.graphql"));

        // Wiring and Data Fetchers
        RuntimeWiring runtimeWiring = buildWiring();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        // GraphQL Setup
        graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private RuntimeWiring buildWiring() {
        EventResolver eventResolver = new EventResolver(session);

        return RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("eventByEventid", eventResolver::eventByEventid)
                        .dataFetcher("eventsByEventtp", eventResolver::eventsByEventtp)
                        .dataFetcher("eventsByLoggernamespace", eventResolver::eventsByLoggernamespace)
                        .dataFetcher("eventsByCategory", eventResolver::eventsByCategory)
                        .dataFetcher("eventsByP1", eventResolver::eventsByP1)
                        .dataFetcher("eventsByP2", eventResolver::eventsByP2)
                        .dataFetcher("eventsByP3", eventResolver::eventsByP3)
                        .dataFetcher("eventMetaByEventidandLevel", eventResolver::eventMetaByEventidandLevel)
                        .dataFetcher("eventsMetaByEventid", eventResolver::eventsMetaByEventid)
                        .dataFetcher("eventsMetaByClzldr", eventResolver::eventsMetaByClzldr)
                        .dataFetcher("eventsMetaByClzcn", eventResolver::eventsMetaByClzcn)
                        .dataFetcher("eventsMetaByClzmethnm", eventResolver::eventsMetaByClzmethnm)
                        .dataFetcher("eventsMetaByClzmodnm", eventResolver::eventsMetaByClzmodnm)
                        .dataFetcher("eventsMetaByClzmodvr", eventResolver::eventsMetaByClzmodvr)
                        .dataFetcher("eventsMetaByClzfilenm", eventResolver::eventsMetaByClzfilenm)
                        .dataFetcher("eventsMetaByClzlocation", eventResolver::eventsMetaByClzlocation)
                )
                .scalar(ExtendedScalars.GraphQLLong)
                .build();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String contenttype = "application/json";
        String pathinfo = request.getPathInfo();
        String msg = "";
        int responseCode = 500;
        try {
            switch (pathinfo) {
                case "/":
                    msg = handleGraphQLRequest(request);
                    responseCode = msg.startsWith("{\"error") ? 404 : 200;
                    break;
                default:
                    msg = "{\"errors\": [{\"message\": \"Client request error.\"}]}";
                    responseCode = 404;
                    break;
            }
            response.setContentType(contenttype);
            response.setStatus(responseCode);
            PrintWriter out = response.getWriter();
            out.write(msg);
            out.flush();
        } catch(Throwable t) {
            logger.error("Server service() error. emsg="+t.getMessage()+" responseCode="+responseCode,t);
        }
    }

    private String handleGraphQLRequest(HttpServletRequest request) {
        String requestContentType = request.getContentType();
        String graphqlQuery = null;

        try {
            if ("application/graphql".equals(requestContentType)) {
                // Handle raw GraphQL query
                graphqlQuery = request.getReader().lines().collect(Collectors.joining());
            } else if ("application/json".equals(requestContentType)) {
                // Extract GraphQL query from JSON
                String requestBody = request.getReader().lines().collect(Collectors.joining());
                // Use a JSON parser to extract the 'query' field
                // Assuming you use Jackson for JSON parsing:
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(requestBody);
                graphqlQuery = rootNode.path("query").asText();
            } else {
                return "{\"errors\": [{\"message\": \"Unsupported content type: " + requestContentType + "\"}]}";
            }
        } catch (IOException e) {
            logger.error("Failed to read the request.", e);
            return "{\"errors\": [{\"message\": \"Failed to read the request\"}]}";
        }

        logger.debug("GraphQL query=" + graphqlQuery);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(graphqlQuery)
                .build();
        Map<String, Object> result = graphQL.execute(executionInput).toSpecification();
        return convertMapToJson(result);
    }


    private String convertMapToJson(Map<String, Object> map) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            logger.error("JSON decoding error.", e);
            return "{}";  // Return empty JSON in case of an error
        }
    }
}
