package tw.nekomimi.nekogram.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.telegram.ui.AllahCallActivity;

public final class AllahCallUtils {

    private AllahCallUtils() {
    }

    public static void open(Context context) {
        Intent intent = new Intent(context, AllahCallActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }
}
