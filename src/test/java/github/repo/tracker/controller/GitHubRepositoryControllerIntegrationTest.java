package github.repo.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.repo.tracker.exception.GitHubApiException;
import github.repo.tracker.exception.GitHubRateLimitException;
import github.repo.tracker.model.GitHubCommit;
import github.repo.tracker.model.GitHubRepository;
import github.repo.tracker.model.GitHubUser;
import github.repo.tracker.model.RepositoryActivityResponse;
import github.repo.tracker.service.GitHubRepositoryService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GitHubRepositoryController
 */
@WebMvcTest(GitHubRepositoryController.class)
class GitHubRepositoryControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private GitHubRepositoryService gitHubRepositoryService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testGetRepositoryActivity_Success() throws Exception {
        // Given
        String username = "testuser";
        RepositoryActivityResponse mockResponse = createMockRepositoryActivityResponse(username);
        
        when(gitHubRepositoryService.getRepositoryActivity(eq(username), eq(1), eq(30)))
            .thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/v1/repositories/activity/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is(username)))
                .andExpect(jsonPath("$.userType", is("User")))
                .andExpect(jsonPath("$.totalRepositories", is(5)))
                .andExpect(jsonPath("$.repositoriesProcessed", is(2)))
                .andExpect(jsonPath("$.repositories", hasSize(2)))
                .andExpect(jsonPath("$.repositories[0].name", is("repo1")))
                .andExpect(jsonPath("$.repositories[0].recentCommits", hasSize(1)))
                .andExpect(jsonPath("$.hasMore", is(false)))
                .andExpect(jsonPath("$.currentPage", is(1)))
                .andExpect(jsonPath("$.message", containsString("Successfully fetched")));
    }
    
    @Test
    void testGetRepositoryActivity_WithPagination() throws Exception {
        // Given
        String username = "testuser";
        RepositoryActivityResponse mockResponse = createMockRepositoryActivityResponse(username);
        mockResponse.setCurrentPage(2);
        mockResponse.setHasMore(true);
        
        when(gitHubRepositoryService.getRepositoryActivity(eq(username), eq(2), eq(15)))
            .thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/v1/repositories/activity/{username}", username)
                .param("page", "2")
                .param("perPage", "15")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(2)))
                .andExpect(jsonPath("$.hasMore", is(true)));
    }
    
    @Test
    void testGetRepositoryActivity_UserNotFound() throws Exception {
        // Given
        String username = "nonexistentuser";
        
        when(gitHubRepositoryService.getRepositoryActivity(eq(username), any(), any()))
            .thenThrow(new GitHubApiException("User not found: " + username, 404, "Not Found"));
        
        // When & Then
        mockMvc.perform(get("/v1/repositories/activity/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("GitHub API Error")))
                .andExpect(jsonPath("$.message", containsString("User not found")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.traceId").exists());
    }
    
    @Test
    void testGetRepositoryActivity_RateLimitExceeded() throws Exception {
        // Given
        String username = "testuser";
        
        when(gitHubRepositoryService.getRepositoryActivity(eq(username), any(), any()))
            .thenThrow(new GitHubRateLimitException("Rate limit exceeded", 0, OffsetDateTime.now().plusHours(1)));
        
        // When & Then
        mockMvc.perform(get("/v1/repositories/activity/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Rate Limit Exceeded")))
                .andExpect(jsonPath("$.message", containsString("rate limit exceeded")))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.details", containsString("Rate limit exceeded")));
    }
    
    @Test
    void testGetRepositoryActivity_InvalidUsername() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/repositories/activity/{username}", "invalid-username!")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Validation Error")))
                .andExpect(jsonPath("$.status", is(400)));
    }
    
    @Test
    void testGetRepositoryActivity_InvalidPagination() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/repositories/activity/{username}", "testuser")
                .param("page", "0")
                .param("perPage", "-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Validation Error")))
                .andExpect(jsonPath("$.status", is(400)));
    }
    

    

    

    
    @Test
    void testHealthEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/repositories/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("GitHub Repository Activity Tracker")))
                .andExpect(jsonPath("$.version", is("1.0.0")))
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void testInfoEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/repositories/info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.service", is("GitHub Repository Activity Tracker")))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.version", is("1.0.0")))
                .andExpect(jsonPath("$.endpoints").exists())
                .andExpect(jsonPath("$.parameters").exists());
    }
    
    // Helper methods for creating mock objects
    
    private RepositoryActivityResponse createMockRepositoryActivityResponse(String username) {
        GitHubRepository repo1 = new GitHubRepository();
        repo1.setName("repo1");
        repo1.setFullName(username + "/repo1");
        repo1.setRecentCommits(Arrays.asList(createMockCommit("commit1", "Test commit 1")));
        
        GitHubRepository repo2 = new GitHubRepository();
        repo2.setName("repo2");
        repo2.setFullName(username + "/repo2");
        repo2.setRecentCommits(Collections.emptyList());
        
        return RepositoryActivityResponse.builder()
                .username(username)
                .userType("User")
                .totalRepositories(5)
                .repositoriesProcessed(2)
                .repositories(Arrays.asList(repo1, repo2))
                .fetchedAt(OffsetDateTime.now())
                .message("Successfully fetched 2 repositories with recent commits")
                .hasMore(false)
                .currentPage(1)
                .totalPages(1)
                .build();
    }
    
    private GitHubRepository createMockRepository(String owner, String repo) {
        GitHubRepository repository = new GitHubRepository();
        repository.setName(repo);
        repository.setFullName(owner + "/" + repo);
        repository.setRecentCommits(Arrays.asList(
            createMockCommit("commit1", "Test commit 1"),
            createMockCommit("commit2", "Test commit 2")
        ));
        
        return repository;
    }
    
    private GitHubCommit createMockCommit(String sha, String message) {
        GitHubCommit commit = new GitHubCommit();
        commit.setSha(sha);
        
        GitHubCommit.CommitDetails details = new GitHubCommit.CommitDetails();
        details.setMessage(message);
        
        GitHubCommit.CommitAuthor author = new GitHubCommit.CommitAuthor();
        author.setName("Test Author");
        author.setEmail("test@example.com");
        author.setDate(OffsetDateTime.now());
        
        details.setAuthor(author);
        commit.setCommit(details);
        
        return commit;
    }
} 