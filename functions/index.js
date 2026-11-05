const express = require("express");
const axios = require("axios");

const app = express();
app.use(express.json());

const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const PROJECT_ID = "smartguard-alarm";

// Webhook for Telegram Bot updates (Zero credentials required!)
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
      // 2. When user sends the pairing code (e.g. 414FA6)
      else if (text.length >= 4 && text.length <= 10) {
        const pairingCode = text.toUpperCase();
        
        // Save chatId to Firestore via REST API (No JSON credentials needed!)
        const firestoreRestUrl = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents/pairing_codes/${pairingCode}`;
        await axios.patch(firestoreRestUrl, {
          fields: {
            telegramChatId: { stringValue: chatId }
          }
        });

        await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, {
          chat_id: chatId,
          text: `✅ *Successfully Connected!*\n\nYour code \`${pairingCode}\` is now linked. You can return to your app!`,
          parse_mode: "Markdown"
        });
      }
    }
    res.status(200).send("OK");
  } catch (error) {
    console.error("Webhook Error:", error.response?.data || error.message);
    res.status(500).send(error.message);
  }
});

// Alert endpoint for Android app
app.post("/alert", async (req, res) => {
  try {
    const { message, deviceName, chatId } = req.body;
    const targetChatId = chatId || process.env.TELEGRAM_CHAT_ID;

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
    console.error("Error sending Telegram message:", error.response?.data || error.message);
    return res.status(500).send({success: false, error: error.message});
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
