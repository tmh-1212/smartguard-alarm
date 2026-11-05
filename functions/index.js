const express = require("express");
const admin = require("firebase-admin");
const axios = require("axios");

const app = express();
app.use(express.json());

// Firebase Admin initialization
try {
  admin.initializeApp();
} catch (e) {
  console.log("Firebase initialized");
}

const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const DEFAULT_CHAT_ID = process.env.TELEGRAM_CHAT_ID;

// 1. Webhook endpoint for Telegram Bot updates (1-Tap Start linking)
app.post("/bot-webhook", async (req, res) => {
  try {
    const update = req.body;
    if (update.message && update.message.text) {
      const text = update.message.text.trim();
      const chatId = update.message.chat.id.toString();
      const firstName = update.message.from.first_name || "User";

      if (text.startsWith("/start")) {
        const parts = text.split(" ");
        if (parts.length > 1) {
          const userId = parts[1]; // e.g. dev_12345...
          
          // Save chatId to Firestore at users/{userId}
          await admin.firestore().collection("users").doc(userId).set({
            telegramChatId: chatId
          }, { merge: true });

          // Send confirmation message to user on Telegram
          if (TELEGRAM_BOT_TOKEN) {
            await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, {
              chat_id: chatId,
              text: `✅ *SmartGuard Alarm Connected successfully!*\n\nHello ${firstName}, your Telegram has been successfully linked to your device. You will now receive instant intruder alerts here!`,
              parse_mode: "Markdown"
            });
          }
        }
      }
    }
    res.status(200).send("OK");
  } catch (error) {
    console.error("Webhook Error:", error);
    res.status(500).send(error.message);
  }
});

// 2. Alert endpoint for Android app
app.post("/alert", async (req, res) => {
  try {
    const { message, deviceName, chatId } = req.body;
    const targetChatId = chatId || DEFAULT_CHAT_ID;

    const alertText = `🚨 *SmartGuard Alarm Alert* 🚨\n\n*Device:* ${deviceName || "Unknown"}\n*Message:* ${message || "Intruder Detected!"}`;

    if (TELEGRAM_BOT_TOKEN && targetChatId) {
      await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, {
        chat_id: targetChatId,
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
