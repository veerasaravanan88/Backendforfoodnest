# AdvanceFoodnest — Full-Stack Food Delivery Platform

AdvanceFoodnest is a commercial-grade, full-stack food delivery web application built with **Java 21**, **Spring Boot 3.3.4**, **PostgreSQL**, **Flyway**, and a modern **Vanilla JavaScript (ES Modules)** frontend with **Leaflet.js** live GPS tracking and OpenStreetMap.

---

## 🌟 Key Features

1. **Three Independent Portals & Authentication Systems:**
   - **Customer Portal**: Restaurant discovery, category filtering, menu explorer, dynamic cart, secure checkout, order tracking, order history, profile management.
   - **Restaurant Partner Portal**: Self-registration, live dashboard, dish catalog management (CRUD with stock and availability), incoming orders workflow, order status management, sales and revenue reports.
   - **Delivery Partner Portal**: Self-registration, availability toggle, active delivery management, real-time browser Geolocation GPS tracking broadcaster, order lifecycle (`PICKED_UP` → `OUT_FOR_DELIVERY` → `DELIVERED`).

2. **Strict Multi-Tenant Restaurant Data Isolation:**
   - Strict database and service-level tenant scoping based on the authenticated principal.
   - **Fastfoods** only sees and manages Fastfoods dishes (e.g. *Chicken Fry*, *Chicken 65*).
   - **Foodies** only sees and manages Foodies dishes (e.g. *Mutton Fry*, *Biryani*).
   - Cross-restaurant dish or order modification attempts are strictly rejected with HTTP 400/403.

3. **Server-Side Cart & Pricing Integrity:**
   - Single restaurant per order constraint enforced on backend.
   - Real-time stock checks and active status validation.
   - Automatic 5% GST calculation + ₹40 flat delivery fee.

4. **Order State Machine & Automated Dispatch:**
   - Order flow: `PLACED` → `ACCEPTED` → `PREPARING` → `READY` → `PICKED_UP` → `OUT_FOR_DELIVERY` → `DELIVERED` (or `CANCELLED`).
   - When a restaurant marks an order as `READY`, the system automatically selects an available delivery rider using **Pessimistic Locking (`SELECT ... FOR UPDATE SKIP LOCKED`)** to prevent race conditions.

5. **Live Interactive Delivery Tracking:**
   - Real-time GPS location broadcasting from the rider portal using the HTML5 Geolocation API.
   - Interactive OpenStreetMap with Leaflet.js showing restaurant origin, rider marker, customer destination, and animated status timelines.

6. **Enterprise Security:**
   - Spring Security 6 with role-based access (`CUSTOMER`, `RESTAURANT`, `DELIVERY_MAN`).
   - BCrypt password hashing.
   - HttpOnly session cookies (`FOODNEST_SESSION`) with SameSite policy.
   - Double-submit CSRF token protection (`X-XSRF-TOKEN`).
   - Full CORS origin support.

---

## 📂 Project Architecture

