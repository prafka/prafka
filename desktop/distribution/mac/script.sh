#!/bin/bash

mkdir target/distribution
mkdir target/distribution-libs
cp target/libs/* target/distribution-libs
cp target/"$MAIN_JAR" target/distribution-libs

echo "jdeps"
detected_modules=$("$JAVA_HOME"/bin/jdeps \
  -q \
  --multi-release "$JAVA_VERSION" \
  --ignore-missing-deps \
  --print-module-deps \
  --class-path "target/distribution-libs/*" \
  target/"$MAIN_JAR")
echo "detected modules: ${detected_modules}"

manual_modules=jdk.security.auth,jdk.security.jgss,jdk.crypto.ec,jdk.localedata
echo "manual modules: ${manual_modules}"

echo "jlink"
"$JAVA_HOME"/bin/jlink \
  --strip-native-commands \
  --no-header-files \
  --no-man-pages  \
  --strip-debug \
  --add-modules "${detected_modules},${manual_modules}" \
  --include-locales=en \
  --output target/java-runtime

function jpackage() {
  distribution_type="$1"

  echo "jpackage $distribution_type"
  "$JAVA_HOME"/bin/jpackage \
    --type "$distribution_type" \
    --dest target/distribution \
    --input target/distribution-libs \
    --runtime-image target/java-runtime \
    --main-class "$MAIN_CLASS" \
    --main-jar "$MAIN_JAR" \
    --java-options "$JAVA_OPTIONS" \
    --name "$PROJECT_NAME" \
    --description "$PROJECT_DESCRIPTION" \
    --app-version "$PROJECT_VERSION" \
    --vendor "$VENDOR_NAME" \
    --about-url "$VENDOR_URL" \
    --icon distribution/mac/icon.icns \
    --mac-package-identifier "$MAIN_CLASS" \
    --mac-package-name "$PROJECT_NAME"

  mv target/distribution/"$PROJECT_NAME"-"$PROJECT_VERSION"."$distribution_type" \
     target/distribution/prafka-"$PROJECT_VERSION"."$distribution_type"
}

jpackage dmg
jpackage pkg

echo "tar.gz"
distribution/mac/soft/7zz x target/distribution/prafka-"$PROJECT_VERSION".dmg -otarget/distribution
mkdir target/distribution/prafka-temp
mv target/distribution/"$PROJECT_NAME"/"$PROJECT_NAME".app target/distribution/prafka-temp/prafka-"$PROJECT_VERSION"
tar -czf target/distribution/prafka-"$PROJECT_VERSION".tar.gz -C target/distribution/prafka-temp .
rm -rf target/distribution/"$PROJECT_NAME"
rm -rf target/distribution/prafka-temp
rm -f target/distribution/Prafka:com.apple.provenance

echo "checksums"
for file in target/distribution/*.dmg target/distribution/*.pkg target/distribution/*.tar.gz; do
  if [ -f "$file" ]; then
    shasum -a 256 "$file" | sed "s/target\/distribution\///g" > "$file.sha256"
  fi
done