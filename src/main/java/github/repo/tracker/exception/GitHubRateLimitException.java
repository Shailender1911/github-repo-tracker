package github.repo.tracker.exception;

import java.time.OffsetDateTime;

/**
 * Custom exception for GitHub API rate limiting
 */
public class GitHubRateLimitException extends GitHubApiException {
    
    private final Integer rateLimitRemaining;
    private final OffsetDateTime rateLimitReset;
    
    public GitHubRateLimitException(String message, Integer rateLimitRemaining, OffsetDateTime rateLimitReset) {
        super(message);
        this.rateLimitRemaining = rateLimitRemaining;
        this.rateLimitReset = rateLimitReset;
    }
    
    public GitHubRateLimitException(String message, Integer statusCode, String responseBody, 
                                   Integer rateLimitRemaining, OffsetDateTime rateLimitReset) {
        super(message, statusCode, responseBody);
        this.rateLimitRemaining = rateLimitRemaining;
        this.rateLimitReset = rateLimitReset;
    }
    
    public Integer getRateLimitRemaining() {
        return rateLimitRemaining;
    }
    
    public OffsetDateTime getRateLimitReset() {
        return rateLimitReset;
    }
} 