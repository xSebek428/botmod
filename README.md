# BotMod (Fabric, Minecraft Java 1.21.4)

## Budowanie
1. Zainstaluj JDK 21 oraz Gradle 8.12+ (https://gradle.org/install/).
2. W folderze projektu wykonaj JEDNORAZOWO: `gradle wrapper --gradle-version 8.12`
3. Potem: `./gradlew build` (Windows: `gradlew.bat build`)
4. Gotowy mod: `build/libs/BotMod-1.0.0.jar` (NIE bierz plików `-sources`).
5. Wrzuć go do `.minecraft/mods` razem z Fabric API 0.119.2+1.21.4.

Alternatywa: otwórz folder w IntelliJ IDEA (import projektu Gradle) i uruchom zadanie `build`.
