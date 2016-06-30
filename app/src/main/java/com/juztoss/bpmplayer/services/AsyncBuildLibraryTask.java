package com.juztoss.bpmplayer.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.MediaStore;

import com.juztoss.bpmplayer.models.DatabaseHelper;
import com.juztoss.bpmplayer.presenters.BPMPlayerApp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JuzTosS on 5/27/2016.
 */

public class AsyncBuildLibraryTask extends AsyncTask<String, String, Void>
{
    private final String UPDATE_COMPLETE = "UpdateComplete";
    private final int MAX_PROGRESS_VALUE = 1000000;
    private BPMPlayerApp mApp;
    private boolean mClear;
    public ArrayList<OnBuildLibraryProgressUpdate> mBuildLibraryProgressUpdate;

    private int mOverallProgress = 0;

    private PowerManager.WakeLock wakeLock;

    public AsyncBuildLibraryTask(BPMPlayerApp app, boolean clear)
    {
        mApp = app;
        mClear = clear;
        mBuildLibraryProgressUpdate = new ArrayList<>();
    }

    public interface OnBuildLibraryProgressUpdate
    {
        void onStartBuildingLibrary(AsyncBuildLibraryTask task);

        void onProgressUpdate(AsyncBuildLibraryTask task,
                              int overallProgress, int maxProgress,
                              boolean mediaStoreTransferDone);

        void onFinishBuildingLibrary(AsyncBuildLibraryTask task);

    }

    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();

        if (mBuildLibraryProgressUpdate != null)
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onStartBuildingLibrary(this);

