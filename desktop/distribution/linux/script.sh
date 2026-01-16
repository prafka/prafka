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

os=$(cat /etc/os-release | grep "^ID=" | awk '{print substr($0, 4)}')
case $os in
  fedora) distribution_type=rpm;;
  ubuntu) distribution_type=deb;;
  *) echo "unknown os $os" && exit 0;;
esac

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
  --icon distribution/linux/icon.png \
  --resource-dir distribution/linux/resource \
  --linux-menu-group Development

function extractBinaries() {
  case $distribution_type in
    rpm) rpm2cpio target/distribution/prafka-"$PROJECT_VERSION"-1.x86_64.rpm | cpio -D target/distribution -idm;;
    deb) dpkg-deb -x target/distribution/prafka_"$PROJECT_VERSION"_amd64.deb target/distribution;;
    *) echo "unknown distribution_type $distribution_type" && exit 0;;
  esac
}

echo "AppImage"
extractBinaries
mkdir target/distribution/prafka.AppDir
mv target/distribution/opt/prafka target/distribution/prafka.AppDir/usr
rm -rf target/distribution/opt
rm -rf target/distribution/usr
ln -s usr/bin/Prafka target/distribution/prafka.AppDir/AppRun
cp -r distribution/linux/AppDir/* target/distribution/prafka.AppDir/
cp distribution/linux/icon.png target/distribution/prafka.AppDir/Prafka.png
distribution/linux/soft/appimagetool-x86_64.AppImage --runtime-file=distribution/linux/soft/runtime-x86_64 target/distribution/prafka.AppDir target/distribution/prafka-"$PROJECT_VERSION"-x86_64.AppImage
rm -rf target/distribution/prafka.AppDir

echo "tar.gz"
extractBinaries
rm target/distribution/opt/prafka/lib/prafka-Prafka.desktop
mv target/distribution/opt/prafka target/distribution/opt/prafka-"$PROJECT_VERSION"
tar -czf target/distribution/prafka-"$PROJECT_VERSION"-x86_64.tar.gz -C target/distribution/opt .
rm -rf target/distribution/opt
rm -rf target/distribution/usr