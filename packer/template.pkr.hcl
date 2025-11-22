packer {
  required_plugins {
    amazon = {
      source  = "github.com/hashicorp/amazon"
      version = ">= 1.2.8"
    }
  }
}

# ==== Variables ====
variable "region" {
  type    = string
  default = " "
}

variable "demo_account_id" {
  type    = string
  default = ""
}

variable "app_artifact" {
  type    = string
  default = "target/webapp.jar"
}

variable "app_port" {
  type    = number
  default = 8080
}

variable "ami_name_prefix" {
  type    = string
  default = "webapp"
}

variable "db_name" {
  type    = string
  default = ""
}

variable "db_user" {
  type    = string
  default = ""
}

variable "db_password" {
  type      = string
  sensitive = true
  default   = ""
}

# ===== Builder (DEV account, default VPC) =====
source "amazon-ebs" "ubuntu2404" {
  region        = var.region
  ssh_username  = "ubuntu"
  instance_type = "t3.small"

  # Ubuntu 24.04 LTS (Noble) from Canonical.
  source_ami_filter {
    filters = {
      name                = "ubuntu/images/*ubuntu-noble-24.04-amd64-server-*"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
      state               = "available"
    }
    owners      = ["099720109477"] # Canonical
    most_recent = true
  }

  # AMI 命名與存放
  ami_name        = "${var.ami_name_prefix}-${formatdate("YYYYMMDD-hhmmss", timestamp())}"
  ami_description = "CSYE6225 custom image with app + deps baked in"
  ami_users       = [var.demo_account_id] # 私有 AMI，僅分享啟動權給 DEMO 帳號

  # Root volume 要求：25GB GP2 並隨實例刪除
  launch_block_device_mappings {
    device_name           = "/dev/sda1"
    volume_size           = 25
    volume_type           = "gp2"
    delete_on_termination = true
  }

  # 預設使用 default VPC 與其子網（不指定 subnet_id 即可）
  # 臨時執行用暫態 key，由 Packer 自管
}

# ==== Build steps =====
build {
  name    = "webapp-ami"
  sources = ["source.amazon-ebs.ubuntu2404"]

  # 由 CI 編譯好的 JAR 與 systemd 服務檔帶入映像
  provisioner "file" {
    source      = var.app_artifact # 例如 target/myapp-1.0.0.jar
    destination = "/tmp/app.jar"
  }

  provisioner "file" {
    source      = "packer/webapp.service" # 建議放在 packer/ 下
    destination = "/tmp/webapp.service"
  }


  # 安裝與配置
  provisioner "shell" {
    scripts = [
      "packer/scripts/prereqs.sh",   # apt update/upgrade、安裝 JRE、安裝 MySQL/PostgreSQL、purge git
      "packer/scripts/app_setup.sh", # 建 csye6225 使用者、建立 DB/USER、搬 /tmp/app.jar 到 /opt/webapp、寫 /etc/webapp/env
      "packer/scripts/systemd.sh"    # 複製 /tmp/webapp.service 到 /etc/systemd/system、daemon-reload、enable（不在 build 期啟動）
    ]
    environment_vars = [
      "APP_PORT=${var.app_port}",
      "DB_NAME=${var.db_name}",
      "DB_USER=${var.db_user}",
      "DB_PASSWORD=${var.db_password}"
    ]
  }

  # 新增：創建 CloudWatch Agent 配置文件
  provisioner "file" {
    source      = "packer/cloudwatch-config.json"
    destination = "/tmp/cloudwatch-config.json"
  }

  # 啟用開機自動啟動
  provisioner "shell" {
    script = "packer/scripts/install_cloudwatch.sh"
  }
} 