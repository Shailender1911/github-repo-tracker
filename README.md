# GitHub Repository Activity Tracker

A Spring Boot application that provides REST APIs to fetch GitHub repository activity including repositories and their recent commits for users and organizations.

## Features

- ✅ Fetch repositories for GitHub users and organizations
- ✅ Get the last 20 commits for each repository with commit message, author, and timestamp
- ✅ Handle GitHub API pagination automatically
- ✅ Rate limiting with retry logic and graceful error handling
- ✅ Comprehensive input validation and error responses
- ✅ Parallel processing for better performance
- ✅ RESTful API endpoints with proper HTTP status codes
- ✅ Built with Java 8 and Spring Boot 2.7.14
- ✅ Comprehensive unit and integration tests
- ✅ Configurable GitHub API settings

## Table of Contents

- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Usage Examples](#usage-examples)
- [Testing](#testing)
- [Architecture](#architecture)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Contributing](#contributing)
- [License](#license)

## Prerequisites

- Java 8 or higher
- Maven 3.6 or higher
- GitHub Personal Access Token

## Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd github-repo-tracker
```

### 2. Create GitHub Personal Access Token

1. Go to [GitHub Settings > Developer settings > Personal access tokens](https://github.com/settings/tokens)
2. Click "Generate new token"
3. Select the following scopes:
   - `public_repo` (to access public repositories)
   - `read:user` (to read user profile information)
   - `read:org` (to read organization information)
4. Copy the generated token

### 3. Configure the Application

Create an `application-local.properties` file in `src/main/resources/` or set environment variables:

```properties
# GitHub configuration
github.token=YOUR_GITHUB_TOKEN_HERE
github.api-url=https://api.github.com
github.max-repositories-per-page=30
github.max-commits-per-repo=20
github.max-retries=3
github.retry-delay-ms=2000
github.enable-request-logging=false

# Server configuration
server.port=8080
```

**Or use environment variables:**

```bash
export GITHUB_TOKEN=your_github_token_here
```

### 4. Build the Project

```bash
mvn clean compile
```

## Configuration

### Application Properties

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `github.token` | GitHub Personal Access Token | - | Yes |
| `github.api-url` | GitHub API base URL | `https://api.github.com` | No |
| `github.connection-timeout-ms` | HTTP connection timeout | `10000` | No |
| `github.read-timeout-ms` | HTTP read timeout | `30000` | No |
| `github.max-repositories-per-page` | Max repos per API page | `30` | No |
| `github.max-commits-per-repo` | Max commits to fetch per repo | `20` | No |
| `github.max-retries` | Max retry attempts for rate limiting | `3` | No |
| `github.retry-delay-ms` | Delay between retry attempts | `2000` | No |
| `github.enable-request-logging` | Enable HTTP request logging | `false` | No |

## Running the Application

### Development Mode

```bash
mvn spring-boot:run
```

### Production Mode

```bash
# Build the JAR
mvn clean package

# Run the JAR
java -jar target/github-repo-tracker-0.0.1-SNAPSHOT.jar
```

### With Custom Configuration

```bash
java -jar target/github-repo-tracker-0.0.1-SNAPSHOT.jar \
  --github.token=your_token_here \
  --server.port=9090
```

The application will start on `http://localhost:8080` (or the configured port).

## API Documentation

### Base URL

```
http://localhost:8080/api
```

### Endpoints

#### 1. Get Repository Activity

Fetch repositories and their recent commits for a GitHub user or organization.

```http
GET /v1/repositories/activity/{username}?page={page}&perPage={perPage}
```

**Parameters:**
- `username` (path) - GitHub username or organization name
- `page` (query, optional) - Page number for pagination (default: 1)
- `perPage` (query, optional) - Number of repositories per page (default: 30, max: 100)

**Response:**
```json
{
  "username": "octocat",
  "userType": "User",
  "totalRepositories": 8,
  "repositoriesProcessed": 3,
  "repositories": [
    {
      "id": 1296269,
      "name": "Hello-World",
      "fullName": "octocat/Hello-World",
      "description": "This your first repo!",
      "htmlUrl": "https://github.com/octocat/Hello-World",
      "defaultBranch": "main",
      "language": "JavaScript",
      "stargazersCount": 80,
      "forksCount": 9,
      "createdAt": "2011-01-26T19:01:12Z",
      "updatedAt": "2011-01-26T19:14:43Z",
      "owner": {
        "login": "octocat",
        "id": 1,
        "type": "User"
      },
      "recentCommits": [
        {
          "sha": "6dcb09b5b57875f334f61aebed695e2e4193db5e",
          "commit": {
            "author": {
              "name": "Monalisa Octocat",
              "email": "support@github.com",
              "date": "2011-04-14T16:00:49Z"
            },
            "message": "Fix all the bugs"
          },
          "htmlUrl": "https://github.com/octocat/Hello-World/commit/6dcb09b5b57875f334f61aebed695e2e4193db5e"
        }
      ]
    }
  ],
  "fetchedAt": "2023-12-07T10:30:00Z",
  "message": "Successfully fetched 3 repositories with recent commits",
  "hasMore": true,
  "currentPage": 1,
  "totalPages": 3
}
```

#### 2. Get Repository Details

Get detailed information for a specific repository including recent commits.

```http
GET /v1/repositories/details/{owner}/{repo}
```

**Parameters:**
- `owner` (path) - Repository owner (username or organization)
- `repo` (path) - Repository name

**Response:**
```json
{
  "name": "Hello-World",
  "fullName": "octocat/Hello-World",
  "recentCommits": [
    {
      "sha": "6dcb09b5b57875f334f61aebed695e2e4193db5e",
      "commit": {
        "author": {
          "name": "Monalisa Octocat",
          "email": "support@github.com",
          "date": "2011-04-14T16:00:49Z"
        },
        "message": "Fix all the bugs"
      }
    }
  ]
}
```

#### 3. Health Check

Check the application health status.

```http
GET /v1/repositories/health
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2023-12-07T10:30:00Z",
  "service": "GitHub Repository Activity Tracker",
  "version": "1.0.0"
}
```

#### 4. API Information

Get information about available endpoints.

```http
GET /v1/repositories/info
```

### Error Responses

All errors follow a consistent format:

```json
{
  "error": "GitHub API Error",
  "message": "User not found: nonexistentuser",
  "details": "Not Found",
  "status": 404,
  "path": "/api/v1/repositories/activity/nonexistentuser",
  "timestamp": "2023-12-07T10:30:00Z",
  "traceId": "abc123def456"
}
```

**Common HTTP Status Codes:**
- `200` - Success
- `400` - Bad Request (invalid parameters)
- `401` - Unauthorized (invalid GitHub token)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found (user/repository not found)
- `429` - Too Many Requests (rate limit exceeded)
- `500` - Internal Server Error

## Usage Examples

### Using cURL

#### Get repository activity for a user
```bash
curl -X GET "http://localhost:8080/api/v1/repositories/activity/octocat" \
  -H "Content-Type: application/json"
```

#### Get repository activity with pagination
```bash
curl -X GET "http://localhost:8080/api/v1/repositories/activity/octocat?page=2&perPage=10" \
  -H "Content-Type: application/json"
```

#### Get specific repository details
```bash
curl -X GET "http://localhost:8080/api/v1/repositories/details/octocat/Hello-World" \
  -H "Content-Type: application/json"
```

### Using HTTPie

```bash
# Get user repositories
http GET localhost:8080/api/v1/repositories/activity/octocat

# Get organization repositories
http GET localhost:8080/api/v1/repositories/activity/github page==1 perPage==20

# Health check
http GET localhost:8080/api/v1/repositories/health
```

### Using JavaScript/Fetch

```javascript
// Get repository activity
async function getRepositoryActivity(username, page = 1, perPage = 30) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/v1/repositories/activity/${username}?page=${page}&perPage=${perPage}`
    );
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const data = await response.json();
    console.log('Repository Activity:', data);
    return data;
  } catch (error) {
    console.error('Error fetching repository activity:', error);
  }
}

// Usage
getRepositoryActivity('octocat', 1, 10);
```

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Classes

```bash
# Service layer tests
mvn test -Dtest=GitHubRepositoryServiceTest

# Controller integration tests
mvn test -Dtest=GitHubRepositoryControllerIntegrationTest
```

### Test Coverage

```bash
mvn jacoco:report
```

The coverage report will be available at `target/site/jacoco/index.html`.

### Manual Testing with Mock Data

For testing without hitting the GitHub API, you can use the WireMock integration in the test classes.

## Architecture

### Package Structure

```
github.repo.tracker/
├── client/          # GitHub API client
├── config/          # Configuration classes
├── controller/      # REST controllers
├── exception/       # Custom exceptions
├── model/           # POJOs and DTOs
└── service/         # Business logic layer
```

### Key Components

1. **GitHubApiClient** - Handles all GitHub API interactions with retry logic and rate limiting
2. **GitHubRepositoryService** - Business logic layer that orchestrates data fetching
3. **GitHubRepositoryController** - REST API endpoints with validation
4. **GlobalExceptionHandler** - Centralized error handling
5. **GitHubProperties** - Configuration properties binding

### Design Patterns Used

- **Repository Pattern** - Data access abstraction
- **Service Layer Pattern** - Business logic separation
- **Builder Pattern** - Object construction
- **Strategy Pattern** - Error handling strategies

## Error Handling

### GitHub API Errors

The application handles various GitHub API errors gracefully:

- **Rate Limiting** - Automatic retry with exponential backoff
- **Authentication Errors** - Clear error messages for token issues
- **Not Found Errors** - Appropriate 404 responses
- **Validation Errors** - Input validation with detailed error messages

### Resilience Features

- **Retry Logic** - Configurable retry attempts for rate-limited requests
- **Circuit Breaker** - Fail fast for repeated failures
- **Timeout Handling** - Configurable connection and read timeouts
- **Graceful Degradation** - Continue processing other repositories if one fails

## Rate Limiting

### GitHub API Rate Limits

- **Authenticated requests** - 5,000 requests per hour
- **Search API** - 30 requests per minute

### Application Rate Limiting Strategy

1. **Request Headers Monitoring** - Track rate limit headers from GitHub
2. **Automatic Retry** - Retry requests when rate limited with configurable delay
3. **Parallel Processing** - Optimize API usage with concurrent requests
4. **Caching** - Cache responses to reduce API calls (future enhancement)

### Configuration

```properties
# Rate limiting configuration
github.max-retries=3
github.retry-delay-ms=2000
```

## Production Deployment

### Docker Deployment

Create a `Dockerfile`:

```dockerfile
FROM openjdk:8-jre-alpine

VOLUME /tmp
ARG JAR_FILE=target/github-repo-tracker-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]
```

Build and run:

```bash
# Build the application
mvn clean package

