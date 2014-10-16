package com.znasibov.powerstats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

//import org.achartengine.ChartFactory;
//import org.achartengine.GraphicalView;
//import org.achartengine.chart.PointStyle;
//import org.achartengine.model.XYMultipleSeriesDataset;
//import org.achartengine.model.XYSeries;
//import org.achartengine.renderer.XYMultipleSeriesRenderer;
//import org.achartengine.renderer.XYSeriesRenderer;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.*;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;

public class QuickStatsFragment extends Fragment
        implements ServiceConnection, PowerStatsReceiver{
    private final long CHART_UPDATE_PERIOD_MS = Util.minutesToMs(1);

    PowerStatsLoggerService pslService;

    TextView batteryInfoText;
    TextView timestampText;
    TextView wifiInfoText;
    QuickStatsPlot quickStatsPlot;

    ImageView gotoChartViewImage;

    PowerRecord lastChartPowerRecord;
    long chartUpdatedTimestamp;

    public QuickStatsFragment() {
        lastChartPowerRecord = new PowerRecord();
        resetChartUpdatedTimestamp();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quickstats, container, false);
        timestampText = (TextView)view.findViewById(R.id.timestamp_text);
        batteryInfoText = (TextView)view.findViewById(R.id.battery_text);
        wifiInfoText = (TextView)view.findViewById(R.id.wifi_text);
        quickStatsPlot = (QuickStatsPlot)view.findViewById(R.id.quickstats_plot);

        TableRow trChart = (TableRow)view.findViewById(R.id.chart_table_row);
        trChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(
                        R.animator.fragment_slide_left_enter,
                        R.animator.fragment_slide_left_exit,
                        R.animator.fragment_slider_right_enter,
                        R.animator.fragment_slider_right_exit);
                ft.replace(R.id.fragment_container, new PowerStatsChartFragment());
                ft.addToBackStack(null);
                ft.commit();
            }
        });

        // Can we get data from the service?
//        if (pslService == null) {
//            quickStatsPlot.render(new PowerRecord[0]);
//        }
        return view;
    }

    @Override
    public void onStart() {
        Intent intent = new Intent(getActivity(), PowerStatsLoggerService.class);
        Context appContext = getActivity().getApplicationContext();
        appContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
        super.onStart();
    }

    @Override
    public void onResume() {
        // Force the service to send the data to this object
        if (pslService != null) {
            pslService.register(this);
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        if (pslService != null) {
            pslService.unregister(this);
        }
        getActivity().getApplicationContext().unbindService(this);
        resetChartUpdatedTimestamp();
        super.onStop();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        pslService = ((PowerStatsLoggerService.ServiceBinder)binder).getService();
        pslService.register(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        pslService = null;
    }

    @Override
    public void onReceive(PowerRecord p) {
        updateTimestampText(p);
        updateBatteryText(p);
        updateWifiText(p);
        updatePlot(p);
    }

    void updateTimestampText(PowerRecord p) {
        String timestampStr = Util.timestampToTimeString(p.getTimestamp());
        String text = String.format(getString(R.string.title_stats_updated_at),
                                   timestampStr);
        timestampText.setText(text);
    }

    void updateBatteryText(PowerRecord p) {
        SpannableString row1 = new SpannableString(
                String.format("%.1f %% - %s\n",
                              p.getBatteryValue(),
                              p.getBatteryStatusAsString()));
        SpannableString row2 = new SpannableString(
                String.format("%s|%d° C|%d mV",
                              p.healthToString(),
                              p.getBatteryTemperature(),
                              p.getBatteryVoltage()));

        CharSequence text = TextUtils.concat(row1, row2);
        batteryInfoText.setText(text);

    }

    void updateWifiText(PowerRecord p) {
        SpannableString row1 = new SpannableString(p.getWifiStateAsString());
        wifiInfoText.setText(row1);
    }

    void updatePlot(PowerRecord p) {
        boolean batteryValueDiffers = lastChartPowerRecord.getBatteryValue() != p.getBatteryValue();
        boolean updatedLongAgo = System.currentTimeMillis() - chartUpdatedTimestamp > CHART_UPDATE_PERIOD_MS;
        if (batteryValueDiffers || updatedLongAgo) {
            chartUpdatedTimestamp = System.currentTimeMillis();
            lastChartPowerRecord = p;
            ArrayList<PowerRecord> records = pslService.getRecords(Util.hoursToMs(1));
            quickStatsPlot.render(records);
        }
    }

    void resetChartUpdatedTimestamp() {
        chartUpdatedTimestamp = 0;
    }

    public static class QuickStatsPlot extends XYPlot {
        private static final int SCALE_OFFSET = 1;

        private SimpleXYSeries series;
        private LineAndPointFormatter formatter;
        private PowerRecord lastRenderedRecord;

        public QuickStatsPlot(android.content.Context context, android.util.AttributeSet attrs) {
            super(context, attrs);
            initializeSeries();
            initializePlot();
        }

        private void initializeSeries() {
            series = new SimpleXYSeries("Battery level");
        }

        private void initializePlot() {
            final Context c = getContext();

//            int margin = 30;
//            setPlotMargins(margin, margin, margin, margin);



            //**** SERIES FORMATTER ****//
            formatter = new LineAndPointFormatter(
                    Color.rgb(128, 255, 128),
                    null,
                    null,
                    null
            );

            Paint linePaint = formatter.getLinePaint();
            linePaint.setStrokeWidth(3);
            linePaint.setPathEffect(new CornerPathEffect(10));

            Paint fillPaint = new Paint();
            fillPaint.setAlpha(100);
            fillPaint.setShader(new LinearGradient(0, 0, 0, 300, Color.GREEN, Color.WHITE, Shader.TileMode.MIRROR));
            formatter.setFillPaint(fillPaint);

            addSeries(series, formatter);

            setRangeBoundaries(0, 100, BoundaryMode.FIXED);
            setTicksPerRangeLabel(3);
            setDomainStep(XYStepMode.SUBDIVIDE, 4);

            setDomainValueFormat(new Format() {
                @Override
                public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                    long timestamp = ((Number)obj).longValue();
                    return new StringBuffer(Util.timestampToTimeString(timestamp));
                }

                @Override
                public Object parseObject(String source, ParsePosition pos) {
                    return null;
                }
            });
        }

        public void render(ArrayList<PowerRecord> records) {
            Context c = getContext();
            ArrayList<Number> modelRecords = generateModel(records);
            if (modelRecords.size() <= 1) {
                // there should be at least 2 items in modelRecords
                return;
            }

            clear();

            series.setModel(modelRecords, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
            addSeries(series, formatter);

            float minValue = Float.MAX_VALUE, maxValue = 0;

            for (PowerRecord r: records) {
                minValue = Math.min(minValue, r.getBatteryValue());
                maxValue = Math.max(maxValue, r.getBatteryValue());
            }

            setRangeBoundaries(Math.max(0, minValue - 2),
                               Math.min(100, maxValue + 2),
                               BoundaryMode.FIXED);
            redraw();
        }

        private ArrayList<Number> generateModel(ArrayList<PowerRecord> records) {
            ArrayList<Number> modelRecords = new ArrayList<Number>();

            for (PowerRecord r: records) {
                float batteryValue = r.getBatteryValue();
                modelRecords.add(r.getTimestamp());
                modelRecords.add(batteryValue);
            }

            return modelRecords;
        }
    }
}


