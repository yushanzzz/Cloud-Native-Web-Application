#!/usr/bin/env bash
set -euo pipefail

echo "Installing CloudWatch Agent..."

# 1. 更新 apt 並安裝依賴（需要 sudo）
sudo apt-get update -y
sudo apt-get install -y wget ca-certificates

# 2. 安裝 CloudWatch Agent
cd /tmp
wget -q https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i -E ./amazon-cloudwatch-agent.deb
rm -f amazon-cloudwatch-agent.deb

# 3. 放置配置
sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
sudo mv -f /tmp/cloudwatch-config.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
sudo chown root:root /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
sudo chmod 0644 /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

# 4. 創建日誌目錄
sudo mkdir -p /opt/webapp/logs
sudo chown csye6225:csye6225 /opt/webapp/logs
sudo chmod 755 /opt/webapp/logs

# 5. 啟用服務
sudo systemctl enable amazon-cloudwatch-agent

echo "CloudWatch Agent installed."
