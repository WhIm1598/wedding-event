#!/usr/bin/env bash
# Run in Oracle Cloud Shell. Retries creating the Always Free Ampere A1 VM until Oracle has capacity
# (docs/setup/deploy-oracle-duckdns.md step 5). Same script as automation-tool's, other defaults.
#   bash oracle-create-vm.sh oracle_demo.pub
# Optional env: NAME (wedding-demo), OCPUS (1), MEM (6 GB), DISK (50 GB), INTERVAL (300 s),
#   SUBNET_NAME (public subnet-learn-automation-vcn: reuses automation-tool's VCN, whose ports 80/443 are already open).
set -uo pipefail

KEY=${1:-}
[[ -f $KEY ]] || { echo "Cách dùng: bash $0 <file khoá công khai .pub>" >&2; exit 1; }
C=${OCI_TENANCY:?Chạy script trong Oracle Cloud Shell}   # root compartment
NAME=${NAME:-wedding-demo} OCPUS=${OCPUS:-1} MEM=${MEM:-6} DISK=${DISK:-50} INTERVAL=${INTERVAL:-300}
SUBNET_NAME=${SUBNET_NAME:-public subnet-learn-automation-vcn}
SHAPE=VM.Standard.A1.Flex

existing=$(oci compute instance list -c "$C" --all --display-name "$NAME" \
  --query 'data[?"lifecycle-state"!=`TERMINATED`].id | [0]' --raw-output)
[[ -z $existing || $existing == null ]] || { echo "Đã có máy $NAME: $existing. Không tạo thêm." >&2; exit 1; }

mapfile -t ADS < <(oci iam availability-domain list -c "$C" --query 'data[].name' | jq -r '.[]')
SUBNET=$(oci network subnet list -c "$C" --all --display-name "$SUBNET_NAME" \
  --query 'data[?"prohibit-public-ip-on-vnic"==`false`].id | [0]' --raw-output)
IMAGE=$(oci compute image list -c "$C" --all --operating-system "Canonical Ubuntu" --operating-system-version "24.04" \
  --shape "$SHAPE" --sort-by TIMECREATED --sort-order DESC \
  --query 'data[?!contains("display-name", `"Minimal"`)].id | [0]' --raw-output)
for v in SUBNET IMAGE; do
  [[ -n ${!v} && ${!v} != null ]] || { echo "Không tìm thấy $v (subnet public '$SUBNET_NAME' hoặc image Ubuntu 24.04 ARM)." >&2; exit 1; }
done
echo "Máy:    $NAME, $OCPUS OCPU / $MEM GB, ổ $DISK GB"
echo "AD:     ${ADS[*]}"
echo "Subnet: $(oci network subnet get --subnet-id "$SUBNET" --query 'data."display-name"' --raw-output)"
echo "Image:  $(oci compute image get --image-id "$IMAGE" --query 'data."display-name"' --raw-output)"

try=0
while :; do
  for AD in "${ADS[@]}"; do
    try=$((try + 1))
    echo "[$(date '+%F %T')] Lần $try, $AD ..."
    if out=$(oci compute instance launch -c "$C" --availability-domain "$AD" --display-name "$NAME" --shape "$SHAPE" \
        --shape-config "{\"ocpus\":$OCPUS,\"memoryInGBs\":$MEM}" --image-id "$IMAGE" --subnet-id "$SUBNET" \
        --assign-public-ip true --boot-volume-size-in-gbs "$DISK" --ssh-authorized-keys-file "$KEY" \
        --wait-for-state RUNNING 2>&1); then
      id=$(oci compute instance list -c "$C" --all --display-name "$NAME" --lifecycle-state RUNNING \
        --query 'data[0].id' --raw-output)
      ip=$(oci compute instance list-vnics --instance-id "$id" --query 'data[0]."public-ip"' --raw-output)
      echo; echo "Tạo xong $NAME. Public IP: $ip"; echo "Tiếp theo: bước 6 (DuckDNS) trong docs/setup/deploy-oracle-duckdns.md"
      exit 0
    fi
    if grep -qiE 'out of (host )?capacity' <<<"$out"; then
      echo "  hết máy, thử lại sau ${INTERVAL}s"
    elif grep -qiE 'TooManyRequests|429' <<<"$out"; then
      echo "  Oracle báo gửi quá nhiều yêu cầu, đợi 5 phút"; sleep 300
    else
      echo "$out" >&2; echo "Lỗi khác hết máy, dừng lại. Sửa theo thông báo ở trên rồi chạy lại." >&2
      exit 1
    fi
    sleep "$INTERVAL"
  done
done
