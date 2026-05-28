# SpendSense

A local-first Android app that automatically tracks your expenses by reading Indian bank SMS messages. No manual entry. No cloud sync. Your data stays on your device.

![Status](https://img.shields.io/badge/status-alpha-orange)
![License](https://img.shields.io/badge/license-AGPL--3.0-blue)
![Android](https://img.shields.io/badge/Android-8.0%2B-green)
![Platform](https://img.shields.io/badge/platform-Android-lightgrey)

## Features

- **Automatic SMS parsing** — reads debit/credit SMS from 11+ Indian banks (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOB, Yes Bank, Paytm, PhonePe, Google Pay) plus generic UPI fallback
- **Smart merchant resolution** — UPI VPA → clean merchant name via bundled lookup + optional LLM
- **Auto-categorisation** — Food, Transport, Utilities, Rent, Shopping, Medical, Entertainment, Savings, Other
- **Budget alerts** — set monthly limits per category, get notified at 80% and 100%
- **Recategorise** — tap any transaction in History to correct its category; future transactions from the same merchant use your correction
- **Provider-agnostic LLM** — works fully offline (rules only), or connect Claude, OpenAI-compatible, or Ollama

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| DI | Hilt |
| Background work | WorkManager |
| Networking | Retrofit + OkHttp |
| Settings | DataStore Preferences |

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 26+ (minSdk)
- A physical Android device or emulator with SMS support

### Build

```bash
git clone https://github.com/YOUR_USERNAME/SpendSense.git
cd SpendSense
./gradlew assembleDebug
```

### Permissions required

| Permission | Why |
|---|---|
| `RECEIVE_SMS` | Intercept bank SMS as they arrive |
| `READ_SMS` | Read existing SMS on first launch |
| `POST_NOTIFICATIONS` | Budget alert notifications (Android 13+) |
| `INTERNET` | Optional — only if an LLM provider is configured |

### LLM Setup (optional)

By default the app runs in **Rules Only** mode — no network, no API key needed. To enable smarter merchant resolution and weekly insights:

1. Open **Settings** in the app
2. Choose a provider: Claude, OpenAI-compatible, or Ollama
3. Enter your API key and model name
4. Any OpenAI-compatible endpoint works

## Architecture

```
SMS → SmsReceiver → TransactionPipeline
                        ├── IngestionAgent   (regex parsing)
                        ├── ParserAgent      (merchant resolution)
                        ├── ClassifierAgent  (categorisation)
                        ├── TransactionRepository (Room/SQLite)
                        └── BudgetAlertAgent (notifications)
```

All data is stored locally in a SQLite database at `/data/data/com.spendsense/databases/spendsense.db`. Nothing leaves the device unless you configure an LLM provider.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This project is licensed under the **GNU Affero General Public License v3.0** — see [LICENSE](LICENSE) for details.

In short: you are free to use, study, modify, and distribute this software, but any modified version you distribute or run as a network service must also be open sourced under AGPL-3.0.
