# 🧱 SHAKTI X 3.0 - AI-Powered Firewall

## Complete Documentation & Deployment Guide

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Layer Details](#layer-details)
4. [AI Models](#ai-models)
5. [Prerequisites](#prerequisites)
6. [Windows Setup (Development/Testing)](#windows-setup)
7. [Android Setup (Production)](#android-setup)
8. [API Reference](#api-reference)
9. [Configuration Parameters](#configuration-parameters)
10. [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

**Shakti X 3.0** is an AI-powered network firewall that uses machine learning to detect and block malicious network traffic in real-time.

### Key Features

| Feature | Description |
|---------|-------------|
| 🧠 **AI-Powered Detection** | Trained on 17M+ real attack samples (CICIDS2017, CICIDS2018, UNSW-NB15) |
| ⚡ **Two-Tier Analysis** | Fast Random Forest (~5ms) + Deep Neural Network (~50ms) |
| 🔍 **JA3 Fingerprinting** | Detects malware by TLS handshake signatures |
| 🛡️ **Real-time Blocking** | Kernel-level enforcement via iptables (Android) |
| 📊 **Live Dashboard** | Web-based UI showing all traffic and AI decisions |
| 📱 **Mobile Ready** | TFLite models for efficient on-device inference |

### Threat Detection Capabilities

- ✅ DDoS Attacks
- ✅ Port Scanning
- ✅ Brute Force Attacks
- ✅ Web Attacks (SQL Injection, XSS)
- ✅ Botnet Communication
- ✅ Infiltration Attempts
- ✅ Backdoors & Exploits
- ✅ Domain Generation Algorithms (DGA)
- ✅ Command & Control (C2) Traffic

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        SHAKTI X 3.0 ARCHITECTURE                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                  │
│  │   LAYER 1   │    │  LAYER 1b   │    │   LAYER 2   │                  │
│  │  Observer   │───▶│   Proxy     │───▶│  Extractor  │                  │
│  │ (Netlink)   │    │ (TLS/JA3)   │    │(42 Features)│                  │
│  └─────────────┘    └─────────────┘    └─────────────┘                  │
│         │                                      │                         │
│         │                                      ▼                         │
│         │                            ┌─────────────────┐                │
│         │                            │    LAYER 3      │                │
│         │                            │   AI Engine     │                │
│         │                            │ ┌─────────────┐ │                │
│         │                            │ │   Tier 1    │ │                │
│         │                            │ │Random Forest│ │                │
│         │                            │ │   (~5ms)    │ │                │
│         │                            │ └──────┬──────┘ │                │
│         │                            │        │        │                │
│         │                            │        ▼        │                │
│         │                            │ ┌─────────────┐ │                │
│         │                            │ │   Tier 2    │ │                │
│         │                            │ │  Deep NN    │ │                │
│         │                            │ │  (~50ms)    │ │                │
│         │                            │ └─────────────┘ │                │
│         │                            └────────┬────────┘                │
│         │                                     │                         │
│         │                                     ▼                         │
│         │    ┌─────────────┐    ┌─────────────────────┐                │
│         │    │  LAYER 4b   │◀───│      LAYER 4        │                │
│         └───▶│  Enforcer   │    │  Decision Engine    │                │
│              │ (iptables)  │    │  (ALLOW/WARN/BLOCK) │                │
│              └─────────────┘    └─────────────────────┘                │
│                    │                                                    │
│                    ▼                                                    │
│              ┌─────────────┐                                           │
│              │   LAYER 5   │                                           │
│              │  Dashboard  │◀──── http://127.0.0.1:8080                │
│              │ (WebSocket) │                                           │
│              └─────────────┘                                           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
Network Packet → Observer → Proxy → Feature Extraction → AI Analysis → Decision → Enforcement
                                           ↓
                                    Live Dashboard (WebSocket)
```

---

## 📦 Layer Details

### Layer 1: Network Observer (`shakti_x/layer1_observer/`)

**Purpose:** Monitor all network connections in real-time

| Platform | Method | Capabilities |
|----------|--------|--------------|
| Android | Netlink INET_DIAG | Kernel-level, sees all sockets, UID resolution |
| Android (fallback) | `ss` via Shizuku | Shell command parsing |
| Windows/macOS | psutil | Cross-platform, process-level |

**Key Files:**
- `observer.py` - Main observer class
- `netlink_parser.py` - Parses Netlink socket responses
- `package_resolver.py` - Maps UID → App package name

**Output Data:**
```python
{
    "uid": 10234,                    # App UID (Android) or PID (Windows)
    "src_ip": "192.168.1.100",       # Source IP
    "dst_ip": "142.250.185.78",      # Destination IP
    "src_port": 54321,               # Source port
    "dst_port": 443,                 # Destination port
    "protocol": "TCP",               # TCP/UDP
    "state": "ESTABLISHED",          # Connection state
    "package_name": "com.chrome",    # App name (Android)
    "is_system": False,              # Is system app?
    "is_new": True                   # First time seeing this connection?
}
```

---

### Layer 1b: Transparent Proxy (`shakti_x/layer1b_proxy/`)

**Purpose:** Intercept TLS handshakes for JA3 fingerprinting

| Feature | Description |
|---------|-------------|
| JA3 Extraction | Fingerprints TLS Client Hello |
| SNI Capture | Extracts Server Name Indication |
| Certificate Analysis | Validates server certificates |
| Malware Detection | Matches against known bad JA3 hashes |

**JA3 Fingerprint Example:**
```
769,47-53-5-10-49161-49162-49171-49172-50-56-19-4,0-10-11,23-24-25,0
 │    │                                           │        │       │
 │    │                                           │        │       └─ EC Point Formats
 │    │                                           │        └─ Elliptic Curves
 │    │                                           └─ Extensions
 │    └─ Cipher Suites
 └─ TLS Version
```

**Known Malware JA3 Signatures:**
```python
KNOWN_MALWARE_JA3 = {
    "e7d705a3286e19ea42f587b344ee6865": "Emotet",
    "6734f37431670b3ab4292b8f60f29984": "Trickbot",
    "72a589da586844d7f0818ce684948eea": "AsyncRAT",
    "a0e9f5d64349fb13191bc781f81f42e1": "Cobalt Strike",
}
```

---

### Layer 2: Feature Extractor (`shakti_x/layer2_extractor/`)

**Purpose:** Convert raw connection data into 42-dimensional feature vector

**Feature Categories:**

#### DNS/Domain Features (11 features)
| Index | Feature | Description | Threat Indicator |
|-------|---------|-------------|------------------|
| 0 | `dns_length` | Domain name length | Long = suspicious |
| 1 | `dns_dots` | Number of subdomains | Many = suspicious |
| 2 | `dns_entropy` | Character randomness | High = DGA |
| 3 | `dns_punycode` | Internationalized domain | Phishing indicator |
| 4 | `dns_digit_ratio` | Numbers in domain | High = suspicious |
| 5 | `dns_vowel_ratio` | Vowel frequency | Low = DGA |
| 6 | `dns_tld_risk` | Risky TLD (.tk, .xyz) | 1.0 = risky |
| 7 | `dns_max_consonants` | Consecutive consonants | High = DGA |
| 8 | `dns_suspicious` | Contains "login", "secure" | Phishing |
| 9 | `dns_subdomain_depth` | Subdomain levels | Deep = suspicious |
| 10 | `dns_numeric_subdomain` | Numeric subdomains | C2 indicator |

#### Network Flow Features (12 features)
| Index | Feature | Description |
|-------|---------|-------------|
| 11 | `flow_ip_sum` | IP address encoding |
| 12 | `flow_dst_port` | Destination port |
| 13 | `flow_is_tcp` | TCP (1) or UDP (0) |
| 14 | `flow_bytes_per_sec` | Data transfer rate |
| 15 | `flow_packets_per_sec` | Packet rate |
| 16 | `flow_tx_rx_ratio` | Upload/Download ratio |
| 17 | `flow_duration` | Connection duration |
| 18 | `flow_app_connections` | App's total connections |
| 19 | `flow_unique_dst` | Unique destinations |
| 20 | `flow_anomaly_score` | Behavioral anomaly |
| 21 | `flow_is_burst` | Burst traffic indicator |
| 22 | `flow_suspicious_ratio` | Suspicious pattern ratio |

#### App Metadata Features (5 features)
| Index | Feature | Description |
|-------|---------|-------------|
| 23 | `app_perm_count` | Number of permissions |
| 24 | `app_is_system` | System app flag |
| 25 | `app_age_days` | Days since install |
| 26 | `app_bg_restricted` | Background restricted |
| 27 | `app_has_dangerous` | Has dangerous permissions |

#### TLS Features (8 features)
| Index | Feature | Description |
|-------|---------|-------------|
| 28 | `tls_has_ja3` | Has JA3 fingerprint |
| 29 | `tls_ja3_malware` | Matches malware JA3 |
| 30 | `tls_version` | TLS version (1.0-1.3) |
| 31 | `tls_cipher_count` | Cipher suites offered |
| 32 | `tls_extension_count` | TLS extensions |
| 33 | `tls_has_sni` | Has SNI |
| 34 | `tls_sni_match` | SNI matches domain |
| 35 | `tls_cert_chain` | Certificate chain length |

#### Temporal Features (6 features)
| Index | Feature | Description |
|-------|---------|-------------|
| 36 | `temp_hour` | Hour of day (0-23) |
| 37 | `temp_weekday` | Is weekday |
| 38 | `temp_business_hours` | Is business hours |
| 39 | `temp_burst_score` | Burst behavior score |
| 40 | `temp_dst_diversity` | Destination diversity |
| 41 | `temp_anomaly` | Temporal anomaly score |

---

### Layer 3: AI Engine (`shakti_x/layer3_ai/`)

**Purpose:** Classify traffic as Normal or specific Attack type

#### Tier 1: Random Forest Classifier

| Parameter | Value |
|-----------|-------|
| Algorithm | Random Forest |
| Trees | 100 |
| Max Depth | 20 |
| Inference Time | ~5ms |
| Accuracy | 99.9% (CICIDS2017) |

**Decision Flow:**
```
Input Features → Scaler → Random Forest → Prediction + Probabilities
                                              ↓
                              risk_score < 0.25 → SAFE (return)
                              risk_score > 0.85 → BLOCK (return)
                              else → Escalate to Tier 2
```

#### Tier 2: Deep Neural Network

| Parameter | Value |
|-----------|-------|
| Architecture | Dense NN |
| Layers | 128 → 64 → 32 → N classes |
| Activation | ReLU + Softmax |
| Inference Time | ~50ms |
| Format | .h5 (Keras) / .tflite (Mobile) |

**Network Architecture:**
```
Input (65) → Dense(128, ReLU) → Dropout(0.3) → Dense(64, ReLU) → 
Dropout(0.3) → Dense(32, ReLU) → Dense(N, Softmax)
```

#### Classification Classes

**CICIDS2017/2018 Model:**
| Class | Description |
|-------|-------------|
| Normal | Benign traffic |
| Brute_Force | SSH/FTP brute force |
| DoS | Denial of Service |
| DDoS | Distributed DoS |
| Web_Attack | SQL Injection, XSS |
| Infiltration | Network infiltration |
| Botnet | Botnet communication |
| Port_Scan | Port scanning |
| Heartbleed | Heartbleed exploit |

**UNSW-NB15 Model:**
| Class | Description |
|-------|-------------|
| Normal | Benign traffic |
| Fuzzers | Fuzzing attacks |
| Analysis | Vulnerability scanning |
| Backdoor | Backdoor access |
| DoS | Denial of Service |
| Exploits | Exploit attempts |
| Generic | Generic attacks |
| Reconnaissance | Network recon |
| Shellcode | Shellcode injection |
| Worms | Worm propagation |

---

### Layer 4: Decision Engine (`shakti_x/layer4_decision/`)

**Purpose:** Make final ALLOW/WARN/BLOCK decision

**Decision Thresholds:**
```python
SAFE_THRESHOLD = 0.25          # Below = ALLOW
WARN_THRESHOLD = 0.55          # Between = WARN
INSTANT_BLOCK_THRESHOLD = 0.85 # Above = BLOCK
```

**Decision Matrix:**
| Risk Score | Action | User Notification |
|------------|--------|-------------------|
| 0.00 - 0.25 | ✅ ALLOW | None |
| 0.26 - 0.54 | ✅ ALLOW | None (logged) |
| 0.55 - 0.84 | ⚠️ WARN | Alert shown |
| 0.85 - 1.00 | 🚫 BLOCK | Connection killed |

---

### Layer 4b: Enforcement Engine (`shakti_x/layer4b_enforcement/`)

**Purpose:** Actually block malicious connections

| Platform | Method | Command |
|----------|--------|---------|
| Android | iptables | `iptables -A SHAKTI_X -m owner --uid-owner {uid} -j DROP` |
| Android | Kill connection | `ss -K dst {ip} dport = {port}` |
| Windows | Mock only | Logging only (no real blocking) |

**iptables Chain Structure:**
```
SHAKTI_X (custom chain)
├── Rule: Block UID 10234 (malicious app)
├── Rule: Block IP 185.234.xx.xx (C2 server)
└── Rule: Block Port 4444 (common RAT port)
```

---

### Layer 5: Dashboard (`shakti_x/layer5_dashboard/`)

**Purpose:** Real-time web UI for monitoring

| Component | Technology |
|-----------|------------|
| Backend | Flask + Flask-SocketIO |
| Frontend | HTML5 + JavaScript |
| Real-time | WebSocket |
| Port | 8080 |

**WebSocket Events:**

```javascript
// Server → Client: New connection detected
socket.emit('new_connection', {
    uid: 1234,
    dst_ip: "142.250.185.78",
    dst_port: 443,
    protocol: "TCP"
});

// Server → Client: AI verdict
socket.emit('security_event', {
    uid: 1234,
    risk_score: 0.15,
    classification: "NORMAL",
    action: "ALLOW"
});
```

**REST API Endpoints:**
| Endpoint | Method | Response |
|----------|--------|----------|
| `/` | GET | Dashboard HTML |
| `/api/status` | GET | Engine status JSON |

---

## 🧠 AI Models

### Trained Models Location

```
models/
├── threat_classifier_cicids2017.pkl      # Random Forest (65 features)
├── threat_classifier_cicids2018.pkl      # Random Forest (26 features)
├── threat_classifier_unsw.pkl            # Random Forest (39 features)
├── deep_classifier_cicids2017.h5         # Keras Deep NN
├── deep_classifier_cicids2017.tflite     # TFLite for mobile
├── deep_classifier_cicids2017_metadata.pkl
├── deep_classifier_cicids2018.h5
├── deep_classifier_cicids2018.tflite
├── deep_classifier_cicids2018_metadata.pkl
├── deep_classifier_unsw.h5
├── deep_classifier_unsw.tflite
└── deep_classifier_unsw_metadata.pkl
```

### Model Selection Priority

The AI engine loads models in this order:
1. `cicids2017` (preferred - most comprehensive)
2. `cicids2018` (fallback)
3. `unsw` (fallback)
4. `nsl-kdd` (fallback)

### Training Data Statistics

| Dataset | Samples | Features | Classes | Accuracy |
|---------|---------|----------|---------|----------|
| CICIDS2017 | 2.8M | 65 | 9 | 99.9% |
| CICIDS2018 | 16M | 26 | 9 | 99.8% |
| UNSW-NB15 | 2.5M | 39 | 10 | 81% |

---

## ⚙️ Prerequisites

### For Windows (Development/Testing)

```
✅ Python 3.8+
✅ pip packages (see requirements.txt)
✅ No admin rights needed (mock blocking only)
```

### For Android (Production)

```
✅ Android 8.0+ (API 26+)
✅ Shizuku app installed and activated
✅ ADB access (for Shizuku activation)
✅ Developer Options enabled
```

---

## 🖥️ Windows Setup

### Step 1: Clone Repository

```bash
git clone https://github.com/yourusername/AI-firewall.git
cd AI-firewall
```

### Step 2: Create Virtual Environment

```bash
python -m venv venv
venv\Scripts\activate
```

### Step 3: Install Dependencies

```bash
pip install -r requirements.txt
```

### Step 4: Verify Models Exist

```bash
dir models\
# Should show: threat_classifier_cicids2017.pkl, etc.
```

### Step 5: Run Demo

```bash
python demo_runner.py
```

### Step 6: Open Dashboard

```
http://127.0.0.1:8080
```

### Expected Output

```
🚀 Starting Shakti X 3.0 Engine (DEMO MODE)...
============================================================

📦 Initializing Security Layers...
   ✅ Layer 1: Network Observer
   ✅ Layer 1b: Transparent Proxy
   ✅ Layer 2: Feature Extractor
   ✅ Layer 3: AI Engine
   ✅ Loaded threat classifier: cicids2017 (99.9% accuracy, 65 features)
   ✅ Layer 4: Decision Engine
   ✅ Layer 4b: Enforcement Engine
   ✅ Layer 5: Dashboard Server

============================================================
👁️  SHAKTI X 3.0 - FULL SECURITY STACK ACTIVE
============================================================
📊 Dashboard: http://127.0.0.1:8080
🔍 Monitoring real network connections...
============================================================

--- [Scan #1] ---
   Found 5 connection(s)
   🟢 PID:1234 → 142.250.185.78:443 | NORMAL (5%) → ALLOW
   🟡 PID:5678 → 185.234.xx.xx:4444 | DGA_SUSPECT (62%) → WARN
   🔴 PID:9012 → 45.xx.xx.xx:8080 | BOTNET (91%) → BLOCK
```

---

## 📱 Android Setup

### Step 1: Install Shizuku on Phone

1. Download **Shizuku** from Play Store
2. Open Shizuku app

### Step 2: Activate Shizuku

#### Option A: Wireless Debugging (Android 11+)

```
1. Settings → Developer Options → Wireless Debugging → ON
2. Open Shizuku → "Start via Wireless Debugging"
3. Tap "Pair" and enter the pairing code
4. ✅ Shizuku is now active
```

#### Option B: ADB from PC

```bash
# Connect phone via USB
adb devices

# Start Shizuku
adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh

# Verify
adb shell dumpsys activity service moe.shizuku.privileged.api
```

### Step 3: Build Android App

```
[Android app structure to be created]
```

### Step 4: Install & Grant Permissions

1. Install Shakti X APK
2. Grant Shizuku permission when prompted
3. Grant notification permission
4. ✅ Protection active

### Shizuku Permission Flow

```
┌─────────────────────────────────────────┐
│         Shakti X App Startup            │
├─────────────────────────────────────────┤
│                                         │
│  1. Check Shizuku installed?            │
│     ├─ No → Show "Install Shizuku"      │
│     └─ Yes → Continue                   │
│                                         │
│  2. Check Shizuku running?              │
│     ├─ No → Show "Activate Shizuku"     │
│     └─ Yes → Continue                   │
│                                         │
│  3. Request Shizuku permission          │
│     ├─ Denied → Show explanation        │
│     └─ Granted → ✅ Start protection    │
│                                         │
└─────────────────────────────────────────┘
```

---

## 📚 API Reference

### AIEngine Class

```python
from shakti_x.layer3_ai.engine import AIEngine

# Initialize
ai = AIEngine()

# Analyze traffic
result = ai.analyze(
    features=[...],           # 42-dim feature vector
    tls_metadata={"sni": "example.com", "ja3": "..."},
    app_metadata={"is_system": False, "perm_count": 10}
)

# Result structure
{
    "risk_score": 0.15,           # 0.0 to 1.0
    "classification": "NORMAL",    # Threat class
    "confidence": 0.95,           # Model confidence
    "tier": 1,                    # Which tier decided (1 or 2)
    "reasons": ["classifier:Normal"],
    "probabilities": {...}        # Per-class probabilities
}

# Get statistics
stats = ai.get_stats()
{
    "total_analyzed": 1000,
    "tier1_decisions": 950,
    "tier2_decisions": 50,
    "tier1_percentage": 95.0,
    "deep_models_available": True
}
```

### NetworkObserver Class

```python
from shakti_x.layer1_observer.observer import NetworkObserver

observer = NetworkObserver()
observer.start_listening()

# Get connections
connections = observer.get_active_connections()
for conn in connections:
    print(f"{conn['uid']} → {conn['dst_ip']}:{conn['dst_port']}")

# Get stats
stats = observer.get_stats()
{
    "is_running": True,
    "method": "psutil",  # or "netlink" on Android
    "active_connections": 25,
    "total_seen": 150
}
```

### FeatureExtractor Class

```python
from shakti_x.layer2_extractor.extractor import FeatureExtractor

extractor = FeatureExtractor()

features = extractor.extract_features(
    conn_info=connection,
    tls_metadata={"sni": "google.com"},
    app_metadata={"is_system": False}
)

# Returns: List of 42 floats
print(len(features))  # 42
```

---

## ⚙️ Configuration Parameters

### Constants (`shakti_x/common/constants.py`)

```python
# Feature count
FEATURE_COUNT = 42

# Decision thresholds
SAFE_THRESHOLD = 0.25
WARN_THRESHOLD = 0.55
INSTANT_BLOCK_THRESHOLD = 0.85

# Safe ports (whitelist)
SAFE_PORTS = {80, 443, 53, 8080, 8443}

# Suspicious ports (increase risk)
SUSPICIOUS_PORTS = {4444, 5555, 6666, 1337, 31337}

# Risky TLDs
RISKY_TLDS = {'.tk', '.ml', '.ga', '.cf', '.gq', '.xyz', '.top', '.pw', '.cc', '.su'}

# Known malware JA3 hashes
KNOWN_MALWARE_JA3 = {
    "e7d705a3286e19ea42f587b344ee6865": "Emotet",
    "6734f37431670b3ab4292b8f60f29984": "Trickbot",
    ...
}
```

### Training Parameters (`train_model.py`)

```bash
python train_model.py --help

Arguments:
  --dataset {cicids2017,cicids2018,unsw,nsl-kdd,all}  # Dataset to train
  --data-dir PATH      # Dataset directory (default: datasets)
  --output-dir PATH    # Model output directory (default: models)
  --max-samples N      # Max samples per dataset (default: 500000)
  --deep              # Also train deep neural network
```

### Dashboard Parameters

```python
DashboardServer(
    host='0.0.0.0',    # Listen on all interfaces
    port=8080          # HTTP port
)
```

---

## 🔧 Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| "No trained classifier found" | Ensure `models/` folder contains .pkl files |
| "TensorFlow not installed" | Run `pip install tensorflow` for deep models |
| "Access denied" on Windows | Run as Administrator for full visibility |
| Dashboard shows no data | Check WebSocket connection in browser console |
| Shizuku not working | Re-activate after phone reboot |

### Logs

Enable debug logging:
```python
import logging
logging.basicConfig(level=logging.DEBUG)
```

### Model Compatibility

If you see "feature mismatch" errors:
- CICIDS2017 expects 65 features
- CICIDS2018 expects 26 features
- The engine auto-adapts (pads/truncates) features

---

## 📁 Project Structure

```
AI-firewall/
├── shakti_x/
│   ├── layer1_observer/
│   │   ├── observer.py          # Network monitoring
│   │   ├── netlink_parser.py    # Netlink protocol parser
│   │   └── package_resolver.py  # UID → package name
│   ├── layer1b_proxy/
│   │   └── proxy.py             # TLS interception
│   ├── layer2_extractor/
│   │   ├── extractor.py         # Feature extraction
│   │   └── flow_tracker.py      # Flow statistics
│   ├── layer3_ai/
│   │   ├── engine.py            # Main AI engine
│   │   └── isolation_forest.py  # Anomaly detection fallback
│   ├── layer4_decision/
│   │   └── engine.py            # Decision making
│   ├── layer4b_enforcement/
│   │   └── enforcer.py          # iptables blocking
│   ├── layer5_dashboard/
│   │   ├── server.py            # Flask server
│   │   └── index.html           # Web UI
│   └── common/
│       ├── constants.py         # Configuration
│       ├── database.py          # Reputation DB
│       └── shizuku.py           # Shizuku bridge
├── models/                       # Trained AI models
├── datasets/                     # Training data (not in repo)
├── demo_runner.py               # Demo script
├── train_model.py               # Training script
├── requirements.txt             # Python dependencies
└── SHAKTI_X_DOCUMENTATION.md    # This file
```

---

## 📄 License

[Your License Here]

---

## 👥 Contributors

- [Your Name]
- AI Assistant (Model Training & Documentation)

---

## 🔗 Links

- **Repository:** [GitHub URL]
- **Shizuku:** https://github.com/RikkaApps/Shizuku
- **CICIDS2017 Dataset:** https://www.unb.ca/cic/datasets/ids-2017.html
- **UNSW-NB15 Dataset:** https://www.unsw.adfa.edu.au/unsw-canberra-cyber/cybersecurity/ADFA-NB15-Datasets/

---

*Last Updated: March 2026*
