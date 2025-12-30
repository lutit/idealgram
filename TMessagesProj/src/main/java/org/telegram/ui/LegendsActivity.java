package org.telegram.ui;

import android.os.Bundle;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.ActionBar.Theme;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LegendsActivity extends BaseFragment {

    private RecyclerListView listView;

    private static class LegendEntry {
        final String title;
        final String username;

        LegendEntry(String title, String username) {
            this.title = title;
            this.username = username;
        }
    }

    private final LegendEntry[] legends = new LegendEntry[] {
            new LegendEntry("@Uzbek_GPTrobot", "Uzbek_GPTrobot"),
            new LegendEntry("@Evgesha_31bgd", "Evgesha_31bgd"),
            new LegendEntry("@Ahmad_0009", "Ahmad_0009"),
            new LegendEntry("@TommyAlghelo", "TommyAlghelo"),
            new LegendEntry("@uzbekgram_client", "Uzbekgram"),
            new LegendEntry("@monk", "monk"),
            new LegendEntry("@itsnolyy", "itsnolyy")
    };

    @Override
    public View createView(android.content.Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.Legends));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(new Adapter());
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= legends.length) {
                return;
            }
            LegendEntry entry = legends[position];
            openUsername(entry.username);
        });

        fragmentView = listView;
        fragmentView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void openUsername(String username) {
        getMessagesController().openByUserName(username, this, 0);
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            TextSettingsCell cell = new TextSettingsCell(parent.getContext());
            cell.setBackground(Theme.getSelectorDrawable(true));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextSettingsCell cell = (TextSettingsCell) holder.itemView;
            LegendEntry entry = legends[position];
            cell.setText(entry.title, false);
        }

        @Override
        public int getItemCount() {
            return legends.length;
        }
    }
}
