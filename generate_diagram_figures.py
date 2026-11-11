import os
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches

# Ukil Bhai Design System Palette
PRIMARY_DARK = '#223832'      # Deep Forest Slate
ACCENT_PEACH = '#F5C29B'      # Soft Peach-Apricot
SECONDARY_MUTED = '#526B64'   # Muted Forest Gray
TABLE_TEAL = '#2D4A43'        # Dark Teal
ROW_CREAM = '#F4F5F0'         # Warm Cream
ALERT_RED = '#B9382C'         # Alert / Overbilling Red
FAIR_GREEN = '#10B981'        # Fair Market Green
DISCOUNT_BLUE = '#2563EB'     # Discounted Bulk Blue
WHITE = '#FFFFFF'

os.makedirs('figures', exist_ok=True)

# -------------------------------------------------------------
# 1. 2-Tier Challan OCR & Market Intelligence Flow Diagram
# -------------------------------------------------------------
fig, ax = plt.subplots(figsize=(12, 6.5), dpi=300)
ax.set_facecolor('#FCFDFD')
ax.axis('off')

# Title
ax.text(0.5, 0.95, "SiteSync 2-Tier Challan OCR & Market Price Anti-Corruption Pipeline", 
        fontsize=14, fontweight='bold', ha='center', va='center', color=PRIMARY_DARK, fontfamily='sans-serif')

boxes = [
    {"rect": (0.05, 0.65, 0.22, 0.20), "color": TABLE_TEAL, "tcolor": WHITE,
     "title": "CameraX Viewfinder", "sub": "• High-res receipt capture\n• Tap-to-focus & torch\n• FileProvider URI caching"},
    {"rect": (0.38, 0.65, 0.25, 0.20), "color": TABLE_TEAL, "tcolor": WHITE,
     "title": "Tier 1: On-Device ML Kit", "sub": "• Local bounding box detection\n• Fast Challan #, Date parsing\n• Zero cloud latency (<50ms)"},
    {"rect": (0.72, 0.65, 0.24, 0.20), "color": PRIMARY_DARK, "tcolor": WHITE,
     "title": "Tier 2: Multimodal LLM", "sub": "• Gemini 1.5 Flash / Groq\n• Bilingual Bangla+Eng tables\n• Line-item material breakdown"},
    {"rect": (0.72, 0.32, 0.24, 0.22), "color": TABLE_TEAL, "tcolor": WHITE,
     "title": "Brave Search API", "sub": "• Real-time web price check\n• Bangladesh market query\n• Material benchmark fetch"},
    {"rect": (0.38, 0.32, 0.25, 0.22), "color": PRIMARY_DARK, "tcolor": WHITE,
     "title": "Price Variance Math", "sub": "Δ% = [(Billed - Bench)/Bench]*100\nEvaluated against ±8% margin"},
    {"rect": (0.05, 0.44, 0.22, 0.12), "color": ALERT_RED, "tcolor": WHITE,
     "title": "OVER_BILLED (> +8%)", "sub": "Amber/Red anti-corruption flag"},
    {"rect": (0.05, 0.29, 0.22, 0.12), "color": FAIR_GREEN, "tcolor": WHITE,
     "title": "FAIR_MARKET (± 8%)", "sub": "Green verified checkmark"},
    {"rect": (0.05, 0.14, 0.22, 0.12), "color": DISCOUNT_BLUE, "tcolor": WHITE,
     "title": "DISCOUNT_BULK (< -8%)", "sub": "Bulk savings confirmed"},
    {"rect": (0.38, 0.05, 0.58, 0.18), "color": ACCENT_PEACH, "tcolor": PRIMARY_DARK,
     "title": "Relational Room Database Engine (@Transaction)", "sub": "Atomic Stock IN Ledger Entry • Material Stock Recalculation • Firebase Cloud Sync"}
]

for b in boxes:
    x, y, w, h = b["rect"]
    rect = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.015,rounding_size=0.02",
                                  facecolor=b["color"], edgecolor=PRIMARY_DARK, linewidth=1.5)
    ax.add_patch(rect)
    ax.text(x + w/2, y + h - 0.04, b["title"], fontsize=9.5, fontweight='bold', ha='center', va='top', color=b["tcolor"])
    ax.text(x + w/2, y + 0.03, b["sub"], fontsize=7.5, ha='center', va='bottom', color=b["tcolor"])

arrowprops = dict(arrowstyle="->,head_length=0.3,head_width=0.2", color=PRIMARY_DARK, lw=2.0)
ax.annotate("", xy=(0.38, 0.75), xytext=(0.27, 0.75), arrowprops=arrowprops)
ax.annotate("", xy=(0.72, 0.75), xytext=(0.63, 0.75), arrowprops=arrowprops)
ax.annotate("", xy=(0.84, 0.54), xytext=(0.84, 0.65), arrowprops=arrowprops)
ax.annotate("", xy=(0.63, 0.43), xytext=(0.72, 0.43), arrowprops=arrowprops)

