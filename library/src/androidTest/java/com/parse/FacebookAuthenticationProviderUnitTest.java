/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

public class FacebookAuthenticationProviderUnitTest extends TestCase {

  private Locale defaultLocale;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    defaultLocale = Locale.getDefault();
  }

  @Override
  protected void tearDown() throws Exception {
    Locale.setDefault(defaultLocale);
    defaultLocale = null;
    super.tearDown();
  }

  public void testCorrectDateFormat() {
    Locale.setDefault(new Locale("ar")); // Mimic the device's locale

    Calendar calendar = new GregorianCalendar(2015, 6, 3);

    FacebookAuthenticationProvider provider = new FacebookAuthenticationProvider(null, null);
    Map<String, String> authData = provider.getAuthData(
        "user_id", "access_token", calendar.getTime());
    String expirationDate = authData.get("expiration_date");
    assertTrue("Invalid expiration_date: " + expirationDate, expirationDate.contains("2015"));
  }
}
