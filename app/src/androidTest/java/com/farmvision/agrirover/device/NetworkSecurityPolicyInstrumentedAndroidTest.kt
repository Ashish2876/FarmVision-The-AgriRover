package com.farmvision.agrirover.device

import android.security.NetworkSecurityPolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test executing directly on Android OS runtime (API 35).
 * Validates platform enforcement of NetworkSecurityPolicy.isCleartextTrafficPermitted(hostname).
 */
@RunWith(AndroidJUnit4::class)
class NetworkSecurityPolicyInstrumentedAndroidTest {

    @Test
    fun isCleartextTrafficPermitted_permitsApprovedLocalEndpoints() {
        val policy = NetworkSecurityPolicy.getInstance()

        assertTrue("Direct ESP32 controller IP must be permitted cleartext", policy.isCleartextTrafficPermitted("192.168.4.1"))
        assertTrue("ESP32-CAM controller IP must be permitted cleartext", policy.isCleartextTrafficPermitted("192.168.4.2"))
        assertTrue("Android emulator loopback host must be permitted cleartext", policy.isCleartextTrafficPermitted("10.0.2.2"))
        assertTrue("Localhost must be permitted cleartext", policy.isCleartextTrafficPermitted("localhost"))
        assertTrue("127.0.0.1 must be permitted cleartext", policy.isCleartextTrafficPermitted("127.0.0.1"))
    }

    @Test
    fun isCleartextTrafficPermitted_rejectsExternalHttpDomains() {
        val policy = NetworkSecurityPolicy.getInstance()

        assertFalse("External cloud API must reject cleartext HTTP", policy.isCleartextTrafficPermitted("api.farmvision.com"))
        assertFalse("Arbitrary external IP must reject cleartext HTTP", policy.isCleartextTrafficPermitted("8.8.8.8"))
        assertFalse("Public internet domain must reject cleartext HTTP", policy.isCleartextTrafficPermitted("example.com"))
    }
}

