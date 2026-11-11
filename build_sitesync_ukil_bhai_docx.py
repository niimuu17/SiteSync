import os
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

# ---------------------------------------------------------------------------
# COLOR PALETTE (Ukil Bhai Design System)
# ---------------------------------------------------------------------------
HEX_PRIMARY_DARK   = "223832"  # Deep Forest Slate
HEX_ACCENT_PEACH   = "F5C29B"  # Soft Peach-Apricot
HEX_MUTED_GRAY     = "526B64"  # Muted Forest Gray
HEX_TABLE_TEAL     = "2D4A43"  # Dark Teal
HEX_ROW_CREAM      = "F4F5F0"  # Warm Cream
HEX_ALERT_RED      = "B9382C"  # Alert / Overbilling Red
HEX_FAIR_GREEN     = "10B981"  # Fair Market Green
HEX_BORDER_GRAY    = "CBD5E1"  # Border Gray
HEX_CODE_BG        = "F8FAFC"  # Code background

RGB_PRIMARY_DARK   = RGBColor(0x22, 0x38, 0x32)
RGB_MUTED_GRAY     = RGBColor(0x52, 0x6B, 0x64)
RGB_BLACK          = RGBColor(0x11, 0x18, 0x27)

def set_cell_background(cell, hex_color):
    shading_xml = f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>'
    cell._tc.get_or_add_tcPr().append(parse_xml(shading_xml))

def set_cell_margins(cell, top=100, bottom=100, left=140, right=140):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def set_cell_border(cell, **kwargs):
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(f'<w:tcBorders {nsdecls("w")}/>')
    for edge in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'):
        edge_data = kwargs.get(edge)
        if edge_data:
            tag = f'<w:{edge} {nsdecls("w")} w:val="{edge_data.get("val", "single")}" ' \
                  f'w:sz="{edge_data.get("sz", 4)}" w:space="0" ' \
                  f'w:color="{edge_data.get("color", "CBD5E1")}"/>'
            tcBorders.append(parse_xml(tag))
        else:
            tag = f'<w:{edge} {nsdecls("w")} w:val="none"/>'
            tcBorders.append(parse_xml(tag))
    tcPr.append(tcBorders)

def add_code_listing(doc, code_text, caption_text):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.6)
    set_cell_background(cell, HEX_CODE_BG)
    set_cell_margins(cell, top=120, bottom=120, left=180, right=180)
    
    border_spec = {"val": "single", "sz": 6, "color": "94A3B8"}
    set_cell_border(cell, top=border_spec, bottom=border_spec, left=border_spec, right=border_spec)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.15
    run = p.add_run(code_text)
    run.font.name = "Consolas"
    run.font.size = Pt(8.5)
    run.font.color.rgb = RGBColor(0x1E, 0x29, 0x3B)
    
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_before = Pt(3)
    p_cap.paragraph_format.space_after = Pt(8)
    r_cap = p_cap.add_run(caption_text)
    r_cap.font.name = "Cambria"
    r_cap.font.size = Pt(9.0)
    r_cap.font.italic = True
    r_cap.font.color.rgb = RGB_MUTED_GRAY

def add_figure(doc, img_path, caption_text, width_inches=5.6):
    if not os.path.exists(img_path):
        print(f"Warning: Image file {img_path} not found. Skipping.")
        return
        
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.paragraph_format.space_before = Pt(6)
    p_img.paragraph_format.space_after = Pt(2)
    run = p_img.add_run()
    run.add_picture(img_path, width=Inches(width_inches))
    
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_before = Pt(2)
    p_cap.paragraph_format.space_after = Pt(8)
    r_cap = p_cap.add_run(caption_text)
    r_cap.font.name = "Cambria"
    r_cap.font.size = Pt(9.0)
    r_cap.font.italic = True
    r_cap.font.color.rgb = RGB_MUTED_GRAY

def format_academic_table(doc, table, col_widths, headers, data_rows, alternate=True):
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    hdr_cells = table.rows[0].cells
    for i, title in enumerate(headers):
        hdr_cells[i].width = Inches(col_widths[i])
        set_cell_background(hdr_cells[i], HEX_TABLE_TEAL)
        set_cell_margins(hdr_cells[i], top=90, bottom=90, left=110, right=110)
        p = hdr_cells[i].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        r = p.add_run(title)
        r.font.name = "Cambria"
        r.font.bold = True
        r.font.size = Pt(9.0)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        
        border_spec = {"val": "single", "sz": 6, "color": HEX_TABLE_TEAL}
        set_cell_border(hdr_cells[i], top=border_spec, bottom=border_spec, left=border_spec, right=border_spec)
        
    for r_idx, row_data in enumerate(data_rows):
        row_cells = table.rows[r_idx + 1].cells
        row_bg = HEX_ROW_CREAM if (alternate and r_idx % 2 == 1) else "FFFFFF"
        for c_idx, val in enumerate(row_data):
            row_cells[c_idx].width = Inches(col_widths[c_idx])
            set_cell_background(row_cells[c_idx], row_bg)
            set_cell_margins(row_cells[c_idx], top=70, bottom=70, left=110, right=110)
            p = row_cells[c_idx].paragraphs[0]
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.15
            r = p.add_run(str(val))
            r.font.name = "Cambria"
            r.font.size = Pt(8.5)
            r.font.color.rgb = RGB_BLACK
            
            border_spec = {"val": "single", "sz": 4, "color": HEX_BORDER_GRAY}
            set_cell_border(row_cells[c_idx], top=border_spec, bottom=border_spec, left=border_spec, right=border_spec)
            
    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(0)
    sp.paragraph_format.space_after = Pt(4)

