# Smart Pantry Manager

A Java Android application prototype for tracking pantry ingredients and suggesting recipes only when all required ingredient names are present.

## Database
Uses Android SQLite via `SQLiteOpenHelper`. Pantry ingredients persist locally. The first database creation seeds 20 recipes.

## Main features
- Add, edit and delete pantry ingredients
- Input validation for names, units and positive numeric quantities
- Pantry list stored in SQLite
- Strict recipe matching: every required ingredient name must exist in the pantry
- Recipe details and preparation steps
- Settings screen with an expiry-alert preference demonstration
- Simple navigation buttons

## Run
1. Open this folder in Android Studio.
2. Allow Gradle to sync. If prompted, install Android SDK Platform 35 (or change `compileSdk` to an installed SDK version).
3. Create/start an Android emulator or connect an Android device with USB debugging enabled.
4. Press Run for the `app` configuration.

## Notes
This is a minimal, single-Activity prototype. Quantity matching and expiry-date storage are not implemented; the assignment brief's ideal matching also checks required quantities and robustness to ingredient-name variants. Test the project in Android Studio before representing it as working. Add your own screenshots and a narrated demonstration of the actual app. Do not claim Git history or a video exists unless you have created them.
