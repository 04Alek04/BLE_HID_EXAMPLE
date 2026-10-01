package com.example.alek.ble_hid_example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.alek.ble_hid_example.databinding.AbsoluteMouseFragmentBinding;

public class AbsoluteMouseFragment extends Fragment {

    private AbsoluteMouseFragmentBinding binding;
    private int tapCount = 0;
    private long lastTapTime = 0;

    public static AbsoluteMouseFragment newInstance() {
        return new AbsoluteMouseFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = AbsoluteMouseFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();

        if (binding != null && binding.absMouseTouchSurface != null) {
            binding.absMouseTouchSurface.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (activity == null) return false;

                    int action = event.getActionMasked();

                    // Triple Tap Exit - only works within the frame
                    if (action == MotionEvent.ACTION_DOWN) {
                        activity.swipePossible(false);
                        
                        long now = System.currentTimeMillis();
                        if (now - lastTapTime < 500) tapCount++;
                        else tapCount = 1;
                        lastTapTime = now;
                        if (tapCount == 3) {
                            tapCount = 0;
                            activity.exitAbsoluteMouseMode();
                            return true;
                        }
                    }

                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        activity.swipePossible(true);
                    }

                    // Movement logic
                    float width = (float) v.getWidth();
                    float height = (float) v.getHeight();
                    
                    if (width > 0 && height > 0) {
                        // Coordinates are already relative to 'v' (touchSurface)
                        float x = event.getX();
                        float y = event.getY();

                        // Lock during move
                        if (action == MotionEvent.ACTION_MOVE) {
                            activity.swipePossible(false);
                        }

                        // Scaling to 0-32767
                        int absX = Math.round((y / height) * 32767); 
                        int absY = Math.round((1 - (x / width)) * 32767);

                        // Clamp to be sure
                        absX = Math.max(0, Math.min(32767, absX));
                        absY = Math.max(0, Math.min(32767, absY));

                        if (binding != null && binding.absMouseDebug != null) {
                            binding.absMouseDebug.setText("X: " + absX + " Y: " + absY);
                        }
                        activity.sendAbsMouseReport(activity.getAbsMouseButtons(), absX, absY);
                    }

                    return true;
                }
            });
        }
    }
}
