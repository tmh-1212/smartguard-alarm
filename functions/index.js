const express = require("express");
const admin = require("firebase-admin");
const axios = require("axios");

const app = express();
app.use(express.json());

try {
  admin.initializeApp();
} catch (e) {
  console.log("Firebase initialized");
}

const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const DEFAULT_CHAT_ID = process.env.TELEGRAM_CHAT_ID;

// Webhook for Telegram Bot updates
app.post("/bot-webhook", async (req, res) => {
  try {
    const update = req.body;
    if (update.message && update.message.text) {
      const text = update.message.text.trim();
      const chatId = update.message.chat.id.toString();
      const firstName = update.message.from.first_name || "User";

      // 1. When user sends /start
      if (text.startsWith("/start")) {
        await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, {
          chat_id: chatId,
          text: `🤖 *Welcome to SmartGuard Alarm, ${firstName}!*\n\nPlease send the 6-character *Pairing Code* shown inside your app to link your device instantly.`,
          parse_mode: "Markdown"
        });
      } 
      // 2. When user sends the pairing code (e.g. A8B9C2)
      else if (text.length >= 4 && text.length <= 10) {
        const pairingCode = text.toUpperCase();
        
        // Save chatId to Firestore under pairing_codes/{code}
        await admin.firestore().collection("pairing_codes").doc(pairingCode).set({
          telegramChatId: chatId,
          timestamp: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, {
          chat_id: chatId,
          text: `✅ *Successfully Connected!*\n\nYour code \`${pairingCode}\` is now linked. You can return to your app!`,
          parse_mode: "Markdown"
        });
      }
    }
    res.status(200).send("OK");
  } catch (error) {
    console.error("Webhook Error:", error);
    res.status(500).send(error.message);
  }
});

// Alert endpoint for Android app
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
