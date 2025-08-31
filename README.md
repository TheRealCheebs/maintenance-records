# Maintenance Records (Nostr Android App)

This project is an Android app for managing and verifying maintenance records using Nostr keys and relays. It allows users to create, view, and transfer ownership of maintenance records, with cryptographic verification and decentralized storage.

## Features
- Create and view maintenance records for vehicles or equipment
- Manage multiple Nostr keys and switch between them
- Sign and verify records using Nostr protocol
- Import records from Nostr relays
- Transfer ownership and technician signoff
- Material Design UI with RecyclerView, FAB, and dialogs
- Local Room database for offline access

## Getting Started

### Prerequisites
- Android Studio (Giraffe or newer recommended)
- Android SDK 24+
- Gradle 8.13+
- Kotlin 2.0+

### Build & Run
1. Clone the repository:
   ```sh
   git clone <repo-url>
   ```
2. Open in Android Studio.
3. Build and run on an emulator or device.

### Project Structure
- `app/src/main/java/com/github/therealcheebs/maintenancerecords/` - Main source code
- `app/src/main/res/layout/` - XML layouts
- `app/src/main/res/menu/` - Toolbar and FAB menus
- `app/src/main/res/drawable/` - Icons and images
- `app/build.gradle.kts` - App module build config
- `README.md` - Project documentation

## Key Concepts
- **Nostr Key Management:** Generate, import, and switch between cryptographic keys.
- **Record Verification:** Sign and verify maintenance records using Nostr events.
- **Relay Integration:** Import records from Nostr relays for the active key.
- **Room Database:** Store records locally for offline access and fast queries.

## Contributing
Pull requests and issues are welcome! Please open an issue for bugs or feature requests.

## License
This project is licensed under the MIT License.

## Nostr Protocol
Learn more about the Nostr protocol:
- [Nostr Protocol GitHub](https://github.com/nostr-protocol/nostr)
- [nostr.com](https://nostr.com/)

## Acknowledgements
- [Android Room](https://developer.android.com/training/data-storage/room)
- [Material Components](https://material.io/develop/android)
