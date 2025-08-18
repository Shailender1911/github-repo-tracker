package github.repo.tracker.service;

import github.repo.tracker.client.GitHubApiClient;
import github.repo.tracker.config.GitHubProperties;
import github.repo.tracker.exception.GitHubApiException;
import github.repo.tracker.model.GitHubCommit;
import github.repo.tracker.model.GitHubRepository;
import github.repo.tracker.model.GitHubUser;
import github.repo.tracker.model.RepositoryActivityResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for managing GitHub repository activity operations
 */
@Service
public class GitHubRepositoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubRepositoryService.class);
    
    private final GitHubApiClient gitHubApiClient;
    private final GitHubProperties gitHubProperties;
    private final ExecutorService executorService;
    
    @Autowired
    public GitHubRepositoryService(GitHubApiClient gitHubApiClient, GitHubProperties gitHubProperties) {
        this.gitHubApiClient = gitHubApiClient;
        this.gitHubProperties = gitHubProperties;
        this.executorService = Executors.newFixedThreadPool(5); // Thread pool for parallel commit fetching
    }
    
    /**
     * Get repository activity for a GitHub user or organization
     */
    public RepositoryActivityResponse getRepositoryActivity(String username, Integer page, Integer perPage) {
        logger.info("Fetching repository activity for user: {}", username);
        
        try {
            // Validate and set defaults
            int currentPage = page != null && page > 0 ? page : 1;
            int pageSize = perPage != null && perPage > 0 ? 
                Math.min(perPage, gitHubProperties.getMaxRepositoriesPerPage()) : 
                gitHubProperties.getMaxRepositoriesPerPage();
            
            // Get user information to determine if it's a user or organization
            GitHubUser user = gitHubApiClient.getUser(username);
            boolean isOrganization = "Organization".equalsIgnoreCase(user.getType());
            
            // Fetch repositories
            List<GitHubRepository> repositories = isOrganization ?
                gitHubApiClient.getOrganizationRepositories(username, currentPage, pageSize) :
                gitHubApiClient.getUserRepositories(username, currentPage, pageSize);
            
            if (repositories.isEmpty()) {
                return createEmptyResponse(username, user.getType(), currentPage);
            }
            
            // Fetch commits for each repository in parallel
            List<GitHubRepository> repositoriesWithCommits = fetchCommitsInParallel(repositories);
            
            // Calculate pagination information
            boolean hasMore = repositories.size() == pageSize;
            
            return RepositoryActivityResponse.builder()
                    .username(username)
                    .userType(user.getType())
                    .totalRepositories(user.getPublicRepos())
                    .repositoriesProcessed(repositoriesWithCommits.size())
                    .repositories(repositoriesWithCommits)
                    .fetchedAt(OffsetDateTime.now())
                    .message(String.format("Successfully fetched %d repositories with recent commits", 
                           repositoriesWithCommits.size()))
                    .hasMore(hasMore)
                    .currentPage(currentPage)
                    .totalPages(calculateTotalPages(user.getPublicRepos(), pageSize))
                    .build();
                    
        } catch (GitHubApiException e) {
            logger.error("GitHub API error while fetching repository activity for user: {}", username, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching repository activity for user: {}", username, e);
            throw new GitHubApiException("Failed to fetch repository activity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get detailed information for a specific repository including commits
     */
    public GitHubRepository getRepositoryDetails(String owner, String repoName) {
        logger.info("Fetching detailed information for repository: {}/{}", owner, repoName);
        
        try {
            // For now, we'll create a basic repository object and fetch commits
            // In a real implementation, you might want to call the repository API endpoint
            GitHubRepository repository = new GitHubRepository();
            repository.setName(repoName);
            repository.setFullName(owner + "/" + repoName);
            
            // Fetch recent commits
            List<GitHubCommit> commits = gitHubApiClient.getRepositoryCommits(
                owner, repoName, gitHubProperties.getMaxCommitsPerRepo());
            
            repository.setRecentCommits(commits);
            
            return repository;
            
        } catch (Exception e) {
            logger.error("Error fetching repository details for {}/{}", owner, repoName, e);
            throw new GitHubApiException("Failed to fetch repository details: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetch commits for repositories in parallel to improve performance
     */
    private List<GitHubRepository> fetchCommitsInParallel(List<GitHubRepository> repositories) {
        logger.debug("Fetching commits for {} repositories in parallel", repositories.size());
        
        List<CompletableFuture<GitHubRepository>> futures = repositories.stream()
                .map(repo -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String owner = repo.getOwner().getLogin();
                        String repoName = repo.getName();
                        
                        logger.debug("Fetching commits for repository: {}/{}", owner, repoName);
                        
                        List<GitHubCommit> commits = gitHubApiClient.getRepositoryCommits(
                            owner, repoName, gitHubProperties.getMaxCommitsPerRepo());
                        
                        repo.setRecentCommits(commits);
                        
                        logger.debug("Successfully fetched {} commits for repository: {}/{}", 
                                   commits.size(), owner, repoName);
                        
                        return repo;
                    } catch (Exception e) {
                        logger.warn("Failed to fetch commits for repository: {}/{} - {}", 
                                  repo.getOwner().getLogin(), repo.getName(), e.getMessage());
                        
                        // Return repository without commits rather than failing the entire operation
                        repo.setRecentCommits(new ArrayList<>());
                        return repo;
                    }
                }, executorService))
                .collect(Collectors.toList());
        
        // Wait for all futures to complete and collect results
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
    
    /**
     * Create an empty response when no repositories are found
     */
    private RepositoryActivityResponse createEmptyResponse(String username, String userType, int currentPage) {
        return RepositoryActivityResponse.builder()
                .username(username)
                .userType(userType)
                .totalRepositories(0)
                .repositoriesProcessed(0)
                .repositories(new ArrayList<>())
                .fetchedAt(OffsetDateTime.now())
                .message("No repositories found for user: " + username)
                .hasMore(false)
                .currentPage(currentPage)
                .totalPages(0)
                .build();
    }
    
    /**
     * Calculate total pages for pagination
     */
    private Integer calculateTotalPages(Integer totalRepos, int pageSize) {
        if (totalRepos == null || totalRepos == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalRepos / pageSize);
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
} 