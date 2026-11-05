# Compose, Room, Hilt, Coil and OkHttp ship their own consumer rules.
# Serializable models used for oEmbed parsing and navigation routes are kept by the serialization plugin rules.

# WorkManager creates these by reflection, and Glance widgets never render without them.
-keep class * extends androidx.work.InputMerger { public <init>(); }
-keep class * extends androidx.work.ListenableWorker { public <init>(...); }
