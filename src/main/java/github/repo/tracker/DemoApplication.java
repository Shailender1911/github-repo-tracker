package github.repo.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * GitHub Repository Activity Tracker Application
 * 
 * This application provides REST endpoints to fetch GitHub repository activity
 * including repositories and their recent commits for users and organizations.
 * 
 * Main features:
 * - Fetch repositories for GitHub users and organizations
 * - Get recent commits for each repository
 * - Handle GitHub API rate limiting with retry logic
 * - Support pagination for large result sets
 * - Comprehensive error handling and logging
 * 
 * @author GitHub Repository Tracker
 * @version 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
@Validated
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
