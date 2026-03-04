# BreadCost Application Setup Guide

## Prerequisites Installation

### 1. Install Java 21

Download and install from: https://adoptium.net/temurin/releases/?version=21

Verify installation:
```powershell
java -version
# Should show: openjdk version "21.x.x"
```

### 2. Install Maven

**Option A: Using Chocolatey (Recommended)**
```powershell
# Install Chocolatey first if not already installed
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Install Maven
choco install maven -y
```

**Option B: Manual Installation**
1. Download Maven from: https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH: `C:\Program Files\Apache\maven\bin`
4. Restart PowerShell

Verify installation:
```powershell
mvn -version
# Should show: Apache Maven 3.x.x
```

### 3. Build and Run

```powershell
cd C:\workspace\hello-genai\work\breadcost-app

# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package the application
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/breadcost-app-1.0.0-SNAPSHOT.jar
```

### 4. Access the Application

Once running:
- API Base URL: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console
- API Documentation: See README.md for endpoint examples

### 5. Default Credentials

Use these credentials with Basic Auth:
- admin/admin (Full access)
- production/production (Production operations)
- finance/finance (Finance operations)
- viewer/viewer (Read-only)

## Quick Test

After starting the application, test with:

```powershell
# Test API is responding
curl http://localhost:8080/v1/views/wip -u viewer:viewer

# Post a receipt
curl -X POST http://localhost:8080/v1/inventory/receipts `
  -H "Content-Type: application/json" `
  -u admin:admin `
  -d '{
    "tenantId": "TENANT001",
    "siteId": "SITE001",
    "receiptId": "RCV001",
    "itemId": "FLOUR",
    "lotId": "LOT123",
    "qty": 1000,
    "uom": "KG",
    "unitCostBase": 2.50,
    "occurredAtUtc": "2026-03-03T10:00:00Z",
    "idempotencyKey": "rcv001-key"
  }'
```

## Troubleshooting

### Maven not found
- Ensure Maven is in PATH
- Restart PowerShell after installation
- Verify with: `$env:PATH -split ';' | Select-String maven`

### Port 8080 already in use
Change port in `src/main/resources/application.properties`:
```properties
server.port=8081
```

### Java version issues
- Ensure JAVA_HOME points to JDK 17+
- Check: `$env:JAVA_HOME`
- Set if needed: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x"`

## IDE Setup

### IntelliJ IDEA
1. File → Open → Select `breadcost-app` folder
2. Trust the Maven project
3. Wait for indexing
4. Right-click `BreadCostApplication.java` → Run

### VS Code
1. Install extensions: Java Extension Pack, Spring Boot Extension Pack
2. Open folder: `breadcost-app`
3. F5 to run with debugger

## Next Steps

See [README.md](README.md) for:
- Architecture overview
- API examples
- Development guidelines
- Feature roadmap
