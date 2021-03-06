package com.juztoss.rhythmo.services;

import com.juztoss.rhythmo.audio.BpmDetector;
import com.juztoss.rhythmo.presenters.RhythmoApp;

/**
 * Created by JuzTosS on 5/27/2016.
 */

public class AsyncDetectBpmByDataTask extends AsyncDetectBpmTaskAbstract
{
    public AsyncDetectBpmByDataTask(RhythmoApp app, int playlistIndex, boolean resetBpm)
    {
        super(app, playlistIndex, resetBpm);
    }

    @Override
    public double detectBpm(double oldBpm, String fullPath, String name)
    {
        if(oldBpm <= 0)
            return BpmDetector.detectFromData(fullPath);
        else
            return oldBpm;
    }
}