        PowerManager pm = (PowerManager) mApp.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        wakeLock.acquire();

    }

    @Override
    protected Void doInBackground(String... params)
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        Cursor mediaStoreCursor = getSongsFromMediaStore();
        try
        {
            if (mediaStoreCursor != null)
            {
                saveMediaStoreDataToDB(mediaStoreCursor);
            }
        }
        finally
        {
            mediaStoreCursor.close();
        }
        publishProgress(UPDATE_COMPLETE);
        return null;
    }

    private Cursor getSongsFromMediaStore()
    {
        String projection[] = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA};

        ContentResolver contentResolver = mApp.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

        return contentResolver.query(uri, projection, selection, null, null);
    }

    private void saveMediaStoreDataToDB(Cursor mediaStoreCursor)
    {
        try
        {
            if(mClear) mApp.getDatabaseHelper().clearAll(mApp.getDatabaseHelper().getWritableDatabase());

            mApp.getDatabaseHelper().getWritableDatabase().beginTransaction();

            //Set all songs as deleted, we'll update each song that exists later
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.MUSIC_LIBRARY_DELETED, true);
            mApp.getDatabaseHelper().getWritableDatabase().update(DatabaseHelper.TABLE_MUSIC_LIBRARY, cv, DatabaseHelper.MUSIC_LIBRARY_MEDIA_ID + " >= 0", null);

            mApp.getDatabaseHelper().getWritableDatabase().delete(DatabaseHelper.TABLE_FOLDERS, null, null);

            //Tracks the progress of this method.
            int subProgress;
            if (mediaStoreCursor.getCount() != 0)
                subProgress = mOverallProgress / 4 / (mediaStoreCursor.getCount());
            else
                subProgress = mOverallProgress / 4;

            class Node
            {
                public Node(long id)
                {
                    mId = id;
                }

                public void add(Node node, String name)
                {
                    mChildren.add(node);
                    mChildrenNames.add(name);
                    node.mParent = this;
                }

                public Long mId;
                Node mParent;
                public List<Node> mChildren = new ArrayList<>();
                public List<String> mChildrenNames = new ArrayList<>();

                public Node get(String folder)
                {
                    return mChildren.get(mChildrenNames.indexOf(folder));
                }
            }

            Node folderIds = new Node(-1);

            final int filePathColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            final int idColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media._ID);

            long lastUpdated = System.currentTimeMillis();
            for (int i = 0; i < mediaStoreCursor.getCount(); i++)
            {
                mediaStoreCursor.moveToPosition(i);
                mOverallProgress += subProgress;
                long now = System.currentTimeMillis();
                if (now - lastUpdated > 1000)
                {
                    lastUpdated = now;
                    publishProgress();
                }

                String songFileFullPath = mediaStoreCursor.getString(filePathColIndex);
                String songId = mediaStoreCursor.getString(idColIndex);

                String[] folders = songFileFullPath.split("/");
                StringBuilder b = new StringBuilder(songFileFullPath);
                String songFileName = folders[folders.length - 1];

                String songNameWithSlash = "/" + songFileName;
                b.replace(songFileFullPath.lastIndexOf(songNameWithSlash), songFileFullPath.lastIndexOf(songNameWithSlash) + songNameWithSlash.length(), "");
                String songFileFolder = b.toString();

                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.MUSIC_LIBRARY_PATH, songFileFolder);
                values.put(DatabaseHelper.MUSIC_LIBRARY_NAME, songFileName);
                values.put(DatabaseHelper.MUSIC_LIBRARY_FULL_PATH, songFileFullPath);
                values.put(DatabaseHelper.MUSIC_LIBRARY_MEDIA_ID, songId);
                values.put(DatabaseHelper.MUSIC_LIBRARY_DELETED, false);

                //Add all the entries to the database to build the songs library.
                long rowId = mApp.getDatabaseHelper().getWritableDatabase().insertWithOnConflict(DatabaseHelper.TABLE_MUSIC_LIBRARY,
                        null,
                        values, SQLiteDatabase.CONFLICT_IGNORE);

                if (rowId != -1)
                {
                    mApp.getDatabaseHelper().getWritableDatabase().update(DatabaseHelper.TABLE_MUSIC_LIBRARY, values, DatabaseHelper._ID + " = ?", new String[]{Long.toString(rowId)});
                }

                Node parentNode = folderIds;
                for (int j = 1; j < (folders.length - 1); j++)
                {
                    String folder = folders[j];
                    if (parentNode.mChildrenNames.contains(folder))
                    {
                        parentNode = parentNode.get(folder);
                        continue;
                    }

                    ContentValues folderValues = new ContentValues();
                    folderValues.put(DatabaseHelper.FOLDERS_NAME, folder);

                    folderValues.put(DatabaseHelper.FOLDERS_PARENT_ID, parentNode.mId);
                    if (j == (folders.length - 2))//This is the last segment
                        folderValues.put(DatabaseHelper.FOLDERS_HAS_SONGS, true);

                    long id = mApp.getDatabaseHelper().getWritableDatabase().insert(DatabaseHelper.TABLE_FOLDERS, null, folderValues);
                    Node newNode = new Node(id);
                    parentNode.add(newNode, folder);
                    parentNode = newNode;
                }


            }

        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            //Close the transaction.
            mApp.getDatabaseHelper().getWritableDatabase().setTransactionSuccessful();
            mApp.getDatabaseHelper().getWritableDatabase().endTransaction();
        }

        mApp.getDatabaseHelper().getWritableDatabase().delete(DatabaseHelper.TABLE_MUSIC_LIBRARY, DatabaseHelper.MUSIC_LIBRARY_DELETED + " = ?", new String[]{"true"});
    }

    @Override
    protected void onProgressUpdate(String... progressParams)
    {
        super.onProgressUpdate(progressParams);

        if (progressParams.length > 0 && progressParams[0].equals(UPDATE_COMPLETE))
        {
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onProgressUpdate(this, mOverallProgress,
                            MAX_PROGRESS_VALUE, true);

            return;
        }

        if (mBuildLibraryProgressUpdate != null)
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onProgressUpdate(this, mOverallProgress, MAX_PROGRESS_VALUE, false);

    }

    @Override
    protected void onPostExecute(Void arg0)
    {
        wakeLock.release();

        if (mBuildLibraryProgressUpdate != null)
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onFinishBuildingLibrary(this);

    }

    /**
     * Setter methods.
     */
    public void setOnBuildLibraryProgressUpdate(OnBuildLibraryProgressUpdate
                                                        buildLibraryProgressUpdate)
    {
        if (buildLibraryProgressUpdate != null)
            mBuildLibraryProgressUpdate.add(buildLibraryProgressUpdate);
    }
}
