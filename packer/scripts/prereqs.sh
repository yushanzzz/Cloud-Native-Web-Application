#!/usr/bin/env bash
set -euo pipefail

echo "[1/3] Updating system and installing runtime..."

# 更新系統套件
sudo apt-get update -y 
sudo apt-get upgrade -y

# 安裝常用工具
sudo apt-get install -y openjdk-21-jre-headless curl unzip

# 安裝 AWS CLI v2（更安全的方法）
echo "Installing AWS CLI v2..."
if ! command -v aws &> /dev/null; then
  # 下載到臨時目錄
  TMP_DIR=$(mktemp -d)
  cd "$TMP_DIR"
  
  # 下載並驗證
  if curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"; then
    echo "Downloaded AWS CLI installer"
    
    # 解壓
    if unzip -q awscliv2.zip; then
      echo "Extracted AWS CLI installer"
      
      # 安裝
      if sudo ./aws/install; then
        echo "AWS CLI installed successfully"
        
        # 驗證安裝
        if /usr/local/bin/aws --version; then
          echo "AWS CLI verification passed"
        else
          echo "ERROR: AWS CLI verification failed"
          exit 1
        fi
      else
        echo "ERROR: AWS CLI installation failed"
        exit 1
      fi
    else
      echo "ERROR: Failed to extract AWS CLI"
      exit 1
    fi
  else
    echo "ERROR: Failed to download AWS CLI"
    exit 1
  fi
  
  # 清理
  cd -
  rm -rf "$TMP_DIR"
else
  echo "AWS CLI already installed"
fi

# 移除不必要套件
if command -v git &> /dev/null; then
  sudo apt-get purge -y git
  sudo apt-get autoremove -y
fi

# 清理 apt cache
sudo apt-get clean
sudo rm -rf /var/lib/apt/lists/*

echo "Runtime dependencies installed successfully"