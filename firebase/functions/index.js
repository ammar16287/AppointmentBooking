/**
 * BookEase Cloud Functions
 *
 * The Android app never sends SMS or schedules exact alarms. All reminders and booking
 * notifications are sent from here:
 *   - sendReminders: every 5 minutes, sends due reminders by push, email and SMS
 *   - onAppointmentCreated: tells the staff member (push) and the customer (email)
 *   - onAppointmentUpdated: tells the other side about approvals, reschedules and cancellations
 */
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions, logger } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
setGlobalOptions({ region: "asia-south1", maxInstances: 5 });

const DEFAULT_SETTINGS = {
  name: "My Salon",
  timezone: "Asia/Karachi",
  reminderOffsetsMin: [1440, 120],
};

async function getSettings() {
  const snap = await db.doc("settings/business").get();
  return { ...DEFAULT_SETTINGS, ...(snap.exists ? snap.data() : {}) };
}

function formatWhen(date, timezone) {
  return new Intl.DateTimeFormat("en-GB", {
    weekday: "short", day: "numeric", month: "short",
    hour: "numeric", minute: "2-digit", hour12: true, timeZone: timezone,
  }).format(date);
}

// ---------- Channels ----------

async function sendPush(user, appointmentId, kind, title, body) {
  if (!user || !user.fcmToken || user.notifyPush === false) return false;
  try {
    // Data-only message: the app shows it itself and records that the reminder arrived.
    await admin.messaging().send({
      token: user.fcmToken,
      data: { title, body, appointmentId, kind },
      android: { priority: "high" },
    });
    return true;
  } catch (e) {
    logger.warn("Push failed", { appointmentId, error: e.message });
    return false;
  }
}

