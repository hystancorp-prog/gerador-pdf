# CLAUDE.md — Hystan Security & Architecture Guide

This file provides permanent context for Claude Code sessions on this repository.
Read this entirely before touching any file.

---

## Project Overview

**Hystan** is a Spring Boot SaaS deployed on Railway that converts Excel (.xlsx)
attendance sheets into formatted PDF reports.

- **Auth:** Firebase (Google OAuth2 + email/password) on the frontend
- **Payments:** Stripe Checkout (subscriptions)
- **PDF Engine:** Apache PDFBox
- **Excel Parser:** Apache POI
- **Deploy:** Railway (Docker, memory-constrained environment ~512MB RAM)
- **Frontend:** Vanilla HTML/CSS/JS (auth.html, dashboard.html, index.html)

---

## Architecture — Data Flow

```
Browser → POST /gerar-pdf (multipart .xlsx)
       → PdfController.java        [ENTRY POINT — highest attack surface]
       → LeitorPlanilha.java       [Excel parsing — XML bomb risk]
       → GeradorPDF.java           [PDF generation — path traversal risk]
       → Response: PDF bytes
```

---

## 🔴 SECURITY RULES — NON-NEGOTIABLE

These rules apply to EVERY change. Never bypass them for convenience.

### 1. File Upload Validation (PdfController.java)

Every uploaded file MUST pass ALL checks before any processing:

```java
// ORDER MATTERS — fail fast, cheapest checks first

// 1. Size limit — reject before reading content
if (file.getSize() > 5 * 1024 * 1024) throw new FileTooLargeException();

// 2. Filename sanitization — never trust client filename
String safeName = Paths.get(file.getOriginalFilename()).getFileName().toString();
if (!safeName.matches("[a-zA-Z0-9_\\-\\.]{1,100}")) throw new InvalidFilenameException();

// 3. Magic bytes — NEVER trust extension alone
byte[] header = new byte[8];
file.getInputStream().read(header);
if (!isValidXlsxMagicBytes(header)) throw new InvalidFileTypeException();
// XLSX magic bytes: 50 4B 03 04 (PK zip signature)

// 4. Content-Type header check (secondary, not primary)
if (!file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    throw new InvalidContentTypeException();
```

### 2. XML Bomb / Billion Laughs Prevention (LeitorPlanilha.java)

Apache POI XSSFWorkbook is vulnerable to XML entity expansion attacks.
ALWAYS configure POI to disable external entities:

```java
// Before opening any workbook:
ZipSecureFile.setMinInflateRatio(0.001); // POI built-in protection
// Row limit — never process more than 5000 rows
if (aba.getLastRowNum() > 5000) throw new TooManyRowsException("Max 5000 rows");
// Column validation — expected: col0=name, col1=service, col2=value, col3=date
// Reject if row has unexpected extra columns with executable content
```

### 3. Temp File Cleanup (PdfController.java + LeitorPlanilha.java)

ALWAYS use try-with-resources or finally blocks. Leaked temp files = disk exhaustion on Railway:

```java
File tempXlsx = null;
File tempPdf = null;
try {
    tempXlsx = File.createTempFile("upload_", ".xlsx");
    tempPdf  = File.createTempFile("report_", ".pdf");
    // ... processing ...
    return ResponseEntity.ok(pdfBytes);
} finally {
    if (tempXlsx != null) tempXlsx.delete();
    if (tempPdf  != null) tempPdf.delete();
}
```

### 4. Endpoint Authentication (SecurityConfig.java + PdfController.java)

`/gerar-pdf` MUST verify Firebase ID token on every request:

```java
// Extract from Authorization header: "Bearer <firebase_id_token>"
// Verify with Firebase Admin SDK:
FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
String uid = decoded.getUid();
// Check plan in DB — block if gratuito and over limit
```

**Never** use `anyRequest().permitAll()` in production. Current SecurityConfig is dev-only.

### 5. Output Path Traversal Prevention (GeradorPDF.java)

Never construct output paths from user input:

```java
// WRONG:
String saida = pastaUsuario + "/" + nomeEmpresa + ".pdf";

// CORRECT:
File tempPdf = File.createTempFile("relatorio_", ".pdf");
String saida = tempPdf.getAbsolutePath();
```

### 6. PDF Content Injection (GeradorPDF.java)

All user-supplied strings written to PDF MUST be sanitized:

