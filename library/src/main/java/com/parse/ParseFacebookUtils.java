/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.facebook.Session;
import com.facebook.Settings;
import com.facebook.android.Facebook;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CancellationException;

import bolts.AggregateException;
import bolts.Continuation;
import bolts.Task;

/**
 * Provides a set of utilities for using Parse with Facebook.
 *
 * To use {@code ParseFacebookUtils}, you must add the Facebook Android SDK to your project.
 */
public final class ParseFacebookUtils {
  /* package */ static FacebookAuthenticationProvider provider;
  private static boolean isInitialized;

  /**
   * @deprecated Please use {@link #getSession()} and related Facebook SDK 3.0+ APIs instead.
   */
  @Deprecated
  public static Facebook getFacebook() {
    if (provider == null) {
      throw new IllegalStateException(
          "You must initialize ParseFacebookUtils before calling getFacebook()");
    }
    return provider.getFacebook();
  }

  /**
   * @return The active Facebook session associated with the logged in ParseUser, or null if there
   *         is none.
   */
  public static Session getSession() {
    if (provider == null) {
      throw new IllegalStateException(
          "You must initialize ParseFacebookUtils before calling getSession()");
    }
    return provider.getSession();
  }

  /**
   * @return {@code true} if the user is linked to a Facebook account.
   */
  public static boolean isLinked(ParseUser user) {
    return user.isLinked(FacebookAuthenticationProvider.AUTH_TYPE);
  }

  /**
   * @deprecated Please use {@link #initialize(Context)} instead.
   */
  @Deprecated
  public static void initialize() {
    initialize(Parse.getApplicationContext());
  }

  /**
   * Initializes Facebook for use with Parse.
   * <p />
   * <b>IMPORTANT:</b> If you choose to enable single sign-on, you must override the
   * {@link Activity#onActivityResult(int, int, android.content.Intent)} method to invoke
   * {@link #finishAuthentication(int, int, Intent)}.
   *
   * @param context The application context.
   * @see #logIn(Collection, Activity, int, LogInCallback)
   * @see #link(ParseUser, Collection, Activity, int, SaveCallback)
   */
  public static void initialize(Context context) {
    String applicationId = null;

    try {
      String packageName = context.getPackageName();
      PackageManager pm = context.getPackageManager();
      ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
      if (info != null && info.metaData != null) {
        applicationId = info.metaData.getString(Settings.APPLICATION_ID_PROPERTY);
      }
    } catch (PackageManager.NameNotFoundException e) {
      // do nothing
    }

    initialize(context, applicationId);
  }

  /**
   * @deprecated Please use {@link #initialize(Context, String)} instead.
   */
  @Deprecated
  public static void initialize(String appId) {
    initialize(Parse.getApplicationContext(), appId);
  }

  /**
   * Initializes Facebook with a custom {@code appId} for use with Parse.
   * <p/>
   * This method is only required if you intend to use a different {@code appId} than is defined by
   * {@code com.facebook.sdk.ApplicationId} in your {@code AndroidManifest.xml}. You may also invoke
   * this method more than once if you need to change the appId.
   * <p />
   * <b>IMPORTANT:</b> If you choose to enable single sign-on, you must override the
   * {@link Activity#onActivityResult(int, int, android.content.Intent)} method to invoke
   * {@link #finishAuthentication(int, int, Intent)}.
   *
   * @param context
   *          The application context.
   * @param appId
   *          The Facebook appId for your application.
   * @see #logIn(Collection, Activity, int, LogInCallback)
   * @see #link(ParseUser, Collection, Activity, int, SaveCallback)
   */
  public static void initialize(Context context, String appId) {
    provider = new FacebookAuthenticationProvider(context, appId);
    ParseUser.registerAuthenticationCallback(FacebookAuthenticationProvider.AUTH_TYPE, provider);
    isInitialized = true;
  }

  private static void checkInitialization() {
    if (!isInitialized) {
      throw new IllegalStateException(
          "You must call ParseFacebookUtils.initialize() before using ParseFacebookUtils");
    }
  }

