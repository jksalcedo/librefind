
> **Google has announced that, starting in 2026/2027, all apps on certified Android devices
> will require the developer to submit personal identity details directly to Google.
> Since the developers of this app do not agree to this requirement, this app will no longer 
> work on certified Android devices after that time.**
> https://keepandroidopen.org/




<h1 align="center">
  <br>
    <img src="./fastlane/metadata/android/en-US/images/icon.png" alt="LibreFind icon" width="150" />
  <br>
  LibreFind
  <br>
</h1>

<h4 align="center">Discover and replace proprietary apps with FOSS alternatives.</h4>

<h4 align=center>
  <a href="https://developer.android.com">
    <img src="https://img.shields.io/badge/Platform-Android-brighgtreen?logo=android" alt="Android Platform">
  </a>
  <img src="https://img.shields.io/badge/API-24%2B-3DDC84?logo=android&color=blue" alt="Minimum SDK">
  <img alt="GitHub Release" src="https://img.shields.io/github/v/release/jksalcedo/librefind?include_prereleases&logo=github&label=Release&color=blue">
  <img alt="GitHub Downloads (all assets, all releases)" src="https://img.shields.io/github/downloads/jksalcedo/librefind/total?label=APK%20Downloads&color=brightgreen">
  <img alt="GitHub License" src="https://img.shields.io/github/license/jksalcedo/librefind?logo=MIT&label=License&color=blue">
  <a href="https://github.com/jksalcedo/librefind/actions/workflows/codeql.yml">
    <img src="https://github.com/jksalcedo/librefind/actions/workflows/codeql.yml/badge.svg" alt="CodeQL Advanced">
  </a>
  <img src="https://img.shields.io/github/last-commit/jksalcedo/librefind?color=blue" alt="Last Commit">
</h4>

<p align="center">
<a href="https://apt.izzysoft.de/packages/com.jksalcedo.librefind">
  <img alt="Get it on IzzyOnDroid" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" width="160">
</a>

<a href="https://f-droid.org/packages/com.jksalcedo.librefind">
    <img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    width="160">
</a>

  <a href="https://github.com/jksalcedo/librefind/releases">
  <img src="https://github.com/SilentCoderHere/aihub/blob/main/fastlane/metadata/android/en-US/images/badge_github.png" width="160" alt="Get it on GitHub">
  </a>

  <br>

  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/jksalcedo/librefind">
    <img alt="Get it on Obtainium" src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" width="140">
  </a>

   <a href="https://zapstore.dev/apps/naddr1qqtkxmmd9e4xkumpd33k2er09ekxjcnjv4nxjmnyqgsx2k5ctva9q7wztnpgrcrcsjnt30s0hq8mjjna7sjcef0j0saqa8grqsqqqlstgxhlut">
    <img alt="Zapstore" src="https://github.com/zapstore/zapstore/blob/master/assets/images/badge.png" width="140">
  </a>
</p>

## What is LibreFind?

LibreFind is a free and lightweight Android app that scans your installed packages locally and
queries our database to identify proprietary software and find FOSS alternatives.

### Core Features

- Scan installed apps to detect proprietary software
- Get FOSS alternative recommendations
- View sovereignty scores showing FOSS vs. proprietary ratio
- Community-driven database

## Screenshots

| Dashboard                                                                           | Alternative List                                                                              | Submission                                                                            | Profile                                                                         |
|-------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| ![Dashboard](fastlane/metadata/android/en-US/images/phoneScreenshots/dashboard.jpg) | ![Alternatives](fastlane/metadata/android/en-US/images/phoneScreenshots/alternative_list.jpg) | ![Submission](fastlane/metadata/android/en-US/images/phoneScreenshots/submission.jpg) | ![Profile](fastlane/metadata/android/en-US/images/phoneScreenshots/profile.jpg) |
| Scan your apps and view sovereignty score                                           | Browse FOSS alternatives                                                                      | Contribute new alternatives                                                           | Track your submissions                                                          |

### Community Contributions

- Propose new FOSS alternatives for review
- Submit pros and cons for existing alternatives
- Vote on app recommendations
- Help build a comprehensive alternative database

You can contribute from within the app, or you can go to [librefind-submission.web.app](https://librefind-submission.web.app/) and use the web version.
  
---

### Required Permissions

- `QUERY_ALL_PACKAGES` - To scan installed apps
    - **Note**: This is a restricted permission.
- `INTERNET` - To query Supabase

Please join the Telegram channel for further discussions.
[Telegram](https://t.me/librefind)

## ☕ Support

LibreFind is a free and open-source project managed independently. Our goal is to promote digital
privacy by making FOSS apps mainstream. Currently, LibreFind is trusted by **1,334 registered users 
** globally.

While the app is free, **infrastructure and hosting servers** cost money to keep running 24/7.

If you find this tool useful for reclaiming your digital privacy, please consider buying me a
coffee. Your support goes directly toward:

* Paying monthly server bills.
* Keeping the database online and fast.
* Development of new features.

<div align="center">
  <a href="https://ko-fi.com/jksalcedo">
    <img src="https://storage.ko-fi.com/cdn/kofi3.png?v=3" alt="Buy Me a Coffee at ko-fi.com" height="50">
  </a>
</div>

## Contributing

Can't donate? You can still make a real difference:

- **Star the repo** — helps others discover LibreFind on GitHub
- **Report bugs** — open an issue if something isn't working
- **Suggest features** — share ideas through GitHub issues
- **Test & give feedback** — usability and performance reports are valuable
- **Translate** — help bring LibreFind to more languages
  via [Weblate](https://hosted.weblate.org/engage/librefind/)
- **Spread the word** — share with friends or on social media
- **Contribute code** — fix bugs or add features via pull requests

Every small contribution helps the project grow.

## 🌍 Translations

[![Crowdin](https://badges.crowdin.net/librefind/localized.svg)](https://crowdin.com/project/librefind)

LibreFind is a global project, and we aim to make FOSS discovery accessible to everyone. 

**Current Status:** We are currently transitioning our localization workflow. While we plan to move to a **self-hosted Weblate instance** in the near future (generously hosted by [@ren7](https://github.com/ren7uk)), we are using **Crowdin** for all active translations in the interim.

### How to contribute:
1. Visit our [Crowdin Project Page](https://crowdin.com/project/librefind).
2. Select your language.
3. Start translating! 

*Note: If your language isn't listed, feel free to open an issue or request it directly on Crowdin.*

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=jksalcedo/librefind&type=date&legend=top-left&theme=dark)](https://www.star-history.com/#jksalcedo/librefind&type=date&legend=top-left&theme=dark)

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE)
file for details.
