#!/usr/bin/env bash
# First deploy of the web admin on a fresh Ubuntu server: steps 2-9 of docs/setup/deploy-linux.md in one go.
#   git clone https://github.com/WhIm1598/wedding-event.git && cd wedding-event
#   sudo deploy/install.sh admin.tenstudio.vn ban@tenstudio.vn
# Safe to run again: it keeps an existing .env, certificate and database, and only redoes what is missing.
set -euo pipefail

DOMAIN=${1:-}
EMAIL=${2:-}
if [[ -z $DOMAIN || -z $EMAIL ]]; then
  echo "Cách dùng: sudo $0 <tên-miền> <email>   (email: chủ tài khoản quản trị nền tảng, nhận thư Let's Encrypt)" >&2
  exit 1
fi
[[ $EUID -eq 0 ]] || { echo "Chạy bằng sudo: sudo $0 $*" >&2; exit 1; }

cd "$(dirname "$(readlink -f "$0")")"            # deploy/
OWNER=${SUDO_USER:-root}
COMPOSE=(docker compose -f docker-compose.prod.yml)
step() { echo; echo "==> $*"; }

step "Cập nhật hệ thống, cài git, curl, ufw"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq git ca-certificates curl openssl ufw dnsutils >/dev/null
timedatectl set-timezone Asia/Ho_Chi_Minh 2>/dev/null || true

step "Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
if [[ $OWNER != root ]]; then usermod -aG docker "$OWNER"; fi
docker compose version

step "Tường lửa: chỉ SSH, 80, 443"
ufw allow OpenSSH >/dev/null 2>&1 || ufw allow 22/tcp >/dev/null   # the profile comes with openssh-server
ufw allow 80,443/tcp >/dev/null
ufw --force enable >/dev/null
ufw status

step "Swap (máy ≤ 4 GB RAM cần để build)"
if [[ $(awk '/SwapTotal/ {print $2}' /proc/meminfo) -eq 0 && $(awk '/MemTotal/ {print $2}' /proc/meminfo) -le 4194304 ]]; then
  fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile >/dev/null && swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
free -h | grep -i swap

step "File bí mật .env"
NEW_ADMIN_PASSWORD=
if [[ -f .env ]]; then
  echo "Giữ nguyên .env đã có (không đổi DB_PASSWORD của database đang chạy)."
  grep -q "^DOMAIN=$DOMAIN$" .env || { echo "DOMAIN trong .env khác $DOMAIN. Sửa .env rồi chạy lại." >&2; exit 1; }
else
  # Letters and digits only, so the password policy (letter + digit) holds and nothing needs quoting
  until [[ $NEW_ADMIN_PASSWORD =~ [A-Za-z] && $NEW_ADMIN_PASSWORD =~ [0-9] ]]; do
    NEW_ADMIN_PASSWORD=$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | cut -c1-16)   # no SIGPIPE under pipefail
  done
  umask 077
  cat > .env <<EOF
DOMAIN=$DOMAIN
DB_PASSWORD=$(openssl rand -hex 24)
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
ADMIN_EMAIL=$EMAIL
ADMIN_PASSWORD=$NEW_ADMIN_PASSWORD
ADMIN_RESET_PASSWORD=false
EOF
  umask 022
  chown "$OWNER" .env
  echo "Đã tạo .env với mật khẩu ngẫu nhiên."
fi

step "Kiểm tra tên miền trỏ về server này"
SERVER_IP=$(curl -fsS4 https://api.ipify.org)
DOMAIN_IP=$(dig +short A "$DOMAIN" | tail -1)
if [[ $DOMAIN_IP != "$SERVER_IP" ]]; then
  echo "$DOMAIN đang trỏ về '${DOMAIN_IP:-không có}', server là $SERVER_IP. Sửa bản ghi A (bước 1), đợi DNS rồi chạy lại." >&2
  exit 1
fi
echo "$DOMAIN → $SERVER_IP"

step "Chứng chỉ HTTPS"
if [[ -f certbot/conf/live/$DOMAIN/fullchain.pem ]]; then
  echo "Đã có chứng chỉ."
else
  "${COMPOSE[@]}" stop gateway 2>/dev/null || true     # standalone certbot needs port 80
  "${COMPOSE[@]}" run --rm -p 80:80 --entrypoint certbot certbot \
    certonly --standalone -d "$DOMAIN" --email "$EMAIL" --agree-tos --no-eff-email --non-interactive
fi

step "Build và chạy (lần đầu 5-15 phút)"
"${COMPOSE[@]}" up -d --build

step "Đợi admin-backend sẵn sàng (chạy migration database)"
for _ in $(seq 120); do
  status=$(docker inspect -f '{{.State.Health.Status}}' "$("${COMPOSE[@]}" ps -q admin-backend)" 2>/dev/null || echo starting)
  [[ $status == healthy ]] && break
  sleep 5
done
if [[ $status != healthy ]]; then
  echo "admin-backend chưa healthy sau 10 phút. Log:" >&2
  "${COMPOSE[@]}" logs --tail 60 admin-backend >&2
  exit 1
fi
"${COMPOSE[@]}" ps

step "Kiểm thử"
fail=0
check() {  # <tên> <mã HTTP mong đợi> <url>
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' "$3")
  if [[ $code == "$2" ]]; then echo "  OK   $1 ($code)"; else echo "  LỖI  $1: $code, mong đợi $2"; fail=1; fi
}
check "HTTP chuyển sang HTTPS" 301 "http://$DOMAIN/"
check "Trang web" 200 "https://$DOMAIN/"
check "API (chưa đăng nhập)" 401 "https://$DOMAIN/api/v1/admin/auth/me"
check "API quản trị nền tảng (chưa đăng nhập)" 401 "https://$DOMAIN/api/v1/platform/studios"
ls backups/*.dump >/dev/null 2>&1 && echo "  OK   Có bản backup đầu tiên" || echo "  ...  Backup đầu tiên chưa xong, kiểm tra lại sau: ls deploy/backups"
[[ $fail -eq 0 ]] || { echo "Có bước kiểm thử lỗi. Xem mục 10 của docs/setup/deploy-linux.md." >&2; exit 1; }

echo
echo "=================================================================="
echo " Xong: https://$DOMAIN"
if [[ -n $NEW_ADMIN_PASSWORD ]]; then
  echo " Đăng nhập quản trị nền tảng: $EMAIL / $NEW_ADMIN_PASSWORD"
  echo " (mật khẩu chỉ hiện lần này; cũng nằm trong deploy/.env cho tới khi bạn xoá)"
fi
echo " Tiếp theo (docs/setup/deploy-linux.md mục 8-9):"
echo "  1. Đăng nhập, bấm biểu tượng chìa khoá để đổi mật khẩu."
echo "  2. Xoá các dòng ADMIN_EMAIL, ADMIN_PASSWORD khỏi deploy/.env, rồi:"
echo "     docker compose -f $(pwd)/docker-compose.prod.yml up -d"
echo "  3. Tạo studio và quản lý, chạy danh sách kiểm thử 9.2, thử khôi phục 9.3, chép backup ra ngoài 9.4."
if [[ $OWNER != root ]]; then echo "  Đăng xuất SSH rồi vào lại để $OWNER dùng được lệnh docker không cần sudo."; fi
echo "=================================================================="
