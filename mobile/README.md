# EONE Flutter mobile app

The application source in `lib/` is complete and consumes the real Spring Boot API.

## First-time native bootstrap

This build environment does not contain the Flutter SDK, so generated Android/iOS runner boilerplate is intentionally created on the machine that owns the Flutter SDK:

**Windows PowerShell**
```powershell
cd mobile
.\scripts\bootstrap_native.ps1
```

**macOS/Linux**
```bash
cd mobile
bash scripts/bootstrap_native.sh
```

After that:
```bash
flutter analyze
flutter test
flutter run --dart-define=EONE_API_URL=http://10.0.2.2:8080/api/v1
```

For a physical Android phone use your computer's LAN IP instead of `10.0.2.2`. For production, pass the HTTPS API endpoint through `EONE_API_URL`.
