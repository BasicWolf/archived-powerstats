package com.znasibov.powerstats;


import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;


public class PowerStatsChartFragment extends Fragment
        implements ServiceConnection
{
    PowerStatsLoggerService pslService;
    PowerStatsPlot powerStatsPlot;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_powerstats, container, false);
        powerStatsPlot = (PowerStatsPlot)rootView.findViewById(R.id.powerstats_plot);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                getFragmentManager().popBackStack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onStart() {
        Intent intent = new Intent(getActivity(), PowerStatsLoggerService.class);
        Context appContext = getActivity().getApplicationContext();
        appContext.bindService(intent, this, Context.BIND_AUTO_CREATE);

        super.onStart();
    }

    @Override
    public void onStop() {
        getActivity().getApplicationContext().unbindService(this);
        super.onStop();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        pslService = ((PowerStatsLoggerService.ServiceBinder)binder).getService();
        // TODO: configure, how many records should we get here
        ArrayList<PowerRecord> records = pslService.getRecords(Util.daysToMs(3));
        powerStatsPlot.render(records);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        pslService = null;
    }
}

