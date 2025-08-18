package github.repo.tracker.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for Repository Activity API endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryActivityResponse {
    
    private String username;
    
    private String userType; // "User" or "Organization"
    
    private Integer totalRepositories;
    
    private Integer repositoriesProcessed;
    
    private List<GitHubRepository> repositories;
    
    private OffsetDateTime fetchedAt;
    
    private String message;
    
    private Boolean hasMore; // Indicates if there are more repositories to fetch
    
    private Integer currentPage;
    
    private Integer totalPages;
} 