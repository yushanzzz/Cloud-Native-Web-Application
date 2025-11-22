#!/bin/bash
set -e

echo "Starting WebApp setup script..."

# 接收環境變數（Packer 會透過 provisioner 傳進來）
APP_USER=${APP_USER:-"csye6225"}
APP_GROUP=${APP_GROUP:-"csye6225"}
APP_DIR=${APP_DIR:-"/opt/webapp"}
APP_CONFIG_DIR=${APP_CONFIG_DIR:-"/etc/webapp"}
APP_JAR_PATH=${APP_JAR_PATH:-"/tmp/app.jar"}

echo "Using variables:"
echo "  User: $APP_USER"
echo "  Group: $APP_GROUP"
echo "  App Dir: $APP_DIR"
echo "  Config Dir: $APP_CONFIG_DIR"

# 建立系統使用者與群組
if ! id "$APP_USER" &>/dev/null; then
  echo "Creating user and group $APP_USER..."
  sudo groupadd "$APP_GROUP"
  sudo useradd -r -s /usr/sbin/nologin -g "$APP_GROUP" "$APP_USER"
fi

# 建立必要資料夾
echo "Creating application directories..."
sudo mkdir -p "$APP_DIR" "$APP_CONFIG_DIR" "$APP_DIR/logs"
sudo chown -R "$APP_USER":"$APP_GROUP" "$APP_DIR" "$APP_CONFIG_DIR"

# 移動應用程式 jar 檔案
echo "Deploying application jar..."
sudo mv "$APP_JAR_PATH" "$APP_DIR/app.jar"
sudo chown "$APP_USER":"$APP_GROUP" "$APP_DIR/app.jar"
sudo chmod 640 "$APP_DIR/app.jar"

echo "WebApp setup complete!"
echo "Note: Database configuration will be provided via EC2 User Data"
# 創建日誌目錄
echo "=== Creating log directory ==="
sudo mkdir -p /opt/webapp/logs
sudo chown csye6225:csye6225 /opt/webapp/logs