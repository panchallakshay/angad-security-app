#!/usr/bin/env bash
#
# Angad Production Go Blockchain — Comprehensive Endpoint & Feature Test Suite
#

set -euo pipefail
BC="http://127.0.0.1:8080"
PASS=0; FAIL=0; TOTAL=0

ok() {
  TOTAL=$((TOTAL+1)); PASS=$((PASS+1))
  printf "  ✅ [PASS] %s\n" "$1"
}
fail() {
  TOTAL=$((TOTAL+1)); FAIL=$((FAIL+1))
  printf "  ❌ [FAIL] %s — %s\n" "$1" "$2"
}

echo "=================================================================="
echo "  Angad Production Go Blockchain — Full Test Suite"
echo "  Node: $BC"
echo "  Time: $(date -Iseconds)"
echo "=================================================================="
echo ""

# ── 1. HEALTH & STATS ────────────────────────────────────────────────
echo "── 1. Health & Stats ──"

H=$(curl -s "$BC/health")
if echo "$H" | grep -q '"status"'; then ok "GET /health returns status"; else fail "GET /health" "$H"; fi

S=$(curl -s "$BC/stats")
if echo "$S" | grep -q '"blockCount"'; then ok "GET /stats returns blockCount"; else fail "GET /stats" "$S"; fi
if echo "$S" | grep -q '"mempoolSize"'; then ok "GET /stats returns mempoolSize"; else fail "GET /stats mempool" "$S"; fi
if echo "$S" | grep -q '"difficulty"'; then ok "GET /stats returns difficulty"; else fail "GET /stats difficulty" "$S"; fi

echo ""

# ── 2. GENESIS BLOCK ─────────────────────────────────────────────────
echo "── 2. Genesis Block Verification ──"

CHAIN=$(curl -s "$BC/chain")
GENESIS_IDX=$(echo "$CHAIN" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['index'])" 2>/dev/null || echo "err")
if [ "$GENESIS_IDX" = "0" ]; then ok "Genesis block exists at index 0"; else fail "Genesis block" "index=$GENESIS_IDX"; fi

GENESIS_PREV=$(echo "$CHAIN" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['prevHash'])" 2>/dev/null || echo "err")
if [ "$GENESIS_PREV" = "0000000000000000000000000000000000000000000000000000000000000000" ]; then
  ok "Genesis prevHash is all-zeros"
else fail "Genesis prevHash" "$GENESIS_PREV"; fi

GENESIS_MERKLE=$(echo "$CHAIN" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['merkleRoot'])" 2>/dev/null || echo "err")
if [ -n "$GENESIS_MERKLE" ] && [ "$GENESIS_MERKLE" != "err" ]; then
  ok "Genesis block has Merkle root ($GENESIS_MERKLE)"
else fail "Genesis Merkle root" "$GENESIS_MERKLE"; fi

echo ""

# ── 3. TRANSACTION SUBMISSION (MEMPOOL) ───────────────────────────────
echo "── 3. Transaction Submission to Mempool ──"

TX1=$(curl -s -X POST "$BC/tx" -H "Content-Type: application/json" \
  -d '{"domain":"evil-phish-site.xyz","category":"phishing","riskScore":0.92,"reportedBy":"ShieldNet"}')
if echo "$TX1" | grep -q '"added_to_mempool"'; then ok "TX1 added to mempool"; else fail "TX1 submit" "$TX1"; fi
TX1_ID=$(echo "$TX1" | python3 -c "import sys,json; print(json.load(sys.stdin).get('txId',''))" 2>/dev/null || echo "")

TX2=$(curl -s -X POST "$BC/tx" -H "Content-Type: application/json" \
  -d '{"domain":"malware-dropper.top","category":"malware","riskScore":0.88,"reportedBy":"NetGuard"}')
if echo "$TX2" | grep -q '"added_to_mempool"'; then ok "TX2 added to mempool"; else fail "TX2 submit" "$TX2"; fi

TX3=$(curl -s -X POST "$BC/tx" -H "Content-Type: application/json" \
  -d '{"domain":"data-exfil-bucket.cc","category":"data_leak","riskScore":0.75,"reportedBy":"NetGuard"}')
if echo "$TX3" | grep -q '"added_to_mempool"'; then ok "TX3 added to mempool"; else fail "TX3 submit" "$TX3"; fi

# Check mempool size
S2=$(curl -s "$BC/stats")
MPOOL=$(echo "$S2" | python3 -c "import sys,json; print(json.load(sys.stdin).get('mempoolSize',0))" 2>/dev/null || echo "0")
if [ "$MPOOL" = "3" ]; then ok "Mempool size is 3 after 3 submissions"; else fail "Mempool size" "expected 3, got $MPOOL"; fi

echo ""

# ── 4. INPUT VALIDATION ──────────────────────────────────────────────
echo "── 4. Transaction Input Validation ──"