  /**
   * Unlinks a user from a Facebook account. Unlinking a user will save the user's data.
   *
   * @param user
   *          The user to unlink from Facebook.
   */
  public static void unlink(ParseUser user) throws ParseException {
    wait(unlinkInBackground(user));
  }

  /**
   * Unlinks a user from a Facebook account in the background. Unlinking a user will save the user's
   * data.
   *
   * @param user
   *          The user to unlink from Facebook.
   * @return A Task that will be resolved when unlinking has completed.
   */
  public static Task<Void> unlinkInBackground(ParseUser user) {
    checkInitialization();
    return user.unlinkFromInBackground(provider.getAuthType());
  }

  /**
   * Unlinks a user from a Facebook account in the background. Unlinking a user will save the user's
   * data.
   *
   * @param user
   *          The user to unlink from a Facebook account.
   * @param callback
   *          Callback for notifying when unlinking is complete.
   */
  public static void unlinkInBackground(ParseUser user, SaveCallback callback) {
    callbackOnMainThreadAsync(unlinkInBackground(user), callback, false);
  }

  /**
   * Links a ParseUser to a Facebook account, allowing you to use Facebook for authentication, and
   * providing access to Facebook data for the user. This method allows you to handle getting access
   * tokens for the user yourself, rather than delegating to the Facebook SDK.
   *
   * @param user
   *          The user to link to a Facebook account.
   * @param facebookId
   *          The facebook ID of the user being linked.
   * @param accessToken
   *          The access token for the user.
   * @param expirationDate
   *          The expiration date of the access token.
   * @return A task that will be resolved when linking is completed.
   */
  public static Task<Void> linkInBackground(ParseUser user, String facebookId, String accessToken,
      Date expirationDate) {
    checkInitialization();
    return user.linkWithInBackground(
        provider.getAuthType(), provider.getAuthData(facebookId, accessToken, expirationDate));
  }

  /**
   * @deprecated Please use {@link #linkInBackground(ParseUser, String, String, java.util.Date)}
   * instead.
   */
  @Deprecated
  public static void link(ParseUser user, String facebookId, String accessToken, Date expirationDate) {
    linkInBackground(user, facebookId, accessToken, expirationDate);
  }

  /**
   * Links a ParseUser to a Facebook account, allowing you to use Facebook for authentication, and
   * providing access to Facebook data for the user. This method allows you to handle getting access
   * tokens for the user yourself, rather than delegating to the Facebook SDK.
   * 
   * @param user
   *          The user to link to a Facebook account.
   * @param facebookId
   *          The facebook ID of the user being linked.
   * @param accessToken
   *          The access token for the user.
   * @param expirationDate
   *          The expiration date of the access token.
   * @param callback
   *          Callback for notifying when the new authentication data has been saved to the user.
   */
  public static void link(ParseUser user, String facebookId, String accessToken,
      Date expirationDate, SaveCallback callback) {
    callbackOnMainThreadAsync(
        linkInBackground(user, facebookId, accessToken, expirationDate),
        callback,
        false
    );
  }

