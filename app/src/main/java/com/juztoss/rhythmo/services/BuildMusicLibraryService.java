package com.juztoss.rhythmo.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.juztoss.rhythmo.R;
import com.juztoss.rhythmo.presenters.RhythmoApp;
import com.juztoss.rhythmo.views.activities.SettingsActivity;

/**
 * Created by JuzTosS on 5/27/2016.
 */
public class BuildMusicLibraryService extends Service
{
    public static final String UPDATE_PROGRESS_ACTION = "com.juztoss.rhythmo.action.UPDATE_PROGRESS";
    public static final String PROGRESS_ACTION_OVERALL_PROGRESS = "OverallProgress";
    public static final String PROGRESS_ACTION_MAX_PROGRESS = "MaxProgress";
    public static final String PROGRESS_ACTION_HEADER = "Header";
    public static final String REBUILD = "Rebuild";
    public static final String STOP_AND_CLEAR = "StopAndClear";
    public static final String SILENT_MODE = "SilentMode";
    public static final String DONT_DETECT_BPM = "DontDetectBPM";

    private RhythmoApp mApp;
    private NotificationCompat.Builder mBuilder;
    private Notification mNotification;
    private NotificationManager mNotifyManager;
    public static final int NOTIFICATION_ID = 43;
    private volatile boolean mSilentMode;
    private volatile boolean mDontDetectBPM;

    private AsyncBuildLibraryTask mTaskBuildLib;
    private AsyncDetectBpmByNamesTask mTaskDetectBpmByNames;
    private AsyncDetectBpmByDataTask mTaskDetectBpmByData;

    @Override
    public void onCreate()
    {
        mApp = (RhythmoApp) this.getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent == null)
        {
            stopSelf();
            return START_NOT_STICKY;
        }

        boolean stopAndClear = false;
        boolean clear = false;

        if(intent.getExtras() != null)
        {
            stopAndClear = intent.getExtras().getBoolean(STOP_AND_CLEAR);
            clear = intent.getExtras().getBoolean(REBUILD);
            mSilentMode = intent.getExtras().getBoolean(SILENT_MODE);
            mDontDetectBPM = intent.getExtras().getBoolean(DONT_DETECT_BPM);
        }

        if(stopAndClear)
        {
            cancelTasks();

            mTaskBuildLib = new AsyncBuildLibraryTask(mApp, true);
            mTaskBuildLib.setOnBuildLibraryProgressUpdate(mOnClearLibraryUpdate);
            mTaskBuildLib.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

            return START_STICKY;
        }

        Toast.makeText(this, getString(R.string.build_library_started), Toast.LENGTH_LONG).show();

        mBuilder = new NotificationCompat.Builder(mApp);
        mBuilder.setSmallIcon(R.drawable.ic_play_arrow_black_36dp);
        mBuilder.setContentTitle(getResources().getString(R.string.building_music_library));
        mBuilder.setTicker(getResources().getString(R.string.building_music_library));
        mBuilder.setContentText("");
        mBuilder.setProgress(0, 0, true);

        mNotifyManager = (NotificationManager) mApp.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = mBuilder.build();
        mNotification.flags |= Notification.FLAG_INSISTENT | Notification.FLAG_NO_CLEAR;
        mNotifyManager.notify(NOTIFICATION_ID, mNotification);

        mTaskBuildLib = new AsyncBuildLibraryTask(mApp, clear);
        mTaskBuildLib.setOnBuildLibraryProgressUpdate(mOnBuildLibraryUpdate);
        mTaskBuildLib.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        String key = getResources().getString(R.string.pref_recognize_bpm_from_name);
        boolean needToGetBPMByNames =  PreferenceManager.getDefaultSharedPreferences(this).getBoolean(key, true);

        if(!mDontDetectBPM)
        {
            if (needToGetBPMByNames)
            {
                mTaskDetectBpmByNames = new AsyncDetectBpmByNamesTask(mApp);
                mTaskDetectBpmByNames.setOnBuildLibraryProgressUpdate(mOnDetectBpmByNamesUpdate);
                mTaskDetectBpmByNames.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }

            mTaskDetectBpmByData = new AsyncDetectBpmByDataTask(mApp);
            mTaskDetectBpmByData.setOnBuildLibraryProgressUpdate(mOnDetectBpmByDataUpdate);
            mTaskDetectBpmByData.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
        return START_REDELIVER_INTENT;
    }

    private void cancelTasks()
    {
        if(mTaskBuildLib != null)
            mTaskBuildLib.cancel(true);

        if(mTaskDetectBpmByNames != null)
            mTaskDetectBpmByNames.cancel(true);

        if(mTaskDetectBpmByData != null)
            mTaskDetectBpmByData.cancel(true);

    }

    @Override
    public void onDestroy()
    {
        cancelTasks();
        if(mNotifyManager != null)
            mNotifyManager.cancel(NOTIFICATION_ID);

        mApp.setIsBuildingLibrary(false);
        mApp.notifyPlaylistsRepresentationUpdated();

        Intent intent = new Intent(UPDATE_PROGRESS_ACTION);
        intent.putExtra(PROGRESS_ACTION_HEADER, mApp.getString(R.string.build_library_desc));
        intent.putExtra(PROGRESS_ACTION_OVERALL_PROGRESS, 0);
        intent.putExtra(PROGRESS_ACTION_MAX_PROGRESS, 0);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(mApp);
        broadcastManager.sendBroadcast(intent);

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return new Binder();
    }

