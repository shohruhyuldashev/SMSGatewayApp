# CyberBro SMS Gateway

Android-based self-hosted SMS Gateway built with Kotlin, Ktor, Room Database, and WorkManager.

## Features

* SMS sending through Android SIM cards
* HTTPS (TLS) support
* API Key authentication
* SMS queue management
* Message history API
* Device monitoring API
* System statistics and logs
* Dual SIM support
* Self-hosted deployment
* REST API interface

---

## Screenshots

### Dashboard

*Add screenshot here*

### Settings

*Add screenshot here*

### SMS Logs

*Add screenshot here*

---

## Technology Stack

* Kotlin
* Android SDK
* Ktor Server
* Room Database
* WorkManager
* OkHttp
* Material Design 3

---

## API Authentication

All protected endpoints require an API key.

Example:

```bash
curl -k https://PHONE_IP:8443/system \
-H "x-api-key: YOUR_API_KEY"
```

---

## API Endpoints

### Health Check

```http
GET /health
```

Example:

```bash
curl -k https://PHONE_IP:8443/health
```

---

### Send SMS

```http
POST /send-sms
```

Request:

```json
{
  "id": "msg001",
  "phone": "+998901234567",
  "message": "Hello from CyberBro SMS Gateway",
  "sim": 1
}
```

Example:

```bash
curl -k -X POST https://PHONE_IP:8443/send-sms \
-H "x-api-key: YOUR_API_KEY" \
-H "Content-Type: application/json" \
-d '{
  "id":"msg001",
  "phone":"+998901234567",
  "message":"Hello from CyberBro SMS Gateway",
  "sim":1
}'
```

---

### Message Status

```http
GET /messages/{id}
```

Example:

```bash
curl -k https://PHONE_IP:8443/messages/msg001 \
-H "x-api-key: YOUR_API_KEY"
```

---

### System Information

```http
GET /system
```

Returns:

* Device model
* Android version
* Battery level
* Local IP address
* Network type
* SIM information
* RAM usage
* Uptime
* HTTPS status

---

### SIM Information

```http
GET /sims
```

Example:

```bash
curl -k https://PHONE_IP:8443/sims \
-H "x-api-key: YOUR_API_KEY"
```

---

### Security Status

```http
GET /security
```

Example:

```bash
curl -k https://PHONE_IP:8443/security \
-H "x-api-key: YOUR_API_KEY"
```

---

### Logs

```http
GET /logs
GET /logs/security
```

Example:

```bash
curl -k https://PHONE_IP:8443/logs/security \
-H "x-api-key: YOUR_API_KEY"
```

---

## HTTPS Support

The gateway supports HTTPS using a PKCS12 keystore.

Example:

```bash
curl -k https://PHONE_IP:8443/system \
-H "x-api-key: YOUR_API_KEY"
```

For development and private networks, a self-signed certificate can be used.

---

## Security Features

* API Key Authentication
* HTTPS/TLS Support
* Request Validation
* Rate Limiting
* Audit Logging
* Queue Processing

---

## Installation

Clone the repository:

```bash
git clone git@github.com:shohruhyuldashev/SMSGatewayApp.git
cd SMSGatewayApp
```

Build APK:

```bash
./gradlew assembleDebug
```

Install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Project Structure

```text
app/
├── api/
├── device/
├── security/
├── service/
├── sms/
├── storage/
├── integration/
└── ui/
```

---

## Roadmap

* SMS Delivery Tracking
* Incoming SMS API
* Webhook Events
* Advanced Analytics
* Swagger/OpenAPI Documentation
* Multi-device Management

---

## Author

Shohruh Yuldashev (CyberBro)

GitHub:
https://github.com/shohruhyuldashev

---

## License

MIT License
