package github.repo.tracker.exception;

/**
 * Custom exception for GitHub API related errors
 */
public class GitHubApiException extends RuntimeException {
    
    private final Integer statusCode;
    private final String responseBody;
    
    public GitHubApiException(String message) {
        super(message);
        this.statusCode = null;
        this.responseBody = null;
    }
    
    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.responseBody = null;
    }
    
    public GitHubApiException(String message, Integer statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public GitHubApiException(String message, Integer statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public String getResponseBody() {
        return responseBody;
    }
} 