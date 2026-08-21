# 🔥 Shakti X 3.0 — Final Real-Time Architecture

## System Overview

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                           ANDROID APPLICATION (Chaquopy)                         │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐              │
│  │   SHIZUKU API   │────│  Shell Commands │────│   Privileged    │              │
│  │   (ADB Shell)   │    │   (ss, iptables)│    │   Operations    │              │
│  └────────┬────────┘    └─────────────────┘    └─────────────────┘              │
│           │                                                                      │
│  ═════════╪══════════════════════════════════════════════════════════════════   │
│           ▼                                                                      │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                      LAYER 1: NETWORK OBSERVER                           │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────┐   │   │
│  │  │   Netlink Socket   │  │   SS Command       │  │  Package Resolver│   │   │
│  │  │   INET_DIAG        │  │   Fallback         │  │  UID → App Name  │   │   │
│  │  │   Real-time events │  │   Full state dump  │  │  /packages.list  │   │   │
│  │  └─────────┬──────────┘  └─────────┬──────────┘  └────────┬─────────┘   │   │
│  │            └──────────────┬────────┘                      │             │   │
│  │                           ▼                               │             │   │
│  │  Output: {uid, src_ip, dst_ip, src_port, dst_port, proto, pkg_name}    │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                              │                                                   │
│                              ▼                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                   LAYER 1b: TRANSPARENT PROXY                            │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────┐   │   │
│  │  │  iptables REDIRECT │  │  TLS ClientHello   │  │  JA3/JA4         │   │   │
│  │  │  :80/:443 → :8888  │  │  SNI Extraction    │  │  Fingerprinting  │   │   │
│  │  │  via Shizuku       │  │  Real parsing      │  │  MD5 hash        │   │   │
│  │  └────────────────────┘  └─────────┬──────────┘  └────────┬─────────┘   │   │
│  │                                    │                      │             │   │
│  │  Output: {sni: "domain.com", ja3: "abc123...", tls_ver: "1.3"}         │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                              │                                                   │
│                              ▼                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                    LAYER 2: FEATURE EXTRACTOR                            │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐        │   │
│  │  │ DNS (11)    │ │ Flow (12)   │ │ App (5)     │ │ TLS (8)     │        │   │
│  │  │ • Length    │ │ • Bytes/sec │ │ • Perms     │ │ • JA3 hash  │        │   │
│  │  │ • Entropy   │ │ • Pkts/sec  │ │ • Age       │ │ • TLS ver   │        │   │
│  │  │ • TLD risk  │ │ • Duration  │ │ • System?   │ │ • Cert len  │        │   │
│  │  │ • Punycode  │ │ • Ratio TX  │ │ • Dangerous │ │ • SNI match │        │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘        │   │
│  │                                                                          │   │
│  │  ┌─────────────┐  ┌──────────────────────────────────────────────────┐  │   │
│  │  │ Temporal(6) │  │           FLOW TRACKER (Real-time)               │  │   │
│  │  │ • Hour      │  │  • Per-connection byte counters                  │  │   │
│  │  │ • Weekday   │  │  • Sliding window packet rates                   │  │   │
│  │  │ • Burst     │  │  • Connection lifetime tracking                  │  │   │
│  │  └─────────────┘  └──────────────────────────────────────────────────┘  │   │
│  │                                                                          │   │
│  │  Output: 42-dimensional float vector [f0, f1, f2, ..., f41]             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                              │                                                   │
│                              ▼                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                    LAYER 3: HYBRID AI ENGINE                             │   │
│  │                                                                          │   │
│  │  ══════════════════════════════════════════════════════════════════════ │   │
│  │                         TIER 1: FAST FILTER (~5ms)                       │   │
│  │  ══════════════════════════════════════════════════════════════════════ │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────┐   │   │
│  │  │  Isolation Forest  │  │  Whitelist Rules   │  │  Known Bad IPs   │   │   │
│  │  │  scikit-learn      │  │  Port 80/443/53    │  │  Threat Intel    │   │   │
│  │  │  Anomaly: -1/+1    │  │  System apps       │  │  URLhaus, etc.   │   │   │
│  │  └─────────┬──────────┘  └─────────┬──────────┘  └────────┬─────────┘   │   │
│  │            │                       │                      │             │   │
│  │            └───────────────────────┼──────────────────────┘             │   │
│  │                                    ▼                                    │   │
│  │                    ┌───────────────────────────────┐                    │   │
│  │                    │  95% Traffic → ALLOW (safe)   │                    │   │
│  │                    │   5% Traffic → TIER 2 (deep)  │                    │   │
│  │                    └───────────────┬───────────────┘                    │   │
│  │                                    ▼                                    │   │
│  │  ══════════════════════════════════════════════════════════════════════ │   │
│  │                      TIER 2: DEEP ANALYSIS (~50ms)                       │   │
│  │  ══════════════════════════════════════════════════════════════════════ │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────┐   │   │
│  │  │  LSTM Autoencoder  │  │  Transformer       │  │  Graph Neural    │   │
│  │  │  TFLite + NNAPI    │  │  Classifier        │  │  Network (GNN)   │   │
│  │  │  Sequence anomaly  │  │  Attack type       │  │  Multi-app collab│   │
│  │  │  Beaconing detect  │  │  DDoS/Exfil/C2     │  │  detection       │   │
│  │  └─────────┬──────────┘  └─────────┬──────────┘  └────────┬─────────┘   │   │
│  │            └───────────────────────┼──────────────────────┘             │   │
│  │                                    ▼                                    │   │
│  │  Output: {risk_score: 0.0-1.0, classification: "DGA_THREAT",            │   │
│  │           confidence: 0.97, model_votes: [0.95, 0.98, 0.91]}            │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                              │                                                   │
│                              ▼                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                    LAYER 4: DECISION ENGINE                              │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────┐   │   │
│  │  │  App Profiler      │  │  Strike System     │  │  Adaptive        │   │
│  │  │  Normal baseline   │  │  5 warnings = ban  │  │  Thresholds      │   │
│  │  │  Per-app history   │  │  Forgiveness decay │  │  Per-app tuned   │   │
│  │  └─────────┬──────────┘  └─────────┬──────────┘  └────────┬─────────┘   │   │
│  │            │                       │                      │             │   │
│  │  ┌─────────┴───────────────────────┴──────────────────────┴─────────┐   │   │
│  │  │                    REPUTATION DATABASE (SQLite)                  │   │   │
│  │  │  • Per-app risk history        • Behavioral baselines            │   │   │
│  │  │  • Strike counts               • User overrides (trust/block)    │   │   │
│  │  │  • Last seen timestamps        • Domain reputation cache         │   │   │
│  │  └──────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                          │   │
│  │  Output: {action: "BLOCK"|"WARN"|"ALLOW", reason: "...", uid: 10045}    │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                              │                                                   │
│                              ▼                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                   LAYER 4b: ENFORCEMENT ENGINE                           │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────┐   │   │
│  │  │  Block IP          │  │  Quarantine App    │  │  Kill Connection │   │   │
│  │  │  iptables -A       │  │  --uid-owner DROP  │  │  TCP RST inject  │   │   │
│  │  │  SHAKTI_X -d IP    │  │  Full app lockdown │  │  Instant cutoff  │   │   │
│  │  └────────────────────┘  └────────────────────┘  └──────────────────┘   │   │
│  │                                                                          │   │
│  │  Commands executed via Shizuku (ADB shell privileges)                    │   │
│  │  Example: iptables -A SHAKTI_X -m owner --uid-owner 10045 -j DROP        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                              │                                                   │
│                              ▼                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                    LAYER 5: DASHBOARD & API                              │   │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────┐   │   │
│  │  │  WebSocket :8765   │  │  REST API :8080    │  │  Android UI      │   │   │
│  │  │  Live threat feed  │  │  /api/status       │  │  Jetpack Compose │   │   │
│  │  │  Real-time stats   │  │  /api/trust/{uid}  │  │  Material 3      │   │   │
│  │  │  Connection log    │  │  /api/block/{uid}  │  │                  │   │   │
│  │  └────────────────────┘  └────────────────────┘  └──────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow (Single Packet Journey)

