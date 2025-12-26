# Library System — Final Project (Group 7)

Group: Final Project — Group 7

Members:
1. Athira Tsaabitha  
2. Nor Umayah  
3. Rasyad P. R  
4. Shonia Maudina  
5. Theresa A.P

Project Overview
----------------
Library System is a modern, aesthetic Android application for library management that helps users explore, borrow, and organize books easily. The app is designed for an intuitive user experience and integrates with Firebase for backend services.

What this project includes:
- Book cataloging
- Category filtering
- Borrowing history
- Personal collection tracking
- Code/QR scanning feature
- Admin navigation (future scope)

Key Features
------------
1. General
- Book catalog (create/update/delete on admin scope)
- Category filtering
- Borrowing history
- Personal collection tracking
- QR / Barcode scanning
- (Future) Admin navigation for advanced management

2. User Side
- Personalized greeting
- Search books by title, author, ISBN, etc.
- View available and recommended books
- Borrow books with status indicators (e.g., `REJECTED`, `INSIDE`)
- Browse books by category
- View recently borrowed books
- Track personal book collection with reading status:
  - `Finished`
  - `Reading`
  - `To Read`
  - `Favorite`

3. Media & Scanner
- Book cover management: upload and display images using Firebase Storage
- Optimized image loading using Glide
- Scan QR/Barcode using Google Code Scanner and ZXing

4. Authentication & Networking
- Login / Register using:
  - Email & Password
  - Google Sign-In (optional, if enabled)
- HTTP requests handled with OkHttp / OkHttp3
- Backend-ready using `firebase-dataconnect` for type-safe data connections

Technologies & Libraries
------------------------
- Language: Java
- Architecture: MVVM (recommended)
- UI: Android Jetpack + Material Design Components
- IDE: Android Studio Giraffe (2022.3.1) or newer
- Firebase:
  - firebase-auth
  - firebase-firestore
  - firebase-storage
  - firebase-dataconnect
- Scanner:
  - com.google.android.gms:play-services-code-scanner
  - com.google.zxing:core
- Image loading:
  - com.github.bumptech.glide:glide
- Networking:
  - com.squareup.okhttp3:okhttp
- UI components:
  - appcompat, constraintlayout, recyclerview, cardview, material

Prerequisites
-------------
Before you begin, make sure you have:
- Android Studio Giraffe (2022.3.1) or later
- JDK 17 or later
- A Firebase account
- Internet connection for Gradle sync and Firebase access

Installation and Setup Guide
----------------------------

1. Clone the repository
```bash
git clone https://github.com/mayonicee/Library-System-Final-Project_.git
cd Library-System-Final-Project_
```

If you downloaded the project as a ZIP:
1. Extract the ZIP to your desired directory.
2. Open Android Studio.
3. Select File > Open and navigate to the extracted folder.
4. Wait for Gradle to sync automatically.

2. Firebase Configuration
This app requires a connection to a Firebase project.

a. Open the Firebase Console (https://console.firebase.google.com).  
b. Create a new Firebase project or use an existing one.  
c. Register an Android app in the Firebase project:
   - Enter the app package name. Some templates use `com.google.firebase.dataconnect.generated` as a placeholder — replace this with the actual `applicationId` from `app/build.gradle` if different.
d. Download the `google-services.json` configuration file from the Firebase project settings.  
e. Place the file in the app module folder:
```
app/google-services.json
```
f. Enable required Firebase services in the Firebase Console:
   - Authentication: enable Email/Password and Google Sign-In (if used)
   - Firestore: create a Firestore database
   - Storage: create a Cloud Storage bucket
   - Data Connect: configure firebase-dataconnect (ensure correct region, e.g., asia-southeast1, if applicable)

3. Local configuration
- If there is an example environment or properties file (e.g., `.env.example` or `local.properties`), copy it to the expected filename and fill in the values.
- Verify `applicationId`, `minSdk`, `targetSdk`, and dependencies in `app/build.gradle`.

Running the App
---------------
1. Open the project in Android Studio.
2. Select an emulator or connect a physical Android device.
3. Build and run the `app` module (Run ▶ in Android Studio).
4. The app will be installed and launched on the selected device.

Notes & Special Instructions
----------------------------
- Google Sign-In requires adding the app's SHA-1 and/or SHA-256 fingerprint in the Firebase Console; configure this if you use OAuth.
- Adjust Firestore and Storage security rules before deploying to production.
- For QR/Barcode scanning, ensure the app requests CAMERA permission at runtime on Android 6.0+.
- If using firebase-dataconnect or Cloud Functions, confirm regional settings and deploy configurations.

Testing
-------
Run unit and instrumentation tests via Gradle (if configured):
```bash
# Run unit tests
./gradlew test

# Run connected Android tests on a device/emulator
./gradlew connectedAndroidTest
```
Adjust commands according to the project's test setup.

Contributing
------------
1. Fork the repository.
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit your changes: `git commit -m "Add feature X"`
4. Push to your fork: `git push origin feat/your-feature`
5. Open a Pull Request describing your changes.

Reporting Issues
----------------
Report bugs or request features via GitHub Issues. Provide steps to reproduce, expected behavior, and actual behavior.

Contact
-------
- Repository owner / maintainer: mayonicee  
- Group members: see the Members section above  
