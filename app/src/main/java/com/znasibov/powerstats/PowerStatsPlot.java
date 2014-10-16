package com.znasibov.powerstats;

import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Shader;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.FillDirection;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class PowerStatsPlot extends XYPlot implements View.OnTouchListener {
    private static final int DOMAIN_STEPS_COUNT = 5;
    private static final long MIN_DOMAIN_BOUNDARIES_LENGTH = Util.hoursToMs(1);

    private float leftBoundary = 0f;
    private float rightBoundary = 0f;
    private float leftBound = 0f;
    private float rightBound = 0f;
    private long stepLength = 0;

    private ArrayList<StatRenderer> statRenderers = new ArrayList<StatRenderer>();

    public PowerStatsPlot(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        initStatRenderers();
        initPlot();
        initTouchHandling();
    }

    private void initPlot() {
        // TODO: try moving these to XML
        getGraphWidget().setRangeLabelHorizontalOffset(0);
        getGraphWidget().getRangeLabelPaint().setTextAlign(Paint.Align.LEFT);

        // TODO: name the constatnts
        setRangeBoundaries(-5 + -10 * statRenderers.size(), 100, BoundaryMode.FIXED);
        setUserRangeOrigin(0);
        setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
        setDomainStep(XYStepMode.SUBDIVIDE, DOMAIN_STEPS_COUNT);

        setDomainValueFormat(new Format() {
            String timePattern; {
                if (DateFormat.is24HourFormat(getContext())) {
                    timePattern = "HH:mm";
                } else {
                    timePattern = "hh:mm a";
                }
            }

            String datePattern; {
                // Screw the US date format for this!
                if (getContext().getResources().getConfiguration().locale == Locale.US) {
                    datePattern = "M/d";
                } else {
                    datePattern = "d/M";
                }
            }

            String dateTimePattern = timePattern + " " + datePattern;

            DateTimeFormatter domainTimeFormatter = DateTimeFormat.forPattern(timePattern);
            DateTimeFormatter domainDateFormatter = DateTimeFormat.forPattern(datePattern);
            DateTimeFormatter domainDateTimeFormatter = DateTimeFormat.forPattern(dateTimePattern);

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                long timestamp = ((Number) obj).longValue();
                if (timestamp <= 0) {
                    return toAppendTo;
                }

                DateTime ts = new DateTime(timestamp);
                long daystamp = ts.dayOfWeek().roundHalfEvenCopy().getMillis();

                long stampsDiff = timestamp - daystamp;
                boolean longDomainSpan = rightBound - leftBound > Util.daysToMs(1);
                boolean timestampNearDaySplit = timestamp >= daystamp && stampsDiff <= stepLength;

                if (longDomainSpan) {
                    toAppendTo.append(domainDateFormatter.print(ts));
                } else if (timestampNearDaySplit) {
                    toAppendTo.append(domainDateTimeFormatter.print(ts));
                } else {
                    toAppendTo.append(domainTimeFormatter.print(ts));
                }

                return toAppendTo;
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });

        setRangeValueFormat(new Format() {
            int rendererIndex = 0;
            HashMap<Float, StatRenderer> valueToRendererMappings = new HashMap<Float, StatRenderer>();

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition fieldPosition) {
                float value = ((Number)obj).floatValue();

                if (value < 0 && value % 10 == -0.0f) { // -10, -20, ...
                    if (valueToRendererMappings.size() < statRenderers.size()) {
                        valueToRendererMappings.put(value, statRenderers.get(rendererIndex));
                        rendererIndex++;
                    }
                    StatRenderer renderer = valueToRendererMappings.get(value);
                    toAppendTo.append(renderer.getLabel());
                }
                else {
                    toAppendTo.append(obj);
                }

                return toAppendTo;
            }

            @Override
            public Object parseObject(String s, ParsePosition parsePosition) {
                return null;
            }
        });
    }

    private void initStatRenderers() {
        statRenderers.add(new PhoneServiceRenderer(this));
        statRenderers.add(new WifiRenderer(this));
        statRenderers.add(new GpsRenderer(this));
        statRenderers.add(new ScreenStateRenderer(this));

        for (int i = 0; i < statRenderers.size(); i++) {
            float rangeValue = -2 + -10 * (i + 1);
            statRenderers.get(i).setRangeValue(rangeValue);
        }
    }

    private void initTouchHandling()
    {
        this.setOnTouchListener(this);
    }

    public void render(ArrayList<PowerRecord> records) {
        if (records.size() == 0) {
            return;
        }
        clear();

        rightBoundary = records.get(records.size() - 1).getTimestamp();
        leftBoundary = records.get(0).getTimestamp();

        float rightBound = records.get(records.size() - 1).getTimestamp();
        float leftBound = rightBound - UserPreferences.getPowerStatsPlotDefaultDomainSize();
        setDomainBoundaries(leftBound, rightBound);

        renderBatteryLevelPlot(records);
        renderBatteryCharging(records);

        for (StatRenderer st: statRenderers) {
            st.render(records);
        }
        redraw();
    }

    private void renderBatteryLevelPlot(ArrayList<PowerRecord> records) {
        // VISUAL
        LineAndPointFormatter formatter = new LineAndPointFormatter(
                Color.rgb(128, 255, 128), null, null, null);

        Paint linePaint = formatter.getLinePaint();
        linePaint.setStrokeWidth(getResources().getDimension(R.dimen.plot_line_width));
        linePaint.setPathEffect(new CornerPathEffect(10));

        // DATA
        ArrayList<Number> values = new ArrayList<Number>();
        for (PowerRecord r: records) {
            values.add(r.getTimestamp());
            values.add(r.getBatteryValue());
        }

        SimpleXYSeries series = new SimpleXYSeries("Battery level");
        series.setModel(values, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
        addSeries(series, formatter);
    }

    private void renderBatteryCharging(ArrayList<PowerRecord> records) {
        // VISUAL
        LineAndPointFormatter formatter = new LineAndPointFormatter(
                Color.TRANSPARENT, null, null, null);

        Paint fillPaint = new Paint();
        fillPaint.setAlpha(100);

        int gradientHeight = getHeight();

        fillPaint.setShader(new LinearGradient(0, 0, 0, gradientHeight, Color.WHITE, Color.GREEN, Shader.TileMode.MIRROR));
        formatter.setFillPaint(fillPaint);
        formatter.setFillDirection(FillDirection.RANGE_ORIGIN);

        // DATA
        ArrayList<Number> values = new ArrayList<Number>();
        for (PowerRecord r: records) {
            values.add(r.getTimestamp());
            if (r.getBatteryStatus() == PowerRecord.BATTERY_STATUS_CHARGING) {
                values.add(r.getBatteryValue());
            } else {
                values.add(null);
            }
        }

        SimpleXYSeries series = new SimpleXYSeries("Battery charging");
        series.setModel(values, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
        addSeries(series, formatter);
    }



    // ***** TOUCH EVENTS AND SCROLLING ******
    // ***************************************

    // finger states
    PointF firstFinger;
    float distBetweenFingers;

    // Definition of the touch states
    static final int TOUCH_NONE = 0;
    static final int TOUCH_ONE_FINGER_DRAG = 1;
    static final int TOUCH_TWO_FINGERS_DRAG = 2;

    int touchMode = TOUCH_NONE;

    // autoscroll
    static final float MINIMAL_VELOCITY_FOR_AUTOSCROLL_START = 300.0f;
    static final int AUTOSCROLL_PERIOD_MS = 1000 / 25; // try 25 FPS, very hard to achieve :(
    static final int VELOCITY_PERIOD_MS = 1000;
    static final float MINIMAL_AUTOSCROLL_VELOCITY = 100.0f;
    VelocityTracker moveVelocity = VelocityTracker.obtain();
    boolean autoScrolling = false;
    Handler autoScrollHandler = new Handler();


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                firstFinger = new PointF(event.getX(), event.getY());
                touchMode = TOUCH_ONE_FINGER_DRAG;
                moveVelocity.clear();
                autoScrolling = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                touchMode = TOUCH_NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // second finger
                distBetweenFingers = getDistanceBetweenFingers(event);
                // the distance check is done to avoid false alarms
                if (distBetweenFingers > 5) {
                    touchMode = TOUCH_TWO_FINGERS_DRAG;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (touchMode == TOUCH_ONE_FINGER_DRAG) {
                    PointF oldFirstFinger = firstFinger;
                    firstFinger = new PointF(event.getX(), event.getY());
                    scroll(oldFirstFinger.x - firstFinger.x);
                    moveVelocity.addMovement(event);
                } else if (touchMode == TOUCH_TWO_FINGERS_DRAG) {
                    float oldDist = distBetweenFingers;
                    distBetweenFingers = getDistanceBetweenFingers(event);
                    zoom(oldDist / distBetweenFingers);
                }
                break;
            case MotionEvent.ACTION_UP:
                touchMode = TOUCH_NONE;
                moveVelocity.computeCurrentVelocity(VELOCITY_PERIOD_MS);

                if (UserPreferences.getPowerStatsPlotSmoothScrollingEnabled()) {
                    if (Math.abs(moveVelocity.getXVelocity()) > MINIMAL_VELOCITY_FOR_AUTOSCROLL_START) {
                        startAutoScroll(moveVelocity.getXVelocity());
                    }
                }
                break;

        }
        return true;
    }

    private float getDistanceBetweenFingers(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    private void zoom(float scale) {
        float domainSpan = rightBound - leftBound;
        float domainMidPoint = rightBound - domainSpan / 2.0f;
        float offset = domainSpan * scale / 2.0f;
        float newLeftBound = domainMidPoint - offset;
        float newRightBound = domainMidPoint + offset;

        // restrict zooming further
        if (newRightBound - newLeftBound < MIN_DOMAIN_BOUNDARIES_LENGTH) {
            return;
        }

        if (newLeftBound < leftBoundary) {
            newLeftBound = leftBoundary;
        }
        if (newRightBound > rightBoundary) {
            newRightBound = rightBoundary;
        }

        setDomainBoundaries(newLeftBound, newRightBound);
        redraw();
    }

    private void scroll(float pan) {
        float domainSpan = rightBound - leftBound;
        float step = domainSpan / getWidth();
        float offset = pan * step;

        float newLeftBound = leftBound + offset;
        float newRightBound = rightBound + offset;

        if (newLeftBound < leftBoundary) {
            newLeftBound = leftBoundary;
            newRightBound = newLeftBound + domainSpan;
        } else if (newRightBound > rightBoundary) {
            newRightBound = rightBoundary;
            newLeftBound = rightBoundary - domainSpan;
        }
        setDomainBoundaries(newLeftBound, newRightBound);
        redraw();
    }

    private void startAutoScroll(final float velocity) {
        autoScrolling = true;

        autoScrollHandler.postDelayed(new Runnable() {
            final float relativeVelocity = velocity * (rightBound - leftBound) / (1000 * getWidth());
            float autoScrollVelocity = 0.01f * relativeVelocity;
            float leftBoundBeforeScroll = leftBound;
            float rightBoundBeforeScroll = rightBound;

            float AUTOSCROLL_SLOWDOWN_FACTOR = 0.8f;

            @Override
            public void run() {
                if (!autoScrolling) {
                    return;
                }

                autoScrollVelocity *= AUTOSCROLL_SLOWDOWN_FACTOR;
                if (Math.abs(autoScrollVelocity) > MINIMAL_AUTOSCROLL_VELOCITY) {
                    scroll(-autoScrollVelocity);
                }

                if (leftBoundBeforeScroll != leftBound || rightBoundBeforeScroll != rightBound) {
                    leftBoundBeforeScroll = leftBound;
                    rightBoundBeforeScroll = rightBound;
                    autoScrollHandler.postDelayed(this, AUTOSCROLL_PERIOD_MS);
                }
            }
        }, 0);


    }

    private void setDomainBoundaries(float leftBound, float rightBound) {
        this.leftBound = leftBound;
        this.rightBound = rightBound;
        stepLength = (long)((rightBound - leftBound) / (DOMAIN_STEPS_COUNT - 1));
        super.setDomainBoundaries(leftBound, rightBound, BoundaryMode.FIXED);
    }

}

abstract class StatRenderer {
    PowerStatsPlot plot;
    float rangeValue = 0;

    public StatRenderer(PowerStatsPlot plot) {
        this.plot = plot;
    }

    public float getRangeValue() {
        return rangeValue;
    }

    public void setRangeValue(float rangeValue) {
        this.rangeValue = rangeValue;
    }

    public abstract String getLabel();
    public abstract void render(ArrayList<PowerRecord> records);
}


abstract class OnOffStatRenderer extends StatRenderer {
    public OnOffStatRenderer (PowerStatsPlot plot) {
        super(plot);
    }


    LineAndPointFormatter getFormatter() {
        LineAndPointFormatter formatter = new LineAndPointFormatter(
                getColorResourceId(), null, null, null);

        Paint linePaint = formatter.getLinePaint();
        linePaint.setStrokeWidth(plot.getContext().getResources().getDimension(R.dimen.plot_line_width));
        linePaint.setPathEffect(new CornerPathEffect(10));
        return formatter;
    }

    public void render(ArrayList<PowerRecord> records) {
        LineAndPointFormatter formatter = getFormatter();

        // DATA
        ArrayList<Number> values = new ArrayList<Number>();
        for (PowerRecord r: records) {
            values.add(r.getTimestamp());
            if (isOn(r)) {
                values.add(getRangeValue());
            } else {
                values.add(null);
            }
        }

        SimpleXYSeries series = new SimpleXYSeries(getLabel());
        series.setModel(values, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
        plot.addSeries(series, formatter);
    }

    abstract boolean isOn(PowerRecord r);
    abstract int getColorResourceId();
}


final class WifiRenderer extends OnOffStatRenderer {
    public WifiRenderer(PowerStatsPlot plot) {
        super(plot);
    }

    public String getLabel() {
        return "WiFi";
    }

    @Override
    boolean isOn(PowerRecord r) {
        return r.getWifiState() == PowerRecord.WIFI_STATE_ENABLED;
    }

    @Override
    int getColorResourceId() {
        return R.color.plot_wifi_enabled;
    }
}

final class GpsRenderer extends OnOffStatRenderer {
    public GpsRenderer(PowerStatsPlot plot) {
        super(plot);
    }

    public String getLabel() {
        return "GPS";
    }

    @Override
    boolean isOn(PowerRecord r) {
        return r.getGpsState() == PowerRecord.GPS_STATE_ON;
    }

    @Override
    int getColorResourceId() {
        return R.color.plot_gps_on;
    }
}

final class PhoneServiceRenderer extends OnOffStatRenderer {
    public PhoneServiceRenderer(PowerStatsPlot plot) {
        super(plot);
    }

    public String getLabel() {
        return "Mobile";
    }

    @Override
    boolean isOn(PowerRecord r) {
        return r.getPhoneServiceState() == PowerRecord.PHONE_SERVICE_POWER_ON;
    }

    @Override
    int getColorResourceId() {
        return R.color.plot_phone_service_on;
    }
}

final class ScreenStateRenderer extends OnOffStatRenderer {
    public ScreenStateRenderer(PowerStatsPlot plot) {
        super(plot);
    }

    public String getLabel() {
        return "Screen On/Off";
    }

    @Override
    boolean isOn(PowerRecord r) {
        return r.getScreenState() == PowerRecord.SCREEN_ON;
    }

    @Override
    int getColorResourceId() {
        return R.color.plot_screen_on;
    }
}