# Build Docker image
docker build -t github-repo-tracker .

# Run the container
docker run -p 8080:8080 \
  -e GITHUB_TOKEN=your_token_here \
  github-repo-tracker
```

### Environment Variables

For production deployment, use environment variables:

```bash
export GITHUB_TOKEN=your_production_token
export SERVER_PORT=8080
export GITHUB_MAX_RETRIES=5
export GITHUB_RETRY_DELAY_MS=3000
```

### Health Monitoring

The application provides health endpoints for monitoring:

- `/api/v1/repositories/health` - Application health status
- `/actuator/health` - Spring Boot actuator health endpoint

## Performance Considerations

- **Parallel Processing** - Commits are fetched in parallel for multiple repositories
- **Connection Pooling** - HTTP client uses connection pooling
- **Timeouts** - Configurable connection and read timeouts
- **Memory Management** - Efficient object creation with builders
- **Rate Limiting** - Smart retry logic to avoid hitting GitHub limits

## Security Considerations

- **Token Security** - Store GitHub token as environment variable
- **Input Validation** - Comprehensive validation for all inputs
- **Error Information** - Don't expose sensitive information in error responses
- **CORS** - Configurable CORS settings for web applications

## Troubleshooting

### Common Issues

1. **Authentication Errors**
   ```
   Error: "Bad credentials"
   Solution: Verify your GitHub token is correct and has required permissions
   ```

2. **Rate Limit Exceeded**
   ```
   Error: "API rate limit exceeded"
   Solution: Wait for rate limit reset or check your token's rate limit status
   ```

3. **Connection Timeouts**
   ```
   Error: "Read timed out"
   Solution: Increase timeout values in configuration
   ```

### Debug Mode

Enable debug logging:

```properties
logging.level.github.repo.tracker=DEBUG
github.enable-request-logging=true
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Java 8 coding standards
- Use meaningful variable and method names
- Add comprehensive JavaDoc comments
- Write unit tests for new features
- Maintain test coverage above 80%

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:

- Create an issue in this repository
- Check the [troubleshooting section](#troubleshooting)
- Review the [API documentation](#api-documentation)

---

**Built with ❤️ using Java 8, Spring Boot, and best coding practices** 