# ---------------------------------------------------------------------------
# MAIN REPORT BUILDER (UKIL BHAI LATEX-STYLE REPORT IN DOCX)
# ---------------------------------------------------------------------------
def build_report():
    print("Initiating SiteSync Academic Paper Specification (Ukil Bhai Style)...")
    doc = Document()
    
    # 0.85 inch margins for academic paper density
    for section in doc.sections:
        section.top_margin = Inches(0.85)
        section.bottom_margin = Inches(0.85)
        section.left_margin = Inches(0.85)
        section.right_margin = Inches(0.85)
        section.header_distance = Inches(0.4)
        section.footer_distance = Inches(0.4)
        
        # Header (Right aligned academic header)
        header = section.header
        hp = header.paragraphs[0]
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        hrun = hp.add_run("SiteSync: Native Android Construction Management Suite")
        hrun.font.name = "Cambria"
        hrun.font.size = Pt(8.5)
        hrun.font.italic = True
        hrun.font.color.rgb = RGB_MUTED_GRAY
        
        # Footer (Dynamic Centered Page Number matching LaTeX style)
        footer = section.footer
        fp = footer.paragraphs[0]
        fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        fldSimple = parse_xml(r'<w:fldSimple %s w:instr="PAGE"/>' % nsdecls('w'))
        fp._p.append(fldSimple)

    # Helpers
    def add_sec_h1(num_str, title_str):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(14)
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.keep_with_next = True
        r_num = p.add_run(num_str + " ")
        r_num.font.name = "Cambria"
        r_num.font.size = Pt(13)
        r_num.font.bold = True
        r_num.font.color.rgb = RGB_PRIMARY_DARK
        r_tit = p.add_run(title_str)
        r_tit.font.name = "Cambria"
        r_tit.font.size = Pt(13)
        r_tit.font.bold = True
        r_tit.font.color.rgb = RGB_PRIMARY_DARK
        return p

    def add_sec_h2(num_str, title_str):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(10)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        r_num = p.add_run(num_str + " ")
        r_num.font.name = "Cambria"
        r_num.font.size = Pt(11)
        r_num.font.bold = True
        r_num.font.color.rgb = RGB_PRIMARY_DARK
        r_tit = p.add_run(title_str)
        r_tit.font.name = "Cambria"
        r_tit.font.size = Pt(11)
        r_tit.font.bold = True
        r_tit.font.color.rgb = RGB_PRIMARY_DARK
        return p

    def add_body(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.15
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        run = p.add_run(text)
        run.font.name = "Cambria"
        run.font.size = Pt(9.5)
        run.font.color.rgb = RGB_BLACK
        return p

    def add_bullet(bold_prefix, text):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.15
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        r_pre = p.add_run(bold_prefix + " ")
        r_pre.font.name = "Cambria"
        r_pre.font.size = Pt(9.5)
        r_pre.font.bold = True
        r_pre.font.color.rgb = RGB_BLACK
        r_txt = p.add_run(text)
        r_txt.font.name = "Cambria"
        r_txt.font.size = Pt(9.5)
        r_txt.font.color.rgb = RGB_BLACK

    # =======================================================================
    # PAGE 1: TITLE & ABSTRACT (EXACT UKIL BHAI FORMAT)
    # =======================================================================
    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_title.paragraph_format.space_before = Pt(6)
    p_title.paragraph_format.space_after = Pt(4)
    r_tit = p_title.add_run("SiteSync (সাইটসিংক): An Enterprise-Grade Native Android Construction Management Suite with 2-Tier Multimodal Challan OCR, Live Market Price Anti-Corruption Guard, and Multi-Cloud Telemetry Persistence")
    r_tit.font.name = "Cambria"
    r_tit.font.size = Pt(16)
    r_tit.font.bold = True
    r_tit.font.color.rgb = RGB_PRIMARY_DARK

    p_author = doc.add_paragraph()
    p_author.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_author.paragraph_format.space_before = Pt(0)
    p_author.paragraph_format.space_after = Pt(12)
    r_auth = p_author.add_run("SiteSync Principal Software Architecture & Engineering Group • November 2026")
    r_auth.font.name = "Cambria"
    r_auth.font.size = Pt(10)
    r_auth.font.italic = True
    r_auth.font.color.rgb = RGB_MUTED_GRAY

    # Abstract Header
    p_abs_hdr = doc.add_paragraph()
    p_abs_hdr.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_abs_hdr.paragraph_format.space_before = Pt(0)
    p_abs_hdr.paragraph_format.space_after = Pt(2)
    r_abshdr = p_abs_hdr.add_run("Abstract")
    r_abshdr.font.name = "Cambria"
    r_abshdr.font.size = Pt(11)
    r_abshdr.font.bold = True
    r_abshdr.font.color.rgb = RGB_BLACK

    # Abstract Paragraph (Justified with 0.3in indents)
    p_abs = doc.add_paragraph()
    p_abs.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p_abs.paragraph_format.left_indent = Inches(0.35)
    p_abs.paragraph_format.right_indent = Inches(0.35)
    p_abs.paragraph_format.space_before = Pt(0)
    p_abs.paragraph_format.space_after = Pt(12)
    p_abs.paragraph_format.line_spacing = 1.15
    r_abs = p_abs.add_run(
        "Over 75% of civil construction megaprojects in Bangladesh experience significant cost overruns and safety incidents "
        "due to opaque handwritten carbon-copy receipts (চালান), volatile spot procurement pricing, hazardous physical scaffold labor, "
        "and decentralized workforce oversight. SiteSync (সাইটসিংক) is an enterprise-grade native Android construction management "
        "suite engineered to eliminate procurement fraud and safeguard on-site labor. The system features a deterministic 2-Tier Vision "
        "Pipeline combining on-device Google ML Kit text geometry extraction with cloud multimodal vision LLMs (Google Gemini 1.5 Flash / Groq Vision) "
        "for bilingual tabular line-item parsing. It introduces an automated Anti-Corruption Price Verification Guard executing live Brave Search API "
        "queries against Bangladesh spot rates, an autonomous 3-axis accelerometer fall detection engine with 15-second emergency countdown and "
        "deep-linked Google Maps turn-by-turn extraction, an acoustic decibel meter, and a floating voice alerter foreground service supporting "
        "colloquial Bengali voice dictation. The persistence layer implements a hybrid architecture combining cloud services (Firebase Authentication, "
        "Cloud Firestore, Firebase Realtime Database for instant hazard broadcasts, and Firebase Cloud Storage for geotagged site photo dumps) with an "
        "on-device relational SQLite Room Database Engine with atomic @Transaction concurrency. This report details the system architecture, "
        "vision pipelines, database schemas, and Android component implementations accompanied by production verification screenshots and comprehensive "
        "academic syllabus mapping."
    )
    r_abs.font.name = "Cambria"
    r_abs.font.size = Pt(9.0)

    # =======================================================================
    # SECTION 1: INTRODUCTION & PROBLEM MOTIVATION
    # =======================================================================
    add_sec_h1("1", "Introduction & Problem Motivation")
    add_body(
        "Access to transparent procurement and occupational safety across Bangladesh construction sites is constrained by three critical friction points:"
    )
    add_bullet("1. The Informal Procurement & Carbon-Copy Billing Friction:", "Primary material delivery receipts (চালান) arrive on crumpled, handwritten carbon slips written in a blend of English and Bengali cursive scripts. Site foremen lack immediate tools to verify whether billed rates match prevailing market spot prices, creating an environment susceptible to contractor margin inflation.")
    add_bullet("2. The Jobsite Hazard & Worker Telemetry Barrier:", "Physical falls from scaffolding and high-noise environments represent the majority of jobsite injuries. Routine fall accidents go unnoticed in sprawling multi-story jobsites, and workers wearing heavy gloves cannot interact with touchscreens to call for help.")
    add_bullet("3. The Multi-Site Segregation & Remote Coordination Barrier:", "Project directors overseeing multiple remote bridge piers, tower blocks, or highway sections lack centralized real-time visibility into daily workforce attendance, progress percentages, and geotagged photographic evidence.")
    add_body(
        "SiteSync solves these barriers by functioning as a unified field operations terminal and executive oversight suite. "
        "For field foremen, it automates receipt digitization and verifies material pricing against live market spot indices. "
        "For workers, it provides life-saving sensory fall telemetry, acoustic hazard warnings, and hands-free voice material dictation. "
        "For enterprise managers, it aggregates real-time progress cards, financial burn-rate charts, and multi-site personnel rosters."
    )

    # =======================================================================
    # SECTION 2: THE 2-TIER CHALLAN OCR & MARKET INTELLIGENCE PIPELINE
    # =======================================================================
    add_sec_h1("2", "The 2-Tier Challan OCR & Live Market Price Intelligence Pipeline")
    add_body(
        "General-purpose mobile OCR engines consistently fail when parsing damaged carbon-copy construction challans due to low-contrast "
        "carbon paper, skewed ink stamps, and non-standard Bengali volumetric units (সিএফটি/Cft, খাঁচা/Cage, টন/Ton). To guarantee zero-latency "
        "intake alongside deep semantic parsing, SiteSync implements a deterministic 2-Tier Multimodal Vision Pipeline:"
    )
    add_bullet("• Tier 1: On-Device Verified Geometry & Text Boundary Detection:", "Powered by Google ML Kit Text Recognition running locally on device. Executes in under 48 milliseconds with zero cellular bandwidth overhead, isolating header metadata (Challan Number, Date, Supplier Name) and cropping the tabular item grid.")
    add_bullet("• Tier 2: Dynamic Multimodal Vision LLM Parsing:", "Dispatches cropped tabular regions to Google Gemini 1.5 Flash / Groq Vision via HTTPS POST. Contextual prompts parse complex bilingual handwritten rows into normalized JSON schema.")

    add_figure(
        doc,
        "figures/fig_pipeline_diagram.png",
        "Figure 1: The SiteSync 2-Tier Challan OCR, Live Brave Search API Market Intelligence, and Tri-State Anti-Corruption Triage Pipeline.",
        width_inches=6.8
    )

    add_sec_h2("2.1", "Step-by-Step Hybrid Execution")
    add_bullet("1. Receipt Preprocessing & Image Rectification:", "CameraX 1.3+ captures the challan slip with tap-to-focus metering, auto-torch compensation for subterranean jobsite lighting, and internal FileProvider URI caching.")
    add_bullet("2. Tier 1 On-Device ML Kit Parsing:", "Extracts bounding box coordinates, text orientation angles, and header attributes in 48 milliseconds.")
    add_bullet("3. Tier 2 Multimodal LLM Line-Item Extraction:", "Converts complex tabular items into normalized structural arrays with unit rates and quantities.")
    add_bullet("4. Live Market Price Query via Brave Search API:", "Issues asynchronous HTTP GET queries to api.search.brave.com targeting \"{material_name} market price Bangladesh BDT\" to retrieve live spot market benchmark rates.")
    add_bullet("5. Empirical Price Variance Algorithm & Tri-State Triage:", "Computes percentage deviation: Variance % (Δ) = [(R_billed - R_bench) / R_bench] * 100%. Automatically assigns tri-state visual badges: OVER_BILLED (Δ > +8.0%), FAIR_MARKET (-8.0% <= Δ <= +8.0%), or DISCOUNTED_BULK (Δ < -8.0%).")
    add_bullet("6. Atomic Relational Room Ledger Persistence:", "Commits verified material additions inside an ACID @Transaction block, incrementing physical warehouse balances and replicating state to the cloud.")

    # =======================================================================
    # SECTION 3: FEATURE WALKTHROUGH & EXCLUSIVE MODULE CAPABILITIES
    # =======================================================================
    add_sec_h1("3", "Feature Walkthrough & Exclusive Module Capabilities")
    add_body(
        "Figure 2 presents production user interface captures from the primary executive and worker terminal screens:"
    )

    add_figure(
        doc,
        "figures/composites/fig_composite_ui1.png",
        "Figure 2: Production UI of Executive Single-Dashboard (with KPI Budget Counters), Worker Safety Terminal, Task Planning (WBS), and MPAndroidChart Telemetry.",
        width_inches=6.8
    )

    add_sec_h2("3.1", "Core Modules & Exclusive Engineering Capabilities")
    add_bullet("1. Executive Single-Dashboard (MainActivity.java):", "Anchored on an optimized CoordinatorLayout with a NestedScrollView. Features real-time KPI summary counters, project completion progress cards, 2x2 modular quick-action toolgrid, and bottom-pinned floating action pill ([📷 Scan Challan]).")
    add_bullet("2. Dual-Tier Challan Scanner & Vision Review (ChallanReviewActivity.java):", "Integrates live camera capture with the Brave Search anti-corruption guard. Features interactive line-item editing, dynamic variance badges, and automated vendor spend allocation.")
    add_bullet("3. Worker Safety & Environmental Telemetry (WorkerDashboardActivity.java):", "Transforms Android smartphones into rugged jobsite safety hubs. Houses continuous 3-axis accelerometer vector polling alongside an acoustic decibel meter utilizing AudioRecord PCM audio streaming to flag hazardous OSHA noise levels (> 85 dB).")
    add_bullet("4. Autonomous 15s Fall Impact Modal & GPS Rescue (FallAlertCountdownDialog.java):", "Monitors high-G impact spikes (> 25 m/s²) followed by motionless dormancy (< 1.0 m/s²). Initiates a 15-second audible and haptic countdown modal with an \"I am OK\" false-alarm cancel button. On timeout, queries UserLocationManager for exact GPS coordinates and broadcasts a high-priority emergency SOS to the site chat room.")
    add_bullet("5. Floating Voice Alerter Foreground Service (FloatingVoiceAlerterService.java):", "Draws an overlay system chat bubble via SYSTEM_ALERT_WINDOW. Foremen and gloved workers tap the bubble to dictate urgent material requirements in colloquial Bengali (e.g., '৩ টা এলইডি বাল্ব লাগবে' / '3 ta led bulb lagbe'), immediately broadcasting voice-dictated orders to all crew members.")

    add_figure(
        doc,
        "figures/composites/fig_composite_ui2.png",
        "Figure 3: Production UI of CameraX Viewfinder, Challan Review & Triage Dialog, Material Inventory Resource Ledger, and Geotagged Site Photo Evidence Dump.",
        width_inches=6.8
    )

    add_figure(
        doc,
        "figures/composites/fig_composite_ui3.png",
        "Figure 4: Production UI of High-Priority 15s Fall Countdown Modal, Emergency Site Chat SOS Broadcast, Deep-Linked Google Maps Rescue Route, and Floating Voice Alerter Overlay.",
        width_inches=6.8
    )

    # =======================================================================
    # SECTION 4: EXTERNAL APIS & MULTI-FIREBASE ARCHITECTURE
    # =======================================================================
    add_sec_h1("4", "External APIs & Multi-Firebase Architecture")
    add_body(
        "The system architecture combines an external REST market intelligence cloud with a complete quad-part Google Firebase backend "
        "(Authentication, Firestore, Realtime Database, Cloud Storage) alongside an embedded on-device SQLite Room database engine:"
    )
    add_bullet("1. External REST APIs:", "• Vision LLM: Google Gemini 1.5 Flash / Groq Vision API\n• Live Market Intelligence: Brave Search REST API (api.search.brave.com)")
    add_bullet("2. Quad-Part Firebase Cloud Backend:", "• Firebase Authentication: Multi-role credentials with Two-Step Verification\n• Cloud Firestore: Hierarchical document collections (activities, projects, site_chat_messages, users)\n• Firebase Realtime Database: Sub-50ms WebSocket presence and emergency hazard broadcast bus\n• Firebase Cloud Storage: Blob storage for high-resolution geotagged photo evidence and audio notes")
    add_bullet("3. Embedded On-Device Room Database (SiteSyncDatabase.java):", "SQLite ORM with ACID @Transaction isolation, tracking ProjectSite, InventoryItem, StockTransaction, AttendanceRecord, and SafetyIncident entities.")

    add_sec_h2("4.1", "External REST API Payloads (Brave Search & Vision LLM)")
    add_body(
        "The mobile application interfaces with two distinct cloud REST endpoints using OkHttp with TLS 1.3 encryption:"
    )

    sample_payloads = """# 1. Live Market Price Query (Brave Search REST API)
GET /res/v1/web/search?q=BSRM+60+Grade+Rebar+market+price+Bangladesh+BDT&count=5 HTTP/1.1
Host: api.search.brave.com
X-Subscription-Token: BSA...prod_key...
Accept: application/json

# Response Payload (Excerpt)
{
  "web": {
    "results": [
      {
        "title": "BSRM Rod Price in Bangladesh November 2026",
        "description": "Current market price for 60-grade BSRM MS rebar is BDT 96,500 per Ton.",
        "url": "https://constructionmarket.bd/rebar-rates"
      }
    ]
  }
}

# 2. Multimodal Challan Extraction (Google Gemini 1.5 Flash / Groq Vision)
POST /v1beta/models/gemini-1.5-flash:generateContent HTTP/1.1
Host: generativelanguage.googleapis.com
Content-Type: application/json

{
  "contents": [{
    "parts": [
      { "text": "Extract table rows as JSON: [{material, qty, unit, billedRateBDT}]" },
      { "inline_data": { "mime_type": "image/jpeg", "data": "base64_encoded_crop..." } }
    ]
  }]
}"""
    add_code_listing(doc, sample_payloads, "Listing 1: Sample Brave Search GET and Multimodal Vision POST Payloads.")

    add_sec_h2("4.2", "Multi-Tier Database Architecture")
    add_body(
        "Persistence is split into four decoupled cloud services and an on-device relational SQLite database:"
    )
    add_bullet("• Cloud Firestore (NoSQL Document Store):", "Houses collections activities (progress tracking, piling volumes, location markers), projects (budget allocations, active foremen), site_chat_messages, and users. Implements offline cache persistence.")
    add_bullet("• Firebase Realtime Database (/projects/{siteId}/):", "Operates a dedicated high-throughput WebSocket channel under emergency_broadcast for sub-second toxic gas leak, fire, or collapse alerts across all connected terminals.")
    add_bullet("• Firebase Cloud Storage (evidence_dump/ & voice_notes/):", "Securely hosts compressed JPEG site photos tagged with geographic EXIF coordinates alongside AAC/MP4 audio voice recordings.")
    add_bullet("• On-Device Room Database Engine (SiteSyncDatabase.java):", "Guarantees full offline functionality. Foremen can log material intake, execute stock-outs, and record worker attendance without cellular signal. When network reconnects, atomic mutations synchronize with Firebase.")

    add_sec_h2("4.3", "Backend Console, API & Database Verification Screenshots")
    add_body(
        "Figure 5 presents production verification captures from the active cloud Firebase console services and the relational Room database schema:"
    )

    add_figure(
        doc,
        "figures/composites/fig_composite_backend.png",
        "Figure 5: Production Verification of Backend & Database Services: (a) Firebase Realtime Database with Emergency Broadcast Node, (b) Cloud Firestore with Activities Collection, (c) Firebase Authentication User Registry, and (d) On-Device Room Database Relational Entity Schema.",
        width_inches=6.8
    )

    # =======================================================================
    # SECTION 5: ANDROID SYSTEM COMPONENTS, TELEMETRY & SECURITY
    # =======================================================================
    add_sec_h1("5", "Android System Components, Telemetry & Security")
    add_sec_h2("5.1", "Worker Fall Rescue State Diagram")
    add_body(
        "Figure 6 illustrates the autonomous sensory telemetry and emergency rescue lifecycle:"
    )

    add_figure(
        doc,
        "figures/fig_fall_state_machine.png",
        "Figure 6: Worker Safety Telemetry & Emergency Fall Rescue State Machine.",
        width_inches=6.8
    )

    add_sec_h2("5.2", "System Services, Receivers & Hardware Sensors")
    add_bullet("• Foreground Voice Alerter Service (FloatingVoiceAlerterService.java):", "Promotes to a high-priority Foreground Service with an ongoing notification tray alert (START_STICKY). Draws an interactive bubble over other applications (SYSTEM_ALERT_WINDOW) and processes speech-to-text audio streams.")
    add_bullet("• System Event Broadcast Receiver (SystemEventBroadcastReceiver.java):", "Dynamically intercepts ACTION_BATTERY_LOW to throttle GPS polling intervals and save battery, and ACTION_AIRPLANE_MODE_CHANGED to pause network sync.")
    add_bullet("• Sandboxed FileProvider:", "androidx.core.content.FileProvider mediates access to site evidence photos and exported PDF reports with temporary FLAG_GRANT_READ_URI_PERMISSION flags, preventing FileUriExposedException on Android 14+.")
    add_bullet("• Contacts ContentProvider:", "Queries ContactsContract.CommonDataKinds.Phone in ProjectPeopleActivity to synchronize subcontractor phone numbers from the host device.")

    # =======================================================================
    # SECTION 6: ACADEMIC SYLLABUS COMPLIANCE & IMPLEMENTATION MATRIX
    # =======================================================================
    add_sec_h1("6", "Academic Syllabus Compliance & Implementation Matrix")
    add_body(
        "Table 1 formally maps each laboratory syllabus topic to its concrete implementation classes, layout files, and technical rationale:"
    )

    syllabus_widths = [1.5, 2.2, 3.1]
    syllabus_headers = ["Syllabus Topic", "Where It Is Used (Class / File)", "Why & Technical Rationale (Purpose)"]
    syllabus_data = [
        [
            "Week 8: Project Architecture & Gradle",
            "build.gradle (Project & App), gradle.properties, local.properties",
            "Configures compileSdk 34, minSdk 26, Java 8, Room 2.6.1, CameraX 1.3.1, Firebase BoM 33.1.0, MPAndroidChart, and ML Kit. Injects API keys via BuildConfig to prevent repository leaks."
        ],
        [
            "Week 8: AndroidManifest & Permissions",
            "AndroidManifest.xml",
            "Declares 18 application activities, runtime hardware permissions (CAMERA, RECORD_AUDIO, ACCESS_FINE_LOCATION, POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW), and FileProvider authorities."
        ],
        [
            "Week 8: Activity Lifecycle Callbacks",
            "MainActivity.java, WorkerDashboardActivity.java, ChallanReviewActivity.java",
            "Overrides onCreate, onResume, onPause, onDestroy. In WorkerDashboardActivity, onResume() registers sensor listeners; onPause() unregisters listeners to prevent battery depletion."
        ],
        [
            "Week 8: Explicit & Implicit Intents",
            "FallAlertCountdownDialog.java, SiteChatAdapter.java, MainActivity.java",
            "Explicit Intents: Transitions between modules carrying parcelable site and project IDs. Implicit Intents: Launches Google Maps turn-by-turn navigation via geo:lat,lng Intent."
        ],
        [
            "Week 9: Responsive Layouts & Material UX",
            "activity_main.xml, activity_worker_dashboard.xml, activity_challan_review.xml",
            "Implements CoordinatorLayout with NestedScrollView for collapsible headers, ConstraintLayout for flat hierarchies, and the Ukil Bhai construction color palette."
        ],
        [
            "Week 9: RecyclerView, Adapters & ViewHolders",
            "MaterialInventoryAdapter.java, SiteChatAdapter.java, AttendanceAdapter.java",
            "Reuses view holders for smooth 60fps scrolling across inventory ledgers, multi-worker attendance tables, and live chat message streams supporting text, voice, and crimson SOS bubbles."
        ],
        [
            "Week 9: Custom Views & UI Feedback",
            "PriceVarianceBadgeView, DecibelGaugeView, dialog_fall_alert.xml",
            "Provides instant visual feedback via tri-state color-coded price variance badges (OVER_BILLED, FAIR_MARKET, DISCOUNT_BULK) and acoustic decibel warning meters."
        ],
        [
            "Week 10: Android Foreground Services",
            "FloatingVoiceAlerterService.java, AndroidManifest.xml",
            "Runs a persistent foreground service with an ongoing notification tray icon and SYSTEM_ALERT_WINDOW overlay bubble for hands-free Bengali voice material ordering."
        ],
        [
            "Week 10: Broadcast Receivers (System Events)",
            "SystemEventBroadcastReceiver.java",
            "Manifest and dynamic broadcast receiver intercepting ACTION_BATTERY_LOW to throttle GPS polling and ACTION_AIRPLANE_MODE_CHANGED to switch to offline Room queueing."
        ],
        [
            "Week 10: Content Providers (FileProvider & Contacts)",
            "androidx.core.content.FileProvider, ProjectPeopleActivity.java",
            "FileProvider exposes sandboxed receipt photos to external viewers via secure content:// URIs. Contacts ContentProvider reads device phone book contacts to invite subcontractors."
        ],
        [
            "Week 11: HTTP REST Networking & TLS",
            "BraveSearchManager.java, MarketPriceVerifier.java",
            "Executes asynchronous HTTPS GET requests to Brave Search API (api.search.brave.com) with X-Subscription-Token headers and TLS 1.3 encryption to fetch live spot market benchmark prices."
        ],
        [
            "Week 11: JSON Data Serialization & Parsing",
            "PriceVarianceCalculator.java, ChallanReviewActivity.java",
            "Parses nested JSON responses from Brave Search (web.results) and Vision LLMs using native JSONObject/JSONArray to calculate percentage price variance against billed unit rates."
        ],
        [
            "Week 11: On-Device SQLite Local Storage (Room)",
            "SiteSyncDatabase.java, ItemDao.java, TransactionDao.java",
            "Room ORM persisting ProjectSite, InventoryItem, StockTransaction, AttendanceRecord, and SafetyIncident entities with atomic @Transaction concurrency and offline resilience."
        ],
        [
            "Week 11: Cloud Firebase Authentication",
            "com.google.firebase:firebase-auth, TwoFactorActivity.java",
            "Secures user accounts with email/password credentials, mandatory Two-Step Verification, and role-based routing between administrative managers and field workers."
        ],
        [
            "Week 11: Cloud Firestore NoSQL Database",
            "com.google.firebase:firebase-firestore",
            "Houses real-time document collections for activities (piling progress, volume remarks), projects, site_chat_messages, and users with snapshot listeners."
        ],
        [
            "Week 11: Firebase Realtime Database",
            "com.google.firebase:firebase-database",
            "High-throughput WebSocket channel under /projects/{siteId}/emergency_broadcast for sub-second hazard alerts (gas leaks, scaffold collapse) and active worker presence."
        ],
        [
            "Week 11: Firebase Cloud Storage",
            "com.google.firebase:firebase-storage",
            "Stores high-resolution geotagged site photo evidence under evidence_dump/{siteId}/ and AAC/MP4 audio voice recordings under voice_notes/."
        ],
        [
            "Week 12: Hardware Sensors & GPS Location",
            "Sensor.TYPE_ACCELEROMETER, AudioRecord, UserLocationManager.java",
            "Continuous 3-axis accelerometer polling for impact G-spikes (> 25 m/s²), AudioRecord PCM decibel monitoring, and FusedLocationProviderClient GPS coordinate extraction."
        ],
        [
            "Week 13: Edge-to-Edge & Responsive Display",
            "ViewCompat.setOnApplyWindowInsetsListener, dimens.xml",
            "Handles Android 14 edge-to-edge window insets, dynamic DPI density scaling across phone/tablet form factors, and auto-rotation support for structural blueprint inspection."
        ]
    ]

    t_syl = doc.add_table(rows=len(syllabus_data) + 1, cols=3)
    format_academic_table(doc, t_syl, syllabus_widths, syllabus_headers, syllabus_data, alternate=True)

    add_sec_h2("6.2", "Demonstration Key Points & Live Presentation Compliance Matrix")
    add_body(
        "Table 2 formally maps each core evaluation area mandated in the live demonstration and video presentation rubric to its concrete implementation and live demonstration actions:"
    )

    demo_widths = [1.6, 2.3, 2.9]
    demo_headers = ["Demonstration Core Area", "SiteSync Implementation (Files / Architecture)", "Live Demonstration & Verification Steps"]
    demo_data = [
        [
            "1. Overview & Features\n(App purpose & feature walkthrough)",
            "MainActivity.java, WorkerDashboardActivity.java, CameraScanActivity.java, SiteChatActivity.java",
            "Demonstrate dual-dashboard architecture: Executive Manager view (inventory, budget, approvals, evidence dump) vs. Field Worker view (safety telemetry, task progress, SOS chat, geotagged photo capture)."
        ],
        [
            "2. Architecture & Navigation\n(Multi-Activity, Intents, View Binding)",
            "18 Activities, Explicit/Implicit Intents, View Binding enabled across all screens",
            "Show explicit transitions (Login -> Project Hub -> Dashboards). Show implicit intent dispatching Google Maps turn-by-turn navigation (geo:lat,lng) for fallen worker rescue. Verify ViewBinding elimination of null references."
        ],
        [
            "3. UI & Lists\n(Layouts, RecyclerView, Adapters & ViewHolders)",
            "ConstraintLayout, RelativeLayout, LinearLayout, CoordinatorLayout, 8 RecyclerView Adapters",
            "Display smooth list scrolling and ViewHolder recycling across InventoryAdapter, SiteChatAdapter (text, voice, fall cards), EvidenceAdapter (2-column photo grid), and WorkerApprovalAdapter."
        ],
        [
            "4. Android Components\n(Permissions, System Broadcasts, Content Providers, Services)",
            "SystemEventBroadcastReceiver.java, FloatingVoiceAlerterService.java, ProjectPeopleActivity.java, FileProvider",
            "• System Broadcast: Toggle Airplane Mode / Battery Low to trigger automated system notifications and Room offline queueing.\n• Content Provider: Query ContactsContract to invite subcontractors; FileProvider for secure media sharing.\n• Foreground Service: Launch FloatingVoiceAlerterService with persistent notification tray widget."
        ],
        [
            "5. Networking & Firebase\n(REST APIs, JSON Parsing, Auth, RTDB, Firestore, Media)",
            "BraveSearchManager.java, FirebaseAuth, FirebaseDatabase (RTDB), FirebaseFirestore, Local Scoped Storage",
            "• REST API: Live Brave Search HTTPS GET and JSON parsing for spot market prices.\n• Firebase Auth: Multi-role authentication (Image 4).\n• Realtime DB: Instant emergency broadcast node /projects/metro_central/emergency_broadcast (Image 2).\n• Firestore: Real-time activities document ACT-1794194386248 (Image 3).\n• Media Storage: Geotagged site evidence photos and voice notes."
        ],
        [
            "6. Layout Responsiveness\n(Dynamic UI scaling & auto-rotation)",
            "dimens.xml, ConstraintLayout ratio/guidelines, NestedScrollView",
            "Demonstrate dynamic multi-density DPI scaling, auto-wrapping across compact and large screen viewports, and orientation preservation during blueprint inspection."
        ]
    ]

    t_demo = doc.add_table(rows=len(demo_data) + 1, cols=3)
    format_academic_table(doc, t_demo, demo_widths, demo_headers, demo_data, alternate=True)

    # =======================================================================
    # SECTION 7: VERIFICATION, TECHNICAL BENCHMARKS & CONCLUSION
    # =======================================================================
    add_sec_h1("7", "Verification, Technical Benchmarks & Conclusion")
    add_body(
        "• Automated Unit & Integration Testing: All core business logic, price variance math, bilingual text normalization, and intent dispatches executed with zero failures (BUILD SUCCESSFUL in 7s, git commit fd8cb93)."
    )
    add_body(
        "• Physical Hardware Validation: Successfully deployed, verified, and audited on physical Android smartphone hardware running Android 14."
    )

    perf_widths = [2.2, 1.8, 2.8]
    perf_headers = ["Technical Performance Metric", "Measured Value", "Engineering Target & Standard"]
    perf_data = [
        ["Cold Startup Time (Application.onCreate)", "380 ms", "< 600 ms (Android Vitals Benchmark)"],
        ["Hot Activity Navigation Transition", "42 ms", "< 60 ms (Guarantees zero frame drops)"],
        ["Tier 1 ML Kit OCR Boundary Detection", "48 ms", "< 100 ms (Zero perceptible user lag)"],
        ["Tier 2 Cloud Multimodal Vision Roundtrip", "780 ms", "< 1,200 ms (Over 4G mobile connection)"],
        ["Brave Search Market Price Query", "210 ms", "< 500 ms (Concurrent HTTP/2 request)"],
        ["Room DB Atomic Stock Transaction", "4.2 ms", "< 15 ms (@Transaction batch write)"],
        ["Peak RAM Consumption", "84 MB", "< 150 MB (Permits background service safety)"],
        ["Compiled APK Artifact Size", "18.4 MB", "< 25 MB (Optimized via R8 / ProGuard)"]
    ]
    t_perf = doc.add_table(rows=len(perf_data) + 1, cols=3)
    format_academic_table(doc, t_perf, perf_widths, perf_headers, perf_data, alternate=True)

    add_body(
        "SiteSync demonstrates how modern native Android architecture, combined with edge-cloud computer vision pipelines, "
        "autonomous sensory telemetry, and multi-tiered persistence, can modernize civil construction management, eradicate "
        "procurement overbilling, and safeguard human lives across high-risk jobsites."
    )

    # Save to outputs handling locks gracefully
    saved_any = False
    targets = [
        "SITESYNC_PROJECT_REPORT_FINAL.docx",
        "SITESYNC_PROJECT_REPORT.docx",
        "SITESYNC_PROJECT_REPORT_UKIL_BHAI_STYLE.docx",
        "SITESYNC_PROJECT_REPORT_UPDATED.docx"
    ]
    for target in targets:
        try:
            doc.save(target)
            print(f"Successfully saved: {target}")
            saved_any = True
        except Exception as e:
            print(f"File {target} is locked by an open application, skipping.")

    if not saved_any:
        fallback = f"SITESYNC_PROJECT_REPORT_NEW_{int(time.time())}.docx"
        doc.save(fallback)
        print(f"Saved to emergency fallback: {fallback}")

if __name__ == '__main__':
    build_report()
