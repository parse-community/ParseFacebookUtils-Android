# Parse Facebook Utils for Android
[![Build Status][build-status-svg]][build-status-link]
[![Coverage Status][coverage-status-svg]][coverage-status-link]
[![License][license-svg]][license-link]
[![](https://jitpack.io/v/parse-community/ParseFacebookUtils-Android.svg)](https://jitpack.io/#parse-community/ParseFacebookUtils-Android)

A utility library to authenticate `ParseUser`s with the Facebook SDK. For more information, see our [guide][guide].

## Dependency

Add this in your root `build.gradle` file (**not** your module `build.gradle` file):

```gradle
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```

Then, add the library to your project `build.gradle`
```gradle
dependencies {
    implementation 'com.github.parse-community:ParseFacebookUtils-Android:latest.version.here'
}
```

## Usage
Extensive docs can be found in the [guide][guide]. The basic steps are:
```java
// in Application.onCreate(); or somewhere similar
ParseFacebookUtils.initialize(context);
```
Within the activity where your user is going to log in with Facebook, include the following:
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  super.onActivityResult(requestCode, resultCode, data);
  ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
}
```
Then elsewhere, when your user taps the login button:
```java
ParseFacebookUtils.logInWithReadPermissionsInBackground(this, permissions, new LogInCallback() {
  @Override
  public void done(ParseUser user, ParseException err) {
    if (user == null) {
      Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
    } else if (user.isNew()) {
      Log.d("MyApp", "User signed up and logged in through Facebook!");
    } else {
      Log.d("MyApp", "User logged in through Facebook!");
    }
  }
});
```

## How Do I Contribute?
We want to make contributing to this project as easy and transparent as possible. Please refer to the [Contribution Guidelines](https://github.com/parse-community/Parse-SDK-Android/blob/master/CONTRIBUTING.md).

## License
    Copyright (c) 2015-present, Parse, LLC.
    All rights reserved.

    This source code is licensed under the BSD-style license found in the
    LICENSE file in the root directory of this source tree. An additional grant
    of patent rights can be found in the PATENTS file in the same directory.

As of April 5, 2017, Parse, LLC has transferred this code to the parse-community organization, and will no longer be contributing to or distributing this code.

 [guide]: https://docs.parseplatform.org/android/guide/#facebook-users

 [build-status-svg]: https://travis-ci.org/parse-community/ParseFacebookUtils-Android.svg?branch=master
 [build-status-link]: https://travis-ci.org/parse-community/ParseFacebookUtils-Android

 [coverage-status-svg]: https://img.shields.io/codecov/c/github/parse-community/ParseFacebookUtils-Android/master.svg
 [coverage-status-link]: https://coveralls.io/github/parse-community/ParseFacebookUtils-Android?branch=master

 [license-svg]: https://img.shields.io/badge/license-BSD-lightgrey.svg
 [license-link]: https://github.com/parse-community/ParseFacebookUtils-Android/blob/master/LICENSE
