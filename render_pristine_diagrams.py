import os
import subprocess

edge_path = r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
if not os.path.exists(edge_path):
    edge_path = r'C:\Program Files\Microsoft\Edge\Application\msedge.exe'

os.makedirs('figures', exist_ok=True)

def render_html_to_png(html_str, out_png, width=1400, height=850):
    html_file = os.path.abspath(out_png.replace('.png', '.html'))
    png_file = os.path.abspath(out_png)
    with open(html_file, 'w', encoding='utf-8') as f:
        f.write(html_str)
        
    cmd = [
        edge_path,
        '--headless=new',
        '--disable-gpu',
        '--force-device-scale-factor=2',
        f'--window-size={width},{height}',
        f'--screenshot={png_file}',
        'file:///' + html_file.replace('\\', '/')
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    if os.path.exists(png_file):
        print(f"✓ Rendered pristine {out_png} ({os.path.getsize(png_file)} bytes)")
    else:
        print(f"✗ Failed to render {out_png}: {res.stderr}")

# ==============================================================================
# 1. FIG_PIPELINE_DIAGRAM: 2-Tier OCR & Market Anti-Corruption Guard
# ==============================================================================
html_pipeline = '''<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    width: 1400px;
    height: 820px;
    background: #F8FAF9;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 30px;
  }
  .header {
    text-align: center;
    margin-bottom: 25px;
  }
  .header h1 {
    font-size: 26px;
    font-weight: 800;
    color: #223832;
    letter-spacing: -0.5px;
  }
  .header p {
    font-size: 14px;
    color: #526B64;
    margin-top: 5px;
    font-weight: 500;
  }
  .board {
    width: 1340px;
    height: 680px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    box-shadow: 0 8px 30px rgba(34, 56, 50, 0.08);
    position: relative;
    padding: 35px 40px;
  }
  
  /* Top Row Pipeline */
  .top-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: relative;
    margin-top: 10px;
  }
  .stage-card {
    width: 280px;
    background: #FFFFFF;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 4px 15px rgba(0,0,0,0.05);
    border: 1.5px solid #2D4A43;
    position: relative;
  }
  .stage-badge {
    position: absolute;
    top: -12px;
    left: 18px;
    background: #2D4A43;
    color: #FFFFFF;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 12px;
    border-radius: 20px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .stage-title {
    font-size: 16px;
    font-weight: 700;
    color: #223832;
    margin-top: 6px;
    margin-bottom: 10px;
  }
  .stage-desc {
    font-size: 12.5px;
    color: #526B64;
    line-height: 1.5;
  }
  .stage-desc li {
    margin-left: 16px;
    margin-top: 4px;
  }

  .arrow-right {
    display: flex;
    align-items: center;
    color: #2D4A43;
    font-size: 26px;
    font-weight: bold;
  }

  /* Mid Section */
  .mid-section {
    display: flex;
    justify-content: space-between;
    margin-top: 45px;
    position: relative;
  }
  .engine-box {
    width: 480px;
    background: #F4F5F0;
    border: 1.5px solid #223832;
    border-radius: 12px;
    padding: 22px;
    position: relative;
  }
  .engine-badge {
    position: absolute;
    top: -12px;
    left: 20px;
    background: #223832;
    color: #F5C29B;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 12px;
    border-radius: 20px;
    text-transform: uppercase;
  }
  .engine-title {
    font-size: 16px;
    font-weight: 700;
    color: #223832;
    margin-top: 4px;
    margin-bottom: 8px;
  }
  .formula-box {
    background: #FFFFFF;
    border: 1px dashed #526B64;
    border-radius: 8px;
    padding: 10px 14px;
    font-family: "Courier New", monospace;
    font-size: 12.5px;
    font-weight: bold;
    color: #223832;
    margin-top: 10px;
    text-align: center;
  }

  .triage-container {
    width: 680px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .triage-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 20px;
    border-radius: 10px;
    border-left: 6px solid;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  }
  .triage-card.red {
    background: #FEF2F2;
    border-color: #B9382C;
  }
  .triage-card.green {
    background: #F0FDF4;
    border-color: #10B981;
  }
  .triage-card.blue {
    background: #EFF6FF;
    border-color: #2563EB;
  }
  .triage-name {
    font-size: 14.5px;
    font-weight: 800;
  }
  .triage-card.red .triage-name { color: #B9382C; }
  .triage-card.green .triage-name { color: #065F46; }
  .triage-card.blue .triage-name { color: #1E40AF; }

  .triage-desc {
    font-size: 12px;
    color: #475569;
    font-weight: 500;
  }

  /* Bottom Bar */
  .bottom-bar {
    margin-top: 35px;
    background: #2D4A43;
    color: #FFFFFF;
    border-radius: 10px;
    padding: 16px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .bottom-title {
    font-size: 15px;
    font-weight: 700;
    color: #F5C29B;
  }
  .bottom-details {
    font-size: 12.5px;
    color: #E2E8F0;
  }
</style>
</head>
<body>
  <div class="header">
    <h1>SiteSync 2-Tier Challan OCR & Market Price Anti-Corruption Pipeline</h1>
    <p>Zero-Latency On-Device Geometry Extraction • Multimodal Bilingual LLM • Live Brave Search Market Auditing</p>
  </div>
  <div class="board">
    <!-- Top Row -->
    <div class="top-row">
      <div class="stage-card">
        <div class="stage-badge">Input Capture</div>
        <div class="stage-title">📷 CameraX Viewfinder</div>
        <div class="stage-desc">
          <ul>
            <li>Tap-to-Focus & Auto-Torch</li>
            <li>Skew & Glare Rectification</li>
            <li>Internal FileProvider URI Cache</li>
          </ul>
        </div>
      </div>

      <div class="arrow-right">➔</div>

      <div class="stage-card">
        <div class="stage-badge">Tier 1: On-Device</div>
        <div class="stage-title">⚡ Google ML Kit</div>
        <div class="stage-desc">
          <ul>
            <li>Zero Cloud Latency (&lt; 48 ms)</li>
            <li>Text Boundary Detection</li>
            <li>Challan #, Date & Supplier</li>
          </ul>
        </div>
      </div>

      <div class="arrow-right">➔</div>

      <div class="stage-card">
        <div class="stage-badge">Tier 2: Multimodal</div>
        <div class="stage-title">🧠 Gemini 1.5 / Groq</div>
        <div class="stage-desc">
          <ul>
            <li>Bilingual Bengali + Eng Line Items</li>
            <li>Cursive & Damaged Carbon Parsing</li>
            <li>Structured JSON Material Ledger</li>
          </ul>
        </div>
      </div>

      <div class="arrow-right">➔</div>

      <div class="stage-card">
        <div class="stage-badge">Live Intelligence</div>
        <div class="stage-title">🌐 Brave Search API</div>
        <div class="stage-desc">
          <ul>
            <li>api.search.brave.com Query</li>
            <li>"Material + market price BD BDT"</li>
            <li>Real-time benchmark rate index</li>
          </ul>
        </div>
      </div>
    </div>

    <!-- Mid Section -->
    <div class="mid-section">
      <div class="engine-box">
        <div class="engine-badge">Audit Algorithm</div>
        <div class="engine-title">Empirical Price Variance Calculator</div>
        <p style="font-size: 12.5px; color: #526B64; line-height: 1.4;">
          Computes deviation between scanned billed unit price and live Bangladesh market benchmark:
        </p>
        <div class="formula-box">
          Variance % (Δ) = [ ( R_billed - R_bench ) / R_bench ] × 100%
        </div>
      </div>

      <div class="triage-container">
        <div class="triage-card red">
          <div>
            <div class="triage-name">⚠️ OVER_BILLED (Δ &gt; +8.0%)</div>
            <div class="triage-desc">Inflated billing detected. Manager alerted; prevents unauthorized margin contractor fraud.</div>
          </div>
          <div style="font-size: 20px; font-weight: 800; color: #B9382C;">FLAGGED</div>
        </div>

        <div class="triage-card green">
          <div>
            <div class="triage-name">✓ FAIR_MARKET (-8.0% ≤ Δ ≤ +8.0%)</div>
            <div class="triage-desc">Price perfectly aligns with current spot market rates in Bangladesh. Verified checkmark rendered.</div>
          </div>
          <div style="font-size: 20px; font-weight: 800; color: #10B981;">PASSED</div>
        </div>

        <div class="triage-card blue">
          <div>
            <div class="triage-name">★ DISCOUNTED_BULK (Δ &lt; -8.0%)</div>
            <div class="triage-desc">Wholesale or supplier bulk negotiated rate confirmed. Substantial project savings logged.</div>
          </div>
          <div style="font-size: 20px; font-weight: 800; color: #2563EB;">SAVINGS</div>
        </div>
      </div>
    </div>

    <!-- Bottom Bar -->
    <div class="bottom-bar">
      <div>
        <div class="bottom-title">ROOM DATABASE ENGINE (@Transaction Atomic Commit)</div>
        <div class="bottom-details">Stock IN Ledger Entry • Material Current Stock Increment • Automatic Low-Stock Threshold Trigger</div>
      </div>
      <div style="background: #223832; padding: 6px 14px; border-radius: 6px; font-size: 12px; font-weight: bold; color: #F5C29B;">
        Firebase Cloud Synced
      </div>
    </div>
  </div>
</body>
</html>'''

# ==============================================================================
# 2. FIG_ARCHITECTURE: Clean MVVM 3-Tier Enterprise Architecture
# ==============================================================================
html_arch = '''<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    width: 1400px;
    height: 860px;
    background: #F8FAF9;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 30px;
  }
  .header {
    text-align: center;
    margin-bottom: 20px;
  }
  .header h1 {
    font-size: 26px;
    font-weight: 800;
    color: #223832;
    letter-spacing: -0.5px;
  }
  .header p {
    font-size: 14px;
    color: #526B64;
    margin-top: 4px;
    font-weight: 500;
  }
  .board {
    width: 1340px;
    height: 740px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    box-shadow: 0 8px 30px rgba(34, 56, 50, 0.08);
    padding: 25px 35px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  /* Tier Row */
  .tier-row {
    border-radius: 12px;
    padding: 16px 20px;
    position: relative;
  }
  .tier-presentation {
    background: #F4F5F0;
    border: 1.5px solid #2D4A43;
  }
  .tier-domain {
    background: #FFFFFF;
    border: 1.5px solid #223832;
    box-shadow: 0 4px 15px rgba(0,0,0,0.04);
  }
  .tier-data {
    background: #F4F5F0;
    border: 1.5px solid #2D4A43;
  }

  .tier-badge {
    position: absolute;
    top: -11px;
    left: 20px;
    font-size: 11px;
    font-weight: 800;
    padding: 3px 12px;
    border-radius: 20px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .tier-presentation .tier-badge { background: #2D4A43; color: #FFFFFF; }
  .tier-domain .tier-badge { background: #223832; color: #F5C29B; }
  .tier-data .tier-badge { background: #2D4A43; color: #FFFFFF; }

  .tier-title {
    font-size: 15px;
    font-weight: 800;
    color: #223832;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .cards-grid {
    display: flex;
    gap: 15px;
  }
  .card-item {
    flex: 1;
    background: #FFFFFF;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    padding: 12px 14px;
    box-shadow: 0 2px 6px rgba(0,0,0,0.03);
  }
  .card-item.dark {
    background: #2D4A43;
    color: #FFFFFF;
    border-color: #223832;
  }
  .card-item-title {
    font-size: 13.5px;
    font-weight: 700;
    color: #223832;
    margin-bottom: 6px;
  }
  .card-item.dark .card-item-title { color: #F5C29B; }
  .card-item-desc {
    font-size: 11.5px;
    color: #526B64;
    line-height: 1.4;
  }
  .card-item.dark .card-item-desc { color: #E2E8F0; }

  /* Inter-tier flow arrows */
  .flow-indicator {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 20px;
    font-size: 12px;
    font-weight: 700;
    color: #526B64;
  }
  .flow-indicator span {
    background: #E2E8F0;
    padding: 2px 12px;
    border-radius: 12px;
  }
</style>
</head>
<body>
  <div class="header">
    <h1>SiteSync Clean MVVM 3-Tier Enterprise Architecture</h1>
    <p>Presentation Tier • Reactive Domain & ViewModel State • Unified Hardware & Cloud Persistence</p>
  </div>
  <div class="board">
    <!-- Tier 1: Presentation -->
    <div class="tier-row tier-presentation">
      <div class="tier-badge">Presentation Layer (ViewBinding & Material 3)</div>
      <div class="tier-title">🖥️ UI Components & User Navigation Workflows</div>
      <div class="cards-grid">
        <div class="card-item">
          <div class="card-item-title">Executive Dashboard</div>
          <div class="card-item-desc">CoordinatorLayout, NestedScrollView, KPI budget cards, MPAndroidChart touch charts.</div>
        </div>
        <div class="card-item">
          <div class="card-item-title">Worker Safety Terminal</div>
          <div class="card-item-desc">Acoustic Decibel Meter, 15s Fall Countdown Dialog, SOS Emergency Chat Dispatch.</div>
        </div>
        <div class="card-item">
          <div class="card-item-title">Field Operations</div>
          <div class="card-item-desc">CameraX Viewfinder, Geotagged Evidence Dump, Attendance Matrix, People Roster.</div>
        </div>
      </div>
    </div>

    <!-- Indicator 1 -->
    <div class="flow-indicator">
      <span>↕ Observes LiveData / StateFlow</span>
      <span>↕ Dispatches User Events (Intents)</span>
    </div>

    <!-- Tier 2: Domain / ViewModel -->
    <div class="tier-row tier-domain">
      <div class="tier-badge">ViewModel & Domain Business Logic</div>
      <div class="tier-title">⚡ Reactive State Management, Coroutines & Telemetry Engines</div>
      <div class="cards-grid">
        <div class="card-item">
          <div class="card-item-title">Inventory & Ledger VM</div>
          <div class="card-item-desc">Stock IN/OUT ledger mutations, automated reorder thresholds, variance calculations.</div>
        </div>
        <div class="card-item">
          <div class="card-item-title">Sensory Telemetry Coordinator</div>
          <div class="card-item-desc">SensorEventListener fusion (Accelerometer G-force), AudioRecord PCM decibels, Fused GPS.</div>
        </div>
        <div class="card-item">
          <div class="card-item-title">Access & Auth Gatekeeper</div>
          <div class="card-item-desc">Two-Step Verification engine, project role authorization, manager worker approval/kick.</div>
        </div>
      </div>
    </div>

    <!-- Indicator 2 -->
    <div class="flow-indicator">
      <span>↕ Asynchronous Repository Abstraction</span>
      <span>↕ Hardware Sensor Sampling & Cloud Sync</span>
    </div>

    <!-- Tier 3: Data & Hardware -->
    <div class="tier-row tier-data">
      <div class="tier-badge">Persistence, Hardware Integration & Cloud Services</div>
      <div class="tier-title">💾 Native SQLite Room, Android OS Bridges & Firebase Infrastructure</div>
      <div class="cards-grid">
        <div class="card-item dark">
          <div class="card-item-title">Room Relational DB</div>
          <div class="card-item-desc">SQLite ORM, @Dao, @Transaction atomic isolation, zero-inventory initialization rules.</div>
        </div>
        <div class="card-item dark">
          <div class="card-item-title">Hardware Telemetry</div>
          <div class="card-item-desc">3-Axis Accelerometer, AudioRecord Mic stream, FusedLocationProvider GPS, CameraX.</div>
        </div>
        <div class="card-item dark">
          <div class="card-item-title">Android OS Components</div>
          <div class="card-item-desc">Floating Voice Alerter Service, SystemEvent BroadcastReceiver, Contacts Provider, FileProvider.</div>
        </div>
        <div class="card-item dark">
          <div class="card-item-title">Cloud Suite & APIs</div>
          <div class="card-item-desc">Firebase Auth, Realtime DB, Cloud Storage (photo evidence/voice audio), Brave Search API.</div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>'''

# ==============================================================================
# 3. FIG_FALL_STATE_MACHINE: Worker Telemetry & Fall Rescue Lifecycle
# ==============================================================================
html_fall = '''<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    width: 1400px;
    height: 720px;
    background: #F8FAF9;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 30px;
  }
  .header {
    text-align: center;
    margin-bottom: 25px;
  }
  .header h1 {
    font-size: 26px;
    font-weight: 800;
    color: #223832;
    letter-spacing: -0.5px;
  }
  .header p {
    font-size: 14px;
    color: #526B64;
    margin-top: 5px;
    font-weight: 500;
  }
  .board {
    width: 1340px;
    height: 560px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    box-shadow: 0 8px 30px rgba(34, 56, 50, 0.08);
    padding: 35px 30px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
  .chain-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .state-node {
    width: 230px;
    height: 180px;
    background: #FFFFFF;
    border-radius: 12px;
    border: 2px solid #2D4A43;
    padding: 16px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    box-shadow: 0 4px 14px rgba(0,0,0,0.05);
    position: relative;
  }
  .state-node.red {
    background: #FEF2F2;
    border-color: #B9382C;
  }
  .state-node.green {
    background: #F0FDF4;
    border-color: #10B981;
  }
  .state-badge {
    position: absolute;
    top: -12px;
    left: 14px;
    background: #2D4A43;
    color: #FFFFFF;
    font-size: 10px;
    font-weight: 800;
    padding: 3px 10px;
    border-radius: 20px;
    text-transform: uppercase;
  }
  .state-node.red .state-badge { background: #B9382C; }
  .state-node.green .state-badge { background: #10B981; }

  .state-title {
    font-size: 14.5px;
    font-weight: 800;
    color: #223832;
    margin-top: 4px;
  }
  .state-node.red .state-title { color: #B9382C; }
  .state-node.green .state-title { color: #065F46; }

  .state-desc {
    font-size: 11.5px;
    color: #526B64;
    line-height: 1.45;
  }
  .state-node.red .state-desc { color: #7F1D1D; }
  .state-node.green .state-desc { color: #064E3B; }

  .state-footer {
    font-size: 10.5px;
    font-weight: 700;
    padding: 4px 8px;
    background: #E2E8F0;
    border-radius: 6px;
    text-align: center;
    color: #223832;
  }
  .state-node.red .state-footer { background: #FEE2E2; color: #991B1B; }
  .state-node.green .state-footer { background: #DCFCE7; color: #166534; }

  .arrow-sep {
    font-size: 24px;
    color: #2D4A43;
    font-weight: bold;
  }

  /* Branch row */
  .branch-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 40px;
    background: #F4F5F0;
    border-radius: 10px;
    padding: 16px 25px;
    border: 1px dashed #526B64;
  }
  .branch-text {
    font-size: 13px;
    font-weight: 600;
    color: #223832;
  }
</style>
</head>
<body>
  <div class="header">
    <h1>Worker Safety Telemetry & Autonomous Fall Rescue Lifecycle</h1>
    <p>Continuous Accelerometer Polling • False-Alarm Cancellation Window • Fused GPS Turn-by-Turn Rescue</p>
  </div>
  <div class="board">
    <div class="chain-row">
      <!-- Node 1 -->
      <div class="state-node">
        <div class="state-badge">Stage 1</div>
        <div class="state-title">📊 Active Sensory Loop</div>
        <div class="state-desc">
          Continuous 3-axis accelerometer monitoring (|a| ≈ 9.8 m/s²) and live acoustic decibel sampling (dB).
        </div>
        <div class="state-footer">Normal Field Activity</div>
      </div>

      <div class="arrow-sep">➔</div>

      <!-- Node 2 -->
      <div class="state-node red">
        <div class="state-badge">Stage 2</div>
        <div class="state-title">💥 Impact G-Spike</div>
        <div class="state-desc">
          High acceleration spike (|a| &gt; 25.0 m/s²) followed immediately by motionless dormancy (|a| &lt; 1.0 m/s²).
        </div>
        <div class="state-footer">Anomaly Detected</div>
      </div>

      <div class="arrow-sep">➔</div>

      <!-- Node 3 -->
      <div class="state-node red">
        <div class="state-badge">Stage 3</div>
        <div class="state-title">⏱️ 15s Countdown</div>
        <div class="state-desc">
          High-priority emergency modal appears with siren & haptic vibration. Worker can tap "I am OK" to dismiss.
        </div>
        <div class="state-footer">15s Safety Window</div>
      </div>

      <div class="arrow-sep">➔</div>

      <!-- Node 4 -->
      <div class="state-node red">
        <div class="state-badge">Stage 4</div>
        <div class="state-title">🚨 Automated SOS</div>
        <div class="state-desc">
          Timeout fires UserLocationManager. Acquires high-precision GPS (lat, lng) and broadcasts SOS to Site Chat.
        </div>
        <div class="state-footer">Site-Wide Alert</div>
      </div>

      <div class="arrow-sep">➔</div>

      <!-- Node 5 -->
      <div class="state-node green">
        <div class="state-badge">Stage 5</div>
        <div class="state-title">🗺️ Google Maps Navigation</div>
        <div class="state-desc">
          Emergency responders click chat message. Deep-links geo:lat,lng Intent; launches instant turn-by-turn routing.
        </div>
        <div class="state-footer">Rapid Extraction</div>
      </div>
    </div>

    <!-- Branching Row -->
    <div class="branch-row">
      <div class="branch-text">🛡️ False Positive Safeguard:</div>
      <div style="font-size: 12.5px; color: #526B64;">
        If worker drops phone or stumbles without injury, tapping <strong>"I am OK"</strong> immediately cancels the countdown, logs zero incident, and silently returns to the active monitoring loop.
      </div>
      <div style="background: #10B981; color: white; padding: 4px 12px; border-radius: 6px; font-size: 11px; font-weight: bold;">
        Zero False Dispatch
      </div>
    </div>
  </div>
</body>
</html>'''

# ==============================================================================
# 4. FIG_ROOM_SCHEMA: Relational Room Database Entity-Relationship Schema
# ==============================================================================
html_schema = '''<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    width: 1400px;
    height: 780px;
    background: #F8FAF9;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 30px;
  }
  .header {
    text-align: center;
    margin-bottom: 20px;
  }
  .header h1 {
    font-size: 26px;
    font-weight: 800;
    color: #223832;
    letter-spacing: -0.5px;
  }
  .header p {
    font-size: 14px;
    color: #526B64;
    margin-top: 4px;
    font-weight: 500;
  }
  .board {
    width: 1340px;
    height: 640px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    box-shadow: 0 8px 30px rgba(34, 56, 50, 0.08);
    padding: 25px 30px;
    display: flex;
    justify-content: space-between;
    gap: 20px;
  }
  .table-box {
    flex: 1;
    background: #FFFFFF;
    border: 1.5px solid #2D4A43;
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0,0,0,0.04);
    display: flex;
    flex-direction: column;
  }
  .table-hdr {
    background: #2D4A43;
    color: #FFFFFF;
    padding: 10px 14px;
    font-size: 14px;
    font-weight: 700;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .table-hdr .badge {
    background: #F5C29B;
    color: #223832;
    font-size: 10px;
    font-weight: 800;
    padding: 2px 8px;
    border-radius: 12px;
  }
  .field-list {
    padding: 12px 14px;
    display: flex;
    flex-direction: column;
    gap: 7px;
    font-size: 11.5px;
    color: #223832;
    background: #FAFBF9;
    flex: 1;
  }
  .field {
    display: flex;
    justify-content: space-between;
    padding: 3px 0;
    border-bottom: 1px solid #E2E8F0;
  }
  .field.pk {
    font-weight: 800;
    color: #B9382C;
  }
  .field.fk {
    font-weight: 700;
    color: #2563EB;
  }
  .type {
    color: #64748B;
    font-family: monospace;
    font-size: 11px;
  }
</style>
</head>
<body>
  <div class="header">
    <h1>SiteSync Relational Room Database Entity Schema</h1>
    <p>Foreign Key Constraints • Atomic Concurrency (@Transaction) • Multi-Site Project Segregation</p>
  </div>
  <div class="board">
    <!-- Entity 1: ProjectSite -->
    <div class="table-box">
      <div class="table-hdr">
        <span>ProjectSite</span>
        <span class="badge">Parent Entity</span>
      </div>
      <div class="field-list">
        <div class="field pk"><span>🔑 siteId</span><span class="type">TEXT [PK]</span></div>
        <div class="field"><span>projectName</span><span class="type">TEXT</span></div>
        <div class="field"><span>siteLocation</span><span class="type">TEXT</span></div>
        <div class="field"><span>budgetBDT</span><span class="type">REAL</span></div>
        <div class="field"><span>managerUid</span><span class="type">TEXT</span></div>
        <div class="field"><span>createdAt</span><span class="type">INTEGER</span></div>
        <div class="field"><span>status</span><span class="type">TEXT</span></div>
      </div>
    </div>

    <!-- Entity 2: InventoryItem -->
    <div class="table-box">
      <div class="table-hdr">
        <span>InventoryItem</span>
        <span class="badge">1:N with Site</span>
      </div>
      <div class="field-list">
        <div class="field pk"><span>🔑 itemId</span><span class="type">TEXT [PK]</span></div>
        <div class="field fk"><span>🔗 siteId</span><span class="type">TEXT [FK]</span></div>
        <div class="field"><span>materialName</span><span class="type">TEXT</span></div>
        <div class="field"><span>category</span><span class="type">TEXT</span></div>
        <div class="field"><span>currentStock</span><span class="type">REAL</span></div>
        <div class="field"><span>unit</span><span class="type">TEXT (Ton,Bag)</span></div>
        <div class="field"><span>reorderThreshold</span><span class="type">REAL</span></div>
        <div class="field"><span>unitPriceBDT</span><span class="type">REAL</span></div>
      </div>
    </div>

    <!-- Entity 3: StockTransaction -->
    <div class="table-box">
      <div class="table-hdr">
        <span>StockTransaction</span>
        <span class="badge">Atomic Ledger</span>
      </div>
      <div class="field-list">
        <div class="field pk"><span>🔑 transactionId</span><span class="type">TEXT [PK]</span></div>
        <div class="field fk"><span>🔗 itemId</span><span class="type">TEXT [FK]</span></div>
        <div class="field fk"><span>🔗 siteId</span><span class="type">TEXT [FK]</span></div>
        <div class="field"><span>type</span><span class="type">TEXT (IN/OUT)</span></div>
        <div class="field"><span>quantity</span><span class="type">REAL</span></div>
        <div class="field"><span>challanNumber</span><span class="type">TEXT</span></div>
        <div class="field"><span>supplierName</span><span class="type">TEXT</span></div>
        <div class="field"><span>billedRateBDT</span><span class="type">REAL</span></div>
        <div class="field"><span>priceVariancePct</span><span class="type">REAL</span></div>
      </div>
    </div>

    <!-- Entity 4: AttendanceRecord -->
    <div class="table-box">
      <div class="table-hdr">
        <span>AttendanceRecord</span>
        <span class="badge">Daily Ledger</span>
      </div>
      <div class="field-list">
        <div class="field pk"><span>🔑 attendanceId</span><span class="type">TEXT [PK]</span></div>
        <div class="field fk"><span>🔗 siteId</span><span class="type">TEXT [FK]</span></div>
        <div class="field"><span>workerUid</span><span class="type">TEXT</span></div>
        <div class="field"><span>workerName</span><span class="type">TEXT</span></div>
        <div class="field"><span>role</span><span class="type">TEXT</span></div>
        <div class="field"><span>checkInTime</span><span class="type">INTEGER</span></div>
        <div class="field"><span>checkOutTime</span><span class="type">INTEGER</span></div>
        <div class="field"><span>status</span><span class="type">TEXT</span></div>
      </div>
    </div>

    <!-- Entity 5: SafetyIncident -->
    <div class="table-box">
      <div class="table-hdr">
        <span>SafetyIncident</span>
        <span class="badge">Sensory SOS</span>
      </div>
      <div class="field-list">
        <div class="field pk"><span>🔑 incidentId</span><span class="type">TEXT [PK]</span></div>
        <div class="field fk"><span>🔗 siteId</span><span class="type">TEXT [FK]</span></div>
        <div class="field"><span>workerUid</span><span class="type">TEXT</span></div>
        <div class="field"><span>incidentType</span><span class="type">TEXT (FALL)</span></div>
        <div class="field"><span>impactGForce</span><span class="type">REAL</span></div>
        <div class="field"><span>decibelLevel</span><span class="type">REAL</span></div>
        <div class="field"><span>gpsLatitude</span><span class="type">REAL</span></div>
        <div class="field"><span>gpsLongitude</span><span class="type">REAL</span></div>
      </div>
    </div>
  </div>
</body>
</html>'''

if __name__ == '__main__':
    render_html_to_png(html_pipeline, 'figures/fig_pipeline_diagram.png', 1400, 820)
    render_html_to_png(html_arch, 'figures/fig_architecture.png', 1400, 860)
    render_html_to_png(html_fall, 'figures/fig_fall_state_machine.png', 1400, 720)
    render_html_to_png(html_schema, 'figures/fig_room_schema.png', 1400, 780)
    print("All diagrams rendered with 2x crisp high-DPI scaling!")
