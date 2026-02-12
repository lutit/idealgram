package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
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
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

public class UzbekProxyListActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private String proxyType; // "mtproto" or "socks5"
    private String countryCode; // If null, show country list. If set, show proxies for this country.
    
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    
    private ArrayList<UzbekProxyInfo> allProxies = new ArrayList<>();
    private ArrayList<UzbekProxyInfo> displayedProxies = new ArrayList<>(); // Used when countryCode != null
    
    private ArrayList<CountryItem> countryList = new ArrayList<>(); // Used when countryCode == null
    
    private int pageSize = 20;
    
    public static class CountryItem {
        public String code;
        public String name;
        public String flag;
        public int count;
    }
    
    public UzbekProxyListActivity(String type) {
        this(type, null);
    }
    
    public UzbekProxyListActivity(String type, String countryCode) {
        this.proxyType = type;
        this.countryCode = countryCode;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyCheckDone);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
        
        loadData();
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
        if (id == NotificationCenter.proxySettingsChanged) {
            loadData();
        } else if (id == NotificationCenter.proxyCheckDone) {
             if (listAdapter != null && countryCode != null) {
                 listAdapter.notifyDataSetChanged();
             }
        } else if (id == NotificationCenter.didUpdateConnectionState) {
             if (listAdapter != null && countryCode != null) {
                 listAdapter.notifyDataSetChanged();
             }
        }
    }
    
    private void loadData() {
        ArrayList<UzbekProxyInfo> source = UzbekVPNController.getInstance().getProxies();
        
        if (countryCode == null) {
            // Country List Mode
            HashMap<String, CountryItem> countryMap = new HashMap<>();
            
            for (UzbekProxyInfo info : source) {
                if (proxyType.equals(info.type)) {
                    String cc = info.country;
                    if (TextUtils.isEmpty(cc)) cc = "Unknown";
                    
                    CountryItem item = countryMap.get(cc);
                    if (item == null) {
                        item = new CountryItem();
                        item.code = cc;
                        item.flag = info.flag;
                        if ("Unknown".equals(cc)) {
                            item.name = "Unknown Region";
                        } else {
                            try {
                                item.name = new Locale("", cc).getDisplayCountry();
                            } catch (Exception e) {
                                item.name = cc;
                            }
                        }
                        countryMap.put(cc, item);
                    }
                    item.count++;
                }
            }
            
            countryList.clear();
            countryList.addAll(countryMap.values());
            Collections.sort(countryList, (o1, o2) -> o1.name.compareTo(o2.name));
            
        } else {
            // Proxy List Mode
            allProxies.clear();
            for (UzbekProxyInfo info : source) {
                if (proxyType.equals(info.type)) {
                    String cc = info.country;
                    if (TextUtils.isEmpty(cc)) cc = "Unknown";
                    
                    if (countryCode.equals(cc)) {
                        allProxies.add(info);
                    }
                }
            }
            
            Comparator<UzbekProxyInfo> comparator = (o1, o2) -> {
                long bias1 = SharedConfig.currentProxy == o1 ? -200000 : 0;
                if (!o1.available) bias1 += 100000;
                long bias2 = SharedConfig.currentProxy == o2 ? -200000 : 0;
                if (!o2.available) bias2 += 100000;
                return Long.compare(o1.ping + bias1, o2.ping + bias2);
            };
            Collections.sort(allProxies, comparator);
            
            displayedProxies.clear();
            loadMore();
        }
        
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }
    
    private void loadMore() {
        if (countryCode == null) return; 
        
        int currentSize = displayedProxies.size();
        int remaining = allProxies.size() - currentSize;
        if (remaining > 0) {
            int toAdd = Math.min(remaining, pageSize);
            displayedProxies.addAll(allProxies.subList(currentSize, currentSize + toAdd));
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        if (countryCode == null) {
            if ("socks5".equals(proxyType)) {
                actionBar.setTitle("SOCKS5 Countries");
            } else {
                actionBar.setTitle("MTProto Countries");
            }
        } else {
            String countryName = countryCode;
            try {
                countryName = new Locale("", countryCode).getDisplayCountry();
            } catch (Exception ignore) {}
            if ("Unknown".equals(countryCode)) countryName = "Unknown Region";
            actionBar.setTitle(countryName);
        }
        
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
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

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (countryCode != null && !recyclerView.canScrollVertically(1)) {
                    loadMore();
                }
            }
        });

        listView.setOnItemClickListener((view, position) -> {
            if (countryCode == null) {
                // Country Mode -> Open Country
                if (position >= 0 && position < countryList.size()) {
                    CountryItem item = countryList.get(position);
                    presentFragment(new UzbekProxyListActivity(proxyType, item.code));
                }
            } else {
                // Proxy Mode -> Connect
                if (position >= 0 && position < displayedProxies.size()) {
                    UzbekProxyInfo info = displayedProxies.get(position);
                    UzbekVPNController.getInstance().enableProxy(info);
                    
                    // Remove Country List from backstack
                    if (getParentLayout() != null && getParentLayout().getFragmentStack().size() >= 2) {
                         BaseFragment previousFragment = getParentLayout().getFragmentStack().get(getParentLayout().getFragmentStack().size() - 2);
                         if (previousFragment instanceof UzbekProxyListActivity) {
                             getParentLayout().removeFragmentFromStack(previousFragment);
                         }
                    }
                    finishFragment(); // Close list
                }
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
            if (countryCode == null) {
                return countryList.size();
            } else {
                return displayedProxies.size();
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (countryCode == null) {
                // Country Cell
                view = new TextSettingsCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else {
                // Proxy Cell
                view = new ProxyCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (countryCode == null) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                CountryItem item = countryList.get(position);
                cell.setTextAndValue( (item.flag != null ? item.flag + " " : "") + item.name, String.valueOf(item.count), true);
            } else {
                ProxyCell cell = (ProxyCell) holder.itemView;
                cell.setProxy(displayedProxies.get(position));
            }
        }
    }

    public static class ProxyCell extends FrameLayout {
        private TextView flagTextView;
        private TextView countryTextView;
        private TextView detailTextView;
        private ImageView checkImageView;
        private View pingDot;
        private Paint pingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public ProxyCell(Context context) {
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
                pingDot.setVisibility(View.GONE);
                detailTextView.setText(info.address);
            } else {
                checkImageView.setVisibility(View.GONE);
                pingDot.setVisibility(View.VISIBLE);
                pingDot.setTranslationX(0);
                
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
                
                detailTextView.setText(info.address + " • " + pingText);
            }
        }
    }
}