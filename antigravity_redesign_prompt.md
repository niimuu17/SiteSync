# Antigravity Task: Enterprise Architecture Refactor & UI Redesign for SiteSync

You are an elite Principal Android Architect and Senior Mobile UI/UX Engineer with 20+ years of experience. Your mission is to take the existing native Android project **SiteSync** (Java, minSdk 26, targetSdk 32, ViewBinding) and execute a complete, production-grade overhaul:
1. **Visual UI Overhaul**: Transition the entire UI from its basic white/flat Material look into the high-end, dark industrial construction telemetry interface shown in the Dribbble design reference (dark graphite canvases, safety-amber/yellow accents, semi-circular telemetry arc gauges, diagonal-hatch progress bars, and floating dark glassmorphism cards).
2. **Backend & Security Migration**: Completely purge all mock local `SharedPreferences` JSON persistence. Migrate to **Firebase Authentication with 2-Step Verification (MFA)**, **Cloud Firestore** (for structured entities, document history, and offline sync), **Firebase Realtime Database** (for live telemetry and active presence), and **Firebase Storage** (for CAD drawings and inspection photos).
3. **Enterprise Architecture**: Implement a clean **MVVM + Repository Pattern** with Room caching and WorkManager for resilient offline-first field synchronization.

---

## SECTION 1: VISUAL DESIGN SYSTEM SPECIFICATION (DRIBBBLE REFERENCE)

The current app uses generic white cards with plain buttons. Redesign every screen to match the exact aesthetic of the Dribbble reference:

### 1. Color Palette & Theming
Create/update `res/values/colors.xml`:
- **Surface & Background**:
  - `bg_canvas`: `#111317` (Deep dark slate canvas)
  - `bg_surface_elevated`: `#1A1D24` (Elevated card background)
  - `bg_surface_card`: `#222631` (Secondary card / metric container)
  - `stroke_card_border`: `#2E3442` (Subtle 1dp border outline for cards)
- **Accents & Telemetry**:
  - `accent_safety_yellow`: `#F59E0B` (Primary action, active tabs, warning meters)
  - `accent_hazard_orange`: `#F97316` (Secondary telemetry accents, notifications)
  - `accent_telemetry_blue`: `#3B82F6` (Electric blueprint blue for MEP & inspections)
  - `gauge_track_dark`: `#2C313E` (Inactive track color for circular meters)
- **Typography & Hierarchy**:
  - `text_primary`: `#FFFFFF` (High contrast headings and KPI values)
  - `text_secondary`: `#94A3B8` (Labels, metadata, timestamps)
  - `text_tertiary`: `#64748B` (Muted section captions, helper text)

### 2. Custom UI Components to Create
1. **`SemiCircleGaugeView` (Custom View)**:
   - Extend `View`. In `onDraw(Canvas canvas)`, draw a 180° or 240° semi-circular arc gauge using `Path` and `Paint(Paint.ANTI_ALIAS_FLAG)`.
   - Attributes: `app:gaugeProgress="85"`, `app:gaugeTrackColor="@color/gauge_track_dark"`, `app:gaugeStrokeColor="@color/accent_safety_yellow"`, `app:strokeWidth="8dp"`.
   - Render a central percentage value with dynamic trend indicators (e.g., `+2.23 ▲` in green/white).
2. **`HatchedProgressBar` (Custom View or Drawable)**:
   - A horizontal progress bar featuring diagonal 45° warning stripes (`BitmapShader` or canvas line loops) inspired by industrial warning tape for "In Progress" milestones.
3. **Floating Segmented Horizontal Filter**:
   - Horizontal pill selector with rounded corners (`corners_radius="16dp"`). Active pill gets solid safety yellow fill with black bold text; inactive pills get dark outline `#2E3442` with muted gray text.
4. **Floating Bottom Navigation Bar**:
   - Dark rounded floating pill dock (`#1A1D24` with 1dp border `#2E3442` and drop elevation), replacing the standard edge-to-edge system nav.

