/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.parse.facebook.test.R;

import bolts.Task;

public class ParseFacebookUtilsUnitTest extends InstrumentationTestCase {

  MockParseUser user;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ParseObject.registerSubclass(MockParseUser.class);
    user = new MockParseUser();

    ParseCorePlugins.getInstance().registerCurrentUserController(
        new MockParseCurrentUserController(user));
  }

  @Override
  protected void tearDown() throws Exception {
    ParseCorePlugins.getInstance().reset();

    user = null;
    super.tearDown();
  }

  // Unit test
  public void testIsLinked() {
    MockParseUser user = new MockParseUser();

    assertFalse(user.calledIsLinked);
    assertTrue(ParseFacebookUtils.isLinked(user));
    assertTrue(user.calledIsLinked);
  }

  // Unit test
  public void testInitializeWithNoParametersReadsFromManifestInfo() {
    Context context = getInstrumentation().getTargetContext();

    String manifestAppId = context.getString(R.string.facebook_app_id);
    ParseFacebookUtils.initialize(context);
    String usedAppId = ParseFacebookUtils.provider.applicationId;
    assertEquals(manifestAppId, usedAppId);
    assertEquals("facebook", user.authType);
  }

  // Unit test
  public void testInitializeWithParameterUsesParameter() {
    Context context = getInstrumentation().getTargetContext();

    String manifestAppId = context.getString(R.string.facebook_app_id);
    String appId = "Random App Id";
    ParseFacebookUtils.initialize(context, appId);
    String usedAppId = ParseFacebookUtils.provider.applicationId;
    assertFalse(usedAppId.equals(manifestAppId));
    assertEquals(usedAppId, appId);
    assertEquals("facebook", user.authType);
  }

  public static class MockParseUser extends ParseUser {

    String authType;
    boolean calledIsLinked = false;

    @Override
    public boolean isLinked(String authType) {
      calledIsLinked = "facebook".equals(authType);
      return calledIsLinked;
    }

    @Override
    /* package */ Task<Void> synchronizeAuthDataAsync(String authType) {
      this.authType = authType;
      return Task.forResult(null);
    }
  }

  // Since we can not user mockito in integration test, we have to create a mock
  // ParseCurrentUserController to make ParseUser.getCurrentUserAsync() and
  // ParseUser.getCurrentSessionTokenAsync() work.
  private static class MockParseCurrentUserController implements ParseCurrentUserController {

    ParseUser user;

    public MockParseCurrentUserController(ParseUser user) {
      this.user = user;
    }

    @Override
    public Task<String> getCurrentSessionTokenAsync() {
      return Task.forResult(null);
    }

    @Override
    public Task<Void> logOutAsync() {
      return null;
    }

    @Override
    public Task<ParseUser> getAsync(boolean shouldAutoCreateUser) {
      return Task.forResult(user);
    }

    @Override
    public Task<Void> setIfNeededAsync(ParseUser user) {
      return null;
    }

    @Override
    public Task<Void> setAsync(ParseUser object) {
      return null;
    }

    @Override
    public Task<ParseUser> getAsync() {
      return getAsync(false);
    }

    @Override
    public Task<Boolean> existsAsync() {
      return null;
    }

    @Override
    public boolean isCurrent(ParseUser object) {
      return false;
    }

    @Override
    public void clearFromMemory() {

    }

    @Override
    public void clearFromDisk() {

    }
  }
}
