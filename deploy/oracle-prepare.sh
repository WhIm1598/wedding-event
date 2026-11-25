#!/usr/bin/env bash
# Run in Oracle Cloud Shell BEFORE oracle-create-vm.sh (docs/setup/deploy-oracle-duckdns.md steps 3-5).
#   bash oracle-prepare.sh
# Makes sure there is a VCN with a public subnet, a route to the Internet, ports 22/80/443 open in the subnet's
# Security List, and enough Always Free A1 quota left for the VM; then prints the oracle-create-vm.sh command.
# Safe to run again: it reuses what exists and only adds what is missing.
# Optional env: VCN_NAME (learn-automation-vcn if it exists, else wedding-vcn), NAME (wedding-demo), OCPUS (1), MEM (6),
#   FREE_OCPUS (2), FREE_MEM (12): the Always Free A1 quota (11/2026).
set -euo pipefail

C=${OCI_TENANCY:?Chạy script trong Oracle Cloud Shell}   # root compartment
NAME=${NAME:-wedding-demo} OCPUS=${OCPUS:-1} MEM=${MEM:-6} FREE_OCPUS=${FREE_OCPUS:-2} FREE_MEM=${FREE_MEM:-12}
SHAPE=VM.Standard.A1.Flex
PORTS='[22,80,443]'
step() { echo; echo "==> $*"; }
none() { [[ -z $1 || $1 == null ]]; }
# `get` returns kebab-case keys, `update` wants camelCase
CAMEL='def camel: walk(if type == "object" then with_entries(.key |= gsub("-(?<c>[a-z])"; .c | ascii_upcase)) else . end);'
vcn_id() {
  oci network vcn list -c "$C" --all --display-name "$1" --lifecycle-state AVAILABLE --query 'data[0].id' --raw-output 2>/dev/null || true
}

step "VCN"
if [[ -n ${VCN_NAME:-} ]]; then
  VCN=$(vcn_id "$VCN_NAME")
else
  for VCN_NAME in learn-automation-vcn wedding-vcn; do VCN=$(vcn_id "$VCN_NAME"); none "$VCN" || break; done
fi
if none "$VCN"; then
  echo "Chưa có VCN $VCN_NAME, tạo mới (10.0.0.0/16)."
  VCN=$(oci network vcn create -c "$C" --display-name "$VCN_NAME" --cidr-blocks '["10.0.0.0/16"]' \
    --wait-for-state AVAILABLE --query 'data.id' --raw-output)
fi
echo "$VCN_NAME: $VCN"

step "Subnet public"
SUBNETS=$(oci network subnet list -c "$C" --vcn-id "$VCN" --all --lifecycle-state AVAILABLE \
  --query 'data[?"prohibit-public-ip-on-vnic"==`false`]' 2>/dev/null || true)
[[ -n $SUBNETS ]] || SUBNETS='[]'
SUBNET_NAME="public subnet-$VCN_NAME"
SUBNET_JSON=$(jq -c --arg n "$SUBNET_NAME" '(map(select(."display-name" == $n)) + .)[0]' <<<"$SUBNETS")
if none "$SUBNET_JSON"; then
  echo "Chưa có subnet public, tạo $SUBNET_NAME (10.0.0.0/24)."
  SUBNET_JSON=$(oci network subnet create -c "$C" --vcn-id "$VCN" --display-name "$SUBNET_NAME" \
    --cidr-block 10.0.0.0/24 --prohibit-public-ip-on-vnic false --wait-for-state AVAILABLE --query 'data')
fi
SUBNET_NAME=$(jq -r '."display-name"' <<<"$SUBNET_JSON")
RT=$(jq -r '."route-table-id"' <<<"$SUBNET_JSON")
SL=$(jq -r '."security-list-ids"[0]' <<<"$SUBNET_JSON")
echo "$SUBNET_NAME ($(jq -r '."cidr-block"' <<<"$SUBNET_JSON"))"

step "Đường ra Internet (Internet Gateway + route 0.0.0.0/0)"
ROUTES=$(oci network route-table get --rt-id "$RT" --query 'data."route-rules"')
if jq -e 'any(.[]; .destination == "0.0.0.0/0")' <<<"$ROUTES" >/dev/null; then
  echo "Đã có route 0.0.0.0/0."
