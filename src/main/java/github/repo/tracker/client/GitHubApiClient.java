package github.repo.tracker.client;

import github.repo.tracker.config.GitHubProperties;
import github.repo.tracker.exception.GitHubApiException;
import github.repo.tracker.exception.GitHubRateLimitException;
import github.repo.tracker.model.GitHubCommit;
import github.repo.tracker.model.GitHubRepository;
import github.repo.tracker.model.GitHubUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * GitHub API Client for handling all GitHub API interactions
 */
@Component
public class GitHubApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubApiClient.class);
    
    private static final String USER_REPOS_ENDPOINT = "/users/{username}/repos";
    private static final String ORG_REPOS_ENDPOINT = "/orgs/{orgname}/repos";
    private static final String REPO_COMMITS_ENDPOINT = "/repos/{owner}/{repo}/commits";
    private static final String USER_ENDPOINT = "/users/{username}";
    
    private final RestTemplate restTemplate;
    private final GitHubProperties gitHubProperties;
    
    @Autowired
    public GitHubApiClient(RestTemplate restTemplate, GitHubProperties gitHubProperties) {
        this.restTemplate = restTemplate;
        this.gitHubProperties = gitHubProperties;
    }
    
    /**
     * Get user information
     */
    public GitHubUser getUser(String username) {
        String url = gitHubProperties.getApiUrl() + USER_ENDPOINT;
        
        try {
            ResponseEntity<GitHubUser> response = executeWithRetry(() -> 
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    createHttpEntity(),
                    GitHubUser.class,
                    username
                )
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new GitHubApiException("User not found: " + username, e.getRawStatusCode(), e.getResponseBodyAsString());
            }
            throw handleHttpError(e);
        }
    }
    
    /**
     * Get repositories for a user
     */
    public List<GitHubRepository> getUserRepositories(String username, int page, int perPage) {
        String url = gitHubProperties.getApiUrl() + USER_REPOS_ENDPOINT;
        return getRepositories(url, username, page, perPage);
    }
    
    /**
     * Get repositories for an organization
     */
    public List<GitHubRepository> getOrganizationRepositories(String orgname, int page, int perPage) {
        String url = gitHubProperties.getApiUrl() + ORG_REPOS_ENDPOINT;
        return getRepositories(url, orgname, page, perPage);
    }
    
    /**
     * Get commits for a specific repository
     */
    public List<GitHubCommit> getRepositoryCommits(String owner, String repo, int perPage) {
        String url = UriComponentsBuilder
                .fromHttpUrl(gitHubProperties.getApiUrl() + REPO_COMMITS_ENDPOINT)
                .queryParam("per_page", Math.min(perPage, gitHubProperties.getMaxCommitsPerRepo()))
                .queryParam("page", 1)
                .build()
                .toUriString();
        
        try {
            ResponseEntity<List<GitHubCommit>> response = executeWithRetry(() ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    createHttpEntity(),
                    new ParameterizedTypeReference<List<GitHubCommit>>() {},
                    owner,
                    repo
                )
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.warn("Repository {}/{} not found or no commits available", owner, repo);
                return Collections.emptyList();
            }
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.warn("Access forbidden to repository {}/{}", owner, repo);
                return Collections.emptyList();
            }
            throw handleHttpError(e);
        }
    }
    
    /**
     * Generic method to get repositories with pagination
     */
    private List<GitHubRepository> getRepositories(String urlTemplate, String identifier, int page, int perPage) {
        String url = UriComponentsBuilder
                .fromHttpUrl(urlTemplate)
                .queryParam("type", "all")
                .queryParam("sort", "updated")
                .queryParam("direction", "desc")
                .queryParam("per_page", Math.min(perPage, gitHubProperties.getMaxRepositoriesPerPage()))
                .queryParam("page", page)
                .build()
                .toUriString();
        
        try {
            ResponseEntity<List<GitHubRepository>> response = executeWithRetry(() ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    createHttpEntity(),
                    new ParameterizedTypeReference<List<GitHubRepository>>() {},
                    identifier
                )
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new GitHubApiException("User/Organization not found: " + identifier, e.getRawStatusCode(), e.getResponseBodyAsString());
            }
            throw handleHttpError(e);
        }
    }
    
    /**
     * Create HTTP entity with authentication headers
     */
    private HttpEntity<?> createHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "token " + gitHubProperties.getToken());
        headers.set("User-Agent", "GitHub-Repo-Tracker/1.0");
        
        return new HttpEntity<>(headers);
    }
    
    /**
     * Execute request with retry logic for rate limiting
     */
    private <T> ResponseEntity<T> executeWithRetry(java.util.function.Supplier<ResponseEntity<T>> requestSupplier) {
        int retryCount = 0;
        
        while (retryCount <= gitHubProperties.getMaxRetries()) {
            try {
                ResponseEntity<T> response = requestSupplier.get();
                logRateLimitInfo(response.getHeaders());
                return response;
                
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.FORBIDDEN && isRateLimited(e)) {
                    if (retryCount < gitHubProperties.getMaxRetries()) {
                        retryCount++;
                        logger.warn("Rate limit exceeded. Retrying in {} ms... (attempt {}/{})", 
                                   gitHubProperties.getRetryDelayMs(), retryCount, gitHubProperties.getMaxRetries());
                        
                        try {
                            Thread.sleep(gitHubProperties.getRetryDelayMs());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new GitHubApiException("Request interrupted during rate limit retry", ie);
                        }
                        continue;
                    } else {
                        throw createRateLimitException(e);
                    }
                }
                throw e;
            }
        }
        
        throw new GitHubApiException("Max retries exceeded");
    }
    
    /**
     * Check if the error is due to rate limiting
     */
    private boolean isRateLimited(HttpClientErrorException e) {
        String responseBody = e.getResponseBodyAsString();
        return responseBody.contains("rate limit exceeded") || 
               responseBody.contains("API rate limit exceeded");
    }
    
    /**
     * Create rate limit exception with details
     */
    private GitHubRateLimitException createRateLimitException(HttpClientErrorException e) {
        String rateLimitRemaining = e.getResponseHeaders().getFirst("X-RateLimit-Remaining");
        String rateLimitReset = e.getResponseHeaders().getFirst("X-RateLimit-Reset");
        
        Integer remaining = rateLimitRemaining != null ? Integer.parseInt(rateLimitRemaining) : null;
        OffsetDateTime resetTime = rateLimitReset != null ? 
            OffsetDateTime.parse(rateLimitReset, DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
        
        return new GitHubRateLimitException(
            "GitHub API rate limit exceeded",
            e.getRawStatusCode(),
            e.getResponseBodyAsString(),
            remaining,
            resetTime
        );
    }
    
    /**
     * Handle HTTP errors and convert to appropriate exceptions
     */
    private GitHubApiException handleHttpError(HttpClientErrorException e) {
        logger.error("GitHub API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        
        return new GitHubApiException(
            "GitHub API request failed: " + e.getMessage(),
            e.getRawStatusCode(),
            e.getResponseBodyAsString(),
            e
        );
    }
    
    /**
     * Log rate limit information for monitoring
     */
    private void logRateLimitInfo(HttpHeaders headers) {
        String rateLimitRemaining = headers.getFirst("X-RateLimit-Remaining");
        String rateLimitReset = headers.getFirst("X-RateLimit-Reset");
        
        if (rateLimitRemaining != null) {
            logger.debug("Rate limit remaining: {} (resets at: {})", rateLimitRemaining, rateLimitReset);
        }
    }
} 