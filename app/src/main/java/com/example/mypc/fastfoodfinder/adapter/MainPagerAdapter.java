package com.example.mypc.fastfoodfinder.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.mypc.fastfoodfinder.ui.main.FavouriteLocationFragment;
import com.example.mypc.fastfoodfinder.ui.main.MainFragment;
import com.example.mypc.fastfoodfinder.ui.main.RecentlyLocationFragment;

/**
 * Created by nhoxb on 11/8/2016.
 */
public class MainPagerAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 3;
    private String pageTitles[] = new String[] { "Bản đồ", "Gần đây", "Yêu thích" };
    private Context context;


    public MainPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position)
        {
            case 0:
                return MainFragment.newInstance();
            case 1:
                return RecentlyLocationFragment.newInstance();
            case 2:
                return FavouriteLocationFragment.newInstance();

        }

        return  null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return  pageTitles[position];
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}