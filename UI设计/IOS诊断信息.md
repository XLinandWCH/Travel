Run echo "===== Xcode 版本 ====="
===== Xcode 版本 =====
Xcode 26.6
Build version 17F113
===== 可用 SDK =====
[
{
"canonicalName" : "driverkit25.5",
"displayName" : "DriverKit 25.5",
"isBaseSdk" : true,
"platform" : "driverkit",
"platformPath" : "/Applications/Xcode_26.6.app/Contents/Developer/Platforms/DriverKit.platform",
"platformVersion" : "25.5",
"sdkPath" : "/Applications/Xcode_26.6.app/Contents/Developer/Platforms/DriverKit.platform/Developer/SDKs/DriverKit25.5.sdk",
"sdkVersion" : "25.5"
},
{
"buildID" : "5DDDAD38-596F-11F1-80E6-834BE4FBF366",
"canonicalName" : "iphoneos26.5",
"displayName" : "iOS 26.5",
"isBaseSdk" : true,
"platform" : "iphoneos",
"platformPath" : "/Applications/Xcode_26.6.app/Contents/Developer/Platforms/iPhoneOS.platform",
"platformVersion" : "26.5",
"productBuildVersion" : "23F81a",
"productCopyright" : "1983-2026 Apple Inc.",
"productName" : "iPhone OS",
"productVersion" : "26.5.1",
"sdkPath" : "/Applications/Xcode_26.6.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS26.5.sdk",
"sdkVersion" : "26.5"
},
{
"buildID" : "5DDDAD38-596F-11F1-80E6-834BE4FBF366",
"canonicalName" : "iphonesimulator26.5",
"displayName" : "Simulator - iOS 26.5",
"isBaseSdk" : true,
"platform" : "iphonesimulator",
"platformPath" : "/Applications/Xcode_26.6.app/Contents/Developer/Platforms/iPhoneSimulator.platform",
"platformVersion" : "26.5",
"productBuildVersion" : "23F81a",
"productCopyright" : "1983-2026 Apple Inc.",
"productName" : "iPhone OS",
"productVersion" : "26.5.1",
"sdkPath" : "/Applications/Xcode_26.6.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator26.5.sdk",
"sdkVersion" : "26.5"
},
{
"buildID" : "68E32B32-42C6-11F1-B58E-42443597F508",
"canonicalName" : "macosx26.5",
"displayName" : "macOS 26.5",
"iOSSupportVersion" : "26.5",
"isBaseSdk" : true,
"platform" : "macosx",
"platformPath" : "/Applications/Xcode_26.6.app/Conte
===== Kotlin/Native 编译错误（Stage 1）=====
e: file:///Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/image_picker/AvatarStorage.ios.kt:81:46 This declaration needs opt-in. Its usage must be marked with '@kotlinx.cinterop.ExperimentalForeignApi' or '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'
e: file:///Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/image_picker/AvatarStorage.ios.kt:83:37 Unresolved reference 'encodeToString'.
e: file:///Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/inputbox/voice/AudioRecorder.ios.kt:70:57 Candidate 'fun String?.orEmpty(): String' is inapplicable because of a receiver type mismatch.
e: file:///Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/inputbox/voice/AudioRecorder.ios.kt:73:16 Return type mismatch: expected 'ByteArray', actual 'String'.
===== Framework 链接错误（Stage 1c）=====
e: file:///Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/image_picker/AvatarStorage.ios.kt:81:46 This declaration needs opt-in. Its usage must be marked with '@kotlinx.cinterop.ExperimentalForeignApi' or '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'
e: file:///Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/image_picker/AvatarStorage.ios.kt:83:37 Unresolved reference 'encodeToString'.
e: file:///Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/inputbox/voice/AudioRecorder.ios.kt:70:57 Candidate 'fun String?.orEmpty(): String' is inapplicable because of a receiver type mismatch.
e: file:///Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/inputbox/voice/AudioRecorder.ios.kt:73:16 Return type mismatch: expected 'ByteArray', actual 'String'.
===== Xcode 构建错误（Stage 3）=====
/Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/image_picker/AvatarStorage.ios.kt:81:46: error: This declaration needs opt-in. Its usage must be marked with '@kotlinx.cinterop.ExperimentalForeignApi' or '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'
/Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/image_picker/AvatarStorage.ios.kt:83:37: error: Unresolved reference 'encodeToString'.
/Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/inputbox/voice/AudioRecorder.ios.kt:70:57: error: Candidate 'fun String?.orEmpty(): String' is inapplicable because of a receiver type mismatch.
/Users/runner/work/Travel/Travel/app/shared/src/iosMain/kotlin/com/xlin/travel/inputbox/voice/AudioRecorder.ios.kt:73:16: error: Return type mismatch: expected 'ByteArray', actual 'String'.
error: Compilation finished with errors
error: Compilation finished with errors