  /**
   * Links a ParseUser to a Facebook account, allowing you to use Facebook for authentication, and
   * providing access to Facebook data for the user. This method delegates to the Facebook SDK's
   * {@code authenticate()} method.
   * <p />
   * <b>IMPORTANT:</b> Note that single sign-on authentication will not function correctly if you do
   * not include a call to the {@code finishAuthentication()} method in your onActivityResult()
   * function! Please see below for more information.
   * <p />
   * From the Facebook SDK documentation:
   * <p />
   * Starts either an Activity or a dialog which prompts the user to log in to Facebook and grant
   * the requested permissions to the given application.
   * <p />
   * This method will, when possible, use Facebook's single sign-on for Android to obtain an access
   * token. This involves proxying a call through the Facebook for Android stand-alone application,
   * which will handle the authentication flow, and return an OAuth access token for making API
   * calls.
   * <p />
   * Because this process will not be available for all users, if single sign-on is not possible,
   * this method will automatically fall back to the OAuth 2.0 User-Agent flow. In this flow, the
   * user credentials are handled by Facebook in an embedded WebView, not by the client application.
   * As such, the dialog makes a network request and renders HTML content rather than a native UI.
   * The access token is retrieved from a redirect to a special URL that the WebView handles.
   *
   * @param user
   *          The user to link to a Facebook account.
   * @param permissions
   *          A list of permissions to be used when logging in. Many of these constants are defined
   *          here: {@link Permissions}.
   * @param activity
   *          The Android activity in which we want to display the authorization dialog.
   * @param activityCode
   *          Single sign-on requires an activity result to be called back to the client application
   *          -- if you are waiting on other activities to return data, pass a custom activity code
   *          here to avoid collisions.
   * @return A Task that will be resolved when linking is completed.
   */
  public static Task<Void> linkInBackground(
      final ParseUser user, Collection<String> permissions, Activity activity, int activityCode) {
    checkInitialization();
    if (permissions == null) {
      permissions = Collections.emptyList();
    }

    return provider.setActivity(activity)
        .setActivityCode(activityCode)
        .setPermissions(permissions)
        .authenticateAsync().onSuccessTask(new Continuation<Map<String, String>, Task<Void>>() {
          @Override
          public Task<Void> then(Task<Map<String, String>> task) throws Exception {
            return user.linkWithInBackground(provider.getAuthType(), task.getResult());
          }
        });
  }

  /**
   * @see #linkInBackground(ParseUser, java.util.Collection, android.app.Activity, int)
   * @see FacebookAuthenticationProvider#DEFAULT_AUTH_ACTIVITY_CODE
   */
  public static Task<Void> linkInBackground(ParseUser user, Collection<String> permissions,
      Activity activity) {
    return linkInBackground(user, permissions, activity,
        FacebookAuthenticationProvider.DEFAULT_AUTH_ACTIVITY_CODE);
  }

  /**
   * @see #linkInBackground(ParseUser, java.util.Collection, android.app.Activity, int)
   */
  public static Task<Void> linkInBackground(ParseUser user, Activity activity, int activityCode) {
    return linkInBackground(user, Collections.<String> emptyList(), activity, activityCode);
  }

  /**
   * @see #linkInBackground(ParseUser, java.util.Collection, android.app.Activity, int)
   * @see FacebookAuthenticationProvider#DEFAULT_AUTH_ACTIVITY_CODE
   */
  public static Task<Void> linkInBackground(ParseUser user, Activity activity) {
    return linkInBackground(user, Collections.<String>emptyList(), activity,
        FacebookAuthenticationProvider.DEFAULT_AUTH_ACTIVITY_CODE);
  }

  /**
   * Links a ParseUser to a Facebook account, allowing you to use Facebook for authentication, and
   * providing access to Facebook data for the user. This method delegates to the Facebook SDK's
   * {@code authenticate()} method.
   * <p />
   * <b>IMPORTANT:</b> Note that single sign-on authentication will not function correctly if you do
   * not include a call to the {@code finishAuthentication()} method in your onActivityResult()
   * function! Please see below for more information.
   * <p />
   * From the Facebook SDK documentation:
   * <p />
   * Starts either an Activity or a dialog which prompts the user to log in to Facebook and grant
   * the requested permissions to the given application.
   * <p />
   * This method will, when possible, use Facebook's single sign-on for Android to obtain an access
   * token. This involves proxying a call through the Facebook for Android stand-alone application,
   * which will handle the authentication flow, and return an OAuth access token for making API
   * calls.
   * <p />
   * Because this process will not be available for all users, if single sign-on is not possible,
   * this method will automatically fall back to the OAuth 2.0 User-Agent flow. In this flow, the
   * user credentials are handled by Facebook in an embedded WebView, not by the client application.
   * As such, the dialog makes a network request and renders HTML content rather than a native UI.
   * The access token is retrieved from a redirect to a special URL that the WebView handles.
   * 
   * @param user
   *          The user to link to a Facebook account.
   * @param permissions
   *          A list of permissions to be used when logging in. Many of these constants are defined
   *          here: {@link Permissions}.
   * @param activity
   *          The Android activity in which we want to display the authorization dialog.
   * @param activityCode
   *          Single sign-on requires an activity result to be called back to the client application
   *          -- if you are waiting on other activities to return data, pass a custom activity code
   *          here to avoid collisions.
   * @param callback
   *          Callback for notifying the calling application when the Facebook authentication has
   *          completed, failed, or been canceled.
   */
  public static void link(ParseUser user, Collection<String> permissions, Activity activity,
      int activityCode, SaveCallback callback) {
    callbackOnMainThreadAsync(
        linkInBackground(user, permissions, activity, activityCode),
        callback,
        true
    );
  }

