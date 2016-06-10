package com.juztoss.bpmplayer.views;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.juztoss.bpmplayer.R;
import com.juztoss.bpmplayer.presenters.BPMPlayerApp;

/**
 * Created by JuzTosS on 4/20/2016.
 */
public class PlaylistFragment extends Fragment implements AdapterView.OnItemClickListener, BPMPlayerApp.OnBPMChangedListener
{
    public static String PLAYLIST_INDEX = "PlaylistID";

    private PlaylistAdapter mPlaylistAdapter;

    private BPMPlayerApp mApp;

    public static PlaylistFragment newInstance(int playlistIndex)
    {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putInt(PLAYLIST_INDEX, playlistIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mApp = (BPMPlayerApp) getActivity().getApplicationContext();

        Bundle arguments = getArguments();
        if (arguments == null) return;
        int playlistIndex = arguments.getInt(PLAYLIST_INDEX, -1);
        if (playlistIndex < 0) return;

        if (!mApp.isPlaybackServiceRunning()) return;

        mPlaylistAdapter = new PlaylistAdapter(getActivity(), mApp.getPlaylists().get(playlistIndex));
        ListView list = (ListView) getView().findViewById(R.id.listView);
        list.setOnItemClickListener(this);
        list.setAdapter(mPlaylistAdapter);
    }



    @Override
    public void onStart()
    {
        super.onStart();
        mApp.addOnRangeChangedListener(this);
        super.onStart();
        mPlaylistAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.songs_list, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        if (mApp.isPlaybackServiceRunning())
        {
            mApp.getPlaybackService().setSource(position);
            mApp.getPlaybackService().startPlayback();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mApp.removeOnRangeChangedListener(this);
    }

    public void onResumeFragment()
    {
        mApp.addOnRangeChangedListener(this);
    }

    public void onPauseFragment()
    {
        mApp.removeOnRangeChangedListener(this);
    }

    @Override
    public void onBPMChanged(float minBPM, float maxBPM)
    {
        mPlaylistAdapter.updatePlaylist();
    }
}