```
1. App "TikTok" (UID 10045) opens socket to 157.240.1.35:443
                              │
                              ▼
2. ┌─────────────────────────────────────────────────────────────┐
   │ LAYER 1: Netlink receives INET_DIAG event                  │
   │ → Extracts: UID=10045, dst=157.240.1.35:443, proto=TCP     │
   │ → Resolves: UID 10045 = "com.zhiliaoapp.musically"         │
   └─────────────────────────────────────────────────────────────┘
                              │
                              ▼
3. ┌─────────────────────────────────────────────────────────────┐
   │ LAYER 1b: iptables redirects :443 → localhost:8888         │
   │ → Proxy receives TLS ClientHello                           │
   │ → Extracts SNI: "api-va.tiktokv.com"                       │
   │ → Computes JA3: "e7d705a3286e19ea42f587b344ee6865"         │
   └─────────────────────────────────────────────────────────────┘
                              │
                              ▼
4. ┌─────────────────────────────────────────────────────────────┐
   │ LAYER 2: Build 42-feature vector                           │
   │ → DNS: len=18, entropy=3.2, dots=2                         │
   │ → Flow: 1.2KB/s, 15 pkts/s, ratio=0.3                      │
   │ → App: perms=24, age=365 days, system=false                │
   │ → TLS: ja3_known=true, tls=1.3, sni_match=true             │
   │ → Output: [18, 2, 3.2, 0, 0.1, 0.4, 0.2, ...]              │
   └─────────────────────────────────────────────────────────────┘
                              │
                              ▼
5. ┌─────────────────────────────────────────────────────────────┐
   │ LAYER 3 TIER 1: Fast Filter                                │
   │ → Port 443 = common web (whitelist hit)                    │
   │ → IsolationForest score = +0.8 (normal)                    │
   │ → Decision: SAFE, skip Tier 2                              │
   └─────────────────────────────────────────────────────────────┘
                              │
                              ▼
6. ┌─────────────────────────────────────────────────────────────┐
   │ LAYER 4: Decision                                          │
   │ → App baseline: TikTok normally uses 443, this is normal   │
   │ → Risk score: 0.12 (well below 0.55 warning threshold)     │
   │ → Action: ALLOW                                            │
   └─────────────────────────────────────────────────────────────┘
                              │
                              ▼
7. ┌─────────────────────────────────────────────────────────────┐
   │ LAYER 5: Dashboard                                         │
   │ → WebSocket emit: {"app": "TikTok", "action": "ALLOW"}     │
   │ → UI shows green checkmark                                 │
   └─────────────────────────────────────────────────────────────┘
```

