package org.jvmxray.collector;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Based upon code examples in part at "Secure your Servlet app by validating
 * incoming Twilio requests" by Twilio.
 * https://www.twilio.com/docs/usage/tutorials/how-to-secure-your-servlet-app-by-validating-incoming-twilio-requests
 */
public class JVMXRayRequestValidatorFilter /* implements Filter */ {

// TODOMS: explore more once we move to api key.
//
//    @Override
//    public void init(FilterConfig filterConfig) throws ServletException {
//    }
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//
//        boolean isValidRequest = false;
//        if (request instanceof HttpServletRequest) {
//            HttpServletRequest httpRequest = (HttpServletRequest) request;
//
//            // Concatenates the request URL with the query string
//            String pathAndQueryUrl = getRequestUrlAndQueryString(httpRequest);
//            // Extracts only the POST parameters and converts the parameters Map type
//            Map<String, String> postParams = extractPostParams(httpRequest);
//            String signatureHeader = httpRequest.getHeader("X-JVMXRay-Signature");
//
//            isValidRequest = requestValidator.validate(
//                    pathAndQueryUrl,
//                    postParams,
//                    signatureHeader);
//        }
//
//        if(isValidRequest || environmentIsTest()) {
//            chain.doFilter(request, response);
//        } else {
//            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN);
//        }
//
//    }
//
//    @Override
//    public void destroy() {
//    }
//
//    private boolean environmentIsTest() {
//        return "test".equals(currentEnvironment);
//    }
//
//    private List<String> getQueryStringKeys(String queryString) {
//        if(queryString == null || queryString.length() == 0) {
//            return Collections.emptyList();
//        } else {
//            return Arrays.stream(queryString.split("&"))
//                    .map(pair -> pair.split("=")[0])
//                    .collect(Collectors.toList());
//        }
//    }
//
//    private String getRequestUrlAndQueryString(HttpServletRequest request) {
//        String queryString = request.getQueryString();
//        String requestUrl = request.getRequestURL().toString();
//        if(queryString != null && queryString != "") {
//            return requestUrl + "?" + queryString;
//        }
//        return requestUrl;
//    }


}