  /**
   * Links a user using the default activity code if single sign-on is enabled.
   * 
   * @see #link(ParseUser, Collection, Activity, int, SaveCallback)
   */
  public static void link(ParseUser user, Collection<String> permissions, Activity activity,
      SaveCallback callback) {
    link(user, permissions, activity, FacebookAuthenticationProvider.DEFAULT_AUTH_ACTIVITY_CODE,
        callback);
  }

  /**
   * @deprecated Please use {@link #linkInBackground(ParseUser, java.util.Collection,
   * android.app.Activity, int)} instead.
   */
  @Deprecated
  public static void link(ParseUser user, Collection<String> permissions, Activity activity,
      int activityCode) {
    linkInBackground(user, permissions, activity, activityCode);
  }

  /**
   * @deprecated Please use {@link #linkInBackground(ParseUser, java.util.Collection,
   * android.app.Activity)} instead.
   */
  @Deprecated
  public static void link(ParseUser user, Collection<String> permissions, Activity activity) {
    linkInBackground(user, permissions, activity);
  }

  /**
   * @see #link(ParseUser, Collection, Activity, int, SaveCallback)
   */
  public static void link(ParseUser user, Activity activity, int activityCode, SaveCallback callback) {
    link(user, Collections.<String> emptyList(), activity, activityCode, callback);
  }

  /**
   * @see #link(ParseUser, Collection, Activity, int, SaveCallback)
   */
  public static void link(ParseUser user, Activity activity, SaveCallback callback) {
    link(user, Collections.<String>emptyList(), activity,
        FacebookAuthenticationProvider.DEFAULT_AUTH_ACTIVITY_CODE, callback);
  }

  /**
   * @deprecated Please use {@link #linkInBackground(ParseUser, android.app.Activity, int)} instead.
   */
  @Deprecated
  public static void link(ParseUser user, Activity activity, int activityCode) {
    linkInBackground(user, activity, activityCode);
  }

  /**
   * @deprecated Please use {@link #linkInBackground(ParseUser, android.app.Activity)} instead.
   */
  @Deprecated
  public static void link(ParseUser user, Activity activity) {
    linkInBackground(user, activity);
  }

  /**
   * Logs in a ParseUser using Facebook for authentication. If a user for the given Facebook
   * credentials does not already exist, a new user will be created. This method allows you to
   * handle getting access tokens for the user yourself, rather than delegating to the Facebook SDK.
   *
   * @param facebookId
   *          The facebook ID of the user being linked.
   * @param accessToken
   *          The access token for the user.
   * @param expirationDate
   *          The expiration date of the access token.
   * @return A Task that will be resolved when logging in is completed.
   */
  public static Task<ParseUser> logInInBackground(String facebookId, String accessToken,
      Date expirationDate) {
    checkInitialization();
    return ParseUser.logInWithInBackground(
        provider.getAuthType(), provider.getAuthData(facebookId, accessToken, expirationDate));
  }

  /**
   * Logs in a ParseUser using Facebook for authentication. If a user for the given Facebook
   * credentials does not already exist, a new user will be created. This method allows you to
   * handle getting access tokens for the user yourself, rather than delegating to the Facebook SDK.
   * 
   * @param facebookId
   *          The facebook ID of the user being linked.
   * @param accessToken
   *          The access token for the user.
   * @param expirationDate
   *          The expiration date of the access token.
   * @param callback
   *          Callback for notifying when the new authentication data has been saved to the user.
   */
  public static void logIn(String facebookId, String accessToken, Date expirationDate,
      LogInCallback callback) {
    callbackOnMainThreadAsync(
        logInInBackground(facebookId, accessToken, expirationDate),
        callback,
        false
    );
  }

