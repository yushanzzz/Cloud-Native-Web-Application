#!/bin/bash
# Setup script for CSYE6225 - All values from .env
set -e

echo "========================================="
echo "Starting CSYE6225 Application Setup"
echo "========================================="

# Check for required files
echo "Checking for required files..."
if [ ! -f .env ]; then
    echo "ERROR: .env file is required!"
    exit 1
fi

if [ ! -f webapp.zip ]; then
    echo "ERROR: webapp.zip not found!"
    exit 1
fi

# Load environment variables
echo "Loading configuration from .env..."
source .env

# Validate required variables
REQUIRED_VARS="DB_NAME DB_USER DB_PASSWORD APP_USER APP_GROUP APP_DIR SERVER_PORT JAVA_PATH"
for var in $REQUIRED_VARS; do
    if [ -z "${!var}" ]; then
        echo "ERROR: Missing required variable $var in .env"
        exit 1
    fi
done
echo "All required variables loaded"

# Step 1: Update package lists
echo "Step 1: Updating package lists..."
sudo apt update -y

# Step 2: Upgrade system packages
echo "Step 2: Upgrading system packages..."
sudo apt upgrade -y

# Step 3: Install Java and Maven
echo "Step 3: Installing Java and Maven..."
sudo apt install -y openjdk-17-jdk maven unzip
java -version
mvn -version

# Step 4: Install MySQL
echo "Step 4: Installing MySQL..."
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# Step 5: Create database and user
echo "Step 5: Creating database..."
sudo ${MYSQL_PATH:-mysql} <<EOF
CREATE DATABASE IF NOT EXISTS ${DB_NAME};
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOF
echo "Database ${DB_NAME} created"

# Step 6: Create application group
echo "Step 6: Creating application group..."
sudo groupadd -f ${APP_GROUP}

# Step 7: Create application user
echo "Step 7: Creating application user..."
sudo useradd -r -s /bin/false -g ${APP_GROUP} -m ${APP_USER} 2>/dev/null || true

# Ensure home directory exists with proper permissions
sudo mkdir -p /home/${APP_USER}/.m2
sudo chown -R ${APP_USER}:${APP_GROUP} /home/${APP_USER}
sudo chmod 755 /home/${APP_USER}

# Step 8: Deploy and build application
echo "Step 8: Deploying application files..."
sudo mkdir -p ${APP_DIR}

# Stop existing service if running
SERVICE_NAME="${APP_USER}.service"
if systemctl is-active --quiet ${SERVICE_NAME}; then
    echo "Stopping existing application..."
    sudo systemctl stop ${SERVICE_NAME}
fi

# Extract source code
echo "Extracting webapp.zip..."
sudo unzip -o webapp.zip -d ${APP_DIR}/

# Copy .env to app directory
sudo cp .env ${APP_DIR}/

# Change ownership for build
sudo chown -R ${APP_USER}:${APP_GROUP} ${APP_DIR}

# Build application
echo "Building application from source..."
cd ${APP_DIR}

# Check if Maven wrapper exists
if [ -f ./mvnw ]; then
    echo "Using Maven wrapper..."
    sudo chmod +x mvnw
    sudo -u ${APP_USER} ./mvnw clean package -DskipTests
else
    echo "Using system Maven..."
    sudo -u ${APP_USER} mvn clean package -DskipTests
fi

# Find the JAR file
JAR_FILE=$(find ${APP_DIR}/target -name "*.jar" -type f ! -name "*original*" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Build failed - no JAR file found!"
    exit 1
fi
echo "Build successful: $(basename $JAR_FILE)"

# Step 9: Set permissions
echo "Step 9: Setting file permissions..."
sudo chmod 750 ${APP_DIR}
sudo chmod 550 "$JAR_FILE"
sudo chmod 400 ${APP_DIR}/.env

# Step 10: Create systemd service
echo "Step 10: Creating systemd service..."
sudo tee /etc/systemd/system/${SERVICE_NAME} > /dev/null <<EOF
[Unit]
Description=CSYE6225 Spring Boot Application
After=network.target mysql.service
Requires=mysql.service

[Service]
Type=simple
User=${APP_USER}
Group=${APP_GROUP}
WorkingDirectory=${APP_DIR}
EnvironmentFile=${APP_DIR}/.env
ExecStart=${JAVA_PATH} -jar ${JAR_FILE}
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# Step 11: Start the application
echo "Step 11: Starting application..."
sudo systemctl daemon-reload
sudo systemctl enable ${SERVICE_NAME}
sudo systemctl start ${SERVICE_NAME}

# Wait for application to start
echo "Waiting for application to start..."
sleep 10

# Check if running
if systemctl is-active --quiet ${SERVICE_NAME}; then
    echo "✓ Application is running!"

    # Test health endpoint
    HEALTH_URL="http://localhost:${SERVER_PORT}/healthz"
    if curl -sf ${HEALTH_URL} > /dev/null; then
        echo "✓ Health check passed"
    else
        echo "⚠ Health check pending"
    fi
else
    echo "✗ Application failed to start"
    sudo journalctl -u ${SERVICE_NAME} -n 50 --no-pager
fi

echo ""
echo "========================================="
echo "Setup Complete!"
echo "========================================="
echo "Database: ${DB_NAME}"
echo "Application: ${SERVICE_NAME}"
echo "Port: ${SERVER_PORT}"
echo "Directory: ${APP_DIR}"
echo ""
echo "Commands:"
echo "  Status: sudo systemctl status ${SERVICE_NAME}"
echo "  Logs: sudo journalctl -u ${SERVICE_NAME} -f"
echo "  Test: curl http://$(curl -s ifconfig.me):${SERVER_PORT}/healthz"