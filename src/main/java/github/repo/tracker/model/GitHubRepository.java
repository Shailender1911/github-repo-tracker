package github.repo.tracker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

/**
 * POJO representing a GitHub Repository from the GitHub API response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubRepository {
    
    private Long id;
    
    private String name;
    
    @JsonProperty("full_name")
    private String fullName;
    
    private String description;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("clone_url")
    private String cloneUrl;
    
    @JsonProperty("git_url")
    private String gitUrl;
    
    @JsonProperty("ssh_url")
    private String sshUrl;
    
    @JsonProperty("default_branch")
    private String defaultBranch;
    
    private String language;
    
    @JsonProperty("stargazers_count")
    private Integer stargazersCount;
    
    @JsonProperty("watchers_count")
    private Integer watchersCount;
    
    @JsonProperty("forks_count")
    private Integer forksCount;
    
    @JsonProperty("open_issues_count")
    private Integer openIssuesCount;
    
    private Boolean fork;
    
    @JsonProperty("private")
    private Boolean isPrivate;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    
    @JsonProperty("pushed_at")
    private OffsetDateTime pushedAt;
    
    private Long size;
    
    private GitHubUser owner;
    
    // Custom field to store fetched commits
    private java.util.List<GitHubCommit> recentCommits;
} 