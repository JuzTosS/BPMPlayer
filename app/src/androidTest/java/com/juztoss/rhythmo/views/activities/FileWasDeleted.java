package com.juztoss.rhythmo.views.activities;


import android.os.Environment;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.juztoss.rhythmo.R;
import com.juztoss.rhythmo.TestHelper;
import com.juztoss.rhythmo.TestSuite;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.juztoss.rhythmo.TestSuite.SONG1;
import static com.juztoss.rhythmo.TestSuite.SONG2;
import static com.juztoss.rhythmo.TestSuite.SONG3;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class FileWasDeleted
{
    @Rule
    public ActivityTestRule<PlayerActivity> mActivityRule = new ActivityTestRule<>(PlayerActivity.class);

    @Test
    public void playerActivityTest() throws Exception
    {
        //Adding playlist
        openActionBarOverflowOrOptionsMenu(mActivityRule.getActivity());
        onView(withText(mActivityRule.getActivity().getString(R.string.new_playlist))).perform(click());

        //Adding folder, click on the apply button
        onView(withId(R.id.btnAddToPlaylist)).perform(click());
        onView(withId(R.id.apply)).perform(click());

        String fileToDelete1 = Environment.getExternalStorageDirectory().getPath() + "/" + TestSuite.MUSIC_FOLDER + "/" + SONG1;
        String fileToDelete2 = Environment.getExternalStorageDirectory().getPath() + "/" + TestSuite.MUSIC_FOLDER + "/" + SONG2;
        String fileToDelete3 = Environment.getExternalStorageDirectory().getPath() + "/" + TestSuite.MUSIC_FOLDER + "/" + SONG3;
        File file1 = new File(fileToDelete1);
        File file2 = new File(fileToDelete2);
        File file3 = new File(fileToDelete3);
        if (!file1.delete()) Assert.assertTrue(false);
        if (!file2.delete()) Assert.assertTrue(false);
        if (!file3.delete()) Assert.assertTrue(false);

        TestHelper.checkScreen(3, "", "", "", -1, true);

        //Adding folder, click on the apply button
        onView(withId(R.id.btnAddToPlaylist)).perform(click());

        TestHelper.updateLibrary();
        //Go back to player activity
        pressBack();

        TestHelper.checkScreen(0, "", "", "", -1, true);

        TestHelper.copyFiles();
        TestHelper.updateLibrary();

    }
}
