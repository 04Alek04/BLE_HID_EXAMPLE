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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.alek.ble_hid_example.databinding.BlocklyFragmentBinding;

public class BlocklyFragment extends Fragment {

    private BlocklyFragmentBinding binding;
    private boolean isRunning = false;

    public static BlocklyFragment newInstance() {
        return new BlocklyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BlocklyFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (binding == null) return;
        
        binding.btnClearBlocks.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(getContext())
                .setTitle("Clear Workspace")
                .setMessage("Are you sure you want to delete all blocks?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (binding != null && binding.blocklyWebview != null) {
                        binding.blocklyWebview.loadUrl("javascript:clearWorkspace()");
                    }
                })
                .setNegativeButton("No", null)
                .show();
        });

        WebSettings settings = binding.blocklyWebview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Prevent opening links in external browser
        binding.blocklyWebview.setWebViewClient(new WebViewClient());
        
        // FIX: Force file refresh (clear cache)
        binding.blocklyWebview.clearCache(true);

        // Blockly interface bridge (Java-JS bridge)
        binding.blocklyWebview.addJavascriptInterface(new BlocklyInterface((MainActivity) getActivity()), "AndroidHID");

        // FIX: Prevent swiping tabs (ViewPager) while working in Blockly
        binding.blocklyWebview.setOnTouchListener((v, event) -> {
            if (binding != null && binding.blocklyWebview != null && binding.blocklyWebview.getParent() != null) {
                binding.blocklyWebview.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });

        // Loading the main Blockly file
        binding.blocklyWebview.loadUrl("file:///android_asset/blockly/index.html");

        binding.btnRunScript.setOnClickListener(v -> {
            if (!isRunning) {
                startScript();
            } else {
                stopScript();
            }
        });

        binding.btnSaveBlocks.setOnClickListener(v -> {
            android.util.Log.i("Blockly", "Java: SAVE button clicked");
            if (binding != null && binding.blocklyWebview != null) {
                binding.blocklyWebview.evaluateJavascript("saveToAndroid()", null);
            }
        });
        binding.btnLoadBlocks.setOnClickListener(v -> {
            android.util.Log.i("Blockly", "Java: LOAD button clicked");
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.openFilePickerForBlockly();
            }
        });
    }

    private void startScript() {
        if (binding == null) return;
        isRunning = true;
        binding.btnRunScript.setText(R.string.blockly_stop);
        binding.btnRunScript.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336));
        binding.blocklyWebview.evaluateJavascript("runCode()", null);
    }

    private void stopScript() {
        if (binding == null) return;
        isRunning = false;
        binding.btnRunScript.setText(R.string.blockly_run);
        binding.btnRunScript.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        binding.blocklyWebview.evaluateJavascript("stopCode()", null);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRunning) {
            stopScript();
        }
    }

    public void onScriptFinished() {
        if (binding == null) return;
        isRunning = false;
        binding.btnRunScript.setText(R.string.blockly_run);
        binding.btnRunScript.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
    }

    public void loadBlocksToWorkspace(String xml) {
        if (binding != null && binding.blocklyWebview != null) {
            android.util.Log.i("Blockly", "Java: Loading blocks to workspace. XML size: " + xml.length());
            String encodedXml = android.util.Base64.encodeToString(xml.getBytes(), android.util.Base64.NO_WRAP);
            binding.blocklyWebview.post(() -> binding.blocklyWebview.evaluateJavascript("loadBlocksFromBase64('" + encodedXml + "')", null));
        } else {
            android.util.Log.e("Blockly", "Java: Cannot load blocks, WebView is null");
        }
    }
}
