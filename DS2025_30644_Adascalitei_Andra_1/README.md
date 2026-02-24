# Energy Management System - Assignment 3

**Student:** Adascalitei Andra  
**Group:** 30644  
**Course:** Distributed Systems 2025

## Project Overview

This project is an Energy Management System built with a microservices architecture. It allows administrators to manage users and devices, while users can monitor their energy consumption through interactive charts. **Assignment 3** introduces real-time communication features including WebSocket notifications, rule-based customer support chatbot, and load balancing capabilities.

## Architecture

### Microservices
- **User Microservice** (Port 8081) - User management
- **Device Microservice** (Port 8082) - Device management  
- **Authorization Microservice** (Port 8083) - Authentication & JWT
- **Monitoring Microservice** (Port 8084) - Energy consumption tracking
- **WebSocket Microservice** (Port 8085) - Real-time notifications and chat
- **Customer Support Microservice** (Port 8086) - Rule-based chatbot and support
- **Frontend** (Port 3000) - React application
- **Traefik** (Port 80) - Reverse proxy & load balancer

### Message Queue
- **RabbitMQ** (Ports 5672, 15672) - Asynchronous communication between microservices

### Databases
- **MySQL** - Separate databases for each microservice (userdb, devicedb, authdb, monitoringdb, customersupportdb)

### Additional Components
- **DeviceSimulator** - Generates random energy consumption data

## New Features (Assignment 3)

### 🤖 Rule-Based Customer Support Chatbot
- **14 intelligent rules** covering common user questions
- Automatic responses for device management, energy consumption, login issues, and more
- Real-time chat interface integrated into user and admin dashboards
- Chat session management with message history
- WebSocket-based real-time message delivery

###  WebSocket Real-Time Communication
- Real-time overconsumption notifications
- Instant chat message delivery
- Persistent WebSocket connections with SockJS fallback
- Multi-channel communication (notifications, chat)

###  Interactive Chat Interface
- Modern chat widget with floating design
- Real-time message exchange between users and support bot
- Admin-to-user chat capability with session takeover
- Message history and session management
- Connection status indicators
- Proper timestamp formatting and display
- Mobile-responsive design

## Chatbot Rules (14 Rules Implemented)

1. **Greeting** - Responds to hello, hi, good morning, etc.
2. **Device Management** - Helps with device-related questions
3. **Energy Consumption** - Explains consumption data and monitoring
4. **Login Issues** - Troubleshooting authentication problems
5. **Dashboard Navigation** - Guides users through the interface
6. **Charts & Visualizations** - Explains data visualization features
7. **Alerts & Notifications** - Information about overconsumption alerts
8. **User Management** - Explains user account features
9. **Technical Issues** - General troubleshooting guidance
10. **System Requirements** - Browser and system compatibility
11. **Data Export** - Information about data export capabilities
12. **Contact Support** - How to reach administrators
13. **Goodbye/Thanks** - Polite conversation endings
14. **Default Fallback** - Catches unmatched queries with helpful response

## Key Features

### Admin Dashboard
- Create, update, delete users
- Create, update, delete devices  
- Assign devices to users
- View all users and devices
- **NEW:** Real-time chat support interface with session management
- **NEW:** Admin chat panel for taking over user conversations
- **NEW:** View active chat sessions and message history

### User Dashboard
- View assigned devices
- Monitor energy consumption with interactive charts (Line & Bar)
- Select date from calendar
- View hourly consumption data (OX: hours, OY: energy in kWh)
- **NEW:** Real-time overconsumption notifications
- **NEW:** Integrated customer support chat

### Real-Time Features
- **WebSocket Notifications:** Instant overconsumption alerts
- **Chat Support:** Real-time messaging with rule-based bot responses
- **Connection Status:** Visual indicators for WebSocket connectivity
- **Message History:** Persistent chat sessions with full history

### RabbitMQ Communication
- User-Device synchronization through events
- Device consumption data collection
- **NEW:** Chat message routing between Customer Support and WebSocket services
- **NEW:** Real-time notification delivery pipeline
- Asynchronous communication for decoupled microservices

### Security
- JWT-based authentication
- Role-based access control (Admin/User)
- Traefik reverse proxy with routing rules
- Secure inter-service communication

## Technology Stack

**Backend:**
- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- Spring WebSocket
- RabbitMQ (AMQP)
- MySQL

**Frontend:**
- React 19
- Chart.js (react-chartjs-2)
- React Router
- **NEW:** SockJS + STOMP for WebSocket communication
- **NEW:** Real-time chat interface

**Infrastructure:**
- Docker & Docker Compose
- Traefik
- RabbitMQ
- Maven

## Prerequisites

- Docker Desktop
- Java 21 JDK
- Maven 3.x
- Node.js 18+ (for local frontend development)
- IntelliJ IDEA or similar IDE (optional)