BAD1=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BC/tx" -H "Content-Type: application/json" \
  -d '{"domain":"","category":"phishing","riskScore":0.5,"reportedBy":"test"}')
if [ "$BAD1" = "400" ]; then ok "Reject empty domain (HTTP $BAD1)"; else fail "Empty domain validation" "HTTP $BAD1"; fi

BAD2=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BC/tx" -H "Content-Type: application/json" \
  -d '{"domain":"test.com","category":"INVALID","riskScore":0.5,"reportedBy":"test"}')
if [ "$BAD2" = "400" ]; then ok "Reject invalid category (HTTP $BAD2)"; else fail "Category validation" "HTTP $BAD2"; fi

BAD3=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BC/tx" -H "Content-Type: application/json" \
  -d '{"domain":"test.com","category":"phishing","riskScore":1.5,"reportedBy":"test"}')
if [ "$BAD3" = "400" ]; then ok "Reject riskScore > 1.0 (HTTP $BAD3)"; else fail "RiskScore validation" "HTTP $BAD3"; fi

BAD4=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BC/tx" -H "Content-Type: application/json" \
  -d '{"domain":"test.com","category":"phishing","riskScore":-0.1,"reportedBy":"test"}')
if [ "$BAD4" = "400" ]; then ok "Reject riskScore < 0.0 (HTTP $BAD4)"; else fail "Negative riskScore" "HTTP $BAD4"; fi

BAD5=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BC/tx" -H "Content-Type: application/json" \
  -d '{"domain":"test.com","category":"scam","riskScore":0.5,"reportedBy":""}')
if [ "$BAD5" = "400" ]; then ok "Reject empty reportedBy (HTTP $BAD5)"; else fail "ReportedBy validation" "HTTP $BAD5"; fi

echo ""

# ── 5. MINING ─────────────────────────────────────────────────────────
echo "── 5. Block Mining (PoW) ──"

MINE=$(curl -s -X POST "$BC/mine" -H "Content-Type: application/json" -d '{}')
MINED_IDX=$(echo "$MINE" | python3 -c "import sys,json; print(json.load(sys.stdin)['block']['index'])" 2>/dev/null || echo "err")
if [ "$MINED_IDX" = "1" ]; then ok "Block #1 mined successfully"; else fail "Block mining" "index=$MINED_IDX"; fi

MINED_HASH=$(echo "$MINE" | python3 -c "import sys,json; print(json.load(sys.stdin)['block']['hash'])" 2>/dev/null || echo "err")
MINED_NONCE=$(echo "$MINE" | python3 -c "import sys,json; print(json.load(sys.stdin)['block']['nonce'])" 2>/dev/null || echo "err")
if echo "$MINED_HASH" | grep -qE "^00"; then ok "Block hash satisfies PoW difficulty (nonce=$MINED_NONCE)"; else fail "PoW hash" "$MINED_HASH"; fi

MINED_MERKLE=$(echo "$MINE" | python3 -c "import sys,json; print(json.load(sys.stdin)['block']['merkleRoot'])" 2>/dev/null || echo "err")
if [ -n "$MINED_MERKLE" ] && [ "$MINED_MERKLE" != "err" ] && [ "$MINED_MERKLE" != "" ]; then
  ok "Block #1 has Merkle root"
else fail "Block Merkle root" "$MINED_MERKLE"; fi

TX_COUNT=$(echo "$MINE" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['block']['transactions']))" 2>/dev/null || echo "0")
if [ "$TX_COUNT" = "3" ]; then ok "Block #1 contains all 3 mempool transactions"; else fail "TX count in block" "expected 3, got $TX_COUNT"; fi

# Verify mempool is drained
S3=$(curl -s "$BC/stats")
MPOOL2=$(echo "$S3" | python3 -c "import sys,json; print(json.load(sys.stdin).get('mempoolSize',0))" 2>/dev/null || echo "err")
if [ "$MPOOL2" = "0" ]; then ok "Mempool drained after mining"; else fail "Mempool drain" "size=$MPOOL2"; fi

echo ""

# ── 6. O(1) DOMAIN VERIFICATION ──────────────────────────────────────
echo "── 6. Domain Verification (O(1) Index Lookup) ──"

V1=$(curl -s "$BC/verify?domain=evil-phish-site.xyz")
if echo "$V1" | grep -q '"found":true'; then ok "evil-phish-site.xyz found on-chain"; else fail "Domain verify" "$V1"; fi
if echo "$V1" | grep -q '"blacklisted_on_blockchain"'; then ok "Status is blacklisted_on_blockchain"; else fail "Blacklist status" "$V1"; fi

V2=$(curl -s "$BC/verify?domain=google.com")
if echo "$V2" | grep -q '"found":false'; then ok "google.com NOT found (clean)"; else fail "Clean domain" "$V2"; fi

