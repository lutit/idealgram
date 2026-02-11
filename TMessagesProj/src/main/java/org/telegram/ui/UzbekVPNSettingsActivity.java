package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.Log;
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
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class UzbekVPNSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    
    private ArrayList<UzbekProxyInfo> mtprotoProxies = new ArrayList<>();
    private ArrayList<UzbekProxyInfo> socks5Proxies = new ArrayList<>();
    
    private int rowCount;
    private int headerRow;
    private int mtprotoHeaderRow;
    private int mtprotoStartRow;
    private int mtprotoEndRow;
    private int socks5HeaderRow;
    private int socks5StartRow;
    private int socks5EndRow;
    private int bottomSectionRow;

    @Override
    public boolean onFragmentCreate() {
        Log.d("UzbekVPN", "UzbekVPNSettingsActivity onFragmentCreate");
        super.onFragmentCreate();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyCheckDone);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
        
        UzbekVPNController.getInstance().checkProxies();
        updateRows(true);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        Log.d("UzbekVPN", "UzbekVPNSettingsActivity onFragmentDestroy");
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxyCheckDone);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didUpdateConnectionState);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.proxySettingsChanged || id == NotificationCenter.proxyCheckDone || id == NotificationCenter.didUpdateConnectionState) {
            updateRows(id == NotificationCenter.proxySettingsChanged);
        }
    }
    
    private void updateRows(boolean updateList) {
        if (updateList) {
            ArrayList<UzbekProxyInfo> allProxies = UzbekVPNController.getInstance().getProxies();
            mtprotoProxies.clear();
            socks5Proxies.clear();
            
            for (UzbekProxyInfo info : allProxies) {
                if ("socks5".equals(info.type)) {
                    socks5Proxies.add(info);
                } else {
                    mtprotoProxies.add(info);
                }
            }
            
            Comparator<UzbekProxyInfo> comparator = (o1, o2) -> {
                long bias1 = SharedConfig.currentProxy == o1 ? -200000 : 0;
                if (!o1.available) bias1 += 100000;
                long bias2 = SharedConfig.currentProxy == o2 ? -200000 : 0;
                if (!o2.available) bias2 += 100000;
                return Long.compare(o1.ping + bias1, o2.ping + bias2);
            };
            
            Collections.sort(mtprotoProxies, comparator);
            Collections.sort(socks5Proxies, comparator);
        }
        
        rowCount = 0;
        headerRow = rowCount++;
        
        mtprotoHeaderRow = -1;
        mtprotoStartRow = -1;
        mtprotoEndRow = -1;
        socks5HeaderRow = -1;
        socks5StartRow = -1;
        socks5EndRow = -1;
        
        if (!mtprotoProxies.isEmpty()) {
            mtprotoHeaderRow = rowCount++;
            mtprotoStartRow = rowCount;
            rowCount += mtprotoProxies.size();
            mtprotoEndRow = rowCount;
        }
        
        if (!socks5Proxies.isEmpty()) {
            socks5HeaderRow = rowCount++;
            socks5StartRow = rowCount;
            rowCount += socks5Proxies.size();
            socks5EndRow = rowCount;
        }
        
        bottomSectionRow = rowCount++;
        
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        Log.d("UzbekVPN", "UzbekVPNSettingsActivity createView");
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("UzbekVPN");
        
        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_other);
        item.addSubItem(1, R.drawable.baseline_sync_white_24, "Update Servers");
        item.addSubItem(2, R.drawable.msg_check, "Check Ping");
        
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    UzbekVPNController.getInstance().forceFetch();
                } else if (id == 2) {
                    UzbekVPNController.getInstance().checkProxies();
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
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            UzbekProxyInfo info = null;
            if (position >= mtprotoStartRow && position < mtprotoEndRow) {
                info = mtprotoProxies.get(position - mtprotoStartRow);
            } else if (position >= socks5StartRow && position < socks5EndRow) {
                info = socks5Proxies.get(position - socks5StartRow);
            }
            
            if (info != null) {
                Log.d("UzbekVPN", "Selected proxy: " + info.address);
                UzbekVPNController.getInstance().enableProxy(info);
                updateRows(false);
            }
        });

        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 0; // VIEW_TYPE_PROXY
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerRow) {
                return 3; // Top Header
            } else if (position == mtprotoHeaderRow || position == socks5HeaderRow) {
                return 1; // Header
            } else if (position == bottomSectionRow) {
                return 2; // Shadow
            }
            return 0; // Proxy
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 3:
                    view = new UzbekHeaderCell(mContext);
                    break;
                case 1:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new ShadowSectionCell(mContext);
                    break;
                default:
                    view = new UzbekProxyCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int type = holder.getItemViewType();
            if (type == 3) {
                UzbekHeaderCell cell = (UzbekHeaderCell) holder.itemView;
                cell.update();
            } else if (type == 1) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                if (position == mtprotoHeaderRow) {
                    cell.setText("MTProto Proxies");
                } else if (position == socks5HeaderRow) {
                    cell.setText("SOCKS5 Proxies");
                }
            } else if (type == 0) {
                UzbekProxyCell cell = (UzbekProxyCell) holder.itemView;
                UzbekProxyInfo info = null;
                if (position >= mtprotoStartRow && position < mtprotoEndRow) {
                    info = mtprotoProxies.get(position - mtprotoStartRow);
                } else if (position >= socks5StartRow && position < socks5EndRow) {
                    info = socks5Proxies.get(position - socks5StartRow);
                }
                if (info != null) {
                    cell.setProxy(info);
                }
            }
        }
    }

    public class UzbekHeaderCell extends FrameLayout {
        private ImageView iconView;
        private TextView titleView;
        private TextView subtitleView;
        private TextView statusView;

        public UzbekHeaderCell(Context context) {
            super(context);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER_HORIZONTAL);
            addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

            iconView = new ImageView(context);
            iconView.setImageResource(R.drawable.msg_policy);
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

            subtitleView = new TextView(context);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subtitleView.setGravity(Gravity.CENTER);
            container.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 24));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }

        public void update() {
             boolean enabled = SharedConfig.isProxyEnabled();
             SharedConfig.ProxyInfo current = SharedConfig.currentProxy;
             
             if (enabled && current != null) {
                 int state = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
                 if (state == ConnectionsManager.ConnectionStateConnected) {
                     statusView.setText(LocaleController.getString(R.string.Connected));
                     statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText));
                     iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText), PorterDuff.Mode.MULTIPLY));
                     subtitleView.setText(current.address + ":" + current.port);
                 } else if (state == ConnectionsManager.ConnectionStateUpdating || state == ConnectionsManager.ConnectionStateConnecting) {
                     statusView.setText(LocaleController.getString(R.string.Connecting));
                     statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                     iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), PorterDuff.Mode.MULTIPLY));
                     subtitleView.setText(current.address + ":" + current.port);
                 } else {
                     statusView.setText(LocaleController.getString(R.string.WaitingForNetwork));
                     statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                     iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.MULTIPLY));
                     subtitleView.setText("Check your internet connection");
                 }
             } else {
                 statusView.setText("Not Connected");
                 statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                 iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.MULTIPLY));
                 subtitleView.setText("Select a proxy to connect");
             }
        }
    }

    public class UzbekProxyCell extends FrameLayout {
        private TextView flagTextView;
        private TextView countryTextView;
        private TextView detailTextView;
        private ImageView checkImageView;
        private View pingDot;
        private Paint pingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public UzbekProxyCell(Context context) {
            super(context);
            
            flagTextView = new TextView(context);
            flagTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
            addView(flagTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 22, 0, 0, 0));

            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 72, 0, 60, 0));

            countryTextView = new TextView(context);
            countryTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            countryTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            countryTextView.setTypeface(AndroidUtilities.bold());
            textLayout.addView(countryTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            detailTextView = new TextView(context);
            detailTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            detailTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            textLayout.addView(detailTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

            checkImageView = new ImageView(context);
            checkImageView.setImageResource(R.drawable.proxy_check);
            checkImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addedIcon), PorterDuff.Mode.MULTIPLY));
            checkImageView.setVisibility(View.GONE);
            addView(checkImageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 22, 0));
            
            pingDot = new View(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    canvas.drawCircle(getMeasuredWidth() / 2, getMeasuredHeight() / 2, AndroidUtilities.dp(4), pingPaint);
                }
            };
            addView(pingDot, LayoutHelper.createFrame(12, 12, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 50, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
        }

        public void setProxy(UzbekProxyInfo info) {
            flagTextView.setText(info.flag != null ? info.flag : "🌐");
            countryTextView.setText(info.country != null && !info.country.isEmpty() ? info.country : "Unknown Region");
            
            boolean isCurrent = SharedConfig.currentProxy == info;
            if (!isCurrent && SharedConfig.currentProxy != null) {
                isCurrent = TextUtils.equals(SharedConfig.currentProxy.address, info.address)
                        && SharedConfig.currentProxy.port == info.port;
            }
            boolean isEnabled = SharedConfig.isProxyEnabled();
            
            if (isCurrent && isEnabled) {
                checkImageView.setVisibility(View.VISIBLE);
                pingDot.setTranslationX(-AndroidUtilities.dp(20));
            } else {
                checkImageView.setVisibility(View.GONE);
                pingDot.setTranslationX(0);
            }
            
            String pingText;
            if (info.checking) {
                 pingText = "Checking...";
                 pingPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            } else if (info.available) {
                 pingText = info.ping + " ms";
                 if (info.ping < 150) {
                     pingPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText));
                 } else if (info.ping < 400) {
                     pingPaint.setColor(0xFFFFD700);
                 } else {
                     pingPaint.setColor(Theme.getColor(Theme.key_text_RedRegular));
                 }
            } else {
                 pingText = "Unavailable";
                 pingPaint.setColor(Theme.getColor(Theme.key_text_RedRegular));
            }
            pingDot.invalidate();
            
            detailTextView.setText(info.address + ":" + info.port + " • " + pingText);
        }
    }
}