---

## Threat Detection Flow (Malware Example)

```
1. Malware "flashlight.apk" (UID 10099) connects to 45.33.32.156:8443
                              │
                              ▼
2. LAYER 1: UID=10099, dst=45.33.32.156:8443 (unusual port!)
   → Package: "com.free.flashlight.torch" (installed 2 days ago)
                              │
                              ▼
3. LAYER 1b: SNI = "xn--80ak6aa92e.com" (punycode = suspicious!)
   → JA3 = "abc123..." matches known TrickBot fingerprint!
                              │
                              ▼
4. LAYER 2: Feature vector shows anomalies
   → DNS entropy: 4.8 (very high - DGA pattern)
   → App age: 2 days (brand new)
   → Permissions: 28 (excessive for flashlight)
   → Port: 8443 (non-standard HTTPS)
                              │
                              ▼
5. LAYER 3 TIER 1: Anomaly detected!
   → IsolationForest score = -0.7 (ANOMALY)
   → JA3 matches threat intel database
   → Escalate to TIER 2
                              │
                              ▼
6. LAYER 3 TIER 2: Deep analysis (50ms on NPU)
   → LSTM: Beaconing pattern detected (connects every 60s)
   → Transformer: Classification = "C2_COMMUNICATION" (98% confidence)
   → GNN: No multi-app collaboration detected
   → Final risk score: 0.94
                              │
                              ▼
7. LAYER 4: Decision
   → Risk 0.94 > 0.90 instant block threshold
   → Action: BLOCK
   → Reason: "C2 communication detected (TrickBot fingerprint)"
                              │
                              ▼
8. LAYER 4b: Enforcement (via Shizuku)
   → Execute: iptables -A SHAKTI_X -m owner --uid-owner 10099 -j DROP
   → All traffic from malware app is now blocked at kernel level
                              │
                              ▼
9. LAYER 5: Dashboard
   → WebSocket emit: {"app": "Flashlight", "action": "BLOCK", "threat": "C2"}
   → UI shows red alert with "BLOCKED: Malware detected"
   → User notification: "Shakti X blocked suspicious app"
```

