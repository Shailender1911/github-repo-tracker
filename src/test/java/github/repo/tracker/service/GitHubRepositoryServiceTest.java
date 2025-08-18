package github.repo.tracker.service;

import github.repo.tracker.client.GitHubApiClient;
import github.repo.tracker.config.GitHubProperties;
import github.repo.tracker.exception.GitHubApiException;
import github.repo.tracker.model.GitHubCommit;
import github.repo.tracker.model.GitHubRepository;
import github.repo.tracker.model.GitHubUser;
import github.repo.tracker.model.RepositoryActivityResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for GitHubRepositoryService
 */
@ExtendWith(MockitoExtension.class)
class GitHubRepositoryServiceTest {
    
    @Mock
    private GitHubApiClient gitHubApiClient;
    
    @Mock
    private GitHubProperties gitHubProperties;
    
    private GitHubRepositoryService gitHubRepositoryService;
    
    @BeforeEach
    void setUp() {
        lenient().when(gitHubProperties.getMaxRepositoriesPerPage()).thenReturn(30);
        lenient().when(gitHubProperties.getMaxCommitsPerRepo()).thenReturn(20);
        
        gitHubRepositoryService = new GitHubRepositoryService(gitHubApiClient, gitHubProperties);
    }
    
    @Test
    void testGetRepositoryActivity_User_Success() {
        // Given
        String username = "testuser";
        GitHubUser mockUser = createMockUser(username, "User", 5);
        List<GitHubRepository> mockRepos = createMockRepositories(3);
        List<GitHubCommit> mockCommits = createMockCommits(2);
        
        when(gitHubApiClient.getUser(username)).thenReturn(mockUser);
        when(gitHubApiClient.getUserRepositories(eq(username), eq(1), eq(30))).thenReturn(mockRepos);
        when(gitHubApiClient.getRepositoryCommits(anyString(), anyString(), eq(20))).thenReturn(mockCommits);
        
        // When
        RepositoryActivityResponse response = gitHubRepositoryService.getRepositoryActivity(username, 1, 30);
        
        // Then
        assertNotNull(response);
        assertEquals(username, response.getUsername());
        assertEquals("User", response.getUserType());
        assertEquals(5, response.getTotalRepositories());
        assertEquals(3, response.getRepositoriesProcessed());
        assertEquals(3, response.getRepositories().size());
        assertNotNull(response.getFetchedAt());
        assertTrue(response.getMessage().contains("Successfully fetched 3 repositories"));
        
        // Verify all repositories have commits
        response.getRepositories().forEach(repo -> {
            assertNotNull(repo.getRecentCommits());
            assertEquals(2, repo.getRecentCommits().size());
        });
        
        verify(gitHubApiClient).getUser(username);
        verify(gitHubApiClient).getUserRepositories(username, 1, 30);
        verify(gitHubApiClient, times(3)).getRepositoryCommits(anyString(), anyString(), eq(20));
    }
    
    @Test
    void testGetRepositoryActivity_Organization_Success() {
        // Given
        String orgName = "testorg";
        GitHubUser mockOrg = createMockUser(orgName, "Organization", 10);
        List<GitHubRepository> mockRepos = createMockRepositories(2);
        List<GitHubCommit> mockCommits = createMockCommits(1);
        
        when(gitHubApiClient.getUser(orgName)).thenReturn(mockOrg);
        when(gitHubApiClient.getOrganizationRepositories(eq(orgName), eq(1), eq(30))).thenReturn(mockRepos);
        when(gitHubApiClient.getRepositoryCommits(anyString(), anyString(), eq(20))).thenReturn(mockCommits);
        
        // When
        RepositoryActivityResponse response = gitHubRepositoryService.getRepositoryActivity(orgName, 1, 30);
        
        // Then
        assertNotNull(response);
        assertEquals(orgName, response.getUsername());
        assertEquals("Organization", response.getUserType());
        assertEquals(10, response.getTotalRepositories());
        assertEquals(2, response.getRepositoriesProcessed());
        
        verify(gitHubApiClient).getUser(orgName);
        verify(gitHubApiClient).getOrganizationRepositories(orgName, 1, 30);
        verify(gitHubApiClient, times(2)).getRepositoryCommits(anyString(), anyString(), eq(20));
    }
    
