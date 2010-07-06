package com.appspot.estadodeltransito.activities;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.appspot.estadodeltransito.R;
import com.appspot.estadodeltransito.Subway;
import com.appspot.estadodeltransito.SubwayLine;
import com.appspot.estadodeltransito.SubwayStation;
import com.appspot.estadodeltransito.mapoverlays.DefaultOverlay;
import com.appspot.estadodeltransito.mapoverlays.LineItemizedOverlay;
import com.appspot.estadodeltransito.mapoverlays.SubwayOverlayItem;
import com.appspot.estadodeltransito.mapoverlays.SubwaysItemizedOverlay;
import com.appspot.estadodeltransito.util.IconsUtil;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.gson.Gson;

public class MapActivity extends com.google.android.maps.MapActivity {

	private static final String TAG = "MapActivity";
	public static final String SHOW_SUBWAY_ACTION = "ShowSubwayLine";


	private Subway subwayLine = null;
	private SubwayLine [] subwayLines = null;
	private List<SubwaysItemizedOverlay> subwayLinesOverlays;
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.map);
	    MapView mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    MapController mc = mapView.getController();

	    try {
	    	// Load subway line information
			loadSubwayLines();
		} catch (IOException e) {
			Log.e(TAG, "This shouldn't happen");
			return;
		}
		// Load subway line overlays
	    loadSubwayOverlays();
	    
	    List<LineItemizedOverlay<? extends OverlayItem>> mapOverlays = new LinkedList<LineItemizedOverlay<? extends OverlayItem>>();
		if ( subwayLines.length != 0 ) {
			Intent i = getIntent();
			if ( SHOW_SUBWAY_ACTION.equals(i.getAction()) ){
				//Show that line only
				subwayLine = (Subway) i.getExtras().get("subway_line");
				addLineOverlay(mapOverlays);
			}
			else
				addAllLinesOverlays(mapOverlays);	
			
			/* Set the zoomToSpan an center it */
			if ( ! mapOverlays.isEmpty() ){
				SubwaysItemizedOverlay lineOverlay = (SubwaysItemizedOverlay) mapOverlays.get(0);
				SubwayOverlayItem firstStation = lineOverlay.getItem(0);
				SubwayOverlayItem lastStation = lineOverlay.getItem(lineOverlay.size()-1);
				int halfLat = ( firstStation.getPoint().getLatitudeE6() + lastStation.getPoint().getLatitudeE6() ) / 2;
				int halfLong = ( firstStation.getPoint().getLongitudeE6() + firstStation.getPoint().getLongitudeE6() ) / 2;
				mc.zoomToSpan(halfLat,halfLong);
				mc.setCenter(new GeoPoint(halfLat,halfLong));
			}
    		
		}
		
		if ( mapOverlays.isEmpty() )
			return;
		
		List<Overlay> overlays = mapView.getOverlays();
		overlays.add(new DefaultOverlay(this));
		for(LineItemizedOverlay<? extends OverlayItem> lio:mapOverlays){
			overlays.add(lio.getLineOverlay());
		}
		overlays.addAll(mapOverlays);
	}

	private void loadSubwayLines() throws IOException {
		if ( subwayLines == null ){
			InputStream subwayJSON = this.getResources().openRawResource(R.raw.subwaygeo);
			subwayLines = readSubwayInfo(subwayJSON);
		}
	}

	private void loadSubwayOverlays() {
		if ( subwayLinesOverlays == null ){
			subwayLinesOverlays = new ArrayList<SubwaysItemizedOverlay>();
			for (SubwayLine sl:subwayLines){
				String lineName = sl.getName();
				Drawable lineIcon = this.getResources().getDrawable(IconsUtil.getLineIconResource(lineName));
				int colorRGB = IconsUtil.getLineColor(lineName);
				List<SubwayStation> lineStations = sl.getStations();
		
				if ( !lineStations.isEmpty() ){
					SubwaysItemizedOverlay lineOverlay = new SubwaysItemizedOverlay(lineIcon, this, colorRGB, sl);
					for(SubwayStation ss:lineStations){
						lineOverlay.addStation(ss);
					}
					subwayLinesOverlays.add(lineOverlay);
				}
			}	
		}
	}


	private void addAllLinesOverlays(List<LineItemizedOverlay<? extends OverlayItem>> mapOverlays) {
		subwayLine = null;
		for (SubwaysItemizedOverlay lineOverlay:subwayLinesOverlays){
				mapOverlays.add(lineOverlay);
		}
	}

	private void addLineOverlay(List<LineItemizedOverlay<? extends OverlayItem>> mapOverlays) {
		for (SubwaysItemizedOverlay lineOverlay:subwayLinesOverlays){
			if ( subwayLine.getName().equals(lineOverlay.getLineName()) )
				mapOverlays.add(lineOverlay);
		}
	}
	private static SubwayLine[] readSubwayInfo(InputStream subwayJSON) throws IOException {
		byte [] data = new byte[subwayJSON.available()];
		
		while( subwayJSON.read(data) != -1);
		String json = new String(data);

		Gson gson = new Gson();
		SubwayLine[] lines = gson.fromJson(json, SubwayLine[].class);
		return lines;
	}
	
}