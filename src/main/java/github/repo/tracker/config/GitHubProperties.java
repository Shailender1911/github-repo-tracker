package github.repo.tracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Configuration properties for GitHub API
 */
@Data
@Component
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {
    
    /**
     * GitHub API base URL
     */
    @NotBlank
    private String apiUrl = "https://api.github.com";
    
    /**
     * GitHub personal access token for authentication
     */
    @NotBlank
    private String token;
    
    /**
     * Connection timeout in milliseconds
     */
    @NotNull
    @Min(1000)
    private Integer connectionTimeoutMs = 10000;
    
    /**
     * Read timeout in milliseconds
     */
    @NotNull
    @Min(1000)
    private Integer readTimeoutMs = 30000;
    
    /**
     * Maximum number of repositories to fetch per page
     */
    @NotNull
    @Min(1)
    private Integer maxRepositoriesPerPage = 30;
    
    /**
     * Maximum number of commits to fetch per repository
     */
    @NotNull
    @Min(1)
    private Integer maxCommitsPerRepo = 20;
    
    /**
     * Maximum number of retries for rate-limited requests
     */
    @NotNull
    @Min(0)
    private Integer maxRetries = 3;
    
    /**
     * Delay between retries in milliseconds
     */
    @NotNull
    @Min(1000)
    private Long retryDelayMs = 2000L;
    
    /**
     * Enable/disable request logging
     */
    private Boolean enableRequestLogging = false;
} 