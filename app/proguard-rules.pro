# Keep JavaScript Interface methods for WebView (Blockly Bridge)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.example.alek.ble_hid_example.BlocklyInterface { *; }
