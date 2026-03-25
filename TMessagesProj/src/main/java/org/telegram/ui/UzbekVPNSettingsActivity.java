package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UzbekVPNController;
import org.telegram.messenger.UzbekVPNController.UzbekProxyInfo;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Locale;

public class UzbekVPNSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private TextView connectButton;
    private FrameLayout bottomLayout;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyCheckDone);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
        
        if (UzbekVPNController.getInstance().getProxies().isEmpty()) {
             UzbekVPNController.getInstance().forceFetch();
        } else {
             UzbekVPNController.getInstance().checkProxies();
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxyCheckDone);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didUpdateConnectionState);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.proxySettingsChanged || id == NotificationCenter.didUpdateConnectionState) {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            updateConnectButton();
            if (listView != null) {
                for (int i = 0; i < listView.getChildCount(); i++) {
                    View child = listView.getChildAt(i);
                    if (child instanceof UzbekHeaderCell) {
                        ((UzbekHeaderCell) child).update();
                    }
                }
            }
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("UzbekVPN");
        
        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_other);
        item.addSubItem(1, R.drawable.baseline_sync_white_24, "Update Servers");
        
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    UzbekVPNController.getInstance().forceFetch();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 60));

        bottomLayout = new FrameLayout(context);
        bottomLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        frameLayout.addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 60, Gravity.BOTTOM));

        connectButton = new TextView(context);
        connectButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        connectButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        connectButton.setGravity(Gravity.CENTER);
        connectButton.setTypeface(AndroidUtilities.bold());
        connectButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        bottomLayout.addView(connectButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 10, 10, 10, 10));

        connectButton.setOnClickListener(v -> {
            boolean enabled = SharedConfig.isProxyEnabled();
            if (enabled) {
                UzbekVPNController.getInstance().disableProxy();
            } else {
                connectToBestProxy();
            }
            updateConnectButton();
        });
        
        updateConnectButton();

        listView.setOnItemClickListener((view, position) -> {
            if (position == 1) { // Locations
                presentFragment(new UzbekProxyListActivity("mtproto"));
            } else if (position == 2) { // Uzbek check ad toggle
                UzbekVerificationHelper.setAdsEnabled(!UzbekVerificationHelper.isAdsEnabled());
                if (listAdapter != null) {
                    listAdapter.notifyItemChanged(2);
                }
            }
        });

        return fragmentView;
    }
    
    private void updateConnectButton() {
        if (connectButton == null) return;
        boolean enabled = SharedConfig.isProxyEnabled();
        int state = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
        
        if (enabled) {
            if (state == ConnectionsManager.ConnectionStateConnected) {
                connectButton.setText("Disconnect");
                connectButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_text_RedRegular), Theme.getColor(Theme.key_text_RedBold)));
                connectButton.setEnabled(true);
                connectButton.setAlpha(1.0f);
            } else {
                connectButton.setText("Connecting...");
                connectButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
                connectButton.setEnabled(false);
                connectButton.setAlpha(0.5f);
            }
        } else {
            connectButton.setText("Connect to Best Proxy");
            connectButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
            connectButton.setEnabled(true);
            connectButton.setAlpha(1.0f);
        }
    }
    
    private void connectToBestProxy() {
        ArrayList<UzbekProxyInfo> proxies = UzbekVPNController.getInstance().getProxies();
        if (proxies.isEmpty()) return;
        
        UzbekProxyInfo best = null;
        long bestPing = Long.MAX_VALUE;
        
        for (UzbekProxyInfo info : proxies) {
            if (info.available && info.ping < bestPing) {
                bestPing = info.ping;
                best = info;
            }
        }
        
        if (best == null && !proxies.isEmpty()) {
            best = proxies.get(0); 
        }
        
        if (best != null) {
            UzbekVPNController.getInstance().enableProxy(best);
        }
    }
    
    private String getCurrentCountryName() {
        if (!SharedConfig.isProxyEnabled() || SharedConfig.currentProxy == null) {
            return "None";
        }
        if (SharedConfig.currentProxy instanceof UzbekProxyInfo) {
             String code = ((UzbekProxyInfo) SharedConfig.currentProxy).country;
             if (code == null) return "Unknown";
             try {
                 return new Locale("", code).getDisplayCountry();
             } catch (Exception e) {
                 return code;
             }
        }
        return "Unknown";
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return 4; // Header, Locations, Toggle, Shadow
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int viewType = holder.getItemViewType();
            return viewType == 1 || viewType == 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return 0; // Header
            if (position == 2) return 2; // Toggle
            if (position == 3) return 3; // Shadow
            return 1; // Cell
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new UzbekHeaderCell(mContext);
                    break;
                case 1:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default:
                    view = new ShadowSectionCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                ((UzbekHeaderCell) holder.itemView).update();
            } else if (holder.getItemViewType() == 1) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (position == 1) {
                    cell.setTextAndValue("Locations", getCurrentCountryName(), false);
                }
            } else if (holder.getItemViewType() == 2) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                cell.setTextAndCheck("Показывать проверку узбека в чатах", UzbekVerificationHelper.isAdsEnabled(), false);
            }
        }
    }
    
    public class UzbekHeaderCell extends FrameLayout {
        private ImageView iconView;
        private TextView titleView;
        private TextView subtitleView;
        private TextView statusView;
        private TextView timerTextView;
        
        private Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimer();
                AndroidUtilities.runOnUIThread(this, 1000);
            }
        };

        public UzbekHeaderCell(Context context) {
            super(context);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER_HORIZONTAL);
            addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

            iconView = new ImageView(context);
            iconView.setImageResource(R.drawable.msg_policy);
            iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.MULTIPLY));
            container.addView(iconView, LayoutHelper.createLinear(64, 64, Gravity.CENTER_HORIZONTAL, 0, 24, 0, 12));

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setText("UzbekVPN");
            titleView.setGravity(Gravity.CENTER);
            container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

            statusView = new TextView(context);
            statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            statusView.setGravity(Gravity.CENTER);
            container.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4));
            
            timerTextView = new TextView(context);
            timerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
            timerTextView.setTypeface(AndroidUtilities.bold());
            timerTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            timerTextView.setGravity(Gravity.CENTER);
            timerTextView.setVisibility(GONE);
            container.addView(timerTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 4));

            subtitleView = new TextView(context);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subtitleView.setGravity(Gravity.CENTER);
            container.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 24));
        }
        
        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            update();
        }
        
        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            AndroidUtilities.cancelRunOnUIThread(timerRunnable);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }
        
        private void updateTimer() {
            long startTime = UzbekVPNController.getInstance().connectionStartTime;
            if (startTime > 0) {
                long diff = (System.currentTimeMillis() - startTime) / 1000;
                long hours = diff / 3600;
                long minutes = (diff % 3600) / 60;
                long seconds = diff % 60;
                String time;
                if (hours > 0) {
                    time = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    time = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                }
                timerTextView.setText(time);
            }
        }

        public void update() {
             boolean enabled = SharedConfig.isProxyEnabled();
             SharedConfig.ProxyInfo current = SharedConfig.currentProxy;
             
             AndroidUtilities.cancelRunOnUIThread(timerRunnable);

             if (enabled && current != null) {
                 int state = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
                 if (state == ConnectionsManager.ConnectionStateConnected) {
                     statusView.setText(LocaleController.getString("Connected", R.string.Connected));
                     statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText));
                     iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText), PorterDuff.Mode.MULTIPLY));
                     subtitleView.setText(current.address);
                     
                     timerTextView.setVisibility(VISIBLE);
                     timerRunnable.run();
                 } else if (state == ConnectionsManager.ConnectionStateUpdating || state == ConnectionsManager.ConnectionStateConnecting) {
                     statusView.setText(LocaleController.getString("Connecting", R.string.Connecting));
                     statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                     iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), PorterDuff.Mode.MULTIPLY));
                     subtitleView.setText(current.address);
                     
                     timerTextView.setVisibility(GONE);
                 } else {
                     statusView.setText(LocaleController.getString("WaitingForNetwork", R.string.WaitingForNetwork));
                     statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                     iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.MULTIPLY));
                     subtitleView.setText("Check your internet connection");
                     
                     timerTextView.setVisibility(GONE);
                 }
             } else {
                 statusView.setText("Not Connected");
                 statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                 iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.MULTIPLY));
                 subtitleView.setText("Select a proxy to connect");
                 
                 timerTextView.setVisibility(GONE);
             }
        }
    }
}
