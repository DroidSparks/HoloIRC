/*
    HoloIRC - an IRC client for Android

    Copyright 2013 Lalit Maganti

    This file is part of HoloIRC.

    HoloIRC is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HoloIRC is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with HoloIRC. If not, see <http://www.gnu.org/licenses/>.
 */

package com.fusionx.lightirc.ui;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.fusionx.lightirc.R;
import com.fusionx.lightirc.adapters.ActionsServerAdapter;
import com.fusionx.lightirc.adapters.ActionsUserChannelAdapter;
import com.fusionx.lightirc.constants.FragmentTypeEnum;
import com.fusionx.lightirc.irc.Server;
import com.fusionx.lightirc.ui.dialogbuilder.ChannelNamePromptDialogBuilder;
import com.fusionx.lightirc.ui.dialogbuilder.NickPromptDialogBuilder;
import com.fusionx.lightirc.uiircinterface.ServerCommandSender;
import com.fusionx.lightirc.util.FragmentUtils;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import java.util.Arrays;

public class ActionsFragment extends ListFragment implements AdapterView.OnItemClickListener,
        SlidingMenu.OnOpenListener {
    private FragmentTypeEnum type;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final MergeAdapter mergeAdapter = new MergeAdapter();
        final ActionsServerAdapter adapter = new ActionsServerAdapter(getActivity(),
                Arrays.asList(getResources().getStringArray(R.array.server_actions)));
        final ActionsUserChannelAdapter channelAdapter = new ActionsUserChannelAdapter
                (getActivity());

        final View serverHeader = inflater.inflate(R.layout.sliding_menu_header, null);
        final TextView textView = (TextView) serverHeader.findViewById(R.id
                .sliding_menu_heading_textview);
        textView.setText(getActivity().getString(R.string.server));

        final View otherHeader = inflater.inflate(R.layout.sliding_menu_header, null);
        final TextView otherTextView = (TextView) otherHeader.findViewById(R.id
                .sliding_menu_heading_textview);

        if (type == null || type.equals(FragmentTypeEnum.Server)) {
            otherHeader.setVisibility(View.GONE);
            channelAdapter.setServerVisible();
        } else if (type.equals(FragmentTypeEnum.Channel)) {
            otherTextView.setText(getActivity().getString(R.string.channel));
        } else {
            otherTextView.setText(getActivity().getString(R.string.user));
        }

        mergeAdapter.addView(serverHeader);
        mergeAdapter.addAdapter(adapter);
        mergeAdapter.addView(otherHeader);
        mergeAdapter.addAdapter(channelAdapter);

        setListAdapter(mergeAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i,
                            final long l) {
        final IRCActionsCallback callback = FragmentUtils.getParent(this, IRCActionsCallback.class);

        switch (i) {
            case 1:
                channelNameDialog();
                break;
            case 2:
                nickChangeDialog();
                break;
            case 3:
                final Server server = callback.getServer(true);
                if (server == null) {
                    callback.onDisconnect(true, false);
                } else {
                    ServerCommandSender.sendDisconnect(server, getActivity());
                }
                return;
            case 4:
                ActionsPagerFragment fragment = (ActionsPagerFragment) getParentFragment();
                fragment.switchToIgnoreFragment();
                return;
            case 6:
                callback.closeOrPartCurrentTab();
                break;
        }
        callback.closeAllSlidingMenus();
    }

    private void nickChangeDialog() {
        final IRCActionsCallback callback = FragmentUtils.getParent(this, IRCActionsCallback.class);
        final NickPromptDialogBuilder nickDialog = new NickPromptDialogBuilder(getActivity(),
                callback.getNick()) {
            @Override
            public void onOkClicked(final String input) {
                ServerCommandSender.sendNickChange(callback.getServer(false), input);
            }
        };
        nickDialog.show();
    }

    private void channelNameDialog() {
        final IRCActionsCallback callback = FragmentUtils.getParent(this, IRCActionsCallback.class);
        final ChannelNamePromptDialogBuilder builder = new ChannelNamePromptDialogBuilder
                (getActivity()) {
            @Override
            public void onOkClicked(final String input) {
                ServerCommandSender.sendJoin(callback.getServer(false), input);
            }
        };
        builder.show();
    }

    @Override
    public void onOpen() {
        final IRCActionsCallback callback = FragmentUtils.getParent(this, IRCActionsCallback.class);
        if (callback.isConnectedToServer() != getServerAdapter().isConnected()) {
            getServerAdapter().setConnected(callback.isConnectedToServer());
            getServerAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public MergeAdapter getListAdapter() {
        return (MergeAdapter) super.getListAdapter();
    }

    private ActionsServerAdapter getServerAdapter() {
        return (ActionsServerAdapter) getListAdapter().getAdapter(1);
    }

    private ActionsUserChannelAdapter getUserChannelAdapter() {
        return (ActionsUserChannelAdapter) getListAdapter().getAdapter(3);
    }

    public void updateConnectionStatus(final boolean isConnected) {
        getServerAdapter().setConnected(isConnected);
        getServerAdapter().notifyDataSetChanged();
    }

    public void onTabChanged(final FragmentTypeEnum selectedType) {
        if (selectedType != type) {
            type = selectedType;
            if (getListAdapter() != null) {
                final View view = (View) getListAdapter().getItem(5);
                final TextView textView = (TextView) view.findViewById(R.id
                        .sliding_menu_heading_textview);
                switch (type) {
                    case Server:
                        view.setVisibility(View.GONE);
                        textView.setVisibility(View.GONE);
                        getUserChannelAdapter().setServerVisible();
                        break;
                    case Channel:
                        view.setVisibility(View.VISIBLE);
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(getActivity().getString(R.string.channel));
                        getUserChannelAdapter().setChannelVisible(true);
                        break;
                    case User:
                        view.setVisibility(View.VISIBLE);
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(getActivity().getString(R.string.user));
                        getUserChannelAdapter().setChannelVisible(false);
                        break;
                }
                getUserChannelAdapter().notifyDataSetChanged();
            }
        }
    }

    public interface IRCActionsCallback {
        public String getNick();

        public void closeOrPartCurrentTab();

        public boolean isConnectedToServer();

        public Server getServer(boolean nullable);

        public void closeAllSlidingMenus();

        public void onDisconnect(boolean expected, boolean retryPending);
    }
}