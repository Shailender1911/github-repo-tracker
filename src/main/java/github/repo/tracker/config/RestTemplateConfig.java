package github.repo.tracker.config;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate used in GitHub API calls
 */
@Configuration
public class RestTemplateConfig {
    
    private final GitHubProperties gitHubProperties;
    
    @Autowired
    public RestTemplateConfig(GitHubProperties gitHubProperties) {
        this.gitHubProperties = gitHubProperties;
    }
    
    @Bean
    public RestTemplate restTemplate() {
        HttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(20)
                .setMaxConnPerRoute(20)
                .build();
        
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(gitHubProperties.getConnectionTimeoutMs());
        factory.setReadTimeout(gitHubProperties.getReadTimeoutMs());
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Add interceptors if needed
        if (gitHubProperties.getEnableRequestLogging()) {
            restTemplate.getInterceptors().add(new LoggingInterceptor());
        }
        
        return restTemplate;
    }
    
    /**
     * Simple logging interceptor for debugging HTTP requests
     */
    private static class LoggingInterceptor implements org.springframework.http.client.ClientHttpRequestInterceptor {
        
        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request,
                byte[] body,
                org.springframework.http.client.ClientHttpRequestExecution execution) throws java.io.IOException {
            
            System.out.println("Request: " + request.getMethod() + " " + request.getURI());
            
            org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);
            
            System.out.println("Response: " + response.getStatusCode());
            
            return response;
        }
    }
} 