

## What is Fossia?

Fossia is an Android app that scans your device, identifies proprietary software, and recommends Free and Open Source Software (FOSS) alternatives. It uses **semantic web federation** (Wikidata + Firebase) instead of maintaining a centralized proprietary database.

### Core Mission
Help users replace proprietary apps with FOSS alternatives.


## ️ Tech Stack

| Component | Library | Version |
|-----------|---------|---------|
| Language | Kotlin | 2.0.21 |
| UI | Jetpack Compose | Latest |
| DI | Hilt | 2.51.1 |
| Database (Local) | Room | 2.6.1 |
| Database (Remote) | Firebase Firestore | 33.7.0 |
| Network | Retrofit + OkHttp | 2.11.0 |
| Async | Coroutines + Flow | 1.9.0 |
| Navigation | Navigation Compose | 2.8.5 |


##  Permission Requirements

### Required Permissions

- `QUERY_ALL_PACKAGES` - To scan installed apps
  - **Note**: This is a restricted permission. 
  - For Play Store, this requires special approval and manual review.

- `INTERNET` - To query Firebase and Wikidata


### Community

- Users can propose new FOSS alternatives
- Reviewers verify submissions
- Community voting system for quality


## License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.
