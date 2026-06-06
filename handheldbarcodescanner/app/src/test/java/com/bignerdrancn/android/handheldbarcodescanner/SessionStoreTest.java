package com.bignerdrancn.android.handheldbarcodescanner;

import static org.junit.Assert.assertEquals;

import com.bignerdrancn.android.handheldbarcodescanner.data.SessionStore;
import org.junit.Test;

public class SessionStoreTest {
    @Test
    public void normalizeBaseUrlAddsApiSuffix() {
        assertEquals(
                "http://10.0.2.2:18080/api",
                SessionStore.Companion.normalizeBaseUrl("http://10.0.2.2:18080")
        );
    }

    @Test
    public void normalizeBaseUrlKeepsApiSuffix() {
        assertEquals(
                "https://wms.example.com/api",
                SessionStore.Companion.normalizeBaseUrl("https://wms.example.com/api/")
        );
    }

    @Test
    public void normalizeBaseUrlFallsBackWhenBlank() {
        assertEquals(
                SessionStore.DEFAULT_BASE_URL,
                SessionStore.Companion.normalizeBaseUrl("   ")
        );
    }
}
