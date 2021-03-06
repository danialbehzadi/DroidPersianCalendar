package com.byagowi.persiancalendar.view.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.byagowi.persiancalendar.CompassListener;
import com.byagowi.persiancalendar.R;
import com.byagowi.persiancalendar.Utils;
import com.byagowi.persiancalendar.view.QiblaCompassView;
import com.github.praytimes.Coordinate;

/**
 * Compass/Qibla activity
 *
 * @author ebraminio
 */
public class CompassFragment extends Fragment {
    public QiblaCompassView compassView;
    SensorManager sensorManager;
    Sensor sensor;
    SensorEventListener compassListener;
    private Utils utils = Utils.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_compass, container, false);

        Context context = getContext();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String location = prefs.getString("Location", "CUSTOM");
        if (location.equals("CUSTOM")) {
            utils.setActivityTitleAndSubtitle(getActivity(), getString(R.string.compass), "");
        } else {
            Utils.City city = utils.getCityByKey(location, context);
            utils.setActivityTitleAndSubtitle(getActivity(), getString(R.string.qibla_compass),
                    prefs.getString("AppLanguage", "fa").equals("en")
                            ? city.en
                            : city.fa);
        }


        compassListener = new CompassListener(this);
        compassView = (QiblaCompassView) view.findViewById(R.id.compass_view);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        compassView.setScreenResolution(width, height - 2 * height / 8);

        Coordinate coordinate = utils.getCoordinate(getContext());
        if (coordinate != null) {
            compassView.setLongitude(coordinate.getLongitude());
            compassView.setLatitude(coordinate.getLatitude());
            compassView.initCompassView();
            compassView.invalidate();
        }

        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (sensor != null) {
            sensorManager.registerListener(compassListener, sensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            utils.quickToast(utils.textShaper(getString(R.string.compass_not_found)), getContext());
        }
        return view;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensor != null) {
            sensorManager.unregisterListener(compassListener);
        }
    }
}