    private AsyncBuildLibraryTask.OnBuildLibraryProgressUpdate mOnClearLibraryUpdate = new AsyncBuildLibraryTask.OnBuildLibraryProgressUpdate()
    {
        @Override
        public void onStartBuildingLibrary(AsyncBuildLibraryTask task)
        {
            mApp.setIsBuildingLibrary(true);
        }

        @Override
        public void onProgressUpdate(AsyncBuildLibraryTask task, int overallProgress, int maxProgress, boolean mediaStoreTransferDone)
        {

        }

        @Override
        public void onFinishBuildingLibrary(AsyncBuildLibraryTask task)
        {
            if(!mSilentMode)
                Toast.makeText(mApp, R.string.clear_library_finished, Toast.LENGTH_LONG).show();

            stopSelf();
        }
    };

    private AsyncBuildLibraryTask.OnBuildLibraryProgressUpdate mOnBuildLibraryUpdate = new AsyncBuildLibraryTask.OnBuildLibraryProgressUpdate()
    {
        @Override
        public void onStartBuildingLibrary(AsyncBuildLibraryTask task)
        {
            mApp.setIsBuildingLibrary(true);
        }

        @Override
        public void onProgressUpdate(AsyncBuildLibraryTask task, int overallProgress, int maxProgress, boolean mediaStoreTransferDone)
        {
            String header = getResources().getString(R.string.building_music_library);
            showNotification(header, overallProgress, maxProgress);
        }

        @Override
        public void onFinishBuildingLibrary(AsyncBuildLibraryTask task)
        {
            mApp.notifyPlaylistsRepresentationUpdated();

            if(mDontDetectBPM)
            {
                if(!mSilentMode)
                    Toast.makeText(mApp, R.string.build_library_finished, Toast.LENGTH_LONG).show();

                stopSelf();
            }
        }
    };

    private OnDetectBpmByNamesUpdate mOnDetectBpmByNamesUpdate = new OnDetectBpmByNamesUpdate()
    {
        @Override
        public void onStartBuildingLibrary(AsyncDetectBpmByNamesTask task)
        {

        }

        @Override
        public void onProgressUpdate(AsyncDetectBpmByNamesTask task, int overallProgress, int maxProgress, boolean mediaStoreTransferDone)
        {
            String header = getResources().getString(R.string.detect_bpm_by_name);
            showNotification(header, overallProgress, maxProgress);
        }

        @Override
        public void onFinishBuildingLibrary(AsyncDetectBpmByNamesTask task)
        {
            mApp.notifyPlaylistsRepresentationUpdated();
        }
    };

    private OnDetectBpmByDataUpdate mOnDetectBpmByDataUpdate = new OnDetectBpmByDataUpdate()
    {
        @Override
        public void onStartBuildingLibrary(AsyncDetectBpmByDataTask task)
        {

        }

        @Override
        public void onProgressUpdate(AsyncDetectBpmByDataTask task, int overallProgress, int maxProgress, boolean mediaStoreTransferDone)
        {
            String header = getResources().getString(R.string.detect_bpm_by_data, overallProgress, maxProgress);
            showNotification(header, overallProgress, maxProgress);
        }

        @Override
        public void onFinishBuildingLibrary(AsyncDetectBpmByDataTask task)
        {
            mApp.notifyPlaylistsRepresentationUpdated();
            if(!mSilentMode)
                Toast.makeText(mApp, R.string.build_library_finished, Toast.LENGTH_LONG).show();

            stopSelf();
        }
    };

    private void showNotification(String header, int overallProgress, int maxProgress)
    {
        if(mSilentMode) return;

        mBuilder = new NotificationCompat.Builder(mApp);
        mBuilder.setSmallIcon(R.drawable.ic_play_arrow_black_36dp);
        mBuilder.setContentTitle(header);
        mBuilder.setTicker(header);
        mBuilder.setContentText("");
        mBuilder.setProgress(maxProgress, overallProgress, false);

        Intent launchNowPlayingIntent = new Intent(this, SettingsActivity.class);
        launchNowPlayingIntent.putExtra(PROGRESS_ACTION_HEADER, header);
        launchNowPlayingIntent.putExtra(PROGRESS_ACTION_OVERALL_PROGRESS, overallProgress);
        launchNowPlayingIntent.putExtra(PROGRESS_ACTION_MAX_PROGRESS, maxProgress);
        PendingIntent launchNowPlayingPendingIntent = PendingIntent.getActivity(this, 0, launchNowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(launchNowPlayingPendingIntent);

        mNotification = mBuilder.build();

        mNotification.flags |= Notification.FLAG_INSISTENT | Notification.FLAG_NO_CLEAR;
        mNotifyManager.notify(NOTIFICATION_ID, mNotification);

        Intent intent = new Intent(UPDATE_PROGRESS_ACTION);
        intent.putExtra(PROGRESS_ACTION_HEADER, header);
        intent.putExtra(PROGRESS_ACTION_OVERALL_PROGRESS, overallProgress);
        intent.putExtra(PROGRESS_ACTION_MAX_PROGRESS, maxProgress);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(mApp);
        broadcastManager.sendBroadcast(intent);
    }
}
