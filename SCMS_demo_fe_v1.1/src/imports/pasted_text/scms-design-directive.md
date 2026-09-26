Role: Senior Product Designer & Enterprise UX Architect.
Project: SPORTS CENTER MANAGEMENT SYSTEM (SCMS) - B2B Operations Web App.
Scope: Desktop & Laptop Web Only (1024px, 1280px, 1440px, 1920px). No Mobile/Tablet UI.

================================================================================
CRITICAL DESIGN DIRECTIVE: STRICT ANTI-AI / ENTERPRISE SAAS STYLEGUIDE
================================================================================
Target Aesthetic: Practical, high-density, reliable Enterprise SaaS (equivalent to Stripe Dashboard, Linear, GitHub Enterprise, or Datadog). 
ABSOLUTELY FORBIDDEN (Do NOT use):
- NO sparkles, magic wands, or energy icons (No ✨, 🪄, etc.).
- NO dark cyber/futuristic gradients, neon cyan, or purple-to-pink gradient fills.
- NO glassmorphism, backdrop-blur, heavy multi-layered glowing drop shadows, or floating cards.
- NO 3D floating clay objects, abstract blobs, or cartoon illustrations.
- NO oversized rounded corners (Strictly limit border-radius: inputs/buttons max 6px, modal/cards max 8px; NEVER use fully rounded 9999px pills for buttons/inputs).
- NO low-density layouts with vast empty spaces. Every pixel must serve operational utility.

TECHNICAL FOUNDATION & TOKENS:
1. Color System (Solid, Functional & WCAG 2.1 AA Compliant):
   - Base Surfaces: Neutral Light Mode only. Background canvas: #F8FAFC (Slate-50), Card/Table surface: #FFFFFF, Dividers/Borders: #E2E8F0 (Slate-200, 1px solid).
   - Brand Primary: Deep Corporate Navy #0F172A (Slate-900) or #1E3A8A (Blue-900). Interactive Accent: #2563EB (Blue-600), Hover: #1D4ED8.
   - Status Palette (Muted backgrounds with high-contrast text, strictly functional):
     * Success / Active / Present: Background #DCFCE7, Text #166534, Border #BBF7D0.
     * Pending / Grace Period / Late: Background #FEF3C7, Text #92400E, Border #FDE68A.
     * Error / Expired / Cancelled / Absent: Background #FEE2E2, Text #991B1B, Border #FECACA.
     * Neutral / Reversed / Draft: Background #F1F5F9, Text #475569, Border #CBD5E1.
2. Typography & Numbers:
   - Font Family: Inter, SF Pro, or standard clean geometric Sans-serif supporting full Vietnamese diacritics.
   - Tabular Numerals: All monetary values (VND), phone numbers, dates, times, and member IDs MUST use Monospace or `font-variant-numeric: tabular-nums` for strict vertical alignment.
   - Scales: Display 24px, H1 20px, H2 16px, Body 14px, Caption/Meta 12px. Row heights: Compact 40px–44px for high operational scanability.

