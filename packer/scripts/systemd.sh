#!/usr/bin/env bash
set -euo pipefail

echo "[3/3] Installing systemd service..."

# 直接寫入 service 檔案
sudo tee /etc/systemd/system/webapp.service > /dev/null << 'SERVICE'
[Unit]
Description=CSYE6225 Web Application
After=network-online.target

[Service]
User=csye6225
Group=csye6225
EnvironmentFile=/etc/webapp/env
WorkingDirectory=/opt/webapp
ExecStart=/usr/bin/java -jar /opt/webapp/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SERVICE

# Reload 和 enable
sudo systemctl daemon-reload
sudo systemctl enable webapp

# 🆕 添加驗證
if systemctl is-enabled webapp >/dev/null 2>&1; then
    echo "✅ Service webapp is enabled for auto-start"
else
    echo "❌ Failed to enable service webapp"
    exit 1
fi

echo "Systemd service installed and enabled."