package com.znasibov.powerstats;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
//        implements ActionBar.OnNavigationListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);

//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//
//        // Set up the dropdown list navigation in the action bar.
//        actionBar.setListNavigationCallbacks(
//                // Specify a SpinnerAdapter to populate the dropdown list.
//                new ArrayAdapter<String>(
//                        actionBar.getThemedContext(),
//                        android.R.layout.simple_list_item_1,
//                        android.R.id.text1,
//                        new String[] {
//                                getString(R.string.title_section1),
//                                getString(R.string.title_section2),
//                                getString(R.string.title_section3),
//                        }),
//                this);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new QuickStatsFragment())
                .commit();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
//        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
//            getSupportActionBar().setSelectedNavigationItem(
//                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public boolean onNavigationItemSelected(int position, long id) {
//        // When the given dropdown item is selected, show its contents in the
//        // container view.
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.container, QuickStatsFragment.newInstance(position + 1))
//                .commit();
//        return true;
//    }



}
