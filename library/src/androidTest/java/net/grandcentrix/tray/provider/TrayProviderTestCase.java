/*
 * Copyright (C) 2015 grandcentrix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.grandcentrix.tray.provider;

import net.grandcentrix.tray.BuildConfig;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.test.IsolatedContext;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import java.util.HashMap;

/**
 * Created by pascalwelsch on 11/21/14.
 */
public abstract class TrayProviderTestCase extends ProviderTestCase2<TrayProvider> {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static class TrayIsolatedContext extends IsolatedContext {

        private final Context mTargetContext;

        IsolatedContext innerContext = new IsolatedContext(getContentResolver(), this);

        boolean mHasMockResolver = false;

        private HashMap<String, ContentProvider> mProviders = new HashMap<>();

        public TrayIsolatedContext(final ContentResolver resolver, final Context targetContext) {
            super(resolver, targetContext);

            mTargetContext = targetContext;
        }

        public void addProvider(String name, ContentProvider provider) {
            mProviders.put(name, provider);
        }

        public void enableMockResolver(final boolean enabled) {
            mHasMockResolver = enabled;
        }

        @Override
        public Context getApplicationContext() {
            return innerContext;
        }

        @Override
        public ContentResolver getContentResolver() {
            if (isHasMockResolver()) {
                return getMockResolver();
            } else {
                return super.getContentResolver();
            }
        }

        public ContentResolver getMockResolver() {
            final MockContentResolver mockContentResolver = new MockContentResolver(mTargetContext);
            for (String authority : mProviders.keySet()) {
                mockContentResolver.addProvider(authority, mProviders.get(authority));
            }
            return mockContentResolver;
        }

        @Override
        public SharedPreferences getSharedPreferences(final String name, final int mode) {
            return super.getSharedPreferences(name, mode);
        }

        @Override
        public String getPackageName() {
            return "package.test";
        }

        public boolean isHasMockResolver() {
            return mHasMockResolver;
        }
    }

    public static final String AUTHORITY = "net.grandcentrix.tray.test";

    private TrayIsolatedContext mIsolatedContext;

    public TrayProviderTestCase() {
        super(TrayProvider.class, AUTHORITY);
        TrayProvider.setAuthority(AUTHORITY);
    }

    /**
     * checks the database size by querying the given {@param contentUri}
     *
     * @param expectedSize the number of items you expect
     */
    protected void assertDatabaseSize(final long expectedSize) {
        assertDatabaseSize(TrayProvider.CONTENT_URI, expectedSize, true);
    }

    /**
     * checks the database size by querying the given {@param contenUri}
     *
     * @param contentUri   uri to query
     * @param expectedSize the number of items you expect
     * @param closeCursor  should the returned cursor be closed?
     */
    protected Cursor assertDatabaseSize(final Uri contentUri, final long expectedSize,
            final boolean closeCursor) {
        Cursor cursor = getMockContentResolver().query(contentUri, null, null, null, null);
        // Move to first, or the cursor count is always 0
        cursor.moveToFirst();
        assertEquals(expectedSize, cursor.getCount());

        if (closeCursor) {
            cursor.close();
        }

        return cursor;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache",
                "/data/data/" + BuildConfig.APPLICATION_ID + ".test/cache");
        getMockContentResolver().delete(TrayProvider.CONTENT_URI, null, null);
        getMockContentResolver().delete(TrayProvider.CONTENT_URI_INTERNAL, null, null);

        assertDatabaseSize(0);
        assertDatabaseSize(TrayProvider.CONTENT_URI_INTERNAL, 0, true);

        mIsolatedContext = new TrayIsolatedContext(getMockContext().getContentResolver(),
                getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getMockContentResolver().delete(TrayProvider.CONTENT_URI, null, null);
    }

    public TrayIsolatedContext getProviderMockContext() {
        return mIsolatedContext;
    }
}
