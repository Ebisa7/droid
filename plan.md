# XSEND Meet – Android WebRTC App Architecture & UX Plan

Goal: Build an extremely user‑friendly Android video call app (Kotlin) that feels like:

* 📤 XSender (instant, simple, local-first sharing vibe)
* 🎥 Zoom (structured rooms, grid layout, controls)

Core principles:

* Zero confusion
* Fast join (QR first)
* Clean UI
* Device-based identity (editable name)
* LAN-first but scalable

---

# 1. Product Philosophy

This app must feel:

* Instant (open → connect in seconds)
* Clean (minimal clutter)
* Safe (clear permissions + status)
* Non-technical for average users

User should never see:

* IP addresses
* Technical errors
* WebRTC jargon

Everything is simplified into:

* Create
* Scan
* Join

---

# 2. Identity System (Very Important UX Detail)

Instead of accounts, we use:

Display Name = Device Name (editable)

On first launch:

* Auto-detect device model (e.g., "Samsung A54")
* Set default name: "Ebisa's A54" (editable field)
* Store locally (SharedPreferences)

User can change name anytime in:
Settings → "Device Name"

Server only knows:
User {
userId: UUID
displayName: String
isHost: Boolean
}

No login system required for v1.

---

# 3. App Structure (User-Friendly Flow)

## 3.1 Launch Screen

Very clean.

Logo
App name

Two main buttons:

[ Create Room ]
[ Join Room ]

Small settings icon (top right)

---

# 4. Room Creation Flow (Host)

User taps "Create Room"

Flow:

1. Connect to signaling server
2. Server creates room
3. Server returns:

   * roomId
   * host confirmation

Then show:

ROOM SCREEN (Pre-call state)

Display:

* Big QR code in center
* Room Code below (6 characters)
* "Waiting for participants..."
* Participant list (live updating)

Buttons:
[ Start Call ] (optional if auto-start disabled)
[ Cancel Room ]

UX rule:
Call auto-starts when host enters camera view.

---

# 5. Join Flow

## 5.1 QR Join (Primary Method)

User taps "Join Room"

Options:
[ Scan QR ]
[ Enter Code Manually ]

If QR:

* Use MLKit
* Extract:
  {
  roomId,
  serverAddress
  }

App connects automatically.

Zero typing.

---

## 5.2 Manual Join

Simple screen:

Input: ROOM CODE
Button: JOIN

If invalid:
"Room not found"

No technical messages.

---

# 6. Room System (Server Side)

Room {
roomId: String
hostId: String
participants: MutableList<User>
createdAt: Timestamp
status: WAITING | ACTIVE | ENDED
}

Stored in:
HashMap<String, Room>

Future:
Redis

Room Code Rules:

* 6 uppercase alphanumeric
* Easy to read (avoid 0/O and 1/I)

Example:
A7K9XQ

---

# 7. Connection Architecture

Model: Mesh (P2P)

Each user creates peer connections with:
All other participants

Good for:
2–6 people

Upgrade path later:
SFU (Mediasoup)

---

# 8. Signaling Design

Server only relays messages.

Signal {
from: userId
to: userId
type: offer | answer | ice
payload: data
}

Server responsibilities:

* Manage rooms
* Relay signals
* Notify joins/leaves

Server never handles media.

---

# 9. Call Screen (Zoom-Style UX)

This is the most important screen.

Layout:

Top Bar:

* Room Code (tap to copy)
* Participant count
* Network indicator (Good / Weak)

Main Area:

* Grid layout (RecyclerView GridLayoutManager)
* Responsive:
  1 user → full screen
  2 users → split
  3–4 → 2x2
  5–6 → adaptive grid

Each tile shows:

* Video
* Display Name (bottom overlay)
* Mute indicator

---

# 10. Bottom Controls (Clean & Large Buttons)

Centered row:

[ Mic ]
[ Camera ]
[ Switch Camera ]
[ Share Screen (future) ]
[ Leave ] (red)

Host only:

* End Room (long press Leave)

Buttons must be:

* Large
* Simple icons
* No text clutter

---

# 11. XSender Feel (Local Fast Experience)

Enhancements:

* Show "Connected via Local Network" badge
* Instant join animation
* Smooth fade transitions
* Sound cue when someone joins

Make it feel instant.

---

# 12. Permissions UX

When first entering call:

Request:

* Camera
* Microphone

If denied:
Show friendly message:
"Camera access is required to join video call"

Never crash.

---

# 13. Video Settings

Target:

* 720p
* 30fps

WebRTC config:

* Hardware acceleration enabled
* Adaptive bitrate

If network weak:
Drop to 480p automatically

User never sees technical values.

---

# 14. Host Logic

Host can:

* Remove participant
* Mute participant
* End room for everyone

If host disconnects:
Room auto-ends (v1 simplicity)

Participants see:
"Host ended the meeting"

---

# 15. Reconnection Logic

If network drops:

Show overlay:
"Reconnecting..."

Try for 5 seconds.
If fail:
Return to home screen.

---

# 16. Settings Screen

Minimal settings:

* Device Name (editable)
* Default Video On/Off
* Default Mic On/Off
* About

Keep it simple.

---

# 17. Tech Stack

Android:

* Kotlin
* WebRTC Android SDK
* WebSocket (preferred over Socket.IO)
* CameraX
* MLKit (QR)

Server:

* Node.js + ws
  OR
* Python + FastAPI + WebSockets

Avoid eventlet.

---

# 18. Development Phases

Phase 1:
1-to-1 video

Phase 2:
Room system

Phase 3:
QR join

Phase 4:
Multi-user mesh

Phase 5:
Zoom-style UI polish

Phase 6:
Stability & reconnection

---

# 19. Future Expansion

* Screen sharing
* Chat
* File sharing (XSEND feature)
* Internet rooms
* TURN server
* Recording

---

# Final Goal

An app that:

* Your grampa can use
* Your friends can join in 3 seconds
* Feels instant like XSender
* Structured like Zoom

Clean. Fast. No confusion.

End of plan.md