```
AdvanceFoodnest/
├── frontend/
│   ├── index.html                     # Animated Landing Page & Discovery
│   ├── customer/                      # Customer Portal
│   │   ├── login.html
│   │   ├── register.html
│   │   ├── dashboard.html
│   │   ├── restaurants.html
│   │   ├── restaurant-menu.html
│   │   ├── cart.html
│   │   ├── checkout.html
│   │   ├── orders.html
│   │   ├── order-track.html           # Leaflet.js live GPS tracking
│   │   └── profile.html
│   ├── restaurant/                    # Restaurant Portal
│   │   ├── login.html
│   │   ├── register.html
│   │   ├── dashboard.html
│   │   ├── dishes.html                # Menu & Dish CRUD
│   │   ├── orders.html                # Kitchen order progression
│   │   ├── reports.html               # Sales analytics
│   │   └── profile.html
│   ├── deliveryman/                   # Delivery Partner Portal
│   │   ├── login.html
│   │   ├── register.html
│   │   ├── dashboard.html             # Active delivery & GPS broadcaster
│   │   ├── deliveries.html            # Delivery history
│   │   └── profile.html
│   └── shared/
│       ├── css/
│       │   └── style.css              # Design system & animations
│       └── js/
│           ├── api.js                 # Fetch client, CSRF, Cart helper
│           ├── auth.js                # Auth guards, role routing, navigation
│           └── toast.js               # Toast notifications
│
├── backend/
│   ├── pom.xml                        # Maven dependencies & build config
│   ├── mvnw / mvnw.cmd                # Maven wrapper
│   └── src/
│       ├── main/
│       │   ├── java/com/foodnest/
│       │   │   ├── AdvanceFoodnestApplication.java
│       │   │   ├── config/            # SecurityConfig, WebConfig, WebSocketConfig
│       │   │   ├── security/          # UserPrincipal, CustomUserDetailsService
│       │   │   ├── auth/              # AuthController, AuthService, DTOs
│       │   │   ├── customer/          # Customer entities, service, controller
│       │   │   ├── restaurant/        # Restaurant entities, service, controllers
│       │   │   ├── deliveryman/       # DeliveryMan entities, service, controller
│       │   │   ├── dish/              # Dish entities, service, controller
│       │   │   ├── cart/              # CartService, CartController
│       │   │   ├── order/             # Order entities, state machine, controller
│       │   │   ├── delivery/          # DeliveryAssignment, pessimistic dispatcher
│       │   │   ├── tracking/          # GPS DeliveryLocation, TrackingController
│       │   │   ├── report/            # Sales & Revenue Analytics
│       │   │   └── common/            # DataSeeder, ApiResponse, GlobalExceptions
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/
│       │           └── V1__initial_schema.sql
│       └── test/java/com/foodnest/    # Comprehensive JUnit 5 & Mockito test suite
```

---

## 🚀 Quick Start Guide

### 1. Prerequisites
- **Java 21 JDK** installed (`java -version`)
- **PostgreSQL 14+** running on `localhost:5432`
- **Maven 3.8+** (or use included `mvnw.cmd`)

### 2. Database Setup
Create the PostgreSQL database using `psql` or pgAdmin:
```sql
CREATE DATABASE advance_foodnest;
```

### 3. Environment Variable Configuration
Set your PostgreSQL password in your environment (never hardcoded in files):
**PowerShell:**
```powershell
$env:DB_PASSWORD="your_postgres_password"
```

**Bash / Linux / macOS:**
```bash
export DB_PASSWORD="your_postgres_password"
```

### 4. Run the Backend Service
From the `backend/` directory:
```bash
cd backend
mvn spring-boot:run
```
Flyway automatically executes the migration `V1__initial_schema.sql` and the `DataSeeder` initializes demo accounts, menus, and riders.

The application starts on: **`http://localhost:8080`**

### 5. Access the Frontend
The Spring Boot web server automatically serves the frontend static assets directly on port 8080:
Open your browser and navigate to:
👉 **`http://localhost:8080`**

*(Alternatively, you can serve the `frontend/` folder with Live Server or Python `python -m http.server 3000`; CORS is configured to support all localhost ports).*

---

## 🔑 Pre-Seeded Demo Accounts

All pre-seeded demo accounts use the default password: **`password123`**

| Role | Portal URL | Email / Username | Password | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Customer** | `/customer/login.html` | `customer@foodnest.com` | `password123` | Rahul Sharma (Indiranagar, Bengaluru) |
| **Customer 2** | `/customer/login.html` | `priya@foodnest.com` | `password123` | Priya Patel (Koramangala, Bengaluru) |
| **Restaurant A** | `/restaurant/login.html` | `fastfoods@foodnest.com` | `password123` | Fastfoods (Chicken Fry, Chicken 65, etc.) |
| **Restaurant B** | `/restaurant/login.html` | `foodies@foodnest.com` | `password123` | Foodies (Mutton Fry, Biryani, Naan) |
| **Restaurant C** | `/restaurant/login.html` | `spicegarden@foodnest.com` | `password123` | SpiceGarden (South Indian Pure Veg) |
| **Delivery Rider 1** | `/deliveryman/login.html` | `rider1@foodnest.com` | `password123` | David Vance (Active & Available) |
| **Delivery Rider 2** | `/deliveryman/login.html` | `rider2@foodnest.com` | `password123` | Anita Roy (Active & Available) |

