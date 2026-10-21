# Google Stitch Frontend Export Reference

## Project Information

- **Stitch Project Name**: `ClaimGuard Review Dashboard`
- **Stitch Project ID**: `9971280076559410786`
- **Retrieval Date**: October 21, 2026
- **Status**: Visual & UI specification reference only (Read-Only Reference)
- **Design System**: ClaimGuard Enterprise

---

## Retrieved Screens

| Screen # | Screen Title | Screen ID | File Name | Description |
|---|---|---|---|---|
| 01 | Executive Dashboard | `3f9b878f94d54d11b83539d046cc103f` | `screen-01-executive-dashboard.html` | Operations KPI dashboard with metric cards, volume trends SVG chart, portfolio donut chart, category bars, and recent claims table |
| 02 | All Claims | `6a179e2c0dc1475d9e6904cc0ccf8ecb` | `screen-02-all-claims.html` | Searchable/filterable claim inventory table with multi-criteria filters, pagination, status pills, and action menus |
| 03 | Submit New Claim | `c16bee79850743c1b5a9d4c0dfe80f0b` | `screen-03-submit-claim.html` | Multi-section claim intake form with client info, policy lookup, incident details, document upload area, and preview dialog |
| 04 | Claim Details & Review | `84fb7586bc9c494682a6f62e78863754` | `screen-04-claim-details-review.html` | Investigator adjudication console with claimant dossier, rule trigger alerts, audit trail timeline, verification checks, and decision action bar |

---

## Technical Characteristics & Architecture

1. **Standalone HTML Files**:
   - Each file is an independent, self-contained HTML document designed for 1280px+ desktop viewports.
   - Standard browser rendering requires no compilation or build pipeline to preview visually.

2. **Tailwind CSS via CDN**:
   - Styles are loaded dynamically using the Tailwind CSS Play CDN:
     `<script src="https://cdn.tailwindcss.com"></script>`
   - Custom design system tokens (`ClaimGuard Enterprise`) are defined inside an inline `<script id="tailwind-config">` block with extended colors, spacing, and typography scales.

3. **Typography & Iconography**:
   - Google Fonts Inter (`https://fonts.googleapis.com/css2?family=Inter:wght@100..900&display=swap`)
   - Google Material Symbols Outlined (`https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined`)

4. **Remote Image Assets**:
   - User profile avatars and logos reference Google-hosted remote URLs (`https://lh3.googleusercontent.com/...`).
   - If opened offline without internet access, these images will fail to load unless replaced by local static assets.

---

## Known Limitations & Integration Boundary (Constraint C-06)

1. **No Backend Connectivity**:
   - All data displayed in these screens is hardcoded synthetic placeholder data.
   - Form controls (`<form>`, `<input>`, `<select>`, `<button>`) do not have functional `action` endpoints, REST API bindings, or AJAX handlers.
2. **No Templating Syntax**:
   - These files contain pure HTML. They do not contain Spring Boot Thymeleaf tags (`th:text`, `th:field`, `th:each`, `th:href`).
3. **No Client-Side Routing**:
   - Sidebar and navigation links use placeholder anchor references (`href="#"` or data attributes like `data-path="dashboard"`).
4. **Usage Notice**:
   - **Do not serve these raw files directly as production application pages.**
   - These files serve as the ground truth visual and CSS specification for Task 3 (UI/UX design & documentation) and subsequent frontend template integration.
