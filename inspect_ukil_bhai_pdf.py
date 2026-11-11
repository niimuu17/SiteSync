import os
import fitz # PyMuPDF

pdf_path = r"C:\Users\User\Downloads\UKIL_BHAI_PROJECT_REPORT.pdf"
doc = fitz.open(pdf_path)
print("Total pages:", len(doc))

toc = doc.get_toc()
print("\n--- TABLE OF CONTENTS ---")
for item in toc[:30]:
    print(item)

print("\n--- PAGE 1 TEXT (COVER PAGE) ---")
print(doc[0].get_text())

print("\n--- PAGE 2 TEXT ---")
if len(doc) > 1:
    print(doc[1].get_text()[:1500])

os.makedirs("figures/ukil_bhai_pages", exist_ok=True)
for i in range(min(6, len(doc))):
    page = doc[i]
    pix = page.get_pixmap(dpi=150)
    out_img = f"figures/ukil_bhai_pages/page_{i+1}.png"
    pix.save(out_img)
    print(f"Saved {out_img}")
