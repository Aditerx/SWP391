# SCMS Repository Analysis Report

**Date:** 10 November 2026  
**Repository:** supplied local checkout `C:\Users\ASUS\Desktop\FA26\SWP\SWP391`  
**Reference:** `Report7_Final Project Report.docx.pdf` (128 pages)

## Repository structure analyzed

- `src/main/java/com/sportscenter/`: Spring modules for auth, users/permissions, catalog, rooms/classes/enrollment, Advanced registration, sessions/incidents, invoices/payments, training/attendance, reports, notifications, chat, media and audit.
- `database/`: PostgreSQL schemas, migrations and README; root schema and rebuild schema have different data-preservation behavior.
- `FE_Project-main/src/`: React/Vite routes, views, components and API adapters. AGENTS.md explicitly calls this an old/reference FE; current FE integration source is unknown.
- Build metadata and operating guidance: `pom.xml`, Maven wrapper, FE package manifest, AGENTS.md, docs00 Active, docs04 D01-D19, relevant docs01 contract and database README. Archived logs/old-docs were not bulk-read.

## Technologies identified

Java 17, Spring Boot 3.3.5, Spring MVC, JPA, Security, Validation, Mail, Actuator and springdoc OpenAPI; React 19, React Router 7, Vite 8, Tailwind 4, Recharts 3, Motion and lucide-react in the legacy/reference FE. PostgreSQL/Supabase is the documented current DB; SQL Server driver/scripts exist but runtime is unaccepted. H2 supports tests. Integrations present: VNPay IPN/return/QueryDR, Google Identity, SMTP, avatar/media and Anthropic/DeepSeek chatbot components. Config does not establish live connectivity.

## Actors and modules

**Actors:** Visitor/Guest, Admin, Center Manager, Receptionist, Coach and Member. Permissions combine role, dynamic authority, state, center, ownership, assignment and workflow checks.

**Modules:** accounts/authentication; profile/account administration; catalog/specializations/rooms; Normal classes/enrollment; Advanced registration/entitlement; schedule/incident recovery; invoice/payment/resolution; attendance/QR/training; holidays; reports/audit; notifications; support chat/AI; scheduled expiry/finalization.

## Implemented, partial and ambiguous areas

- Backend controllers and services implement the core flows. D01-D19 describe decisions separately from code and release status.
- General membership routes/tables remain structurally represented, while current decisions retire the general membership product. `member_packages` represents Advanced entitlements.
- Current frontend source/deployment is not identified. `FE_Project-main` screens are described as repository reference only; live API wiring is not verified.
- VNPay, Google, SMTP, media and AI depend on credentials/network/provider state. No live verification was made.
- `ai_consultations` and `support_requests` structures do not prove a separately shipped AI workout recommendation feature.
- No live DB was queried. Entity summaries reflect checked-in schema/JPA, not production schema.

## Assumptions and limitations

1. The pasted GitHub URL was a placeholder. Analysis used the accessible local checkout, not an external clone.
2. The supplied PDF was used for organization/style, not SCMS business logic.
3. Approved policy does not imply cloud migration or live UI acceptance.
4. No tests, deploy, provider calls or cloud mutations were run for this documentation task.
5. SRS is a repository snapshot dated 10 November 2026; refresh when current FE, production schema or deployment evidence is available.