================================================================================
CORE APPLICATION SHELL & LAYOUT CONSTRAINTS
================================================================================
- Left Sidebar (Fixed width: 240px, collapsible to 64px icon-only):
  * Crisp border-right: 1px solid #E2E8F0.
  * Role-based menu items (hidden if unauthorized, not disabled). Active state uses a 2px solid left accent bar with a subtle tinted background (#EFF6FF), not heavy gradients.
- Top Bar (Height: 56px, sticky):
  * Left: Structural Breadcrumb trail.
  * Right: Language Switcher (Segmented control: "VI | EN" with crisp 1px borders), Notification bell, User avatar + Role badge (e.g., [Manager]), Logout.
- Main Workspace:
  * Maximum content constraint on 1920px (centered max-width: 1600px) to prevent table rows from stretching unnaturally.
  * 12-column grid system, 16px/24px gutters, 8px spacing system.

================================================================================
TASK-ORIENTED UX PATTERNS (NO CONVERSATIONAL / CHAT INTERFACES)
================================================================================
1. High-Density Data Tables (Members, Staff, Payments, Audit Logs):
   - Sticky table headers with subtle background (#F8FAFC).
   - Inline column filters, multi-column sorting indicators, search bar with integrated filter drawer.
   - Row actions grouped in a clean triple-dot icon button (Hover: #F1F5F9) or direct text links ("Chi tiết / View", "Chỉnh sửa / Edit").
   - Explicit pagination footer: "Showing 1-20 of 248 records" with page selector.
2. Operational Class Schedule (Timeline / Matrix Grid):
   - True calendar timeline: X-axis = Time blocks (06:00 to 22:00 in 30/60 min increments); Y-axis = Rooms / Studios / Courts.
   - Sessions rendered as solid, crisp schedule blocks indicating: Class Name, Coach, Capacity ratio (e.g., 18/20), and Status Badge.
3. Rapid Front-Desk Check-in & Attendance:
   - Top shortcut bar with barcode/phone number input for instant 1-second lookup.
   - Coach Attendance sheet: Matrix list of students with 3 distinct radio-style segmented buttons: [Có mặt / Present], [Đi muộn / Late], [Vắng / Absent].
   - Attendance Correction Modal: Displays "Current Value: Present" -> "New Value: Absent" with a mandatory single-line textarea "Reason for correction" and a warning badge: "This action is immutable and logged in Audit Trail".
4. Inherited Membership Package Cards:
   - Visual tier comparison: Visual stacked hierarchy showing that higher packages contain 100% of lower-tier benefits with unambiguous checkmarks.

================================================================================
STRICT LOCALIZATION & MICROCOPY RULES (VIETNAMESE & ENGLISH)
================================================================================
- Language Switcher maintains exact page state, active filters, and scroll position.
- Strictly single-language per view. Never use slash concatenation like "Thành viên / Members" on production mockups.
- Text Overflow Protection: All buttons, badges, table headers, and form labels must be built with Figma Auto Layout (H: Hug Contents, W: Hug or Fill) to accommodate Vietnamese text which is typically 20%–35% longer than English.
- Formatting Standards:
  * VI: Date DD/MM/YYYY, 24-hour time (e.g., 14:30), Currency formatted as "1.500.000 đ".
  * EN: Date DD MMM YYYY (e.g., 19 Sep 2026), 24-hour or 12-hour AM/PM, Currency formatted as "1,500,000 VND".

================================================================================
ROLES, PERMISSIONS & SCOPE BOUNDARIES (STRICT MVP)
================================================================================
1. Center Manager: Complete analytics, Revenue KPI, Staff/Member CRUD, Audit Logs (Read-only immutable table), Class & Room assignment.
2. Coach: Restricted to assigned classes and assigned members. Attendance entry & reason-required modification. Progression logs (Form-based text/reps, no medical diagnosis).
3. Receptionist: Member lookup, Package enrollment, Manual simulated payment entry (Cash/Transfer/POS mock), Class booking on behalf of members.
4. Member: Read-only profile, class catalog with real-time seat availability (Denial banner when package is invalid/expired), my bookings, personal attendance history.

================================================================================
FIGMA FILE ARCHITECTURE & DELIVERABLES REQUIRED
================================================================================
Organize deliverables into pages:
00 – Cover & Changelog
01 – Foundations (Tokens, 8px Grid, Typography, Neutral/Functional Color Palette)
02 – Localization VI/EN (Side-by-side comparison of 8 core screens in VI and EN)
03 – Component Library (Variants, Auto Layout, Form controls, Tables, Modals, Badges)
04 – Architecture & Sitemap
05 – Core User Flows
06 to 09 – Role-specific Screen Suites (Manager, Coach, Receptionist, Member)
10 – Business State Handling (Grace Period 72h banner, Booking denial states, Validation errors)
11 – Responsive Desktop Breakpoints (1024px, 1280px, 1440px, 1920px)
12 – Prototype Specs & UX Handoff Notes