---

## File Structure (Final)

```
shakti_x/
├── __init__.py
├── main.py                          # Main orchestrator loop
│
├── common/
│   ├── __init__.py
│   ├── shizuku.py                   # Privileged command execution
│   ├── database.py                  # SQLite reputation & config DB
│   └── constants.py                 # Thresholds, ports, paths
│
├── layer1_observer/
│   ├── __init__.py
│   ├── observer.py                  # Main observer class
│   ├── netlink_parser.py            # INET_DIAG protocol parsing
│   └── package_resolver.py          # UID → package name mapping
│
├── layer1b_proxy/
│   ├── __init__.py
│   ├── proxy.py                     # Transparent proxy server
│   └── tls_parser.py                # SNI extraction & JA3 fingerprinting
│
├── layer2_extractor/
│   ├── __init__.py
│   ├── extractor.py                 # 42-feature vector builder
│   └── flow_tracker.py              # Real-time bytes/packets tracking
│
├── layer3_ai/
│   ├── __init__.py
│   ├── engine.py                    # Hybrid AI orchestrator
│   ├── isolation_forest.py          # Tier 1: Fast anomaly detection
│   ├── deep_classifier.py           # Tier 2: TFLite models
│   └── models/
│       ├── isolation_forest.joblib  # Trained sklearn model
│       ├── lstm_autoencoder.tflite  # Sequence anomaly detection
│       └── threat_classifier.tflite # Attack classification
│
├── layer4_decision/
│   ├── __init__.py
│   ├── engine.py                    # Decision fusion & thresholds
│   └── profiler.py                  # Per-app behavioral baselines
│
├── layer4b_enforcement/
│   ├── __init__.py
│   └── enforcer.py                  # iptables rule execution
│
└── layer5_dashboard/
    ├── __init__.py
    ├── server.py                    # Flask + SocketIO server
    └── index.html                   # Live monitoring UI
```

---

## AI Model Accuracy Targets

| Model | Type | Target Accuracy | False Positive Rate | Latency |
|-------|------|-----------------|---------------------|---------|
| Isolation Forest | Anomaly | 90% | <5% | <5ms |
| LSTM Autoencoder | Sequence | 94% | <3% | ~30ms |
| Threat Classifier | Multi-class | 97% | <2% | ~20ms |
| **Ensemble (Combined)** | **Hybrid** | **98%+** | **<1%** | **~50ms** |

---

## Required Dependencies

```python
# requirements.txt
psutil>=5.9.0           # Cross-platform network monitoring
flask>=2.0.0            # REST API
flask-socketio>=5.0.0   # WebSocket support
scikit-learn>=1.0.0     # Isolation Forest
numpy>=1.21.0           # Numerical operations
joblib>=1.0.0           # Model serialization

# Android-specific (Chaquopy)
# tflite-runtime         # TensorFlow Lite inference
# nnapi-delegate         # NPU acceleration
```

---

## Security Considerations

1. **Shizuku permissions**: Only request minimal shell access needed
2. **No root required**: Shizuku provides ADB-level access without root
3. **Local-only**: All AI inference happens on-device, no cloud
4. **Encrypted DB**: SQLCipher for reputation database
5. **No data exfiltration**: Firewall never sends user data anywhere
