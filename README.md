# Cloud-Native Web Application

A production-ready RESTful web application built with Spring Boot and deployed on AWS with complete infrastructure automation and serverless email verification.

---

## Architecture Overview

This application implements a highly available, auto-scaling architecture on AWS:

- **Multi-tier Architecture**: Load-balanced EC2 instances in Auto Scaling Groups across multiple AZs
- **Serverless Email Verification**: SNS → Lambda → SES workflow with DynamoDB deduplication
- **Infrastructure as Code**: Complete automation using Terraform and Packer
- **Zero-downtime Deployments**: Automated CI/CD with GitHub Actions and instance refresh

## Key Features

- **RESTful API**: User and product management with Spring Boot
- **Secure Authentication**: BCrypt password hashing with Basic Authentication
- **Image Management**: S3-backed storage with IAM role-based access
- **Email Verification**: Serverless workflow with time-limited tokens
- **Auto Scaling**: Dynamic scaling (3-5 instances) based on CPU metrics
- **High Availability**: Multi-AZ deployment with Application Load Balancer
- **Observability**: CloudWatch Logs and custom StatsD metrics
- **Security**: KMS encryption, Secrets Manager, SSL/TLS, private subnets

---

## Technology Stack

**Backend**: Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA, MySQL 8.0

**AWS Services**: EC2, Auto Scaling Groups, Application Load Balancer, RDS MySQL, S3, SNS, Lambda, SES, DynamoDB, CloudWatch, KMS, Secrets Manager, Route 53, VPC

**DevOps**: Terraform, Packer, GitHub Actions, Maven, StatsD

---

## 📋 API Endpoints

### User Management

```
GET    /healthz                    - Health check endpoint
POST   /v1/user                    - Create new user account
GET    /v1/user/self               - Get authenticated user details
PUT    /v1/user/self               - Update user information
GET    /validateEmail              - Verify email with token
```

### Product Management

```
POST   /v1/product                 - Create new product
GET    /v1/product/{id}            - Get product details
PUT    /v1/product/{id}            - Update product (owner only)
PATCH  /v1/product/{id}            - Partial update product
DELETE /v1/product/{id}            - Delete product (owner only)
```

### Image Management

```
POST   /v1/product/{id}/image      - Upload product image
GET    /v1/product/{id}/image      - List all product images
GET    /v1/product/{id}/image/{imageId}  - Get specific image
DELETE /v1/product/{id}/image/{imageId}  - Delete image (owner only)
```

---

## Security Features

- **Password Security**: BCrypt hashing with unique salts per user
- **Authentication**: HTTP Basic Authentication for protected endpoints
- **Encryption at Rest**: KMS encryption for EC2, RDS, S3
- **Encryption in Transit**: SSL/TLS via Application Load Balancer
- **Network Isolation**: Private subnets for databases
- **Secret Management**: AWS Secrets Manager for credentials
- **IAM Roles**: Principle of least privilege
- **Token Expiration**: 5-minute validity for email verification

---

## CI/CD Pipeline

### Automated Workflows

**Pull Request Workflow** - Status checks on PR raised

- Run integration tests
- Packer template format and validation
- Block merge if checks fail

**Deployment Workflow** - Triggered on merge to main

```
Run Tests → Build JAR → Build AMI (Packer) → Share to DEMO Account
→ Update Launch Template → Trigger Instance Refresh → Wait for Completion
```

**Custom AMI includes:**

- Ubuntu 24.04 LTS
- Java 21, CloudWatch Agent
- Application JAR and systemd service
- Auto-start on boot

---

## Monitoring

**CloudWatch Integration**:

- Application logs streamed to CloudWatch Logs
- Custom StatsD metrics for API performance
- Auto Scaling based on CPU utilization
- ALB health checks every 30 seconds

**Custom Metrics**:

- API call count and duration per endpoint
- Database query execution time
- S3 operation latency

---

## Local Development

### Prerequisites

- Java 21, Maven 3.8+, MySQL 8.0
- AWS CLI configured

### Setup

```bash
# Create database
mysql -u root -p
CREATE DATABASE webappdb;

# Create .env file
cat > .env <<EOF
DB_HOST=localhost
DB_PORT=3306
DB_NAME=webappdb
DB_USER=root
DB_PASSWORD=your_password
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-dev-bucket
SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789012:topic
EOF

# Build and run
mvn clean install
mvn spring-boot:run
```

## Infrastructure Deployment

### Terraform Setup

```bash
# Clone infrastructure repository
git clone https://github.com/your-org/tf-aws-infra-fork.git
cd tf-aws-infra-fork

# Initialize and apply
terraform init
terraform plan
terraform apply
```

### Import SSL Certificate (DEMO Account)

```bash
aws acm import-certificate \
  --certificate fileb://certificate.crt \
  --private-key fileb://private.key \
  --certificate-chain fileb://certificate-chain.crt \
  --region us-east-1 \
  --profile demo
```

## Troubleshooting

```bash
# Check CloudWatch Logs
aws logs tail /aws/ec2/webapp --follow --profile demo

# Check application status on EC2
ssh ubuntu@<instance-ip>
sudo systemctl status webapp
sudo journalctl -u webapp -n 100

# Verify Secrets Manager access
aws secretsmanager get-secret-value --secret-id db-password

# Check target health
aws elbv2 describe-target-health --target-group-arn <arn>

# Lambda logs
aws logs tail /aws/lambda/email-verification --follow
```
