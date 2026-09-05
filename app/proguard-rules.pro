# MapMe keeps no rules it cannot justify.
#
# The app is Compose end to end with no reflection, no serialization and no
# JNI, so R8's defaults plus the AndroidX consumer rules are enough. When a
# rule does become necessary, write down here *why* — an unexplained -keep is
# how a release build quietly stops shrinking.

# Line numbers survive minification so a crash report is worth reading.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
