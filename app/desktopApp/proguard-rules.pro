# Optional logging, Android, and GraalVM integrations are not present in the Desktop runtime.
-dontwarn android.util.Log
-dontwarn ch.qos.logback.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn kotlin.Deprecated$Container
