import os
from PIL import Image, ImageDraw, ImageFont

os.makedirs('figures/composites', exist_ok=True)

def create_4pane_phone_grid(img_paths, labels, output_path, canvas_w=1600, canvas_h=850):
    """Combines 4 mobile phone screenshots into a clean 1x4 horizontal grid with lettered labels."""
    canvas = Image.new('RGB', (canvas_w, canvas_h), '#FFFFFF')
    draw = ImageDraw.Draw(canvas)
    
    try:
        sub_font = ImageFont.truetype("segoeuib.ttf", 26)
    except:
        sub_font = ImageFont.load_default()

    pane_w = (canvas_w - 40) // 4
    target_img_w = pane_w - 16
    target_img_h = canvas_h - 75

    for i, path in enumerate(img_paths):
        if not os.path.exists(path):
            continue
        img = Image.open(path)
        img_ratio = img.width / img.height
        new_h = target_img_h
        new_w = int(new_h * img_ratio)
        if new_w > target_img_w:
            new_w = target_img_w
            new_h = int(new_w / img_ratio)
            
        resized_img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
        
        pane_x = 20 + i * pane_w
        x_offset = pane_x + (pane_w - new_w) // 2
        y_offset = 12 + (target_img_h - new_h) // 2
        
        draw.rectangle([x_offset - 1, y_offset - 1, x_offset + new_w + 1, y_offset + new_h + 1], outline='#CBD5E1', width=2)
        canvas.paste(resized_img, (x_offset, y_offset))
        
        lbl = labels[i]
        bbox = draw.textbbox((0, 0), lbl, font=sub_font)
        lbl_w = bbox[2] - bbox[0]
        lbl_x = pane_x + (pane_w - lbl_w) // 2
        draw.text((lbl_x, canvas_h - 42), lbl, fill='#223832', font=sub_font)
        
    canvas.save(output_path, quality=95)
    print(f"Generated 4-pane composite: {output_path}")

def create_2x2_backend_grid(img_paths, labels, output_path, canvas_w=1600, canvas_h=1100):
    """Combines 4 backend/database screenshots into a 2x2 grid."""
    canvas = Image.new('RGB', (canvas_w, canvas_h), '#FFFFFF')
    draw = ImageDraw.Draw(canvas)
    
    try:
        sub_font = ImageFont.truetype("segoeuib.ttf", 26)
    except:
        sub_font = ImageFont.load_default()

    col_w = (canvas_w - 60) // 2
    row_h = (canvas_h - 100) // 2

    positions = [
        (20, 20),
        (canvas_w // 2 + 10, 20),
        (20, canvas_h // 2 + 10),
        (canvas_w // 2 + 10, canvas_h // 2 + 10)
    ]

    for i, path in enumerate(img_paths):
        if not os.path.exists(path):
            continue
        img = Image.open(path)
        img_ratio = img.width / img.height
        
        target_w = col_w - 20
        target_h = row_h - 60
        
        new_w = target_w
        new_h = int(new_w / img_ratio)
        if new_h > target_h:
            new_h = target_h
            new_w = int(new_h * img_ratio)
            
        resized_img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
        
        box_x, box_y = positions[i]
        x_offset = box_x + (col_w - new_w) // 2
        y_offset = box_y + (target_h - new_h) // 2
        
        draw.rectangle([x_offset - 2, y_offset - 2, x_offset + new_w + 2, y_offset + new_h + 2], outline='#CBD5E1', width=2)
        canvas.paste(resized_img, (x_offset, y_offset))
        
        lbl = labels[i]
        bbox = draw.textbbox((0, 0), lbl, font=sub_font)
        lbl_w = bbox[2] - bbox[0]
        lbl_x = box_x + (col_w - lbl_w) // 2
        draw.text((lbl_x, box_y + row_h - 40), lbl, fill='#223832', font=sub_font)

    canvas.save(output_path, quality=95)
    print(f"Generated 2x2 backend composite: {output_path}")

if __name__ == '__main__':
    # Figure 2: Citizen/Field Production UI
    create_4pane_phone_grid(
        ['figures/fig_dashboard.png', 'figures/fig_worker_dashboard.png', 'figures/fig_tasks.png', 'figures/fig_charts.png'],
        ['(a) Executive Dashboard', '(b) Worker Safety Terminal', '(c) Task Breakdown (WBS)', '(d) MPAndroidChart Telemetry'],
        'figures/composites/fig_composite_ui1.png'
    )
    
    # Figure 3: Procurement, OCR & Evidence
    create_4pane_phone_grid(
        ['figures/fig_scanner.png', 'figures/fig_review.png', 'figures/fig_ledger.png', 'figures/fig_evidence_dump.png'],
        ['(a) CameraX Viewfinder', '(b) Challan Review & Triage', '(c) Inventory Stock-In Ledger', '(d) Geotagged Evidence Dump'],
        'figures/composites/fig_composite_ui2.png'
    )
    
    # Figure 4: Fall Rescue, Chat & Voice Alerter
    create_4pane_phone_grid(
        ['figures/fig_fall_dialog.png', 'figures/fig_chat_fall.png', 'figures/fig_maps_rescue.png', 'figures/fig_voice_alerter.png'],
        ['(a) 15s Fall Countdown Modal', '(b) Emergency Chat SOS', '(c) Google Maps Turn-by-Turn', '(d) Floating Voice Alerter'],
        'figures/composites/fig_composite_ui3.png'
    )
    
    # Figure 5: Backend & Database Verification
    create_2x2_backend_grid(
        ['figures/fig_db_realtime.png', 'figures/fig_db_firestore.png', 'figures/fig_db_auth.png', 'figures/fig_room_schema.png'],
        ['(a) Firebase Realtime DB: Emergency Broadcast', '(b) Cloud Firestore: Activities & Projects', '(c) Firebase Authentication: User Registry', '(d) Room DB: Relational SQLite Schema'],
        'figures/composites/fig_composite_backend.png'
    )
    print("All composite figures created successfully!")
