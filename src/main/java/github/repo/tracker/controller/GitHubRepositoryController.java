package github.repo.tracker.controller;

import github.repo.tracker.model.ApiErrorResponse;
import github.repo.tracker.model.GitHubRepository;
import github.repo.tracker.model.RepositoryActivityResponse;
import github.repo.tracker.service.GitHubRepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * REST Controller for GitHub Repository Activity endpoints
 */
@RestController
@RequestMapping("/v1/repositories")
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
public class GitHubRepositoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubRepositoryController.class);
    
    private final GitHubRepositoryService gitHubRepositoryService;
    
    @Autowired
    public GitHubRepositoryController(GitHubRepositoryService gitHubRepositoryService) {
        this.gitHubRepositoryService = gitHubRepositoryService;
    }
    
    /**
     * Get repository activity for a GitHub user or organization
     * 
     * @param username GitHub username or organization name
     * @param page Page number for pagination (default: 1)
     * @param perPage Number of repositories per page (default: 30, max: 100)
     * @return Repository activity response with repositories and their recent commits
     */
    @GetMapping("/activity/{username}")
    public ResponseEntity<RepositoryActivityResponse> getRepositoryActivity(
            @PathVariable 
            @NotBlank(message = "Username cannot be blank")
            @Size(min = 1, max = 39, message = "Username must be between 1 and 39 characters")
            @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9]|-(?=[a-zA-Z0-9]))*$", 
                    message = "Username contains invalid characters")
            String username,
            
            @RequestParam(defaultValue = "1") 
            @Min(value = 1, message = "Page must be greater than 0")
            Integer page,
            
            @RequestParam(defaultValue = "30") 
            @Min(value = 1, message = "Per page must be greater than 0")
            Integer perPage) {
        
        logger.info("Received request to get repository activity for user: {} (page: {}, perPage: {})", 
                   username, page, perPage);
        
        try {
            RepositoryActivityResponse response = gitHubRepositoryService.getRepositoryActivity(username, page, perPage);
            
            logger.info("Successfully processed repository activity request for user: {} - returned {} repositories", 
                       username, response.getRepositoriesProcessed());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing repository activity request for user: {}", username, e);
            throw e; // Let the global exception handler deal with it
        }
    }
    
    /**
     * Get detailed information for a specific repository including recent commits
     * 
     * @param owner Repository owner (username or organization)
     * @param repo Repository name
     * @return Repository details with recent commits
     */
    @GetMapping("/details/{owner}/{repo}")
    public ResponseEntity<GitHubRepository> getRepositoryDetails(
            @PathVariable 
            @NotBlank(message = "Owner cannot be blank")
            @Size(min = 1, max = 39, message = "Owner must be between 1 and 39 characters")
            @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9]|-(?=[a-zA-Z0-9]))*$", 
                    message = "Owner contains invalid characters")
            String owner,
            
            @PathVariable 
            @NotBlank(message = "Repository name cannot be blank")
            @Size(min = 1, max = 100, message = "Repository name must be between 1 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", 
                    message = "Repository name contains invalid characters")
            String repo) {
        
        logger.info("Received request to get repository details for: {}/{}", owner, repo);
        
        try {
            GitHubRepository repository = gitHubRepositoryService.getRepositoryDetails(owner, repo);
            
            logger.info("Successfully processed repository details request for: {}/{} - returned {} commits", 
                       owner, repo, repository.getRecentCommits().size());
            
            return ResponseEntity.ok(repository);
            
        } catch (Exception e) {
            logger.error("Error processing repository details request for: {}/{}", owner, repo, e);
            throw e; // Let the global exception handler deal with it
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        return ResponseEntity.ok()
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", "UP");
                    put("timestamp", OffsetDateTime.now());
                    put("service", "GitHub Repository Activity Tracker");
                    put("version", "1.0.0");
                }});
    }
    
    /**
     * API information endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<Object> info() {
        return ResponseEntity.ok()
                .body(new java.util.HashMap<String, Object>() {{
                    put("service", "GitHub Repository Activity Tracker");
                    put("description", "Fetch GitHub repository activity including recent commits for users and organizations");
                    put("version", "1.0.0");
                    put("endpoints", new java.util.HashMap<String, String>() {{
                        put("GET /api/v1/repositories/activity/{username}", "Get repository activity for a user/organization");
                        put("GET /api/v1/repositories/details/{owner}/{repo}", "Get detailed repository information");
                        put("GET /api/v1/repositories/health", "Health check endpoint");
                        put("GET /api/v1/repositories/info", "API information");
                    }});
                    put("parameters", new java.util.HashMap<String, Object>() {{
                        put("page", "Page number for pagination (default: 1)");
                        put("perPage", "Number of repositories per page (default: 30, max: 100)");
                    }});
                    put("timestamp", OffsetDateTime.now());
                }});
    }
} 