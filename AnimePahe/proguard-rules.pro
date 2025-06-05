# Main entry point (do not change/remove unless you know what you're doing)
-keep,allowobfuscation class * extends com.lagradost.cloudstream3.plugins.Plugin {
    <init>(...);
    *;
}

# Add libraries that are reported missing by :compileDex
# they are *probably* available at runtime/in the app


# VVV uncomment to disable obfuscation VVV
#-dontobfuscate

# VVV uncomment to generate list of unused classes VVV
-printusage "build/r8_pruned_classes.txt"

# You may ignore the rest of the file if you don't know what you're doing







# Keep constructors, getters and setters
# needed for json de/serialization unless you target your classes directly
-keepclassmembers class * {
    <init>(...);
    public * get*();
    public boolean is*();
    public void set*(***);
}

# Keep enum types
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Kotlin metadata
-keep,allowobfuscation class * {
    @kotlin.Metadata *;
}

# Keep annotations and Kotlin metadata
-keepattributes *Annotation*,Signature,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,KotlinMetadata,MethodParameters