---

## SECTION 2: SECURITY, AUTHENTICATION & FIREBASE ARCHITECTURE

### 1. Firebase Authentication with 2-Step Verification (MFA)
- Replace mock user authentication in `AuthActivity` and `AuthRepository`.
- **Primary Authentication**: Firebase Email/Password or Phone Auth.
- **Two-Step Verification (MFA)**:
  - Implement Firebase Multi-Factor Authentication (`FirebaseAuthMultiFactor`).
  - Enroll second factor via SMS (`PhoneMultiFactorGenerator.getAssertion(...)`).
  - If `FirebaseAuthMultiFactorException` is thrown during login, intercept the `MultiFactorResolver`, trigger an SMS code dispatch, display the 6-digit OTP bottom sheet, and finalize login with `resolveSignIn()`.
- **Role-Based Access Control (RBAC)**:
  - Store the user's role (`PROJECT_MANAGER`, `SITE_SUPERVISOR`, `ENGINEER`) in **Firebase Custom Claims** and mirror in `/users/{uid}` in Firestore.
  - Enforce token claims in `AuthRouter` before allowing navigation to dashboards.

### 2. Cloud Firestore & Realtime Database Split
- **Cloud Firestore**:
  - Use for core document-oriented data:
    - `/projects/{projectId}`
    - `/projects/{projectId}/activities/{activityId}` (Site progress milestones)
    - `/inspections/{inspectionId}` (QA/QC records, digital checklists)
    - `/documents/{documentId}/revisions/{revId}` (Drawing metadata & revision trees)
    - `/daily_reports/{reportId}` (5-section Daily Site Reports)
  - Enable offline caching:
    ```java
    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(true)
        .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
        .build();
    FirebaseFirestore.getInstance().setFirestoreSettings(settings);
    ```
- **Firebase Realtime Database (RTDB)**:
  - Use for real-time site telemetry and fast synchronization:
    - `/site_telemetry/{projectId}/active_workers` (Live headcount)
    - `/site_telemetry/{projectId}/cut_and_fill` (Real-time volume deltas)
    - `/issues/{issueId}/comments` (Live real-time discussion stream with typing indicators)
    - `/presence/{userId}` (Online/Offline connectivity state on site)

### 3. Production Security Rules
Generate strict Firestore Security Rules file (`firestore.rules`):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAuthenticated() {
      return request.auth != null;
    }
    function hasRole(role) {
      return isAuthenticated() && (
        request.auth.token.role == role || 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == role
      );
    }
    
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if request.auth.uid == userId;
    }
    match /projects/{projectId}/activities/{activityId} {
      allow read: if isAuthenticated();
      allow write: if hasRole('ENGINEER') || hasRole('PROJECT_MANAGER');
    }
    match /inspections/{inspectionId} {
      allow read: if isAuthenticated();
      allow write: if hasRole('ENGINEER') || hasRole('SITE_SUPERVISOR');
    }
    match /daily_reports/{reportId} {
      allow read: if isAuthenticated();
      allow create, update: if hasRole('ENGINEER');
      allow delete: if hasRole('PROJECT_MANAGER');
    }
  }
}
```

---

## SECTION 3: SCREEN-BY-SCREEN IMPLEMENTATION TASKS

### Screen 1: Welcome & Role Gateway (`WelcomeActivity.java`, `activity_welcome.xml`)
- Rebuild layout to use a full-height dark slate theme (`#111317`).
- Top banner: Minimalist orange brand badge with industrial typography: **"Centralized Construction Project Progress"**.
- Replace old white cards with 3 sleek, dark frosted glass role cards:
  - Card 1: **Project Manager** (Executive overview, budgets, sign-offs)
  - Card 2: **Site Supervisor** (Workforce attendance, shifts, safety checks)
  - Card 3: **Field Engineer** (Telemetry, blueprint sync, QA checks)