```java
private static String sanitizePdfText(String input) {
    if (input == null) return "";
    return input
        .replaceAll("[\\x00-\\x1F\\x7F]", "") // remove control chars
        .replaceAll("[(){}<>\\[\\]/\\\\%]", "") // remove PDF operators
        .trim()
        .substring(0, Math.min(input.length(), 200)); // hard length limit
}
// Apply to: nomeEmpresa, nome, servico, valor, data — ALL fields
```

### 7. Rate Limiting

Every endpoint needs rate limiting to prevent abuse:

```java
// Use bucket4j or a simple in-memory map:
// /gerar-pdf     → max 10 requests/minute per IP
// /criar-checkout → max 5 requests/minute per IP
// Global         → max 100 requests/minute per IP
```

### 8. Request Size Limits (application.properties)

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB
server.tomcat.max-http-form-post-size=6MB
```

### 9. Security Headers (SecurityConfig.java)

Add to every HTTP response:

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; script-src 'self' https://www.gstatic.com; " +
        "connect-src 'self' https://identitytoolkit.googleapis.com"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.enable())
    .contentTypeOptions(Customizer.withDefaults())
);
```

### 10. Stripe Webhook Validation (StripeController.java)

Never trust Stripe events without signature verification:

```java
// ALWAYS verify webhook signature:
Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
// Never process plan upgrades without this check
```

### 11. Environment Variables — Never Hardcode

These values MUST come from Railway environment variables, never from code:

- `STRIPE_SECRET_KEY`
- `STRIPE_PRICE_BASICO`
- `STRIPE_PRICE_PRO`
- `STRIPE_WEBHOOK_SECRET`
- `FIREBASE_PROJECT_ID`
- `DATABASE_URL`

### 12. Exception Handling — Never Leak Stack Traces

```java
// WRONG — leaks internal structure to attacker:
return ResponseEntity.status(500).body(e.getMessage());

// CORRECT:
log.error("PDF generation failed for uid={}", uid, e);
return ResponseEntity.status(500).body("Erro interno. Tente novamente.");
```

---

## Memory Constraints (Railway ~512MB RAM)

- **Max upload:** 5MB
- **Max rows per sheet:** 5.000
- **Max concurrent requests:** configure Tomcat thread pool max=20
- **PDFBox:** always call `doc.close()` — PDFBox leaks memory if not closed
- **POI:** always call `workbook.close()` — same reason
- **Never** load the entire PDF into memory for large reports — stream directly

```properties
# application.properties
server.tomcat.threads.max=20
server.tomcat.threads.min-spare=5
```

---

## Column Contract — Excel Input

The parser expects EXACTLY this column order. Validate before processing:

| Col | Index | Type     | Example          |
|-----|-------|----------|------------------|
| A   | 0     | String   | "Maria Silva"    |
| B   | 1     | String   | "Limpeza facial" |
| C   | 2     | Numeric  | 150.00           |
| D   | 3     | Date     | 15/04/2025       |

If any row has null in col 2 (value), skip with warning — never throw NPE.

---

## What NOT to Touch Without Discussion

- `auth.html` — Firebase config is production, any change breaks login
- `StripeController.java` — price IDs are live, wrong change = billing failure
- `Dockerfile` — Railway build depends on exact structure
- `pom.xml` dependencies — version changes need compatibility testing
- `SecurityConfig.java` — changes need full security review

---

## Known Technical Debt (fix in order)

1. `SecurityConfig` allows all requests — needs Firebase token filter
2. `LeitorPlanilha` has no row limit — XML bomb vulnerability open
3. `PdfController` temp files not cleaned on exception — disk leak
4. No rate limiting on any endpoint — DDoS surface open
5. `GeradorPDF` accepts raw user strings in PDF — injection possible
6. `Usuario/auth` entities exist but are not enforced on `/gerar-pdf`
7. No Stripe webhook signature verification
8. Stack traces exposed in 500 responses

---

## Build & Run

```bash
# Local build
./mvnw clean package -DskipTests
java -jar target/*.jar

# Docker
docker build -t hystan .
docker run -p 8080:8080 --env-file .env hystan

# Railway deploys automatically on git push to main
```

---

## Environment Setup (local .env)

```env
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PRICE_BASICO=price_...
STRIPE_PRICE_PRO=price_...
STRIPE_WEBHOOK_SECRET=whsec_...
DATABASE_URL=postgresql://...
```

---

## Testing a Fix

After any security change:

1. Test with a valid .xlsx — must work normally
2. Test with a .exe renamed to .xlsx — must reject at magic bytes check
3. Test with a 10MB file — must reject at size check
4. Test with a crafted XML bomb — must reject or timeout safely
5. Test without Authorization header — must return 401
6. Test with expired Firebase token — must return 401