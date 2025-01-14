# /bin/sh
./gradlew spotlessApply clean shadowJar && cp build/libs/quick-hub-1.0-fat.jar ../android/app/lib/backend.jar
