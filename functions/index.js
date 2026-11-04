const express = require("express");
const admin = require("firebase-admin");
const axios = require("axios");

const app = express();
app.use(express.json());

// Firebase Admin initialization
admin.initializeApp();

const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const TELEGRAM_CHAT_ID = process.env.TELEGRAM_CHAT_ID;

// Webhook for Firestore or Android App trigger
app.post("/alert", async (req, res) => {
  try {
    const { message, deviceName } = req.body;
    const alertText = `🚨 *SmartGuard Alarm Alert* 🚨\n\n*Device:* ${deviceName || "Unknown"}\n*Message:* ${message || "Intruder Detected!"}`;

    if (TELEGRAM_BOT_TOKEN && TELEGRAM_CHAT_ID) {
      await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, {
        chat_id: TELEGRAM_CHAT_ID,
        text: alertText,
        parse_mode: "Markdown",
      });
      return res.status(200).send({ success: true, message: "Telegram notification sent!" });
    } else {
      return res.status(500).send({ success: false, error: "Telegram credentials missing." });
    }
  } catch (error) {
    console.error("Error sending Telegram message:", error);
    return res.status(500).send({success: false, error: error.message});
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
