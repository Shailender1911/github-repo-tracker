package github.repo.tracker.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * Error response DTO for API error responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {
    
    private String error;
    
    private String message;
    
    private String details;
    
    private Integer status;
    
    private String path;
    
    private OffsetDateTime timestamp;
    
    private String traceId; // For debugging purposes
} 