    @Test
    void testGetRepositoryActivity_EmptyRepositories() {
        // Given
        String username = "emptyuser";
        GitHubUser mockUser = createMockUser(username, "User", 0);
        
        when(gitHubApiClient.getUser(username)).thenReturn(mockUser);
        when(gitHubApiClient.getUserRepositories(eq(username), eq(1), eq(30))).thenReturn(Collections.emptyList());
        
        // When
        RepositoryActivityResponse response = gitHubRepositoryService.getRepositoryActivity(username, 1, 30);
        
        // Then
        assertNotNull(response);
        assertEquals(username, response.getUsername());
        assertEquals("User", response.getUserType());
        assertEquals(0, response.getTotalRepositories());
        assertEquals(0, response.getRepositoriesProcessed());
        assertTrue(response.getRepositories().isEmpty());
        assertTrue(response.getMessage().contains("No repositories found"));
        assertFalse(response.getHasMore());
        
        verify(gitHubApiClient).getUser(username);
        verify(gitHubApiClient).getUserRepositories(username, 1, 30);
        verify(gitHubApiClient, never()).getRepositoryCommits(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testGetRepositoryActivity_PaginationParameters() {
        // Given
        String username = "testuser";
        GitHubUser mockUser = createMockUser(username, "User", 100);
        List<GitHubRepository> mockRepos = createMockRepositories(30); // Full page
        List<GitHubCommit> mockCommits = createMockCommits(1);
        
        when(gitHubApiClient.getUser(username)).thenReturn(mockUser);
        when(gitHubApiClient.getUserRepositories(eq(username), eq(2), eq(30))).thenReturn(mockRepos);
        when(gitHubApiClient.getRepositoryCommits(anyString(), anyString(), eq(20))).thenReturn(mockCommits);
        
        // When
        RepositoryActivityResponse response = gitHubRepositoryService.getRepositoryActivity(username, 2, 30);
        
        // Then
        assertNotNull(response);
        assertEquals(2, response.getCurrentPage());
        assertTrue(response.getHasMore()); // Full page indicates more data
        assertEquals(4, response.getTotalPages()); // 100 repos / 30 per page = 4 pages (rounded up)
        
        verify(gitHubApiClient).getUserRepositories(username, 2, 30);
    }
    
    @Test
    void testGetRepositoryActivity_UserNotFound() {
        // Given
        String username = "nonexistentuser";
        
        when(gitHubApiClient.getUser(username)).thenThrow(
            new GitHubApiException("User not found: " + username, 404, "Not Found"));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () -> {
            gitHubRepositoryService.getRepositoryActivity(username, 1, 30);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
        assertEquals(Integer.valueOf(404), exception.getStatusCode());
        
        verify(gitHubApiClient).getUser(username);
        verify(gitHubApiClient, never()).getUserRepositories(anyString(), anyInt(), anyInt());
    }
    
    @Test
    void testGetRepositoryActivity_CommitFetchFailure() {
        // Given
        String username = "testuser";
        GitHubUser mockUser = createMockUser(username, "User", 2);
        List<GitHubRepository> mockRepos = createMockRepositories(2);
        List<GitHubCommit> mockCommits = createMockCommits(1);
        
        when(gitHubApiClient.getUser(username)).thenReturn(mockUser);
        when(gitHubApiClient.getUserRepositories(eq(username), eq(1), eq(30))).thenReturn(mockRepos);
        
        // First repo succeeds, second fails
        when(gitHubApiClient.getRepositoryCommits(eq("owner0"), eq("repo0"), eq(20))).thenReturn(mockCommits);
        when(gitHubApiClient.getRepositoryCommits(eq("owner1"), eq("repo1"), eq(20)))
            .thenThrow(new GitHubApiException("Repository access forbidden", 403, "Forbidden"));
        
        // When
        RepositoryActivityResponse response = gitHubRepositoryService.getRepositoryActivity(username, 1, 30);
        
        // Then
        assertNotNull(response);
        assertEquals(2, response.getRepositoriesProcessed());
        
        // First repository should have commits, second should have empty list
        assertEquals(1, response.getRepositories().get(0).getRecentCommits().size());
        assertEquals(0, response.getRepositories().get(1).getRecentCommits().size());
    }
    
    @Test
    void testGetRepositoryDetails_Success() {
        // Given
        String owner = "testowner";
        String repoName = "testrepo";
        List<GitHubCommit> mockCommits = createMockCommits(3);
        
        when(gitHubApiClient.getRepositoryCommits(owner, repoName, 20)).thenReturn(mockCommits);
        
        // When
        GitHubRepository result = gitHubRepositoryService.getRepositoryDetails(owner, repoName);
        
        // Then
        assertNotNull(result);
        assertEquals(repoName, result.getName());
        assertEquals(owner + "/" + repoName, result.getFullName());
        assertEquals(3, result.getRecentCommits().size());
        
        verify(gitHubApiClient).getRepositoryCommits(owner, repoName, 20);
    }
    
    @Test
    void testGetRepositoryDetails_CommitFetchFailure() {
        // Given
        String owner = "testowner";
        String repoName = "testrepo";
        
        when(gitHubApiClient.getRepositoryCommits(owner, repoName, 20))
            .thenThrow(new GitHubApiException("Repository not found", 404, "Not Found"));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () -> {
            gitHubRepositoryService.getRepositoryDetails(owner, repoName);
        });
        
        assertTrue(exception.getMessage().contains("Failed to fetch repository details"));
    }
    
    // Helper methods for creating mock objects
    
    private GitHubUser createMockUser(String username, String type, int publicRepos) {
        GitHubUser user = new GitHubUser();
        user.setLogin(username);
        user.setType(type);
        user.setPublicRepos(publicRepos);
        return user;
    }
    
    private List<GitHubRepository> createMockRepositories(int count) {
        List<GitHubRepository> repos = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            GitHubRepository repo = new GitHubRepository();
            repo.setId((long) i);
            repo.setName("repo" + i);
            repo.setFullName("owner" + i + "/repo" + i);
            
            GitHubUser owner = new GitHubUser();
            owner.setLogin("owner" + i);
            repo.setOwner(owner);
            
            repos.add(repo);
        }
        return repos;
    }
    
    private List<GitHubCommit> createMockCommits(int count) {
        List<GitHubCommit> commits = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            GitHubCommit commit = new GitHubCommit();
            commit.setSha("sha" + i);
            
            GitHubCommit.CommitDetails details = new GitHubCommit.CommitDetails();
            details.setMessage("Commit message " + i);
            
            GitHubCommit.CommitAuthor author = new GitHubCommit.CommitAuthor();
            author.setName("Author " + i);
            author.setEmail("author" + i + "@example.com");
            author.setDate(OffsetDateTime.now().minusHours(i));
            
            details.setAuthor(author);
            commit.setCommit(details);
            
            commits.add(commit);
        }
        return commits;
    }
} 