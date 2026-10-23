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
                        if (now - lastTapTime < HidConstants.TRIPLE_TAP_WINDOW_MS) tapCount++;
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

                    if (action == MotionEvent.ACTION_MOVE) {
                        activity.swipePossible(false);
                    }

                    android.graphics.Point coords = TouchInputCalculator.calculateAbsoluteCoordinates(
                            event.getX(), event.getY(), (float) v.getWidth(), (float) v.getHeight());

                    if (binding != null && binding.absMouseDebug != null) {
                        binding.absMouseDebug.setText("X: " + coords.x + " Y: " + coords.y);
                    }
                    activity.sendAbsMouseReport(activity.getAbsMouseButtons(), coords.x, coords.y);

                    return true;
                }
            });
        }
    }
}
