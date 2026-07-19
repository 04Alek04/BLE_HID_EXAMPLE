package com.example.alek.ble_hid_example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AbsoluteMouseFragment extends Fragment {

    private int tapCount = 0;
    private long lastTapTime = 0;

    public static AbsoluteMouseFragment newInstance() {
        return new AbsoluteMouseFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.absolute_mouse_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();
        final TextView debugText = view.findViewById(R.id.abs_mouse_debug);
        final View root = view.findViewById(R.id.abs_mouse_root);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (activity == null) return false;

                int action = event.getActionMasked();

                // Triple Tap Exit
                if (action == MotionEvent.ACTION_DOWN) {
                    if (activity.pager != null) activity.pager.swipePossible(false);
                    
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
                    if (activity.pager != null) activity.pager.swipePossible(true);
                }

                // Movement logic
                float width = (float) v.getWidth();
                float height = (float) v.getHeight();
                if (width > 0 && height > 0) {
                    // Lock during move
                    if (action == MotionEvent.ACTION_MOVE) {
                        if (activity.pager != null) activity.pager.swipePossible(false);
                    }

                    int absX = Math.round((event.getY() / height) * 32767); 
                    int absY = Math.round((1 - (event.getX() / width)) * 32767);

                    absX = Math.max(0, Math.min(32767, absX));
                    absY = Math.max(0, Math.min(32767, absY));

                    if (debugText != null) {
                        debugText.setText("X: " + absX + " Y: " + absY);
                    }
                    activity.sendAbsMouseReport(activity.getAbsMouseButtons(), absX, absY);
                }

                return true;
            }
        });
    }
}
