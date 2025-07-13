# 🔐 SecureMsgX - Enchanted Message Vault

*"Speak, friend, and enter... but only if you know the secret words"*  
**A mystical message system with elven-grade encryption and access control**

## 🌟 Features

### 🧙‍♂️ Enchanted Message Types

#### ❌ No Replies
| Scroll Type     | Replies | Views Limit | Description                        |
|-----------------|---------|-------------|------------------------------------|
| `SINGLE`        | ❌      | 5 views     | One-time private message           |
| `SECURE_SINGLE` | ❌      | 1 view      | Self-destructs after reading       |
| `BROADCAST`     | ❌      | 1–1B views  | Announcement to many recipients    |

#### ✅ With Replies
| Scroll Type | Replies | Views Limit | Description                     |
|-------------|---------|-------------|---------------------------------|
| `THREAD`    | ✅      | Custom      | Private 1-to-1 conversation     |
| `GROUP`     | ✅      | Custom      | Multi-person discussion thread  |



## 🏰 API Endpoints (Doors of Durin)

### 🎨 Create New Sigil Scroll
```bash
POST /doors-of-durin/sigil-scrolls/new-ticket
```

### 🔍 View Sigil Scroll Content
```bash
POST /doors-of-durin/sigil-scrolls/view
```

### 💬 Reply to a Sigil Scroll
```bash
POST /doors-of-durin/sigil-scrolls/replies
```

### 🗑️ Permanently Delete a Sigil Scroll
```bash
DELETE /doors-of-durin/sigil-scrolls/delete/{ticketId}
```