---

## 🧪 Automated Testing

### Backend Unit & Integration Tests
Run the JUnit 5 test suite:
```bash
cd backend
mvn test
```
**Test Coverage Includes:**
- `CartCalculationTest`: Subtotal, 5% tax, delivery fee calculation, multi-restaurant rejection, stock checks.
- `DeliveryAssignmentServiceTest`: Automated dispatch on `READY` status with pessimistic locking, unassigned fallback queue.
- `OrderStatusStateMachineTest`: Valid and invalid status transitions.
- `RestaurantIsolationTest`: Strict isolation preventing cross-restaurant dish modifications and deletions.
- `AuthServiceTest`: User registration, duplicate email/phone prevention.

---

## 📡 REST API Summary

### Authentication (`/api/auth`)
- `POST /api/auth/customer/register` — Self-register new customer
- `POST /api/auth/customer/login` — Customer login & session creation
- `POST /api/auth/restaurant/register` — Self-register new restaurant partner
- `POST /api/auth/restaurant/login` — Restaurant partner login
- `POST /api/auth/deliveryman/register` — Self-register delivery rider
- `POST /api/auth/deliveryman/login` — Delivery rider login
- `POST /api/auth/logout` — Invalidate session and clear cookies
- `GET /api/auth/me` — Get authenticated user details & role
- `GET /api/auth/csrf` — Retrieve active CSRF token

### Public Browsing (`/api/restaurants`)
- `GET /api/restaurants` — List all active restaurants
- `GET /api/restaurants/{id}` — Get restaurant details
- `GET /api/restaurants/{id}/menu` — Get restaurant menu items

### Cart (`/api/cart`)
- `POST /api/cart/calculate` — Server-side cart validation and pricing calculation

### Customer (`/api/customer`)
- `GET /api/customer/me` — Customer profile
- `PUT /api/customer/me` — Update customer profile
- `GET /api/customer/me/orders` — Customer order history

### Orders (`/api/orders`)
- `POST /api/orders` — Create new food order
- `GET /api/orders/{id}` — Order details (accessible only by owner customer, restaurant, or assigned rider)
- `POST /api/orders/{id}/cancel` — Cancel order (if permitted by state machine)

### Restaurant Management (`/api/restaurant/me`)
- `GET /api/restaurant/me` — Restaurant profile
- `GET /api/restaurant/me/dashboard` — Live kitchen stats & pending orders
- `GET /api/restaurant/me/dishes` — Restaurant's own dishes
- `POST /api/restaurant/me/dishes` — Add new dish
- `PUT /api/restaurant/me/dishes/{dishId}` — Update dish details
- `DELETE /api/restaurant/me/dishes/{dishId}` — Delete dish
- `GET /api/restaurant/me/orders` — Incoming orders for this restaurant
- `PUT /api/restaurant/me/orders/{orderId}/status` — Advance status (`ACCEPTED`, `PREPARING`, `READY`)
- `GET /api/restaurant/me/reports` — Sales, revenue, and order analytics

### Delivery Partner (`/api/deliveryman/me`)
- `GET /api/deliveryman/me` — Rider profile
- `POST /api/deliveryman/me/availability` — Toggle active/available status
- `GET /api/deliveryman/me/active` — Current active delivery assignment
- `GET /api/deliveryman/me/deliveries` — Delivery history
- `POST /api/deliveryman/me/location` — Broadcast current GPS coordinates
- `PUT /api/deliveryman/me/deliveries/{id}/status` — Update delivery status (`PICKED_UP`, `OUT_FOR_DELIVERY`, `DELIVERED`)

### Tracking (`/api/tracking`)
- `GET /api/tracking/orders/{orderId}` — Live order status, assigned rider details, and latest GPS coordinates
