# Assignment 9 - Cloud-Native Web Application

## Description

Cloud-native web application with serverless email verification system, enhanced security, and dual-account CI/CD deployment.

## Prerequisites for Building and Deploying Your Application Locally

### Required Software

- **Java 21** or higher
- **Apache Maven 3.6+**
- **MySQL 8.0+** (for local development)

### Environment Setup

```bash
# Verify versions
java -version
mvn -version
```

## Build and Deploy Instructions

### Local Development

```bash
# Clone repository
git clone https://github.com/yushanzzz/webapp-fork.git
cd webapp-fork

# Build and test
mvn clean compile
mvn test

# Run application
mvn spring-boot:run
```

### Environment Configuration

Create `.env` file:

```properties
DATABASE_URL=jdbc:mysql://localhost:3306/csye6225
DATABASE_USERNAME=csye6225
DATABASE_PASSWORD=your_password
APP_PORT=8080
```

### Automated Deployment

The application uses automated CI/CD deployment through GitHub Actions and Terraform. Push to main branch triggers automated AMI building and deployment.

### Email Verification System

- User registration triggers SNS message
- Lambda function sends verification email
- Users must verify email within 1 minute
- Endpoint: `GET /validateEmail?email=user@example.com&token=uuid`

### Security Enhancements

- KMS encryption for all AWS resources
- SSL certificates for HTTPS
- Secrets Manager for sensitive data
- 90-day automatic key rotation

### Advanced CI/CD

- Dual-account deployment (DEV/DEMO)
- Zero-downtime rolling updates
- Automated AMI sharing between accounts
- Instance refresh with health checks

## Testing

### Local Testing

```bash
# Run all tests
mvn test

# Start application
mvn spring-boot:run

# Test health check
curl http://localhost:8080/healthz
```

## Architecture

The application runs on AWS with:

- **Load Balancer**: Distributes traffic across multiple instances
- **Auto Scaling**: 3-5 EC2 instances based on demand
- **RDS**: MySQL database with encryption
- **S3**: File storage with lifecycle policies
- **SNS + Lambda**: Email verification system
- **CloudWatch**: Monitoring and logging

All infrastructure is managed through Terraform and deployed via GitHub Actions.
