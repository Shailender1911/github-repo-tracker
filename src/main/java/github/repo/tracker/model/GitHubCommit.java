package github.repo.tracker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

/**
 * POJO representing a GitHub Commit from the GitHub API response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubCommit {
    
    private String sha;
    
    @JsonProperty("node_id")
    private String nodeId;
    
    private CommitDetails commit;
    
    private String url;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("comments_url")
    private String commentsUrl;
    
    private GitHubUser author;
    
    private GitHubUser committer;
    
    /**
     * Nested class representing the actual commit details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitDetails {
        
        private CommitAuthor author;
        
        private CommitAuthor committer;
        
        private String message;
        
        private Tree tree;
        
        private String url;
        
        @JsonProperty("comment_count")
        private Integer commentCount;
        
        private Verification verification;
    }
    
    /**
     * Nested class representing commit author/committer information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitAuthor {
        
        private String name;
        
        private String email;
        
        private OffsetDateTime date;
    }
    
    /**
     * Nested class representing the tree information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tree {
        
        private String sha;
        
        private String url;
    }
    
    /**
     * Nested class representing commit verification status
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Verification {
        
        private Boolean verified;
        
        private String reason;
        
        private String signature;
        
        private String payload;
    }
} 