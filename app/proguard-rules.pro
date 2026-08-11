-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile
-dontobfuscate
-keepparameternames

# okio references this optional JSR-305 annotation at compile time only.
-dontwarn javax.annotation.Nullable
