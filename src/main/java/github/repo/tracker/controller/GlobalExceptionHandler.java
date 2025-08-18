package github.repo.tracker.controller;

import github.repo.tracker.exception.GitHubApiException;
import github.repo.tracker.exception.GitHubRateLimitException;
import github.repo.tracker.model.ApiErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for handling all application exceptions
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle GitHub API exceptions
     */
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ApiErrorResponse> handleGitHubApiException(GitHubApiException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        
        logger.error("GitHub API error [{}]: {}", traceId, ex.getMessage(), ex);
        
        HttpStatus status = determineHttpStatus(ex.getStatusCode());
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .error("GitHub API Error")
                .message(ex.getMessage())
                .details(ex.getResponseBody())
                .status(status.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle GitHub rate limit exceptions specifically
     */
    @ExceptionHandler(GitHubRateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleGitHubRateLimitException(GitHubRateLimitException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        
        logger.warn("GitHub rate limit exceeded [{}]: {}", traceId, ex.getMessage());
        
        String details = String.format("Rate limit exceeded. Remaining: %d, Reset time: %s", 
                                     ex.getRateLimitRemaining(), 
                                     ex.getRateLimitReset());
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .error("Rate Limit Exceeded")
                .message("GitHub API rate limit exceeded. Please try again later.")
                .details(details)
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * Handle validation errors for request parameters
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        
        logger.warn("Validation error [{}]: {}", traceId, ex.getMessage());
        
        String details = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .error("Validation Error")
                .message("Invalid request parameters")
                .details(details)
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle validation errors for request body
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        
        logger.warn("Method argument validation error [{}]: {}", traceId, ex.getMessage());
        
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .error("Validation Error")
                .message("Invalid request data")
                .details(details)
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle type mismatch errors (e.g., invalid parameter types)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        
        logger.warn("Method argument type mismatch [{}]: {}", traceId, ex.getMessage());
        
        String details = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
                                     ex.getValue(), 
                                     ex.getName(), 
                                     ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .error("Invalid Parameter Type")
                .message("Invalid parameter type provided")
                .details(details)
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        
        logger.warn("Illegal argument error [{}]: {}", traceId, ex.getMessage());
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .error("Invalid Argument")
                .message(ex.getMessage())
                .details("Please check your request parameters and try again")
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        
        logger.error("Unexpected error [{}]: {}", traceId, ex.getMessage(), ex);
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .error("Internal Server Error")
                .message("An unexpected error occurred while processing your request")
                .details("Please try again later or contact support if the problem persists")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Determine appropriate HTTP status based on GitHub API status code
     */
    private HttpStatus determineHttpStatus(Integer statusCode) {
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        switch (statusCode) {
            case 400:
                return HttpStatus.BAD_REQUEST;
            case 401:
                return HttpStatus.UNAUTHORIZED;
            case 403:
                return HttpStatus.FORBIDDEN;
            case 404:
                return HttpStatus.NOT_FOUND;
            case 422:
                return HttpStatus.UNPROCESSABLE_ENTITY;
            case 429:
                return HttpStatus.TOO_MANY_REQUESTS;
            case 500:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case 502:
                return HttpStatus.BAD_GATEWAY;
            case 503:
                return HttpStatus.SERVICE_UNAVAILABLE;
            default:
                return statusCode >= 400 && statusCode < 500 ? 
                    HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
} 