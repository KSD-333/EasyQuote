package com.example.quotationcreator.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quotationcreator.R;

public class EasyToast {

    public static void show(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }

    public static void show(Context context, int messageResId) {
        show(context, context.getString(messageResId), Toast.LENGTH_SHORT);
    }

    public static void show(Context context, String message, int duration) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.layout_custom_toast, null);

            TextView text = layout.findViewById(R.id.toast_message);
            text.setText(message);

            Toast toast = new Toast(context.getApplicationContext());
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            // Fallback to standard toast if custom inflation fails
            Toast.makeText(context, message, duration).show();
        }
    }

    public static void success(Context context, String message) {
        // You could add different layouts for success/error/info
        show(context, message);
    }

    public static void error(Context context, String message) {
        show(context, message);
    }
}