async function sendEmail(to, subject, text) {
  const key = process.env.SENDGRID_API_KEY;
  const from = process.env.EMAIL_FROM;
  if (!key || !from || !to) return false;
  const res = await fetch("https://api.sendgrid.com/v3/mail/send", {
    method: "POST",
    headers: { Authorization: `Bearer ${key}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      personalizations: [{ to: [{ email: to }] }],
      from: { email: from },
      subject,
      content: [{ type: "text/plain", value: text }],
    }),
  });
  if (!res.ok) logger.warn("Email failed", { status: res.status, body: await res.text() });
  return res.ok;
}

async function sendSms(to, text) {
  const sid = process.env.TWILIO_ACCOUNT_SID;
  const token = process.env.TWILIO_AUTH_TOKEN;
  const from = process.env.TWILIO_FROM;
  if (!sid || !token || !from || !to) return false;
  const res = await fetch(`https://api.twilio.com/2010-04-01/Accounts/${sid}/Messages.json`, {
    method: "POST",
    headers: {
      Authorization: "Basic " + Buffer.from(`${sid}:${token}`).toString("base64"),
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({ To: to, From: from, Body: text }).toString(),
  });
  if (!res.ok) logger.warn("SMS failed", { status: res.status, body: await res.text() });
  return res.ok;
}

async function getUser(uid) {
  if (!uid || uid === "walkin") return null;
  const snap = await db.doc(`users/${uid}`).get();
  return snap.exists ? snap.data() : null;
}

/** Sends to the customer on every channel they allow. */
async function notifyCustomer(appt, apptId, kind, title, body, { email = true, sms = false } = {}) {
  const user = await getUser(appt.customerId);
  const results = [];
  results.push(await sendPush(user, apptId, kind, title, body));
  const emailTo = (user && user.email) || appt.customerEmail;
  if (email && (!user || user.notifyEmail !== false)) results.push(await sendEmail(emailTo, title, body));
  const phone = (user && user.phone) || appt.customerPhone;
  if (sms && user && user.notifySms === true) results.push(await sendSms(phone, `${title}: ${body} Reply STOP to opt out.`));
  return results.some(Boolean);
}

async function notifyStaff(appt, apptId, title, body) {
  if (!appt.staffUserId) return;
  const user = await getUser(appt.staffUserId);
  await sendPush(user, apptId, "booking", title, body);
}

// ---------- Scheduled reminders ----------

exports.sendReminders = onSchedule(
  { schedule: "every 5 minutes", timeZone: "Asia/Karachi" },
  async () => {
    const settings = await getSettings();
    const offsets = [...(settings.reminderOffsetsMin || [])].sort((a, b) => b - a); // e.g. [1440, 120]
    if (offsets.length === 0) return;
    const now = Date.now();
    const horizon = new Date(now + (offsets[0] + 5) * 60_000);

    const snap = await db.collection("appointments")
      .where("startAt", ">", admin.firestore.Timestamp.fromMillis(now))
      .where("startAt", "<=", admin.firestore.Timestamp.fromDate(horizon))
      .get();

    for (const doc of snap.docs) {
      const a = doc.data();
      if (a.status !== "CONFIRMED") continue;
      const start = a.startAt.toMillis();
      const minutesLeft = (start - now) / 60_000;
      const sent = a.remindersSent || [];
      // Offsets whose time has come and that were not sent yet.
      const due = offsets.filter((o) => minutesLeft <= o && !sent.includes(o));
      if (due.length === 0) continue;

      const when = formatWhen(a.startAt.toDate(), settings.timezone);
      const title = `Reminder: ${a.serviceName}`;
      const body = `${when} with ${a.staffName} at ${settings.name}.`;
      // Send once for the closest offset; mark the larger missed ones as done too.
      await notifyCustomer(a, doc.id, "reminder", title, body, { email: true, sms: true });
      await doc.ref.update({ remindersSent: admin.firestore.FieldValue.arrayUnion(...due) });
      logger.info("Reminder sent", { id: doc.id, due });
    }
  }
);

// ---------- Booking events ----------

exports.onAppointmentCreated = onDocumentCreated("appointments/{id}", async (event) => {
  const a = event.data && event.data.data();
  if (!a) return;
  const settings = await getSettings();
  const when = formatWhen(a.startAt.toDate(), settings.timezone);
  const pending = a.status === "PENDING";

  await notifyStaff(a, event.params.id,
    pending ? "New booking request" : "New booking",
    `${a.customerName}: ${a.serviceName}, ${when}`);

  if (a.updatedBy === a.customerId) {
    await notifyCustomer(a, event.params.id, "booking",
      pending ? `Request received: ${a.serviceName}` : `Booking confirmed: ${a.serviceName}`,
      `${when} with ${a.staffName} at ${settings.name}.` +
        (pending ? " We'll let you know once it's approved." : ""),
      { email: true, sms: false });
  }
});

exports.onAppointmentUpdated = onDocumentUpdated("appointments/{id}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  const id = event.params.id;
  const statusChanged = before.status !== after.status;
  const moved = !before.startAt.isEqual(after.startAt) || before.staffId !== after.staffId;
  if (!statusChanged && !moved) return;

  const settings = await getSettings();
  const when = formatWhen(after.startAt.toDate(), settings.timezone);
  const byCustomer = after.updatedBy === after.customerId;

  if (byCustomer) {
    if (after.status === "CANCELLED") {
      await notifyStaff(after, id, "Booking cancelled", `${after.customerName} cancelled ${after.serviceName}, ${when}`);
    } else if (moved) {
      await notifyStaff(after, id, "Booking moved", `${after.customerName}: ${after.serviceName}, now ${when}`);
    }
    return;
  }

  // Changed by staff or admin: tell the customer.
  let title = null;
  if (after.status === "CANCELLED") title = `Cancelled: ${after.serviceName}`;
  else if (moved) title = `Rescheduled: ${after.serviceName}`;
  else if (before.status === "PENDING" && after.status === "CONFIRMED") title = `Confirmed: ${after.serviceName}`;
  if (!title) return;

  const body = after.status === "CANCELLED"
    ? `Your booking for ${when} has been cancelled. Open the app to book a new time.`
    : `${when} with ${after.staffName} at ${settings.name}.`;
  await notifyCustomer(after, id, "booking", title, body, { email: true, sms: after.status === "CANCELLED" || moved });
  if (moved && after.staffUserId) {
    await notifyStaff(after, id, "Booking moved", `${after.customerName}: ${after.serviceName}, now ${when}`);
  }
});
