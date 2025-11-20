package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.widget.Toast;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SupporterBadgeHelper {

    private static final Set<Long> SUPPORTER_IDS;

    static {
        HashSet<Long> ids = new HashSet<>();
        ids.add(8592165157L);
        ids.add(7107188595L);
        ids.add(2975680882L);
        SUPPORTER_IDS = Collections.unmodifiableSet(ids);
    }

    private SupporterBadgeHelper() {
    }

    public static boolean hasBadge(long dialogId) {
        long normalizedId = dialogId;
        if (dialogId < 0) {
            normalizedId = -dialogId;
        }
        return SUPPORTER_IDS.contains(normalizedId);
    }

    public static SupporterBadgeDrawable newDrawable() {
        return new SupporterBadgeDrawable();
    }

    public static void showInfo(Context context) {
        if (context == null) {
            return;
        }
        Toast.makeText(context.getApplicationContext(), LocaleController.getString(R.string.SupporterBadgeToast), Toast.LENGTH_LONG).show();
    }
}