- Each card must feature:
  - Background: `#1A1D24` with 1dp border `#2E3442`.
  - Icon inside an amber/blue tinted squircle container.
  - Right chevron indicator or safety-yellow action pill (`SELECT`).
  - Active state ripple effect in translucent yellow (`#33F59E0B`).

### Screen 2: Engineer Control Center & Telemetry Hub (`EngineerDashboardActivity.java`)
- Redesign into a multi-section construction telemetry cockpit:
  1. **Top Bar**: Construction site switcher dropdown (e.g., `Metro Central Tower • Level 14`), notification bell with orange dot, and user avatar.
  2. **Work Progress Hero Card**:
     - Large numeric KPI: `68%` in 48sp bold typography.
     - Hatched progress bar with start date (`11/02/2026`) and target completion (`23/03/2026`).
     - "Delay: 12 days" warning badge in dark orange.
  3. **Telemetry 2-Column Grid**:
     - Foundation (`85%` with `SemiCircleGaugeView` + `+2.23 ▲`)
     - Structural Work (`62%` with `SemiCircleGaugeView` + `+1.23 ▲`)
     - Electrical & MEP (`40%` with `SemiCircleGaugeView` + `+4.10 ▲`)
     - Architectural Finishing (`18%` with `SemiCircleGaugeView` - `1.00 ▼`)
  4. **Module Quick Launchers**: Horizontal cards with icons linking to the 5 core modules:
     - Module 1: Site Progress Tracking
     - Module 2: QA/QC Inspections
     - Module 3: Blueprint & CAD Hub
     - Module 4: Issues & RFIs
     - Module 5: Daily Site Reports (PDF)

### Screen 3: Site Progress & Cut/Fill Telemetry (`ProgressMonitoringActivity.java`)
- Implement the "Cut and Fill" telemetry widget from the reference:
  - Coordinate elevation indicators: `-10 Ft`, `-3.10 Ft`, `14 Ft`, `-15 Ft`, `-5.12 Ft`, `00 Ft`.
  - Volume counter: `Net: 224,400 cu yd`.
- RecyclerView displaying active site milestones with dark card containers, live status pills (`Pending`, `In Progress`, `Completed`), and progress update sliders.

### Screen 4: Blueprint CAD Viewer & Revision Tree (`DocumentManagementActivity.java`)
- Rebuild category filter: Architectural, Structural, Electrical, Plumbing.
- Card items showing:
  - Blueprint preview thumbnail.
  - "Latest Approved Revision" badge in safety green.
  - Submittal timestamp and approving consultant's name.
  - One-tap intent to open in `PdfViewerActivity` with pinch-to-zoom controls.

### Screen 5: Daily Site Reports & PDF Sharing Engine (`DailyReportListActivity.java`)
- Sync report drafts directly with Firestore offline persistence.
- Update `PdfReportGenerator.java`:
  - Output vector PDF using standard A4 dimensions (`595 x 842 pt`).
  - Style header with enterprise branding, jobsite telemetry, contractor license, weather conditions, labor headcounts, materials, and digital sign-off blocks.
  - Trigger secure dispatch to Project Managers using `androidx.core.content.FileProvider`.

---

## SECTION 4: REPOSITORY & DATA REFACTOR DIRECTIVES

1. Delete `SharedPreferences` JSON serialization logic inside:
   - `ProgressRepository.java`
   - `InspectionRepository.java`
   - `DocumentRepository.java`
   - `IssueRepository.java`
   - `DailyReportRepository.java`
2. Implement Room entities and DAOs for local caching:
   - `AppDatabase.java` with tables: `activities`, `inspections`, `documents`, `issues`, `daily_reports`.
3. In each Repository, implement a Single Source of Truth pattern:
   - Expose `LiveData<List<T>>` or `Flow<List<T>>` queried directly from Room.
   - Attach Firestore `addSnapshotListener()` in the Repository to push incoming cloud updates into Room asynchronously on an IO executor.
   - Enqueue outbound writes to Firestore with offline write support (`set()` / `add()`), falling back to `WorkManager` for large binary media (photos, PDF files).