  /**
   * Logs in a ParseUser using Facebook for authentication. If a user for the given Facebook
   * credentials does not already exist, a new user will be created. This method delegates to the
   * Facebook SDK's {@code authenticate()} method.
   * <p />
   * <b>IMPORTANT:</b> Note that single sign-on authentication will not function correctly if you do
   * not include a call to the {@code finishAuthentication()} method in your onActivityResult()
   * function! Please see below for more information.
   * <p />
   * From the Facebook SDK documentation:
   * <p />
   * Starts either an Activity or a dialog which prompts the user to log in to Facebook and grant
   * the requested permissions to the given application.
   * <p />
   * This method will, when possible, use Facebook's single sign-on for Android to obtain an access
   * token. This involves proxying a call through the Facebook for Android stand-alone application,
   * which will handle the authentication flow, and return an OAuth access token for making API
   * calls.
   * <p />
   * Because this process will not be available for all users, if single sign-on is not possible,
   * this method will automatically fall back to the OAuth 2.0 User-Agent flow. In this flow, the
   * user credentials are handled by Facebook in an embedded WebView, not by the client application.
   * As such, the dialog makes a network request and renders HTML content rather than a native UI.
   * The access token is retrieved from a redirect to a special URL that the WebView handles.
   *
   * @param permissions
   *          A list of permissions to be used when logging in. Many of these constants are defined
   *          here: {@link Permissions}.
   * @param activity
   *          The Android activity in which we want to display the authorization dialog.
   * @param activityCode
   *          Single sign-on requires an activity result to be called back to the client application
   *          -- if you are waiting on other activities to return data, pass a custom activity code
   *          here to avoid collisions.
   * @return A Task that will be resolved when logging in is completed.
   */
  public static Task<ParseUser> logInInBackground(Collection<String> permissions, Activity activity,
      int activityCode) {
    checkInitialization();
    if (permissions == null) {
      permissions = Collections.emptyList();
    }

    return provider.setActivity(activity)
        .setActivityCode(activityCode)
        .setPermissions(permissions)
        .authenticateAsync().onSuccessTask(new Continuation<Map<String, String>, Task<ParseUser>>() {
          @Override
          public Task<ParseUser> then(Task<Map<String, String>> task) throws Exception {
            return ParseUser.logInWithInBackground(provider.getAuthType(), task.getResult());
          }
        });
  }

  /**
   * Logs in a ParseUser using Facebook for authentication. If a user for the given Facebook
   * credentials does not already exist, a new user will be created. This method delegates to the
   * Facebook SDK's {@code authenticate()} method.
   * <p />
   * <b>IMPORTANT:</b> Note that single sign-on authentication will not function correctly if you do
   * not include a call to the {@code finishAuthentication()} method in your onActivityResult()
   * function! Please see below for more information.
   * <p />
   * From the Facebook SDK documentation:
   * <p />
   * Starts either an Activity or a dialog which prompts the user to log in to Facebook and grant
   * the requested permissions to the given application.
   * <p />
   * This method will, when possible, use Facebook's single sign-on for Android to obtain an access
   * token. This involves proxying a call through the Facebook for Android stand-alone application,
   * which will handle the authentication flow, and return an OAuth access token for making API
   * calls.
   * <p />
   * Because this process will not be available for all users, if single sign-on is not possible,
   * this method will automatically fall back to the OAuth 2.0 User-Agent flow. In this flow, the
   * user credentials are handled by Facebook in an embedded WebView, not by the client application.
   * As such, the dialog makes a network request and renders HTML content rather than a native UI.
   * The access token is retrieved from a redirect to a special URL that the WebView handles.
   * 
   * @param permissions
   *          A list of permissions to be used when logging in. Many of these constants are defined
   *          here: {@link Permissions}.
   * @param activity
   *          The Android activity in which we want to display the authorization dialog.
   * @param activityCode
   *          Single sign-on requires an activity result to be called back to the client application
   *          -- if you are waiting on other activities to return data, pass a custom activity code
   *          here to avoid collisions.
   * @param callback
   *          Callback for notifying the calling application when the Facebook authentication has
   *          completed, failed, or been canceled.
   */
  public static void logIn(Collection<String> permissions, Activity activity, int activityCode,
      LogInCallback callback) {
    callbackOnMainThreadAsync(
        logInInBackground(permissions, activity, activityCode),
        callback,
        true
    );
  }

