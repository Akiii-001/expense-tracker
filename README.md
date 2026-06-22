# UPI Expense Tracker

A private, on-device Android app that automatically tracks your UPI spending
**and income** from bank SMS, lets you tag each payment with a reason, and gives
you weekly and monthly reports with your net balance.

## Why it's private

- **No internet permission.** The app's manifest deliberately does not request
  `INTERNET`. Android therefore makes it physically impossible for the app to
  send any data off your phone. Your spending data never leaves the device.
- **OTPs and other messages are ignored.** The SMS parser only accepts messages
  that look like a UPI *debit* and contain an amount. OTP, promotional, and
  credit messages are filtered out and never stored.
- **Local storage only.** Everything is kept in a private database inside the
  app's sandbox, which other apps cannot read.

## What it does

- Catches each bank SMS about a UPI debit or credit and records amount + payee.
- For spends, sends a notification: "What was ₹120 for?" → tap to pick a category.
- For credits (salary, refunds), auto-tags them as income so you see money in vs out.
- Remembers the category for a payee and applies it automatically next time —
  but only when that payee has been *consistent*. If you've ever given the same
  payee two different categories, it stops guessing and leaves it for you to set,
  so it won't mis-categorize.
- Lets you add transactions manually too (spent or received).
- Shows weekly and monthly received / spent / balance, plus spending by category.

---

## How to build it without installing anything (cloud build)

You don't need Android Studio. GitHub builds the app for you and gives you an
installable `.apk` file.

### 1. Put the code on GitHub
1. Create a free account at https://github.com (you can do this from your tablet).
2. Create a new **private** repository, e.g. `upi-expense-tracker`.
3. Upload the contents of this folder to the repo.
   - Easiest from a browser: open the repo → "Add file" → "Upload files" →
     drag the whole project in → Commit. (You may need to upload folder by folder.)
   - Or, if you have `git` somewhere: `git init`, `git add .`,
     `git commit -m "initial"`, then push to the repo.

### 2. Let GitHub build the APK
1. In your repo, open the **Actions** tab.
2. The **Build APK** workflow runs automatically on push. You can also click
   **Run workflow** to trigger it manually.
3. Wait a few minutes for it to finish (green check).

### 3. Download and install on your phone
1. Open the finished workflow run → scroll to **Artifacts** →
   download **upi-expense-tracker-apk**.
2. Unzip it to get `app-debug.apk`.
3. Transfer that file to your **phone** (the one with your bank SIM) and tap it
   to install. Allow "install from unknown sources" when asked.
4. Open the app and grant the SMS and notification permissions when prompted.

> Install the app on the **phone that receives your bank SMS**, not the tablet.
> The tablet doesn't get those messages, so it can't capture payments.

---

## Trying it out

- Make a small UPI payment, or wait for your next one. When the bank SMS
  arrives, a notification appears asking you to categorize it.
- Add a manual expense with the **+** button to see reports populate immediately.
- Check the **Reports** tab for weekly and monthly breakdowns.

## Tuning the SMS parser

Banks word their SMS differently. If a real payment isn't being captured, the
patterns in `app/src/main/java/com/upi/expensetracker/sms/SmsParser.kt` may need
a small tweak for your bank's wording. Send me an example SMS (with the amount
changed) and I can adjust the patterns.

## Notes

- This produces a **debug** APK, which is fine for personal use. It isn't
  published to the Play Store.
- `minSdk` is 26 (Android 8.0+). Your phone almost certainly qualifies.
