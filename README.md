# Parse Facebook Utils for Android
[![Build Status][build-status-svg]][build-status-link]
[![Coverage Status][coverage-status-svg]][coverage-status-link]
[![Bintray][bintray-svg]][bintray-link]
[![License][license-svg]][license-link]

A utility library to authenticate `ParseUser`s with the Facebook SDK. For more information, see our [guide][guide].

## Download
Download [the latest AAR][latest] or define in Gradle:

```groovy
dependencies {
  implementation 'com.parse:parsefacebookutils-v4-android:X.X.X' 
}
```

where `X.X.X` is the latest version: [![Bintray][bintray-svg]][bintray-link]

Snapshots of the development version are available by using [Jitpack][jitpack-snapshot-link].

## Usage
Everything can done through the supplied gradle wrapper:

### Compile a JAR
```
./gradlew clean jarRelease
```
Outputs can be found in `Parse/build/libs/`

### Run the Tests
```
./gradlew clean testDebug
```
Results can be found in `Parse/build/reports/`

### Get Code Coverage Reports
```
./gradlew clean jacocoTestReport
```
Results can be found in `Parse/build/reports/`

## How Do I Contribute?
We want to make contributing to this project as easy and transparent as possible. Please refer to the [Contribution Guidelines](CONTRIBUTING.md).

## License
    Copyright (c) 2015-present, Parse, LLC.
    All rights reserved.

    This source code is licensed under the BSD-style license found in the
    LICENSE file in the root directory of this source tree. An additional grant
    of patent rights can be found in the PATENTS file in the same directory.

As of April 5, 2017, Parse, LLC has transferred this code to the parse-community organization, and will no longer be contributing to or distributing this code.

 [guide]: https://parse.com/docs/android/guide#users-facebook-users

 [latest]: https://search.maven.org/remote_content?g=com.parse&a=parsefacebookutils-v4-android&v=LATEST
 [jitpack-snapshot-link]: https://jitpack.io/#parse-community/ParseFacebookUtils-Android/master-SNAPSHOT

 [build-status-svg]: https://travis-ci.org/ParsePlatform/ParseFacebookUtils-Android.svg?branch=master
 [build-status-link]: https://travis-ci.org/ParsePlatform/ParseFacebookUtils-Android
 [coverage-status-svg]: https://coveralls.io/repos/ParsePlatform/ParseFacebookUtils-Android/badge.svg?branch=master&service=github
 [coverage-status-link]: https://coveralls.io/github/ParsePlatform/ParseFacebookUtils-Android?branch=master
 [bintray-svg]: https://api.bintray.com/packages/parse/maven/ParseFacebookUtils-Android/images/download.svg
 [bintray-link]: https://bintray.com/parse/maven/ParseFacebookUtils-Android/
 [license-svg]: https://img.shields.io/badge/license-BSD-lightgrey.svg
 [license-link]: https://github.com/ParsePlatform/ParseFacebookUtils-Android/blob/master/LICENSE
