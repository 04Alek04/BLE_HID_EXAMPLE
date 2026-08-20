package com.example.alek.ble_hid_example;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BlocklyFragment extends Fragment {

    private WebView webView;
    private Button btnRun;
    private ToggleButton toggleBackground;
    private boolean isRunning = false;

    public static BlocklyFragment newInstance() {
        return new BlocklyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.blockly_fragment, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        webView = view.findViewById(R.id.blockly_webview);
        btnRun = view.findViewById(R.id.btn_run_script);
        ImageButton btnClear = view.findViewById(R.id.btn_clear_blocks);
        ImageButton btnSave = view.findViewById(R.id.btn_save_blocks);
        ImageButton btnLoad = view.findViewById(R.id.btn_load_blocks);
        
        // ... (settings remain the same)
        
        btnClear.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(getContext())
                .setTitle("Clear Workspace")
                .setMessage("Are you sure you want to delete all blocks?")
                .setPositiveButton("Yes", (dialog, which) -> webView.loadUrl("javascript:clearWorkspace()"))
                .setNegativeButton("No", null)
                .show();
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Prevent opening links in external browser
        webView.setWebViewClient(new WebViewClient());
        
        // FIX: Force file refresh (clear cache)
        webView.clearCache(true);

        // Blockly interface bridge (Java-JS bridge)
        webView.addJavascriptInterface(new BlocklyInterface((MainActivity) getActivity()), "AndroidHID");

        // FIX: Prevent swiping tabs (ViewPager) while working in Blockly
        webView.setOnTouchListener((v, event) -> {
            if (v.getId() == R.id.blockly_webview) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });

        // Loading the main Blockly file
        webView.loadUrl("file:///android_asset/blockly/index.html");

        btnRun.setOnClickListener(v -> {
            if (!isRunning) {
                startScript();
            } else {
                stopScript();
            }
        });

        btnSave.setOnClickListener(v -> {
            android.util.Log.i("Blockly", "Java: SAVE button clicked");
            webView.evaluateJavascript("saveToAndroid()", null);
        });
        btnLoad.setOnClickListener(v -> {
            android.util.Log.i("Blockly", "Java: LOAD button clicked");
            ((MainActivity)getActivity()).openFilePickerForBlockly();
        });
    }

    private void startScript() {
        isRunning = true;
        btnRun.setText(R.string.blockly_stop);
        btnRun.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336));
        webView.evaluateJavascript("runCode()", null);
    }

    private void stopScript() {
        isRunning = false;
        btnRun.setText(R.string.blockly_run);
        btnRun.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        webView.evaluateJavascript("stopCode()", null);
    }

    public void onScriptFinished() {
        isRunning = false;
        btnRun.setText(R.string.blockly_run);
        btnRun.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
    }

    public void loadBlocksToWorkspace(String xml) {
        if (webView != null) {
            android.util.Log.i("Blockly", "Java: Loading blocks to workspace. XML size: " + xml.length());
            String encodedXml = android.util.Base64.encodeToString(xml.getBytes(), android.util.Base64.NO_WRAP);
            webView.post(() -> webView.evaluateJavascript("loadBlocksFromBase64('" + encodedXml + "')", null));
        } else {
            android.util.Log.e("Blockly", "Java: Cannot load blocks, WebView is null");
        }
    }
}
