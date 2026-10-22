-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.openxmlformats.**
-keep class org.openxmlformats.** { *; }

-dontwarn com.itextpdf.**
-keep class com.itextpdf.** { *; }

-dontwarn javax.xml.stream.**
-dontwarn org.slf4j.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.commons.compress.**
