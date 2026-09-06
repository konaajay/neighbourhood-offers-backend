# 🛍️ Neighbourhood Offers — Pay for Footfall, Not Ads

> **Hyperlocal Merchant Footfall & Voucher Redemption Platform**  
> *Production-grade Spring Boot 3 & React application demonstrating verified counter redemption, multi-tenant isolation, pessimistic locking idempotency under weak networks, and human-in-the-loop AI offer generation.*

---

## 📌 Executive Summary & Assessment Deliverables

This repository contains the complete implementation of the **Neighbourhood Offers** platform. The core thesis is simple: **Small merchants should pay only when a real customer walks into their store and completes a purchase, not for passive views, impressions, or unclaimed clicks.**

### 🔗 Live URLs & Repository Information
- **Deployed Frontend**: [https://neighbourhood-offers-frontend.netlify.app/login](https://neighbourhood-offers-frontend.netlify.app/login)
- **Deployed Backend API**: [https://neighbourhood-offers-backend-1.onrender.com](https://neighbourhood-offers-backend-1.onrender.com)
- **Public GitHub Repository**: [https://github.com/konaajay/neighbourhood-offers-backend](https://github.com/konaajay/neighbourhood-offers-backend)
- **Database**: Cloud MySQL 8.0.45 hosted on **Aiven** (`defaultdb`)

---

## 1. 🌟 Project Overview

Neighbourhood Offers connects local neighborhood retail shops with nearby shoppers through a three-sided flow:

```
+-----------------------------------------------------------------------------------+
|                                 PLATFORM FLOW                                     |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ SHOPKEEPER ]                                                                   |
|   1. Tops up points balance (1 pt = ₹1 nominal value).                            |
|   2. Creates discounts using AI Studio (natural language or image -> structured). |
|   3. Toggles offers active/inactive and tracks footfall analytics.                |
|                               |                                                   |
|                               v                                                   |
|  [ SHOPPER ]                                                                      |
|   1. Browses hyperlocal offers without upfront payment.                           |
|   2. Claims a digital voucher (Generates unique QR code & 8-char code).           |
|   3. ⚠️ ZERO points are deducted from the merchant at claim time.                 |
|   4. Shopper visits the physical shop with voucher in hand.                       |
|                               |                                                   |
|                               v                                                   |
|  [ COUNTER STAFF / CASHIER ]                                                      |
|   1. Looks up voucher code at counter terminal.                                  |
|   2. Adds customer's actual in-store products to the current purchase bill.       |
|   3. Verifies minimum bill satisfaction and reviews discounted total.             |
|   4. Taps Redeem:                                                                 |
|      - Merchant points deducted (10 points per redemption).                       |
|      - Voucher status transitions to REDEEMED.                                    |
|      - Idempotency guards protect against double-deductions on lagging networks.  |
+-----------------------------------------------------------------------------------+
```

---

## 2. 🚀 How to Run Locally

### Prerequisites
- **Java**: OpenJDK 21 LTS
- **Node.js**: v18+ or v20+ / v22+
- **MySQL**: MySQL 8.0 (running locally on port `3306` or cloud instance)
- **Maven**: Included via Maven Wrapper (`./mvnw` / `mvnw.cmd`)

---

### Step 1: Database Setup
Create a local MySQL database:
```sql
CREATE DATABASE neighbourhood_offers CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### Step 2: Backend Configuration & Startup

1. Open `backend/src/main/resources/application.properties` (or supply environment variables):
   ```properties
   # Server Port
   server.port=8088

   # MySQL DataSource (Defaults connect to localhost:3306)
   spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/neighbourhood_offers?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
   spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
   spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root}
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

   # Hibernate DDL Auto
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

   # JWT Secret
   app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
   app.jwt.expiration-ms=86400000

   # Optional Gemini AI API Key
   app.ai.gemini.api-key=${GEMINI_API_KEY:}
   app.ai.gemini.model=gemini-1.5-flash
   ```

2. Run the Backend using the Maven wrapper:
   ```bash
   cd backend
   # Windows PowerShell / CMD:
   ./mvnw.cmd spring-boot:run

   # macOS / Linux:
   ./mvnw spring-boot:run
   ```
   *The backend starts on `http://localhost:8088` and automatically seeds realistic demonstration data on initial boot.*

---

### Step 3: Frontend Setup & Startup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   npm install
   ```

2. Create or verify `.env`:
   ```env
   VITE_API_URL=http://localhost:8088
   ```

3. Launch the development server:
   ```bash
   npm run dev
   ```
   *Frontend is accessible at `http://localhost:5173`.*

---

## 3. 👥 Seeded Demo Accounts

The application automatically seeds realistic retail personas with ready-to-test accounts. On the login screen (`/login`), 1-click login cards allow instant role switching:

| Persona Name | Email | Password | Role | Assigned Shop | Balance | Demonstration Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Platform Super Admin** | `admin@platform.com` | `password123` | `ROLE_SUPER_ADMIN` | *Global* | — | Shop onboarding, merchant provisioning, top-up request approval/rejection. |
| **Anitha Devi** | `anitha@shop.com` | `password123` | `ROLE_SHOPKEEPER` | Anitha Silks & Sarees | 100 pts | Active apparel merchant; AI offer creation, offer toggles, busy-day analytics. |
| **Rahul Verma** | `rahul@shop.com` | `password123` | `ROLE_SHOPKEEPER` | Rahul Organic Groceries | 120 pts | **Multi-tenant isolation**: cannot view or edit Anitha's offers (`403 Forbidden`). |
| **Meena Joseph** | `meena@shop.com` | `password123` | `ROLE_SHOPKEEPER` | Meena Bakeries & Cafe | **8 pts** | **Low-points guardrail**: 8 pts balance is below 10 pts redemption cost; triggers alerts. |
| **Ramesh Sharma** | `sharmaji@shop.com` | `password123` | `ROLE_SHOPKEEPER` | Sharmaji Electronics | 220 pts | High-balance electronics shopkeeper. |
| **Deepa Nair** | `deepa@counter.com` | `password123` | `ROLE_COUNTER_STAFF` | Anitha Silks & Sarees | — | Cashier redemption, product catalog bill builder, minimum bill validation. |
| **Amit Kulkarni** | `amit@counter.com` | `password123` | `ROLE_COUNTER_STAFF` | Rahul Organic Groceries | — | **Weak-network testing**: rapid double-tap idempotency protection. |
| **Meena Staff** | `meenastaff@counter.com` | `password123` | `ROLE_COUNTER_STAFF` | Meena Bakeries & Cafe | — | Rejection test when shop balance is depleted. |
| **Priya Sharma** | `priya@shopper.com` | `password123` | `ROLE_SHOPPER` | *Customer* | — | Hyperlocal browsing, claiming vouchers (0 points deducted from merchant). |
| **Vikram Patel** | `vikram@shopper.com` | `password123` | `ROLE_SHOPPER` | *Customer* | — | Secondary shopper persona for cross-user tests. |

---

## 4. 🧠 Key Decisions and Assumptions

Where the assessment brief was open-ended, the following technical and architectural decisions were made:

### 1. Technology Stack
- **Spring Boot 3.4.3 & Java 21**: Selected for strong transactional semantics (`@Transactional`), robust enterprise concurrency controls, and seamless JPA repository abstractions.
- **React 18 + TypeScript + Vite**: Chosen for fast client-side rendering, strict interface typing matching backend DTOs, and modular component design.
- **Tailwind CSS + Lucide Icons**: Enabled an accessible, modern, and responsive UI with mobile-friendly counter screens.

### 2. Database & Data Integrity
- **Relational Model (MySQL 8.0)**: Chosen because financial points deductions, voucher statuses, and multi-item bill transactions require strict ACID guarantees and foreign-key integrity.
- **`spring.jpa.hibernate.ddl-auto=update`**: Tables are automatically created and updated by Hibernate without manual DDL script execution, while preserving existing production data.
- **`User.active` Handling**: Mapped with `@Builder.Default private Boolean active = true` and `@PrePersist` fallback hooks to guarantee compatibility with `NOT NULL` constraints on legacy databases.

### 3. Points and Footfall Billing Model
- **Cost Per Redemption**: Set to **10 Points** per verified walk-in redemption.
- **Points Deduction Point**: Points are **never** deducted on voucher creation or voucher claiming. Points are deducted **strictly upon physical counter redemption**.
- **Low Points Guardrail**: If a shop's points balance is less than `costPerRedemption` (e.g. Meena's 8 points < 10 points), redemption is blocked with HTTP 400 `InsufficientPointsException` ("Shop has insufficient points balance (8 points left, 10 required)").

### 4. Idempotency & Weak-Network Protection
- Counter staff in physical shops frequently face intermittent 3G/4G or spotty Wi-Fi. A cashier might tap "Redeem" 2 or 3 times while waiting for the response.
- **Solution**:
  1. Requests pass an `X-Idempotency-Key` (UUIDv4) and body `idempotencyKey`.
  2. Database locks are acquired using **Pessimistic Write Locking** (`SELECT ... FOR UPDATE` via `shopRepository.findByIdForUpdate` and `claimRepository.findByClaimCodeForUpdate`).
  3. If a voucher is already in `REDEEMED` status, the backend detects the replay and returns an **Idempotent Replay Receipt** (`isIdempotentReplay: true`, `pointsDeducted: 0`, HTTP 200 OK) with the original transaction timestamp and remaining balance. Points are **never deducted twice**.

### 5. In-Store Bill & Product Purchase Flow
- Rather than an online shopping cart with courier checkout, the counter interface acts as a **Point of Sale (POS) Assistant**:
  1. Cashier enters or scans the customer's voucher code.
  2. Cashier selects the actual products purchased from the store's product catalog.
  3. System verifies if the bill meets `minBillAmount`.
  4. System applies percentage or flat discount (capped by `maxDiscountAmount`).
  5. Cashier verifies the discounted bill and executes redemption.

### 6. Multi-Tenant Shop Isolation
- Every shopkeeper and counter staff member belongs to a specific `shop_id`.
- Shopkeepers cannot view, toggle, or modify offers belonging to other shops. Any attempt triggers HTTP 403 `UnauthorizedShopAccessException`.
- Counter staff can only redeem vouchers issued by their own shop (`claim.getShop().getId() == staff.getShop().getId()`).

### 7. AI Offer Parsing: Human-in-the-Loop
- Shopkeepers can enter messy conversational inputs like:  
  *"flat 20% off all sarees till diwali, min bill 1500"*
- The AI extracts: `discountType: PERCENTAGE`, `discountValue: 20`, `minBillAmount: 1500`, `category: Apparel & Sarees`, `endDate`.
- **Key Decision**: The system never publishes directly without confirmation. The parsed output opens in an **editable verification modal** where the shopkeeper can adjust values, upload banners, and confirm before going live.
- **Offline Fallback Engine**: If no Gemini API key is supplied, a built-in deterministic regex/NLP parser extracts the fields so the feature works out of the box in all environments.

### 8. Payment Emulation
- To emulate merchant point top-ups without introducing third-party payment gateway costs, shopkeepers submit a `PointTopUpRequest`.
- Platform Super Admins can review, approve, or reject top-up requests on `/admin/topups`, creating transactional point audit entries upon approval.

---

## 5. 🤖 AI Tools Used

### Development Assistance AI
- **Google Antigravity**: Primary agentic pair programmer used for architecture scaffolding, full-stack implementation, concurrency test generation, and CORS/deployment debugging.

### Runtime Application AI
- **Google Gemini API (`gemini-1.5-flash`)**:
  - Located in: `AiOfferParserService.java` (`POST /api/ai/parse-offer`).
  - Converts unstructured raw natural language text and uploaded promotional images into structured JSON offer attributes:
    ```json
    {
      "title": "Diwali Special Saree Discount",
      "description": "Flat 20% off on all sarees",
      "discountType": "PERCENTAGE",
      "discountValue": 20.0,
      "minBillAmount": 1500.0,
      "maxDiscountAmount": 2000.0,
      "applicableCategory": "Apparel & Sarees"
    }
    ```
- **Heuristic Rule-Based NLP Fallback**:
  - Embedded inside `AiOfferParserService.java` to extract discount percentages, rupee amounts, and date limits when external AI APIs are unreachable or unconfigured.

---

## 6. 🔒 Security & Authorization

- **Stateless JWT Authentication**: Tokens signed with HMAC-SHA256 containing user ID, email, role, and assigned shop ID.
- **Role-Based Access Control (RBAC)**:
  - `ROLE_SUPER_ADMIN`: `/api/admin/**` (Create shops, approve points top-ups).
  - `ROLE_SHOPKEEPER`: `/api/shopkeeper/**`, `/api/offers/**`, `/api/analytics/**`, `/api/products/**`.
  - `ROLE_COUNTER_STAFF`: `/api/counter/**` (Voucher lookup and redemption).
  - `ROLE_SHOPPER`: `/api/shopper/**` (Browse offers, claim vouchers).
- **Shop-Level Data Isolation**: Verified on all mutation endpoints (`offer.getShop().getId().equals(currentUser.getShop().getId())`).
- **Production CORS Policy**: Explicitly configured in `SecurityConfig.java` to permit preflight `OPTIONS` requests, allow credentials with exact domain matching (`https://neighbourhood-offers-frontend.netlify.app`), and support standard local development origins without insecure wildcards.

---

## 7. ⚖️ Important Business Rules

1. **Points Deducted Only Upon In-Store Redemption**: Claiming an offer is 100% free for shoppers and costs the merchant 0 points. Only completed walk-in redemptions deduct 10 points.
2. **Strict Single-Use Vouchers**: Each claim code can only transition to `REDEEMED` once. Subsequent redemptions return idempotent receipts with 0 additional deductions.
3. **Cross-Shop Redemption Blocked**: If a customer presents an Anitha Silks voucher at Rahul Organic Groceries, counter verification fails with `UnauthorizedShopAccessException` (HTTP 403).
4. **Minimum Bill Validation**: If an offer has a minimum bill amount (e.g. ₹1500) and the counter purchase subtotal is below that threshold, the discount cannot be redeemed.
5. **Low Balance Protection**: A merchant with depleted points (< 10) cannot have offers redeemed until their balance is replenished.

---

## 8. 🌐 Production Deployment Architecture

```
                                      +---------------------------------------------+
                                      |            NETLIFY (Edge CDN)               |
                                      |  https://neighbourhood-offers-frontend.     |
                                      |  netlify.app                                |
                                      +---------------------------------------------+
                                                             |
                                                             | HTTPS (REST + JWT)
                                                             v
+-----------------------------------------------------------------------------------+
|                            RENDER (Docker Web Service)                            |
|               https://neighbourhood-offers-backend-1.onrender.com                 |
|                                                                                   |
|  - Eclipse Temurin 21 JRE Runtime                                                 |
|  - Spring Boot 3.4.3 REST API                                                     |
|  - Hibernate ORM with ddl-auto=update                                             |
+-----------------------------------------------------------------------------------+
                                                             |
                                                             | TLS 1.3 (Port 22344)
                                                             v
+-----------------------------------------------------------------------------------+
|                            AIVEN CLOUD (Managed MySQL)                            |
|             mysql-10b471b2-konaajay0022-511d.g.aivencloud.com:22344/defaultdb     |
|                                                                                   |
|  - MySQL 8.0.45 Enterprise Database                                               |
|  - SSL Mode: REQUIRED                                                             |
|  - Automated Tables: users, shops, offers, claims, products, point_transactions   |
+-----------------------------------------------------------------------------------+
```

---

## 9. 🔮 What I Would Do With One More Week

If given another week to take this prototype further:
1. **End-to-End Automated Testing**: Add Playwright or Cypress tests running simulated browser flows for the entire Shopper-to-Counter staff lifecycle.
2. **Production Database Migrations (Flyway / Liquibase)**: Replace `ddl-auto=update` with versioned, reproducible SQL migration scripts.
3. **Enhanced OCR & Multimodal Ingestion**: Integrate cloud vision models (Google Cloud Vision API or AWS Textract) to extract discounts directly from crumpled physical flyers, handwritten chalkboard menus, and receipts.
4. **Proximity Geofencing**: Integrate browser Geolocation API to surface offers sorted by real-time walking distance.
5. **Observability & APM**: Add Prometheus metrics endpoints and OpenTelemetry tracing to monitor database lock acquisition times during redemption spikes.
6. **Cashier Offline Sync**: Implement a Progressive Web App (PWA) Service Worker with local SQLite/IndexedDB so counter staff can queue redemptions during full internet outages.

---

## 10. 📁 Project Structure

```
Neighbourhood Offers/
├── backend/                                   # Spring Boot Application
│   ├── src/main/java/com/neighbourhood/offers/
│   │   ├── config/                            # SecurityConfig, CorsConfig
│   │   ├── controller/                        # REST Controllers (Auth, Offer, Claim, Counter, Admin, etc.)
│   │   ├── dto/                               # Data Transfer Objects & API Schemas
│   │   ├── entity/                            # JPA Entities (User, Shop, Offer, Claim, Product, etc.)
│   │   ├── exception/                         # GlobalExceptionHandler & Domain Exceptions
│   │   ├── repository/                        # Spring Data JPA Repositories (with Pessimistic Locks)
│   │   ├── security/                          # JwtTokenProvider, UserPrincipal, UserDetailsService
│   │   └── service/                           # Business Services (Redemption, Claim, Offer, Admin, AI)
│   ├── src/test/java/                         # 40 Automated Tests
│   ├── Dockerfile                             # Multi-stage production container build
│   └── pom.xml                                # Maven configuration
│
├── frontend/                                  # React + Vite Application
│   ├── src/
│   │   ├── api/                               # Axios HTTP clients with JWT injection
│   │   ├── components/                        # Navigation, Modals, AI Studio, Persona Switcher
│   │   ├── context/                           # AuthContext with 1-click persona switching
│   │   ├── pages/                             # ShopperBrowse, CounterTerminal, ShopkeeperOffers, Wallet, Analytics
│   │   └── types/                             # TypeScript interfaces matching backend models
│   ├── Dockerfile                             # Nginx production build
│   └── vite.config.ts                         # Vite configuration
│
├── docker-compose.yml                         # Local multi-container orchestration
└── README.md                                  # Complete system documentation
```

---

## 11. 🧪 Testing & Verification Status

### Backend Automated Test Suite
The backend contains comprehensive automated integration and concurrency tests:

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.neighbourhood.offers.config.CorsSecurityTest (4 tests)
[INFO] Running com.neighbourhood.offers.OffersApplicationTests (1 test)
[INFO] Running com.neighbourhood.offers.service.AdminServiceTest (6 tests)
[INFO] Running com.neighbourhood.offers.service.AiOfferParserTest (5 tests)
[INFO] Running com.neighbourhood.offers.service.ClaimBusinessLogicTest (3 tests)
[INFO] Running com.neighbourhood.offers.service.ProductServiceTest (7 tests)
[INFO] Running com.neighbourhood.offers.service.RedemptionServiceTest (6 tests)
[INFO] Running com.neighbourhood.offers.service.ShopkeeperStaffServiceTest (2 tests)
[INFO] Running com.neighbourhood.offers.service.TenantIsolationTest (3 tests)
[INFO] Running com.neighbourhood.offers.service.UserActiveTest (3 tests)
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

- **Frontend Build Status**: Built successfully via `npm run build` with zero TypeScript errors.
- **Backend Build Status**: Packaged into executable jar `offers-0.0.1-SNAPSHOT.jar` with zero errors.

---

## 12. ⚠️ Known Limitations

1. **Simulated Payment Gateway**: Merchant points top-up is emulated via an admin approval workflow (`PointTopUpRequest`) rather than an integrated real-money gateway (e.g. Razorpay or Stripe).
2. **Local Vision Fallback**: Image OCR parsing when running without a Google Gemini API key relies on system-level heuristics which are less flexible than multimodal LLM vision.
3. **Single Shop Staff Binding**: A counter staff user is currently associated with a single shop location rather than supporting multi-branch roaming cashiers.
