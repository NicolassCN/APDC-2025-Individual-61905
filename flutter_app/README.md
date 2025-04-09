# User Management System - Flutter App

This is a Flutter application that demonstrates the REST operations of the User Management System.

## Features

- User registration
- User login
- Role-based access control
- User profile management
- Admin controls (for ADMIN and BACKOFFICE roles)

## Prerequisites

- Flutter SDK (version 3.0.0 or higher)
- Dart SDK (version 3.0.0 or higher)
- Android Studio / VS Code with Flutter extensions
- A running instance of the backend server

## Getting Started

1. Clone the repository
2. Navigate to the Flutter app directory:
   ```bash
   cd flutter_app
   ```

3. Install dependencies:
   ```bash
   flutter pub get
   ```

4. Run the app:
   ```bash
   flutter run
   ```

## Configuration

The app is configured to connect to the backend server at `http://localhost:8080/rest`. If your backend is running on a different URL, update the `baseUrl` in `lib/providers/auth_provider.dart`.

## Project Structure

```
lib/
  ├── main.dart              # Application entry point
  ├── providers/             # State management
  │   └── auth_provider.dart # Authentication state
  └── screens/              # UI screens
      ├── login_screen.dart  # Login screen
      ├── register_screen.dart # Registration screen
      └── home_screen.dart  # Home screen with role-based UI
```

## Testing

To run the tests:

```bash
flutter test
```

## Building for Production

To build the app for production:

```bash
flutter build apk  # For Android
flutter build ios  # For iOS
```

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request 