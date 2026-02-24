
# Energy Management System

This project implements an **Energy Management System (EMS)** using a **microservices architecture**.  
It is containerized with **Docker** and exposed via a **Traefik API Gateway**.  
Communication between components uses **REST (Request-Reply)**.

---

## Project Overview

The system allows **authenticated users** to manage, monitor, and view smart energy metering devices.  
There are two user roles:

* **ADMIN**: Can manage users and devices (CRUD) and assign devices to users.
* **USER**: Can log in and view only the devices assigned to them.

The system consists of:

* A **React Frontend**
* Three independent **Spring Boot Microservices**:
  * **Authorization Service**
  * **User Service**
  * **Device Service**
* A **Traefik Reverse Proxy**
* Three **MySQL Databases** (one per microservice)

Each microservice is deployed independently and communicates via REST APIs.

---

## Microservices Architecture

### Authorization Microservice

* Handles **user registration** and **login**.
* Issues **JWT tokens** for authentication.
* Validates user credentials via the `auth_db` database.
* Ensures only authenticated users can access protected endpoints.

### User Microservice

* Handles **CRUD operations for users**.
* Stores data in the `user_db` database.
* Provides internal endpoints for inter-service communication.

### Device Microservice

* Handles **CRUD operations for devices**.
* Stores data in the `device_db` database.
* Manages **device-to-user mappings**.
* Each device has:
  * `id`
  * `name`
  * `type`

### Frontend

* React-based UI for login, registration, and management actions.
* Role-based pages:
  * **ADMIN**: CRUD users/devices, assign devices to users
  * **USER**: View assigned devices only

### Traefik

* Acts as **API gateway and reverse proxy**.
* Validates requests and routes traffic to the correct service.
* Enforces security policies (authentication and authorization).

---

## Technologies Used

| Layer | Technology |
|-------|------------|
| Frontend | React, JavaScript, HTML, CSS |
| Backend | Spring Boot, Spring Data JPA, Spring Security |
| Database | MySQL (separate DBs per microservice) |
| Authentication | JWT |
| API Gateway | Traefik |
| Containerization | Docker, Docker Compose |
| Build Tools | Maven (Java), npm (React) |
| API Documentation | Swagger / OpenAPI |

---

## Build & Run Instructions

### Prerequisites

* Docker & Docker Compose
* Java 17+
* Maven
* Node.js & npm

## Frontend Build


**cd frontend**

**npm install**

**npm run build**

# Docker: Building Individual Images and Running the Project

## Building Individual Images

### 1.1. Auth Microservice

**cd /path/to/auth-microservice/**

**docker build -t auth-microservice**
### 1.2. User Microservice
**cd /path/to/user-microservice/**

**docker build -t user-microservice**

### 1.3. Device Microservice
**cd /path/to/device-microservice/**

**docker build -t device-microservice**

### 1.4. Frontend
**cd /path/to/frontend/**

**npm install**

**npm run build**

## Start Entire Stack (Recommended)

From the project root (where `docker-compose.yml` is located):

**docker compose up --build -d**


This will:
* create and run three MySQL containers (user_db, device_db, auth_db)
* start user-microservice 
* start device-microservice 
* start auth-microservice 
* start frontend container 
* start Traefik 


## Stop and Remove Containers / Networks
**docker compose down**

## User Roles
**ADMIN**

* Perform CRUD on users

* Perform CRUD on devices

* Assign devices to users

**USER**

* Log in using credentials

* View assigned devices **only**

## Security

**JWT**-based authentication: All secured endpoints require a valid token.

### Role-Based Access Control:

**ADMIN**: full access

**USER**: read-only for assigned data

Traefik gateway ensures only authorized requests reach backend services.

## Databases

**auth_db** → credentials and authentication

**user_db** → user information

**device_db** → device data and mappings

This ensures loose coupling and data isolation between microservices.

## API Testing

REST endpoints can be tested via Postman or Swagger UI.

Example: Authenticate via /auth/login, copy token, and use:

**Authorization: Bearer <your_token_here>**
"# Energy-Management-System-" 