  /**
   * @see #logIn(Collection, Activity, int, LogInCallback)
   */
  public static void logIn(Activity activity, int activityCode, LogInCallback callback) {
    callbackOnMainThreadAsync(
        logInInBackground(Collections.<String>emptyList(), activity, activityCode),
        callback,
        true
    );
  }

  /**
   * Logs in a user using the default activity code if single sign-on is enabled.
   * 
   * @see #logIn(Collection, Activity, int, LogInCallback)
   */
  public static void logIn(Collection<String> permissions, Activity activity, LogInCallback callback) {
    callbackOnMainThreadAsync(
        logInInBackground(permissions,
            activity,
            FacebookAuthenticationProvider.DEFAULT_AUTH_ACTIVITY_CODE),
        callback,
        true
    );
  }

  /**
   * @see #logIn(Collection, Activity, int, LogInCallback)
   */
  public static void logIn(Activity activity, LogInCallback callback) {
    callbackOnMainThreadAsync(
        logInInBackground(Collections.<String>emptyList(),
            activity,
            FacebookAuthenticationProvider.DEFAULT_AUTH_ACTIVITY_CODE),
        callback,
        true
    );
  }

  /**
   * Completes authentication after the Facebook app returns an activity result. <b>IMPORTANT:</b>
   * This method must be invoked at the top of the calling activity's onActivityResult() function or
   * Facebook authentication will not function properly!
   * <p />
   * If your calling activity does not currently implement onActivityResult(), you must implement it
   * and include a call to this method if you intend to use the
   * {@link #logIn(Activity, int, LogInCallback)} or
   * {@link #link(ParseUser, Activity, int, SaveCallback)} methods in {@code ParseFacebookUtilities}
   * . For more information, see http://developer.android.com/reference/android/app/
   * Activity.html#onActivityResult(int, int, android.content.Intent)
   */
  public static void finishAuthentication(int requestCode, int resultCode, Intent data) {
    if (provider != null) {
      provider.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Saves the latest session data to the user. Call this after requesting new read or publish
   * permissions for the user's Facebook session.
   *
   * @param user
   *          The user whose session information should be updated.
   * @return A Task that is resolved when the session save is completed.
   */
  public static Task<Void> saveLatestSessionDataInBackground(ParseUser user) {
    checkInitialization();
    if (!isLinked(user)) {
      throw new IllegalStateException("The user must already be linked to Facebook.");
    }
    Session session = getSession();
    return linkInBackground(user, provider.getUserId(), session.getAccessToken(),
        session.getExpirationDate());
  }

  /**
   * Saves the latest session data to the user. Call this after requesting new read or publish
   * permissions for the user's Facebook session.
   * 
   * @param user
   *          The user whose session information should be updated.
   * @param callback
   *          Callback invoked when the session data has been saved.
   */
  public static void saveLatestSessionData(final ParseUser user, final SaveCallback callback) {
    callbackOnMainThreadAsync(saveLatestSessionDataInBackground(user), callback, false);
  }

  /**
   * @deprecated Please use {@link #saveLatestSessionDataInBackground(ParseUser)} instead.
   */
  @Deprecated
  public static void saveLatestSessionData(final ParseUser user) {
    saveLatestSessionDataInBackground(user);
  }

  /**
   * @deprecated This is now handled automatically by the Facebook SDK.
   */
  @Deprecated
  public static boolean shouldExtendAccessToken(ParseUser user) {
    return user != null && isLinked(user) && getFacebook().shouldExtendAccessToken();
  }

  /**
   * @deprecated This is now handled automatically by the Facebook SDK.
   */
  @Deprecated
  public static void extendAccessToken(final ParseUser user, Context context,
      final SaveCallback callback) {
    checkInitialization();
    Task<Void> task = provider.extendAccessToken(context).onSuccessTask(new Continuation<Map<String, String>, Task<Void>>() {
      @Override
      public Task<Void> then(Task<Map<String, String>> task) throws Exception {
        Map<String, String> authData = task.getResult();
        return user.linkWithInBackground(provider.getAuthType(), authData);
      }
    });
    callbackOnMainThreadAsync(task, callback, true);
  }

  /**
   * @deprecated This is now handled automatically by the Facebook SDK.
   */
  @Deprecated
  public static boolean extendAccessTokenIfNeeded(final ParseUser user, Context context,
      final SaveCallback callback) {
    if (shouldExtendAccessToken(user)) {
      extendAccessToken(user, context, callback);
      return true;
    }
    return false;
  }

  //region TaskUtils

  /**
   * Converts a task execution into a synchronous action.
   */
  //TODO (grantland): Task.cs actually throws an AggregateException if the task was cancelled with
  // TaskCancellationException as an inner exception or an AggregateException with the original
  // exception as an inner exception if task.isFaulted().
  // https://msdn.microsoft.com/en-us/library/dd235635(v=vs.110).aspx
  /* package */ static <T> T wait(Task<T> task) throws ParseException {
    try {
      task.waitForCompletion();
      if (task.isFaulted()) {
        Exception error = task.getError();
        if (error instanceof ParseException) {
          throw (ParseException) error;
        }
        if (error instanceof AggregateException) {
          throw new ParseException(error);
        }
        if (error instanceof RuntimeException) {
          throw (RuntimeException) error;
        }
        throw new RuntimeException(error);
      } else if (task.isCancelled()) {
        throw new RuntimeException(new CancellationException());
      }
      return task.getResult();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Calls the callback after a task completes on the main thread, returning a Task that completes
   * with the same result as the input task after the callback has been run.
   */
  private static <T> Task<T> callbackOnMainThreadAsync(
      Task<T> task, LogInCallback callback, boolean reportCancellation) {
    return callbackOnMainThreadInternalAsync(task, callback, reportCancellation);
  }

  /**
   * Calls the callback after a task completes on the main thread, returning a Task that completes
   * with the same result as the input task after the callback has been run.
   */
  private static <T> Task<T> callbackOnMainThreadAsync(
      Task<T> task, SaveCallback callback, boolean reportCancellation) {
    return callbackOnMainThreadInternalAsync(task, callback, reportCancellation);
  }

  /**
   * Calls the callback after a task completes on the main thread, returning a Task that completes
   * with the same result as the input task after the callback has been run. If reportCancellation
   * is false, the callback will not be called if the task was cancelled.
   */
  private static <T> Task<T> callbackOnMainThreadInternalAsync(
      Task<T> task, final Object callback, final boolean reportCancellation) {
    if (callback == null) {
      return task;
    }
    final Task<T>.TaskCompletionSource tcs = Task.create();
    task.continueWith(new Continuation<T, Void>() {
      @Override
      public Void then(final Task<T> task) throws Exception {
        if (task.isCancelled() && !reportCancellation) {
          tcs.setCancelled();
          return null;
        }
        Task.UI_THREAD_EXECUTOR.execute(new Runnable() {
          @Override
          public void run() {
            try {
              Exception error = task.getError();
              if (error != null && !(error instanceof ParseException)) {
                error = new ParseException(error);
              }
              if (callback instanceof SaveCallback) {
                ((SaveCallback) callback).done((ParseException) error);
              } else if (callback instanceof LogInCallback) {
                ((LogInCallback) callback).done(
                    (ParseUser) task.getResult(), (ParseException) error);
              }
            } finally {
              if (task.isCancelled()) {
                tcs.setCancelled();
              } else if (task.isFaulted()) {
                tcs.setError(task.getError());
              } else {
                tcs.setResult(task.getResult());
              }
            }
          }
        });
        return null;
      }
    });
    return tcs.getTask();
  }

  //endregion

  /**
   * Provides easy access to Facebook permission string constants for use when authenticating with
   * Facebook. The complete list can be found on Facebook's <a
   * href="https://developers.facebook.com/docs/reference/api/permissions/" >developer page for
   * permissions</a>. Please see this page for a description of what the permissions provide access
   * to.
   * 
   * @exclude
   */
  public static final class Permissions {
    private Permissions() {
    }

    /**
     * @exclude
     */
    public static final class User {
      private User() {
      }

      public static final String ABOUT_ME = "user_about_me";
      public static final String ACTIVITIES = "user_activities";
      public static final String BIRTHDAY = "user_birthday";
      public static final String CHECKINS = "user_checkins";
      public static final String EDUCATION_HISTORY = "user_education_history";
      public static final String EVENTS = "user_events";
      public static final String GROUPS = "user_groups";
      public static final String HOMETOWN = "user_hometown";
      public static final String INTERESTS = "user_interests";
      public static final String LIKES = "user_likes";
      public static final String LOCATION = "user_location";
      public static final String NOTES = "user_notes";
      public static final String ONLINE_PRESENCE = "user_online_presence";
      public static final String PHOTOS = "user_photos";
      public static final String QUESTIONS = "user_questions";
      public static final String RELATIONSHIPS = "user_relationships";
      public static final String RELATIONSHIP_DETAILS = "user_relationship_details";
      public static final String RELIGION_POLITICS = "user_religion_politics";
      public static final String STATUS = "user_status";
      public static final String VIDEOS = "user_videos";
      public static final String WEBSITE = "user_website";
      public static final String WORK_HISTORY = "user_work_history";
      public static final String EMAIL = "email";
    }

    /**
     * @exclude
     */
    public static final class Friends {
      private Friends() {
      }

      public static final String ABOUT_ME = "friends_about_me";
      public static final String ACTIVITIES = "friends_activities";
      public static final String BIRTHDAY = "friends_birthday";
      public static final String CHECKINS = "friends_checkins";
      public static final String EDUCATION_HISTORY = "friends_education_history";
      public static final String EVENTS = "friends_events";
      public static final String GROUPS = "friends_groups";
      public static final String HOMETOWN = "friends_hometown";
      public static final String INTERESTS = "friends_interests";
      public static final String LIKES = "friends_likes";
      public static final String LOCATION = "friends_location";
      public static final String NOTES = "friends_notes";
      public static final String ONLINE_PRESENCE = "friends_online_presence";
      public static final String PHOTOS = "friends_photos";
      public static final String QUESTIONS = "friends_questions";
      public static final String RELATIONSHIPS = "friends_relationships";
      public static final String RELATIONSHIP_DETAILS = "friends_relationship_details";
      public static final String RELIGION_POLITICS = "friends_religion_politics";
      public static final String STATUS = "friends_status";
      public static final String VIDEOS = "friends_videos";
      public static final String WEBSITE = "friends_website";
      public static final String WORK_HISTORY = "friends_work_history";
    }

    /**
     * @exclude
     */
    public static final class Extended {
      private Extended() {
      }

      public static final String READ_FRIEND_LISTS = "read_friendlists";
      public static final String READ_INSIGHTS = "read_insights";
      public static final String READ_MAILBOX = "read_mailbox";
      public static final String READ_REQUESTS = "read_requests";
      public static final String READ_STREAM = "read_stream";
      public static final String XMPP_LOGIN = "xmpp_login";
      public static final String ADS_MANAGEMENT = "ads_management";
      public static final String CREATE_EVENT = "create_event";
      public static final String MANAGE_FRIEND_LISTS = "manage_friendlists";
      public static final String MANAGE_NOTIFICATIONS = "manage_notifications";
      public static final String OFFLINE_ACCESS = "offline_access";
      public static final String PUBLISH_CHECKINS = "publish_checkins";
      public static final String PUBLISH_STREAM = "publish_stream";
      public static final String RSVP_EVENT = "rsvp_event";
      public static final String PUBLISH_ACTIONS = "publish_actions";
    }

    /**
     * @exclude
     */
    public static final class Page {
      private Page() {
      }

      public static final String MANAGE_PAGES = "manage_pages";
    }
  }

  private ParseFacebookUtils() {
    // do nothing
  }
}
