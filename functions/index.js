const express = require("express");
const admin = require("firebase-admin");
const axios = require("axios");

const app = express();
app.use(express.json());

// Firebase Admin initialization (if used)
try {
  admin.initializeApp();
} catch (e) {
  console.log("Firebase Admin already initialized or not needed.");
}

const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const DEFAULT_CHAT_ID = process.env.TELEGRAM_CHAT_ID;

// Webhook for Android App trigger
app.post("/alert", async (req, res) => {
  try {
    const { message, deviceName, chatId } = req.body;
    
    // Use chatId from app if provided, otherwise fallback to environment variable
    const targetChatId = chatId || DEFAULT_CHAT_ID;

    const alertText = `🚨 *SmartGuard Alarm Alert* 🚨\n\n*Device:* ${deviceName || "Unknown"}\n*Message:* ${message || "Intruder Detected!"}`;

    if (TELEGRAM_BOT_TOKEN && targetChatId) {
      await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, {
        chat_id: targetChatId,
        text: alertText,
        parse_mode: "Markdown",
      });
      return res.status(200).send({ success: true, message: "Telegram notification sent via Render!" });
    } else {
      return res.status(500).send({ success: false, error: "Telegram credentials (token or chatId) missing." });
    }
  } catch (error) {
    console.error("Error sending Telegram message:", error.response?.data || error.message);
    return res.status(500).send({ success: false, error: error.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
