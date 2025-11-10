# VideoStreamGRPC

A microservices-based video streaming platform built with Spring Boot and gRPC, featuring video upload, storage, and streaming capabilities.

## 🏗️ Architecture

This project consists of two main services:

- **Video Service** (`video/`) - REST API for video management and client interactions
- **Streaming Service** (`streaming/`) - gRPC service for video file operations and streaming

```
┌─────────────────┐    gRPC     ┌─────────────────┐
│   Video Service │ ──────────► │ Streaming       │
│   (Port 8090)   │             │ Service         │
│   REST API      │             │ (Port 9090)     │
└─────────────────┘             └─────────────────┘
         │                               │
         ▼                               ▼
┌─────────────────┐             ┌─────────────────┐
│   PostgreSQL    │             │   File System   │
│   (videos db)   │             │   (videos/)     │
│   Port 5732     │             │                 │
└─────────────────┘             └─────────────────┘
```

## 🚀 Features

- **Video Upload**: Upload large video files via REST API with chunked streaming to gRPC service
- **Video Streaming**: Stream videos with HTTP range support for seeking and progressive loading
- **Microservices Architecture**: Separate concerns with dedicated services for API and streaming
- **Database Integration**: PostgreSQL for metadata storage
- **File Management**: Efficient file storage and retrieval system
- **Error Handling**: Comprehensive error handling and logging

## 🛠️ Technology Stack

- **Java 21**
- **Spring Boot 3.5.7**
- **gRPC** - Inter-service communication
- **PostgreSQL** - Database
- **Lombok** - Code generation
- **Maven** - Build tool
- **Docker Compose** - Database containerization

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL (via Docker)

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd VideoStreamGRPC
```

### 2. Start the Database

```bash
docker-compose up -d
```

This will start a PostgreSQL container on port 5732 with:

- Username: `samwell`
- Password: `password`
- Databases: `videos`, `streams`

### 3. Build and Run Services

#### Start Streaming Service (gRPC Server)

```bash
cd streaming
./mvnw spring-boot:run
```

The streaming service will start on port 9090.

#### Start Video Service (REST API)

```bash
cd video
./mvnw spring-boot:run
```

The video service will start on port 8090.

## 📚 API Documentation

### Upload Video

Upload a video file with metadata.

**Endpoint:** `POST /upload`

**Request:**

- Content-Type: `multipart/form-data`
- `data`: JSON string with video metadata
- `file`: Video file

**Example:**

```bash
curl -X POST http://localhost:8090/upload \
  -F "data={\"title\":\"My Video\",\"description\":\"A sample video\"}" \
  -F "file=@/path/to/video.mp4"
```

**Response:**

```json
{
  "status": "200",
  "message": "Upload completed: videos/VID-1234567890123.mp4",
  "data": null
}
```

### Stream Video

Stream a video by its ID.

**Endpoint:** `GET /stream/{videoId}`

**Example:**

```bash
curl http://localhost:8090/stream/VID-1234567890123
```

**Response:** Video stream with proper HTTP headers for video playback.

## 🗄️ Database Schema

### Video Service Database (`videos`)

```sql
-- Video metadata table
CREATE TABLE video (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP,
    video_id VARCHAR(255) UNIQUE
);
```

### Streaming Service Database (`streams`)

```sql
-- Video file details table
CREATE TABLE video_detail (
    id BIGSERIAL PRIMARY KEY,
    video_id VARCHAR(255) UNIQUE,
    storage_path VARCHAR(500),
    file_size VARCHAR(50)
);
```

## 🔧 Configuration

### Video Service Configuration

File: [`video/src/main/resources/application.properties`](video/src/main/resources/application.properties)

```properties
# Server Configuration
server.port=8090

# gRPC Client Configuration
grpc.client.streaming-service.address=static://localhost:9090
grpc.client.streaming-service.negotiation-type=PLAINTEXT

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5732/videos
spring.datasource.username=samwell
spring.datasource.password=password

# File Upload Configuration
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB
```

### Streaming Service Configuration

File: [`streaming/src/main/resources/application.properties`](streaming/src/main/resources/application.properties)

```properties
# gRPC Server Configuration
spring.grpc.server.port=9090
spring.grpc.server.reflection.enabled=true

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5732/streams
spring.datasource.username=samwell
spring.datasource.password=password
```

## 📁 Project Structure

```
VideoStreamGRPC/
├── video/                          # Video Service (REST API)
│   ├── src/main/java/com/lockedfunds/video/
│   │   ├── controller/             # REST Controllers
│   │   ├── service/                # Business Logic
│   │   ├── repository/             # Data Access Layer
│   │   ├── entity/                 # JPA Entities
│   │   └── dto/                    # Data Transfer Objects
│   ├── src/main/proto/             # Protocol Buffer Definitions
│   └── src/main/resources/         # Configuration Files
├── streaming/                      # Streaming Service (gRPC)
│   ├── src/main/java/com/lockedfunds/streaming/
│   │   ├── service/                # gRPC Service Implementation
│   │   ├── repository/             # Data Access Layer
│   │   └── entity/                 # JPA Entities
│   ├── src/main/proto/             # Protocol Buffer Definitions
│   └── src/main/resources/         # Configuration Files
├── videos/                         # Video Storage Directory
└── docker-compose.yaml            # Database Container Configuration
```

## 🔄 How It Works

1. **Video Upload Flow:**

   - Client uploads video via REST API to Video Service
   - Video Service streams file chunks to Streaming Service via gRPC
   - Streaming Service saves file to disk and stores metadata in database
   - Video Service stores video metadata in its database

2. **Video Streaming Flow:**
   - Client requests video stream from Video Service
   - Video Service requests stream from Streaming Service via gRPC
   - Streaming Service reads file from disk and streams chunks
   - Video Service forwards stream to client with proper HTTP headers

## 🚧 Development

### Build Projects

```bash
# Build all projects
cd video && ./mvnw clean compile
cd ../streaming && ./mvnw clean compile
```

### Generate Protocol Buffer Classes

Protocol buffer classes are automatically generated during Maven compilation using the `protobuf-maven-plugin`.

### Running Tests

```bash
# Run tests for Video Service
cd video && ./mvnw test

# Run tests for Streaming Service
cd streaming && ./mvnw test
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🔍 Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 8090, 9090, and 5732 are available
2. **Database connection issues**: Verify PostgreSQL container is running
3. **File upload size limits**: Check multipart configuration in application.properties
4. **gRPC connection issues**: Ensure Streaming Service is running before starting Video Service

### Logs

- Video Service logs: Check console output or configure logging in `application.properties`
- Streaming Service logs: Check console output with detailed gRPC operation logs
- Database logs: Check Docker container logs with `docker-compose logs postgres`