# Variance branches to triage
ax.annotate("", xy=(0.27, 0.50), xytext=(0.38, 0.43), arrowprops=arrowprops)
ax.annotate("", xy=(0.27, 0.35), xytext=(0.38, 0.43), arrowprops=arrowprops)
ax.annotate("", xy=(0.27, 0.20), xytext=(0.38, 0.43), arrowprops=arrowprops)

# Variance to Room DB
ax.annotate("", xy=(0.67, 0.23), xytext=(0.50, 0.32), arrowprops=arrowprops)

plt.tight_layout()
plt.savefig('figures/fig_pipeline_diagram.png', dpi=300)
plt.close()
print("Generated fig_pipeline_diagram.png")

# -------------------------------------------------------------
# 2. SiteSync MVVM 3-Tier Architecture Diagram
# -------------------------------------------------------------
fig, ax = plt.subplots(figsize=(11, 7), dpi=300)
ax.set_facecolor('#FCFDFD')
ax.axis('off')

ax.text(0.5, 0.96, "SiteSync 3-Tier Enterprise MVVM Architecture", 
        fontsize=14, fontweight='bold', ha='center', va='center', color=PRIMARY_DARK)

tiers = [
    # Presentation
    {"x": 0.05, "y": 0.68, "w": 0.90, "h": 0.22, "bg": ROW_CREAM, "edge": TABLE_TEAL,
     "title": "PRESENTATION LAYER (Activities, Fragments, ViewBinding, Adapters)",
     "items": [
         ("Executive Dashboard", "CoordinatorLayout, MPAndroidChart, KPI Summary Cards"),
         ("Safety & Telemetry", "Worker Dashboard, Live Decibel Meter, Fall Countdown Modal"),
         ("Field Operations", "Challan Scanner, Geotagged Evidence Dump, Attendance Ledger, Voice Chat")
     ]},
    # ViewModel
    {"x": 0.05, "y": 0.37, "w": 0.90, "h": 0.24, "bg": WHITE, "edge": PRIMARY_DARK,
     "title": "VIEWMODEL & BUSINESS LOGIC LAYER (Coroutines, LiveData, Executors)",
     "items": [
         ("Inventory & Ledger VM", "Stock IN/OUT tracking, Price variance math, Triage tagging"),
         ("Sensor & Safety Coordinator", "SensorEventListener, AudioRecord FFT, FusedLocationProvider"),
         ("Authentication & Auth Gate", "Two-step verification, Project permission, Manager worker kick")
     ]},
    # Data & System
    {"x": 0.05, "y": 0.05, "w": 0.90, "h": 0.25, "bg": ROW_CREAM, "edge": TABLE_TEAL,
     "title": "DATA PERSISTENCE & HARDWARE INTEGRATION LAYER",
     "items": [
         ("Room Relational DB", "SiteSyncDatabase, ItemDao, TransactionDao, IncidentDao, Migrations"),
         ("Hardware Telemetry", "3-Axis Accelerometer, Microphone, GPS Antenna, CameraX"),
         ("Cloud & External APIs", "Firebase Realtime DB, Firebase Cloud Storage, Brave Search Engine")
     ]}
]

for t in tiers:
    rect = patches.FancyBboxPatch((t["x"], t["y"]), t["w"], t["h"], boxstyle="round,pad=0.015,rounding_size=0.02",
                                  facecolor=t["bg"], edgecolor=t["edge"], linewidth=1.8)
    ax.add_patch(rect)
    ax.text(t["x"] + 0.02, t["y"] + t["h"] - 0.035, t["title"], fontsize=9.5, fontweight='bold', color=PRIMARY_DARK)
    
    # Render items
    sub_w = (t["w"] - 0.06) / 3
    for i, (head, desc) in enumerate(t["items"]):
        ix = t["x"] + 0.02 + i * (sub_w + 0.01)
        iy = t["y"] + 0.03
        ih = t["h"] - 0.08
        sub_rect = patches.FancyBboxPatch((ix, iy), sub_w, ih, boxstyle="round,pad=0.01,rounding_size=0.01",
                                          facecolor=TABLE_TEAL if t["y"] > 0.5 else PRIMARY_DARK if t["y"] > 0.2 else ACCENT_PEACH,
                                          edgecolor=PRIMARY_DARK, linewidth=1)
        ax.add_patch(sub_rect)
        tc = WHITE if (t["y"] > 0.5 or t["y"] > 0.2) else PRIMARY_DARK
        ax.text(ix + sub_w/2, iy + ih - 0.03, head, fontsize=8.5, fontweight='bold', ha='center', color=tc)
        ax.text(ix + sub_w/2, iy + 0.02, desc, fontsize=7.0, ha='center', va='bottom', color=tc)