V3=$(curl -s "$BC/verify?domain=malware-dropper.top")
if echo "$V3" | grep -q '"found":true'; then ok "malware-dropper.top found on-chain"; else fail "Domain verify malware" "$V3"; fi

echo ""

# ── 7. CHAIN VALIDATION ──────────────────────────────────────────────
echo "── 7. Full Chain Integrity Validation ──"

VAL=$(curl -s "$BC/validate")
if echo "$VAL" | grep -q '"valid":true'; then
  ok "Chain integrity validation PASSED"
else
  fail "Chain validation" "$VAL"
fi

echo ""

# ── 8. PAGINATION ─────────────────────────────────────────────────────
echo "── 8. Chain Pagination ──"

PAGE=$(curl -s "$BC/chain?from=0&to=1")
PLEN=$(echo "$PAGE" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "err")
if [ "$PLEN" = "1" ]; then ok "Pagination from=0&to=1 returns 1 block"; else fail "Pagination" "expected 1, got $PLEN"; fi

echo ""

# ── 9. PERSISTENCE ────────────────────────────────────────────────────
echo "── 9. Persistence (chain.json) ──"

if [ -f "/home/sonu/lakshay/blockchain_node/chain.json" ]; then
  FSIZE=$(stat -c %s /home/sonu/lakshay/blockchain_node/chain.json)
  ok "chain.json exists ($FSIZE bytes)"
  FBLOCKS=$(python3 -c "import json; print(len(json.load(open('/home/sonu/lakshay/blockchain_node/chain.json'))))" 2>/dev/null || echo "err")
  if [ "$FBLOCKS" = "2" ]; then ok "chain.json contains 2 blocks (genesis + mined)"; else fail "Persistence block count" "$FBLOCKS"; fi
else
  fail "Persistence" "chain.json not found"
fi

echo ""

# ── 10. RATE LIMITING ─────────────────────────────────────────────────
echo "── 10. Rate Limiting ──"

RATE_BLOCKED=0
for i in $(seq 1 12); do
  RS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BC/tx" -H "Content-Type: application/json" \
    -d '{"domain":"ratetest.com","category":"scam","riskScore":0.5,"reportedBy":"test"}')
  if [ "$RS" = "429" ]; then RATE_BLOCKED=$((RATE_BLOCKED+1)); fi
done
if [ "$RATE_BLOCKED" -gt 0 ]; then
  ok "Rate limiting triggered ($RATE_BLOCKED of 12 requests got HTTP 429)"
else
  fail "Rate limiting" "No 429 responses in 12 rapid requests"
fi

echo ""

# ── 11. BACKWARD COMPATIBILITY (mine with inline TX) ──────────────────
echo "── 11. Backward Compatibility (inline mine) ──"

MINE2=$(curl -s -X POST "$BC/mine" -H "Content-Type: application/json" \
  -d '{"domain":"compat-test-scam.site","category":"scam","riskScore":0.81,"reportedBy":"LiteRT-ShieldNet-AutoMine"}')
MINE2_STATUS=$(echo "$MINE2" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null || echo "err")
if [ "$MINE2_STATUS" = "mined" ]; then ok "Backward-compat inline mine succeeded"; else fail "Inline mine" "$MINE2"; fi

V4=$(curl -s "$BC/verify?domain=compat-test-scam.site")
if echo "$V4" | grep -q '"found":true'; then ok "Inline-mined domain found on-chain"; else fail "Inline mine verify" "$V4"; fi

echo ""

# ── 12. HMAC SIGNATURES ──────────────────────────────────────────────
echo "── 12. HMAC Signature Verification ──"

FULL_CHAIN=$(curl -s "$BC/chain")
SIG_CHECK=$(echo "$FULL_CHAIN" | python3 -c "
import sys, json
chain = json.load(sys.stdin)
for b in chain:
    for tx in b.get('transactions', []):
        if not tx.get('signature'):
            print('MISSING')
            sys.exit(0)
        if not tx.get('txId'):
            print('MISSING_TXID')
            sys.exit(0)
print('ALL_SIGNED')
" 2>/dev/null || echo "err")
if [ "$SIG_CHECK" = "ALL_SIGNED" ]; then ok "All transactions have HMAC signatures and TxIDs"; else fail "HMAC signatures" "$SIG_CHECK"; fi

echo ""

# ── 13. FINAL STATS ──────────────────────────────────────────────────
echo "── 13. Final Chain Statistics ──"

FINAL=$(curl -s "$BC/stats")
echo "  $FINAL" | python3 -m json.tool 2>/dev/null || echo "  $FINAL"

echo ""
echo "=================================================================="
echo "  SUMMARY"
echo "  Total: $TOTAL | Passed: $PASS | Failed: $FAIL"
if [ "$TOTAL" -gt 0 ]; then
  echo "  Pass Rate: $(echo "scale=1; $PASS * 100 / $TOTAL" | bc)%"
fi
echo "=================================================================="
