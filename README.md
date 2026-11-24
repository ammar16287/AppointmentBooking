# BookEase — Clinic & Salon Appointment Booking (Android + Firebase)

Customers book services with a chosen staff member; staff run their day from a calendar; admins manage
services, staff, hours and rules. Reminders go out by push, email and SMS **from the server**, so the
Android app needs no restricted Play Store permissions.

| Part | Tech |
| --- | --- |
| Android app | Kotlin, Jetpack Compose, Material 3, Navigation, WorkManager |
| Login | Firebase Authentication (email + password) |
| Data | Cloud Firestore |
| Reminders and alerts | Cloud Functions (Node 22) + Firebase Cloud Messaging, SendGrid email, Twilio SMS |

## What's in the app

- **Customer:** browse services → pick staff or "Any available" → pick a date → pick a free time → confirm.
  My bookings (upcoming / past), cancel or reschedule before the cut-off, add to phone calendar, call or email the business.
- **Staff:** "My schedule" day calendar, approve / check in / complete / no-show, visit or style notes, block time off, book walk-ins.
- **Admin:** Day (one column per staff), Week and Month calendars, services, staff (skills, weekly hours, login link),
  time off for anyone, business settings (salon or clinic wording, notice period, cancel cut-off, auto-confirm,
  reminder times, closed dates), make other admins.
- **No double booking:** every 15-minute block of a booking gets a lock document, written in a Firestore transaction.
- **Account deletion** inside the app (Play Store requirement).

## Play Store–safe permissions

Only `INTERNET`, `ACCESS_NETWORK_STATE` and `POST_NOTIFICATIONS` (plus normal ones that Firebase and WorkManager
add, such as `WAKE_LOCK`). Calling, email, maps and "Add to calendar" use intents. Never added: SMS, call log,
contacts, calendar, exact alarms, background location, `QUERY_ALL_PACKAGES`, foreground service.
Check **Merged Manifest** in Android Studio before each release.

---

## Setup (about 20 minutes)

### 1. Firebase project
1. Go to <https://console.firebase.google.com> → **Add project** (e.g. `bookease`).
2. **Build → Authentication → Get started → Email/Password → Enable.**
3. **Build → Firestore Database → Create database →** production mode, location **asia-south1 (Mumbai)**.
4. **Project settings → Add app → Android**, package name `com.ammar.appointmentbooking`.
5. Download **google-services.json** and put it in `app/` (next to `app/build.gradle.kts`).
   The build fails without this file.

### 2. Run the app
1. Open this folder in Android Studio (Narwhal or newer) and let Gradle sync.
   If Studio offers to upgrade AGP or library versions, accepting is fine.
2. Run on a phone or emulator (Android 8.0+). Register an account.

### 3. Make yourself admin (once)
Firestore console → `users` → your document → change `role` from `CUSTOMER` to `ADMIN`.
Restart the app: you'll see Calendar, Walk-in, Manage, Profile. From then on, add other admins in
**Manage → Business settings → Admins**.

### 4. Set up the business in the app
1. **Manage → Business settings:** name, salon or clinic, phone, address, rules. Save.
2. **Manage → Services:** add each service (duration, buffer, price).
3. **Manage → Staff:** add each person, tick their services, set weekly hours.
   To give them a staff login: they register in the app first, then you enter their email in
   "Login email" and save — their account switches to staff.

### 5. Deploy security rules and functions
Requires the **Blaze (pay-as-you-go)** plan for Cloud Functions. Typical small-business usage stays within the free quota.

```bash
npm install -g firebase-tools
firebase login
cd firebase
firebase use --add            # choose your project
cd functions
copy .env.example .env        # Windows (use cp on Mac/Linux), then fill in SendGrid / Twilio keys
npm install
cd ..
firebase deploy --only firestore:rules,functions
```

Email and SMS are optional: leave their keys blank and only push notifications are sent.
For Pakistani numbers, a local SMS gateway is usually cheaper than Twilio — swap the `sendSms` function in
`firebase/functions/index.js` for your provider's REST call.

### 6. Release to Google Play
- **Build → Generate Signed App Bundle** (.aab), `targetSdk` is already 36.
- Host a privacy policy (e.g. GitHub Pages) and add its URL in Play Console.
- **Data safety:** name, email, phone, appointment history; encrypted in transit; users can delete their account.
- **App access:** give reviewers a demo customer, staff and admin login.
- New personal developer accounts need a closed test (12 testers, 14 days) before production.

---

## Project map

```
app/src/main/java/com/ammar/appointmentbooking/
  BookingApp.kt, MainActivity.kt       app start, notification channels, permission prompt
  data/                                Firestore models + AuthRepository + BookingRepository
  domain/SlotEngine.kt                 free-slot calculation
  notify/                              FCM service, notifications, WorkManager fallback reminder
  util/                                time formatting, intents (dial, email, maps, calendar)
  ui/AppRoot.kt                        login gate, role-based tabs, navigation
  ui/auth, ui/customer, ui/staff, ui/admin, ui/profile, ui/common (calendar views, dialogs)
firebase/
  firestore.rules                      who can read and write what
  functions/index.js                   reminders + booking notifications
```

## Firestore collections

| Collection | Holds |
| --- | --- |
| `users/{uid}` | name, email, phone, role (CUSTOMER/STAFF/ADMIN), staffId, fcmToken, notify toggles |
| `settings/business` | business info and booking rules |
| `services/{id}` | name, durationMin, bufferMin, price, active |
| `staff/{id}` | name, title, userId, serviceIds, hours (per weekday, minutes from midnight) |
| `timeOff/{id}` | staffId, startAt, endAt, reason |
| `appointments/{id}` | customer, staff, service, startAt, endAt, status, notes, lockIds, remindersSent |
| `slotLocks/{staffId_date_minute}` | one per taken 15-min block (no personal data) |

## Ideas for next steps
- Phone-number (OTP) sign-in with Firebase Auth.
- Urdu translation: move UI text into `res/values/strings.xml` and add `values-ur/strings.xml`.
- Online payment (JazzCash, Easypaisa or Stripe) — allowed outside Play Billing because the service is in person.
