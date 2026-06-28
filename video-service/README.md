# Video Service

Lightweight self-hosted REST API for uploading, listing, streaming and deleting videos.

**No database. No Docker. Just Java.**

Metadata is stored in a `metadata.json` file. Videos are stored in a folder on disk.

---

## Requirements

- Java 17+
- Maven 3.8+ (only needed to build once)

---

## Setup & Run

### Step 1 — Build the JAR

```bash
mvn clean package -DskipTests
```

### Step 2 — Run

```bash
java -jar target/video-service-1.0.0.jar
```

Or use the helper script:

```bash
chmod +x start.sh
./start.sh
```

Service starts on **http://localhost:8080**

---

## Storage Layout

After first run, a `video-storage/` folder is created automatically:

```
video-storage/
├── metadata.json          ← all video metadata (title, size, date, etc.)
├── a3f1bc92-xxxx.mp4     ← actual video files (UUID-named)
├── d9e2aa11-xxxx.mov
└── ...
```

You can change the storage path in `application.properties`:
```properties
video.storage.path=/home/ubuntu/my-videos
```

Or pass it at runtime:
```bash
java -jar target/video-service-1.0.0.jar --video.storage.path=/home/ubuntu/my-videos
```

---

## API Reference

Base URL: `http://<server-ip>:8080`

---

### Upload a Video

```
POST /api/videos/upload
Content-Type: multipart/form-data
```

| Field | Required | Description |
|---|---|---|
| `file` | ✅ | Video file |
| `title` | ❌ | Display title |
| `description` | ❌ | Optional description |

```bash
curl -X POST http://localhost:8080/api/videos/upload \
  -F "file=@/path/to/video.mp4" \
  -F "title=My Video" \
  -F "description=Demo recording"
```

**Response (201):**
```json
{
  "success": true,
  "message": "Video uploaded successfully",
  "data": {
    "id": 1,
    "title": "My Video",
    "description": "Demo recording",
    "originalFileName": "video.mp4",
    "contentType": "video/mp4",
    "fileSizeBytes": 10485760,
    "fileSizeMb": "10.00 MB",
    "uploadedAt": "2026-06-28T10:30:00",
    "downloadUrl": "/api/videos/1/download",
    "streamUrl": "/api/videos/1/stream"
  }
}
```

---

### List Videos

```
GET /api/videos?page=0&size=20
```

```bash
curl http://localhost:8080/api/videos
```

**Response (200):**
```json
{
  "success": true,
  "message": "Videos fetched",
  "data": {
    "videos": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

### Get Single Video

```
GET /api/videos/{id}
```

```bash
curl http://localhost:8080/api/videos/1
```

---

### Stream Video

```
GET /api/videos/{id}/stream
```

Supports HTTP **Range** requests — works directly in browser `<video>` tags and media players (VLC, etc.).

```bash
# Use in HTML:
# <video src="http://yourserver:8080/api/videos/1/stream" controls></video>

# Or with curl:
curl -H "Range: bytes=0-" http://localhost:8080/api/videos/1/stream -o out.mp4
```

---

### Download Video

```
GET /api/videos/{id}/download
```

Returns the file as a browser-downloadable attachment.

```bash
curl -OJ http://localhost:8080/api/videos/1/download
```

---

### Delete Video

```
DELETE /api/videos/{id}
```

Removes both the file from disk and the entry from metadata.json.

```bash
curl -X DELETE http://localhost:8080/api/videos/1
```

**Response:**
```json
{ "success": true, "message": "Video deleted", "data": null }
```

---

## Error Responses

```json
{ "success": false, "message": "Video not found with id: 99", "data": null }
```

| Status | When |
|---|---|
| 400 | Wrong file type, empty file |
| 404 | ID not found |
| 413 | File > 5 GB |
| 500 | Disk error |

---

## Run as Background Service (Linux)

To keep it running after you close SSH:

```bash
nohup java -jar target/video-service-1.0.0.jar > video-service.log 2>&1 &
echo $! > video-service.pid
```

To stop it:
```bash
kill $(cat video-service.pid)
```
