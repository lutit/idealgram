package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.UZBEKGRAM_LEGACY);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.UZBEKGRAM_LEGACY;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        IDEALGRAM("IdealgramIcon", R.mipmap.ic_launcher_idealgram, R.mipmap.ic_launcher_idealgram_foreground, R.string.AppIconIdealgram),
        IDEALGRAM_BLUE("IdealgramIconBlue", R.mipmap.ic_launcher_idealgram_blue, R.mipmap.ic_launcher_idealgram_blue_foreground, R.string.AppIconIdealgramBlue),
        IDEALGRAM_RED("IdealgramIconRed", R.mipmap.ic_launcher_idealgram_red, R.mipmap.ic_launcher_idealgram_red_foreground, R.string.AppIconIdealgramRed),
        IDEALGRAM_FILLED_BLACK("IdealgramIconFilledBlack", R.mipmap.ic_launcher_idealgram_filled_black, R.mipmap.ic_launcher_idealgram_filled_black_foreground, R.string.AppIconIdealgramFilledBlack),
        IDEALGRAM_FILLED_WHITE("IdealgramIconFilledWhite", R.mipmap.ic_launcher_idealgram_filled_white, R.mipmap.ic_launcher_idealgram_filled_white_foreground, R.string.AppIconIdealgramFilledWhite),
        IDEALGRAM_UZBEK("IdealgramIconUzbek", R.mipmap.ic_launcher_idealgram_uzbek, R.mipmap.ic_launcher_idealgram_uzbek_foreground, R.string.AppIconIdealgramUzbek),
        UZBEKGRAM("UzbekgramIcon", R.mipmap.ic_launcher_uzbekgram, R.mipmap.ic_launcher_uzbekgram_foreground, R.string.AppIconUzbekgram),
        UZBEKGRAM_LEGACY("UzbekgramLegacyIcon", R.mipmap.ic_launcher_uzbekgram_legacy, R.mipmap.ic_launcher_uzbekgram_legacy_foreground, R.string.AppIconUzbekgramLegacy),
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }

        public boolean isNekoX() {
            return this == UZBEKGRAM_LEGACY;
        }
    }
}
