# Contributing to SpendSense

Thank you for taking the time to contribute. SpendSense is a privacy-first, community-driven project — every contribution, big or small, matters.

Please read this document before opening an issue or submitting a pull request.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Contributor License Agreement](#contributor-license-agreement)
- [Ways to Contribute](#ways-to-contribute)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)
- [Your First Pull Request](#your-first-pull-request)
- [Development Setup](#development-setup)
- [Adding a New Bank SMS Format](#adding-a-new-bank-sms-format)
- [Code Style](#code-style)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [What Gets Merged](#what-gets-merged)

---

## Code of Conduct

This project follows our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold it. Please report unacceptable behaviour to **conduct@spendsense.dev** (or open a private GitHub issue).

---

## Contributor License Agreement

By submitting a pull request or any contribution to this repository, you agree to the [Contributor License Agreement](CLA.md).

In short: you keep ownership of your code, but you grant the SpendSense maintainers the rights needed to distribute it under AGPL-3.0 and to relicense it in the future if necessary. You also confirm that you have the right to contribute the code (i.e. it is your own work or you have permission).

If you are contributing on behalf of a company, an authorised representative of that company must sign the CLA. Open an issue titled **"CLA — Company Sign-off: [Company Name]"** and we will coordinate.

---

## Ways to Contribute

You do not need to write code to contribute meaningfully:

| Type | Examples |
|---|---|
| 🐛 Bug reports | Incorrect SMS parsing, wrong category, crash on specific device |
| 🏦 Bank SMS samples | New bank formats, edge cases, regional variants |
| 🌐 Translations | Hindi, Tamil, Telugu, Kannada, Bengali, Marathi UI strings |
| 📝 Documentation | Improve README, fix typos, add examples |
| 🧪 Testing | Test on new devices, Android versions, bank accounts |
| 💡 Feature ideas | Open a discussion before building |
| 🔍 Code review | Review open PRs, catch issues early |

---

## Reporting Bugs

Before opening a bug report:

1. Search [existing issues](https://github.com/YOUR_USERNAME/SpendSense/issues) to avoid duplicates
2. Check if it is fixed in the latest commit on `main`

When opening a bug report, include:

- **Device and Android version** (e.g. Pixel 6, Android 13)
- **SpendSense version** (Settings → About)
- **Steps to reproduce** — be specific
- **Expected behaviour** vs **actual behaviour**
- **Anonymised SMS sample** if the issue is a parsing failure — replace your account number and balance with `XXXX` before posting

> ⚠️ **Never post real SMS content with actual account numbers, balances, or transaction amounts.** Anonymise all financial data before sharing.

### SMS parsing failures

If a bank SMS is not being parsed correctly, use this template:

```
Bank: HDFC
SMS (anonymised): "Dear Customer, INR XXXX.00 debited from A/c XXXXXX on dd-mm-yy. UPI Ref XXXXXXXXXX. Avl Bal INR XXXX.00"
Expected result: amount=XXXX, type=debit, merchant=<expected>
Actual result: <what the app showed>
```

---

## Suggesting Features

Open a [GitHub Discussion](https://github.com/YOUR_USERNAME/SpendSense/discussions) rather than an issue for feature ideas. This lets the community weigh in before implementation begins.

Include:
- The problem you are trying to solve (not just the solution)
- Who else would benefit from this
- Any implementation thoughts (optional)

**Please open a discussion before starting work on a large feature.** We want to avoid situations where someone spends days building something that does not fit the project's direction.

---

## Your First Pull Request

Looking for a good first issue? Check issues labelled [`good first issue`](https://github.com/YOUR_USERNAME/SpendSense/issues?q=label%3A%22good+first+issue%22).

These are typically:
- Adding a new bank SMS regex pattern
- Fixing a categorisation rule
- Improving error messages
- Adding/fixing tests
- Documentation improvements

---

## Development Setup

### Requirements

- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK 26+
- Git

### Clone and build

```bash
git clone https://github.com/YOUR_USERNAME/SpendSense.git
cd SpendSense
./gradlew assembleDebug
```

### Run tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest
```

### Project structure

```
com.spendsense/
├── MainActivity.kt
├── SpendSenseApplication.kt
├── agents/              TransactionPipeline, IngestionAgent, ParserAgent,
│                        ClassifierAgent, BudgetAlertAgent, InsightAgent
├── llm/                 LlmProvider (interface), DynamicLlmProvider,
│                        RulesOnlyProvider, ClaudeProvider,
│                        OpenAiProvider, OllamaProvider
├── data/
│   ├── model/           Transaction, Budget, MerchantMap, WeeklyInsight
│   ├── db/              AppDatabase, TransactionDao, BudgetDao,
│   │                    MerchantMapDao, InsightDao
│   └── repository/      TransactionRepository, BudgetRepository,
│                        InsightRepository, SettingsRepository
├── receiver/            SmsReceiver
├── work/                InsightWorker (WorkManager)
├── di/                  DatabaseModule, LlmModule, WorkManagerModule
└── ui/
    ├── dashboard/       DashboardScreen, DashboardViewModel
    ├── history/         HistoryScreen, HistoryViewModel
    ├── budgets/         BudgetsScreen, BudgetsViewModel
    ├── settings/        SettingsScreen, SettingsViewModel
    ├── debug/           DebugScreen (debug builds only)
    └── theme/           Color, Theme, Type
```

---

## Adding a New Bank SMS Format

This is one of the most valuable contributions you can make. Here is the exact process:

### 1. Collect samples

Gather at least **5 real SMS samples** from the bank, covering:
- Debit transactions
- Credit transactions
- UPI transactions
- NEFT/IMPS/RTGS if applicable
- Edge cases (failed transactions, reversals)

Anonymise all samples before sharing — replace account numbers, balances, and personal details with `XXXX`.

### 2. Write the regex

Add your pattern to `app/src/main/java/com/spendsense/agents/IngestionAgent.kt`. Bank-specific regex patterns live in this file — find the section labelled `// Bank SMS patterns` and add yours:

```kotlin
// Example pattern — replace with actual bank format
val NEWBANK_DEBIT = Regex(
    """NewBank:\s*Rs\.(?<amount>[\d,]+\.?\d*)\s+debited.*?(?<account>X+\d{4})""",
    RegexOption.IGNORE_CASE
)
```

### 3. Add unit tests

Every new parser pattern **must** have unit tests. Add them to `app/src/test/java/com/spendsense/agents/IngestionAgentTest.kt`:

```kotlin
@Test
fun `parse NewBank debit SMS`() {
    val sms = "NewBank: Rs.450.00 debited from A/c XXXX1234 on 24-05-26"
    val result = ingestionAgent.parse(sms)
    assertThat(result.amount).isEqualTo(450.0)
    assertThat(result.type).isEqualTo(TransactionType.DEBIT)
    assertThat(result.account).isEqualTo("XXXX1234")
}
```

### 4. Open a PR

Title your PR: `feat(parser): add NewBank SMS format support`

Include in the PR description:
- The bank name and any regional variants
- Your anonymised sample SMS strings
- Test results

---

## Code Style

- Follow [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `ktlint` — run `./gradlew ktlintCheck` before submitting. Auto-fix with `./gradlew ktlintFormat`
- This project uses **KSP** (not KAPT) for annotation processing — if you add a new Room entity or Hilt module, make sure KSP is processing it correctly before submitting
- Compose UI: follow [Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)
- No hardcoded strings — use `strings.xml` for all user-facing text
- No hardcoded colours — use Material 3 theme tokens from `ui/theme/`

### What we avoid

- Third-party analytics or crash reporting libraries — we do not collect data
- Libraries that require network access for core functionality
- Dependencies with GPL-incompatible licenses

---

## Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```

**Types:** `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `perf`

**Scopes:** `parser`, `classifier`, `ui`, `db`, `agents`, `llm`, `notifications`, `export`

**Examples:**

```
feat(parser): add IndusInd Bank SMS format
fix(classifier): correctly categorise Swiggy Instamart as Groceries
docs(readme): update permissions table
test(parser): add edge cases for HDFC UPI credit SMS
```

---

## Pull Request Process

1. **Fork** the repository and create a branch from `main`
2. Branch naming: `feat/add-indusind-parser`, `fix/hdfc-credit-parsing`, `docs/update-contributing`
3. Make your changes with tests
4. Run `./gradlew test ktlintCheck` — both must pass
5. Open a PR against `main` with a clear description of what and why
6. A maintainer will review within 7 days
7. Address review comments — do not force-push during review
8. Once approved, a maintainer will merge using squash merge

### PR checklist

Before marking your PR as ready for review:

- [ ] Tests pass locally (`./gradlew test`)
- [ ] ktlint passes (`./gradlew ktlintCheck`)
- [ ] New functionality has unit tests
- [ ] No new analytics or network dependencies added
- [ ] Anonymised SMS samples included (for parser PRs)
- [ ] CLA agreed to (first-time contributors)

---

## What Gets Merged

We merge contributions that:

- Fix real bugs with a clear reproduction case
- Add bank SMS formats with proper tests and anonymised samples
- Improve privacy, performance, or correctness
- Add translations with full string coverage
- Improve documentation with accurate information

We generally do not merge:

- Features that require cloud dependencies or data collection
- UI redesigns without prior discussion
- Dependencies with incompatible licenses
- Code without tests (for logic-heavy changes)