## Installation & Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd DS2025_30644_Adascalitei_Andra_Assignment1/DS2025_30644_Adascalitei_Andra_1
```

### 2. Start All Services with Docker Compose

```bash
docker-compose up --build -d
```

This will start:
- All microservices (including new Customer Support)
- Databases (including new customersupportdb)
- RabbitMQ
- Frontend
- Traefik

### 3. Verify Services are Running

```bash
docker ps
```

You should see all containers running, including:
- `customer-support-microservice`
- `websocket-microservice`

### 4. Access the Application

- **Frontend:** http://localhost
- **Admin Login:** username: `admin`, password: `admin`
- **RabbitMQ Management:** http://localhost:15672 (user: `user`, password: `password`)
- **Customer Support API:** http://localhost/api/support/health

## Using the New Features

### Customer Support Chat

1. **For Users:**
   - Login to user dashboard
   - Click the chat widget (💬) in bottom-right corner
   - Start typing questions about devices, energy consumption, etc.
   - Get instant automated responses based on intelligent rules
   - Request human support if needed

2. **For Admins:**
   - Login to admin dashboard
   - Access the Admin Chat Panel from the navigation
   - View all active chat sessions with status indicators
   - Take over sessions that require human intervention
   - Chat directly with users in real-time
   - Monitor chat sessions and full message history

### Real-Time Notifications

1. Ensure WebSocket connection is active (green indicator)
2. Run DeviceSimulator with high consumption values
3. Receive instant overconsumption alerts
4. Notifications appear automatically without page refresh

### Testing the Chatbot

Try these sample questions to test different rules:

- "Hello" → Greeting response
- "How do I add a device?" → Device management help
- "My energy consumption is high" → Energy consumption guidance
- "I can't login" → Login troubleshooting
- "How do I navigate the dashboard?" → Navigation help
- "Show me the charts" → Visualization explanation
- "What are alerts?" → Notification information
- "Technical problem" → General troubleshooting
- "System requirements" → Compatibility info
- "Thank you" → Polite goodbye

## API Endpoints

### Customer Support Microservice (8086)
- `POST /api/support/chat/user` - Send user message
- `POST /api/support/chat/admin` - Send admin message  
- `GET /api/support/chat/history/{sessionId}` - Get chat history
- `GET /api/support/sessions/active` - Get active sessions
- `GET /api/support/sessions/user/{userId}` - Get user sessions
- `POST /api/support/sessions/{sessionId}/request-admin` - Request admin support
- `POST /api/support/sessions/{sessionId}/take-over` - Admin takes over session
- `POST /api/support/sessions/{sessionId}/close` - Close session
- `GET /api/support/health` - Health check

### WebSocket Microservice (8085)
- `WS /ws` - WebSocket endpoint with SockJS
- `WS /websocket` - Native WebSocket endpoint
- `/topic/notifications` - Overconsumption notifications
- `/topic/chat/{sessionId}` - Chat messages for specific session
- `/topic/admin/chat` - Admin chat monitoring channel
- `/topic/admin/notifications` - Admin notification channel

### Existing Endpoints
- **User Microservice (8081)** - User management
- **Device Microservice (8082)** - Device management
- **Authorization Microservice (8083)** - Authentication
- **Monitoring Microservice (8084)** - Consumption data

## RabbitMQ Queues & Exchanges

### New Queues (Assignment 3)
- `chat.queue` - Chat messages between Customer Support and WebSocket
- `chat.exchange` - Chat message routing

### Existing Queues
- `synchronization_queue` - User/Device sync events
- `data_collection_queue` - Energy consumption data
- `overconsumption_queue` - Overconsumption notifications

## Database Schema Updates

### New: Customer Support Database
- `chat_sessions` table: id, session_id, user_id, username, status, timestamps
- `chat_messages` table: id, session_id, user_id, username, message, sender_type, timestamp, is_automated, rule_matched

### Existing Databases
- User, Device, Auth, and Monitoring databases remain unchanged

## Project Structure

```
DS2025_30644_Adascalitei_Andra_1/
├── DS2025_30644_Adascalitei_Andra_UserMicroservice/
├── DS2025_30644_Adascalitei_Andra_DeviceMicroservice/
├── DS2025_30644_Adascalitei_Andra_AuthorizationMicroservice/
├── MonitoringService/
├── WebSocketMicroservice/                    # ← NEW
├── CustomerSupportMicroservice/              # ← NEW
├── DeviceSimulator/
├── frontend/
│   └── src/
│       ├── components/
│       │   ├── ChatWidget.js                 # ← NEW: User chat interface
│       │   └── AdminChatPanel.js             # ← NEW: Admin chat management
│       └── pages/
│           └── AdminDashboard.js             # ← UPDATED: Added chat panel
├── docker-compose.yml                        # ← UPDATED
├── traefik.yml
└── README.md                                 # ← UPDATED
```

## License

This project is developed for educational purposes as part of the Distributed Systems course.