# Vertical flow arrows
arrowprops2 = dict(arrowstyle="<->,head_length=0.3,head_width=0.2", color=PRIMARY_DARK, lw=2.2)
ax.annotate("", xy=(0.20, 0.61), xytext=(0.20, 0.68), arrowprops=arrowprops2)
ax.annotate("", xy=(0.50, 0.61), xytext=(0.50, 0.68), arrowprops=arrowprops2)
ax.annotate("", xy=(0.80, 0.61), xytext=(0.80, 0.68), arrowprops=arrowprops2)

ax.annotate("", xy=(0.20, 0.30), xytext=(0.20, 0.37), arrowprops=arrowprops2)
ax.annotate("", xy=(0.50, 0.30), xytext=(0.50, 0.37), arrowprops=arrowprops2)
ax.annotate("", xy=(0.80, 0.30), xytext=(0.80, 0.37), arrowprops=arrowprops2)

plt.tight_layout()
plt.savefig('figures/fig_architecture.png', dpi=300)
plt.close()
print("Generated fig_architecture.png")

# -------------------------------------------------------------
# 3. Fall Emergency Telemetry Flow Diagram
# -------------------------------------------------------------
fig, ax = plt.subplots(figsize=(10, 5), dpi=300)
ax.set_facecolor('#FCFDFD')
ax.axis('off')

ax.text(0.5, 0.93, "Worker Safety Telemetry & Autonomous Fall Rescue Loop", 
        fontsize=13, fontweight='bold', ha='center', va='center', color=PRIMARY_DARK)

steps = [
    (0.04, 0.45, 0.16, 0.30, TABLE_TEAL, WHITE, "Active Sensory\nMonitoring", "3-Axis Accelerometer\n|a| ~ 9.8 m/s²\nLive Acoustic Decibel"),
    (0.24, 0.45, 0.16, 0.30, ACCENT_PEACH, PRIMARY_DARK, "Impact & Fall\nDetection", "G-Force spike >25 m/s²\nfollowed by immobility\n(<1.0 m/s²)"),
    (0.44, 0.45, 0.18, 0.30, ALERT_RED, WHITE, "15s Emergency\nCountdown Modal", "Haptic pulse & siren\nWorker option to cancel\n'I am OK' dismissal"),
    (0.66, 0.45, 0.16, 0.30, ALERT_RED, WHITE, "GPS Rescue\nDispatch", "Fetch exact latitude/longitude\nBroadcast to Chat Room\nTag high-priority SOS"),
    (0.85, 0.45, 0.13, 0.30, TABLE_TEAL, WHITE, "Google Maps\nNavigation", "Manager clicks SOS\nOpens turn-by-turn\ngeo:lat,lng route")
]

for x, y, w, h, bg, tc, tit, desc in steps:
    rect = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.015,rounding_size=0.02",
                                  facecolor=bg, edgecolor=PRIMARY_DARK, linewidth=1.4)
    ax.add_patch(rect)
    ax.text(x + w/2, y + h - 0.04, tit, fontsize=8.5, fontweight='bold', ha='center', va='top', color=tc)
    ax.text(x + w/2, y + 0.03, desc, fontsize=7.0, ha='center', va='bottom', color=tc)

for i in range(len(steps)-1):
    sx = steps[i][0] + steps[i][2]
    sy = 0.60
    ex = steps[i+1][0]
    ax.annotate("", xy=(ex, sy), xytext=(sx, sy), arrowprops=dict(arrowstyle="->,head_length=0.3,head_width=0.2", color=PRIMARY_DARK, lw=2.0))

# Dismiss branch
cancel_rect = patches.FancyBboxPatch((0.44, 0.10), 0.18, 0.20, boxstyle="round,pad=0.015,rounding_size=0.02",
                                     facecolor=FAIR_GREEN, edgecolor=PRIMARY_DARK, linewidth=1.4)
ax.add_patch(cancel_rect)
ax.text(0.53, 0.24, "False Alarm Cancelled", fontsize=8.5, fontweight='bold', ha='center', va='top', color=WHITE)
ax.text(0.53, 0.12, "Worker taps cancel\nMonitoring resumes", fontsize=7.0, ha='center', va='bottom', color=WHITE)
ax.annotate("", xy=(0.53, 0.30), xytext=(0.53, 0.45), arrowprops=dict(arrowstyle="->,head_length=0.3,head_width=0.2", color=PRIMARY_DARK, lw=1.8))
ax.annotate("", xy=(0.12, 0.45), xytext=(0.44, 0.20), arrowprops=dict(arrowstyle="->,head_length=0.3,head_width=0.2", color=PRIMARY_DARK, lw=1.5, connectionstyle="arc3,rad=-0.2"))

plt.tight_layout()
plt.savefig('figures/fig_fall_state_machine.png', dpi=300)
plt.close()
print("Generated fig_fall_state_machine.png")
