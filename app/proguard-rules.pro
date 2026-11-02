# ==============================================================================
# FarmVision — The AgriRover ProGuard / R8 Optimization & Obfuscation Rules
# Phase 15: Production Hardening, Release Packaging & Deployment
# ==============================================================================

# --- General Android & Kotlin Rules ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# --- ViewModels & Architecture Components ---
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel

# --- Jetpack Compose Runtime & UI ---
-keep class androidx.compose.material.icons.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class com.farmvision.agrirover.ui.** { *; }

# --- Domain & Data Models (Serialization & Reflection Preservation) ---
# Preserve all data classes, enum classes, and JSON serializers to prevent runtime deserialization failures
-keep class com.farmvision.agrirover.data.model.** {
    <fields>;
    <methods>;
}
-keep enum com.farmvision.agrirover.data.model.** { *; }

-keep class com.farmvision.agrirover.domain.assistant.model.** {
    <fields>;
    <methods>;
}
-keep enum com.farmvision.agrirover.domain.assistant.model.** { *; }

-keep class com.farmvision.agrirover.domain.alert.model.** {
    <fields>;
    <methods>;
}
-keep enum com.farmvision.agrirover.domain.alert.model.** { *; }

-keep class com.farmvision.agrirover.domain.device.model.** {
    <fields>;
    <methods>;
}
-keep enum com.farmvision.agrirover.domain.device.model.** { *; }

-keep class com.farmvision.agrirover.data.device.model.** {
    <fields>;
    <methods>;
}
-keep enum com.farmvision.agrirover.data.device.model.** { *; }

# --- Serializers & Pure Domain Engines ---
-keep class com.farmvision.agrirover.data.assistant.local.AssistantJsonSerializer { *; }
-keep class com.farmvision.agrirover.domain.assistant.context.ContextBuilder { *; }
-keep class com.farmvision.agrirover.domain.analytics.AnalyticsMathEngine { *; }
-keep class com.farmvision.agrirover.domain.alert.engine.** { *; }
-keep class com.farmvision.agrirover.domain.alert.lifecycle.** { *; }

# --- Networking & HTTP Transport Clients ---
-keep class com.farmvision.agrirover.data.network.** { *; }
-keep class com.farmvision.agrirover.data.assistant.provider.** { *; }
-keep class com.farmvision.agrirover.data.device.repository.** { *; }