else
  IGW=$(oci network internet-gateway list -c "$C" --vcn-id "$VCN" --all --query 'data[0].id' --raw-output 2>/dev/null || true)
  if none "$IGW"; then
    IGW=$(oci network internet-gateway create -c "$C" --vcn-id "$VCN" --is-enabled true \
      --display-name "Internet Gateway-$VCN_NAME" --wait-for-state AVAILABLE --query 'data.id' --raw-output)
  fi
  NEW=$(jq -c --arg igw "$IGW" "$CAMEL"' camel + [{destination: "0.0.0.0/0", destinationType: "CIDR_BLOCK", networkEntityId: $igw}]' <<<"$ROUTES")
  oci network route-table update --rt-id "$RT" --route-rules "$NEW" --force --wait-for-state AVAILABLE >/dev/null
  echo "Đã thêm route 0.0.0.0/0 qua Internet Gateway."
fi

step "Security List: mở cổng 22, 80, 443"
RULES=$(oci network security-list get --security-list-id "$SL" --query 'data."ingress-security-rules"')
# A port counts as open when a rule from 0.0.0.0/0 allows all protocols, or TCP on a range (or all ports) covering it
MISSING=$(jq -c --argjson ports "$PORTS" '
  def opens($p): .source == "0.0.0.0/0"
    and (.protocol == "all" or (.protocol == "6"
      and (."tcp-options"."destination-port-range" as $d | $d == null or ($d.min <= $p and $d.max >= $p))));
  . as $r | [$ports[] | . as $p | select(any($r[]; opens($p)) | not)]' <<<"$RULES")
if [[ $MISSING == '[]' ]]; then
  echo "Đã mở đủ."
else
  NEW=$(jq -c --argjson add "$MISSING" "$CAMEL"' camel + [$add[] | {protocol: "6", source: "0.0.0.0/0", sourceType: "CIDR_BLOCK",
    isStateless: false, description: "TCP \(.)", tcpOptions: {destinationPortRange: {min: ., max: .}}}]' <<<"$RULES")
  oci network security-list update --security-list-id "$SL" --ingress-security-rules "$NEW" --force \
    --wait-for-state AVAILABLE >/dev/null
  echo "Đã mở thêm cổng: $(jq -r 'join(", ")' <<<"$MISSING")"
fi

step "Hạn mức A1 miễn phí (tổng $FREE_OCPUS OCPU / $FREE_MEM GB)"
A1=$(oci compute instance list -c "$C" --all \
  --query "data[?shape=='$SHAPE' && \"lifecycle-state\"!='TERMINATED']" 2>/dev/null || true)
[[ -n $A1 ]] || A1='[]'
jq -r '.[] | "  \(."display-name"): \(."shape-config".ocpus) OCPU / \(."shape-config"."memory-in-gbs") GB (\(."lifecycle-state"))"' <<<"$A1"
if jq -e --arg n "$NAME" 'any(.[]; ."display-name" == $n)' <<<"$A1" >/dev/null; then
  echo "Đã có máy $NAME, oracle-create-vm.sh sẽ không tạo thêm."
else
  read -r USED_OCPUS USED_MEM < <(jq -r '[(map(."shape-config".ocpus) | add // 0), (map(."shape-config"."memory-in-gbs") | add // 0)] | @tsv' <<<"$A1")
  echo "Đã dùng $USED_OCPUS OCPU / $USED_MEM GB, máy $NAME cần $OCPUS OCPU / $MEM GB."
  if ! awk -v uo="$USED_OCPUS" -v um="$USED_MEM" -v o="$OCPUS" -v m="$MEM" -v fo="$FREE_OCPUS" -v fm="$FREE_MEM" \
      'BEGIN { exit !(uo + o <= fo && um + m <= fm) }'; then
    echo "Không đủ hạn mức miễn phí: đổi máy khác xuống nhỏ hơn (Edit → Shape), hoặc giảm OCPUS/MEM, rồi chạy lại." >&2
    exit 1
  fi
  echo "Đủ hạn mức."
fi

echo
echo "=================================================================="
echo " Mạng sẵn sàng: $VCN_NAME / $SUBNET_NAME"
echo " Tiếp theo (bước 5), trong Cloud Shell:"
echo "   SUBNET_NAME=\"$SUBNET_NAME\" NAME=$NAME OCPUS=$OCPUS MEM=$MEM bash oracle-create-vm.sh oracle_demo.pub"
echo "=================================================================="
