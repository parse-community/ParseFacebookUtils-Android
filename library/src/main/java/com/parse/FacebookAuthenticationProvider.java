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
import android.os.Bundle;

import com.facebook.Request;
import com.facebook.Request.Callback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.OpenRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.SharedPreferencesTokenCachingStrategy;
import com.facebook.TokenCachingStrategy;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.ServiceListener;
import com.facebook.android.FacebookError;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;

import bolts.Task;

/* package */ class FacebookAuthenticationProvider implements AuthenticationCallback {

  /**
   * Precise date format required for auth expiration data.
   */
  private final DateFormat preciseDateFormat;
  {
    preciseDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    preciseDateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
  }

  // Used as default activityCode. Copied from Facebook.java.
  public static final int DEFAULT_AUTH_ACTIVITY_CODE = 32665;

  public static final String AUTH_TYPE = "facebook";
  private Facebook facebook;
  private Session session;
  protected String applicationId;
  private int activityCode = DEFAULT_AUTH_ACTIVITY_CODE;
  // Avoid holding a strong reference to the activity (a prime way to end up
  // with memory leaks). The activity needs to be alive anyway in order for
  // authentication to work. The developer needs to set the Activity again in
  // order to re-authenticate.
  private WeakReference<Activity> baseActivity;
  private Context applicationContext;
  private Collection<String> permissions;
  private String userId;

  private Task<Map<String, String>>.TaskCompletionSource currentTcs;

  @SuppressWarnings("deprecation")
  public FacebookAuthenticationProvider(Context context, String applicationId) {
    this.applicationId = applicationId;
    if (context != null) {
      applicationContext = context.getApplicationContext();
    }

    // Initializes the deprecated Facebook object for compatibility.
    if (applicationId != null) {
      this.facebook = new Facebook(applicationId);
    }
  }

  @Deprecated
  public synchronized Task<Map<String, String>> extendAccessToken(Context context) {
    Task<Map<String, String>>.TaskCompletionSource tcs = Task.create();
    if (currentTcs != null) {
      handleCancel();
    }
    currentTcs = tcs;
    boolean result = facebook.extendAccessToken(context, new ServiceListener() {
      @Override
      public void onComplete(Bundle values) {
        handleSuccess(userId);
      }

      @Override
      public void onFacebookError(FacebookError e) {
        handleError(e);
      }

      @Override
      public void onError(Error e) {
        handleError(new ParseException(e));
      }
    });
    if (!result) {
      handleCancel();
    }
    return tcs.getTask();
  }

  public synchronized Task<Map<String, String>> authenticateAsync() {
    Task<Map<String, String>>.TaskCompletionSource tcs = Task.create();
    if (currentTcs != null) {
      handleCancel();
    }
    currentTcs = tcs;
    Activity activity = baseActivity == null ? null : baseActivity.get();
    if (activity == null) {
      throw new IllegalStateException(
          "Activity must be non-null for Facebook authentication to proceed.");
    }
    int activityCode = this.activityCode;
    session = new Session.Builder(activity).setApplicationId(applicationId)
        .setTokenCachingStrategy(new SharedPreferencesTokenCachingStrategy(activity)).build();

    OpenRequest openRequest = new OpenRequest(activity);
    openRequest.setRequestCode(activityCode);
    if (permissions != null) {
      openRequest.setPermissions(new ArrayList<>(permissions));
    }
    openRequest.setCallback(new StatusCallback() {
      @Override
      public void call(Session session, SessionState state, Exception exception) {
        if (state == SessionState.OPENING) {
          return;
        }
        if (state.isOpened()) {
          if (currentTcs == null) {
            return;
          }
          Request meRequest = Request.newGraphPathRequest(session, "me", new Callback() {
            @Override
            public void onCompleted(Response response) {
              if (response.getError() != null) {
                if (response.getError().getException() != null) {
                  handleError(response.getError().getException());
                } else {
                  handleError(new ParseException(ParseException.OTHER_CAUSE,
                      "An error occurred while fetching the Facebook user's identity."));
                }
              } else {
                handleSuccess((String) response.getGraphObject().getProperty("id"));
              }
            }
          });
          meRequest.getParameters().putString("fields", "id");
          meRequest.executeAsync();
        } else if (exception != null) {
          handleError(exception);
        } else {
          handleCancel();
        }
      }
    });
    session.openForRead(openRequest);
    return tcs.getTask();
  }

  /**
   * You must invoke this method in your Activity's onActivityResult() method
   * override.
   */
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Activity activity = baseActivity.get();
    if (activity != null) {
      session.onActivityResult(activity, requestCode, resultCode, data);
    }
  }

  public String getAuthType() {
    return AUTH_TYPE;
  }

  /**
   * Gets the Facebook object which can be used for your own queries after
   * login.
   */
  public Facebook getFacebook() {
    return facebook;
  }

  /**
   * Gets the Facebook Session object for the provider.
   */
  public Session getSession() {
    return session;
  }

  private void handleCancel() {
    if (currentTcs == null)
      return;
    try {
      currentTcs.trySetCancelled();
    } finally {
      currentTcs = null;
    }
  }

  private void handleError(Exception error) {
    if (currentTcs == null)
      return;
    try {
      currentTcs.trySetError(error);
    } finally {
      currentTcs = null;
    }
  }

  public Map<String, String> getAuthData(String id, String accessToken, Date expiration) {
    Map<String, String> authData = new HashMap<>();
    authData.put("id", id);
    authData.put("access_token", accessToken);
    authData.put("expiration_date", preciseDateFormat.format(expiration));
    return authData;
  }

  private void handleSuccess(String userId) {
    if (currentTcs == null) {
      return;
    }

    this.userId = userId;
    Map<String, String> authData = getAuthData(
        userId, session.getAccessToken(), session.getExpirationDate());

    try {
      currentTcs.trySetResult(authData);
    } finally {
      currentTcs = null;
    }
  }

  /**
   * Sets the Activity that will be used to launch the login dialogs/activity.
   */
  public synchronized FacebookAuthenticationProvider setActivity(Activity activity) {
    baseActivity = new WeakReference<>(activity);
    return this;
  }

  /**
   * Sets the activity code used when single sign on for Facebook is enabled.
   * Set this value if you are using an activity that is waiting for other
   * activity results. The activity code will be passed into your
   * onActivityResult() handler, which you should then forward to a
   * finishAuthentication() call to identify when an activity result came from a
   * Facebook login.
   * 
   * @param activityCode
   *          the activity code with which to identify a Facebook login result
   */
  public synchronized FacebookAuthenticationProvider setActivityCode(int activityCode) {
    this.activityCode = activityCode;
    return this;
  }

  public synchronized FacebookAuthenticationProvider setPermissions(Collection<String> permissions) {
    this.permissions = permissions;
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onRestore(Map<String, String> authData) {
    if (authData == null) {
      //TODO closeAndClear
      // Synchronize with the deprecated Facebook object for compatibility.
      if (facebook != null) {
        facebook.setAccessExpires(0);
        facebook.setAccessToken(null);
      }
      session = null;
      return true;
    } else {
      try {
        String accessToken = authData.get("access_token");
        Date expirationDate = preciseDateFormat.parse(authData.get("expiration_date"));

        // Synchronize with the deprecated Facebook object for compatibility.
        if (facebook != null) {
          facebook.setAccessToken(accessToken);
          facebook.setAccessExpires(expirationDate.getTime());
        }
        TokenCachingStrategy tcs = new SharedPreferencesTokenCachingStrategy(
            this.applicationContext);
        Bundle data = tcs.load();
        TokenCachingStrategy.putToken(data, authData.get("access_token"));
        TokenCachingStrategy.putExpirationDate(data, expirationDate);
        tcs.save(data);

        // Open the session from the newly-updated cache.
        Session newSession = new Session.Builder(applicationContext)
            .setApplicationId(applicationId).setTokenCachingStrategy(tcs).build();
        if (newSession.getState() == SessionState.CREATED_TOKEN_LOADED) {
          newSession.openForRead(null);
          session = newSession;
          Session.setActiveSession(session);
        } else {
          session = null;
        }
        return true;
      } catch (Exception e) {
        return false;
      }
    }
  }

  public String getUserId() {
    return this.userId;
  }
}
