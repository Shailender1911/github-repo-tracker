package github.repo.tracker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

/**
 * POJO representing a GitHub User from the GitHub API response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubUser {
    
    private Long id;
    
    private String login;
    
    private String name;
    
    private String email;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("gravatar_id")
    private String gravatarId;
    
    private String url;
    
    @JsonProperty("followers_url")
    private String followersUrl;
    
    @JsonProperty("following_url")
    private String followingUrl;
    
    @JsonProperty("repos_url")
    private String reposUrl;
    
    private String type;
    
    @JsonProperty("site_admin")
    private Boolean siteAdmin;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    
    @JsonProperty("public_repos")
    private Integer publicRepos;
    
    @JsonProperty("public_gists")
    private Integer publicGists;
    
    private Integer followers;
    
    private Integer following;
} 