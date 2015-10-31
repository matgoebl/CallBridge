package com.github.abusalam.callbridge;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.net.SocketException;
import java.io.IOException;
import java.util.Map;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
//import android.net.DhcpInfo;
import android.net.Uri;
//import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;


public class BridgeConfig extends Activity {
    private CallListener CallListener; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bridge_config);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
        
        CallListener = new CallListener();
        try {
        	CallListener.start();
        } catch(IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
        Log.w("Httpd", "Web server initialized.");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
    		setContentView(R.layout.activity_bridge_config);
    		TextView tv = (TextView)findViewById(R.id.fullscreen_content);
    		String ipAddress=getLocalIpAddress();
    		if(ipAddress==null){
    			tv.setText(R.string.msg_no_network);
    		}else{
    			tv.setText("http://"+ipAddress+":8080/?cellNo=[PhoneNo]");
    		}
            return false;
        }
    };

    /**
     * Obtain Local IP Address
     */
    public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
					String ipv4;
					if (!inetAddress.isLoopbackAddress()
	                		&& InetAddressUtils.isIPv4Address(ipv4 = inetAddress.getHostAddress())) {
	                    return ipv4;
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e("WebServer", ex.toString());
	    }
	    return null;
	}
    
    /**
     * Start WebServer and listen for call request
     */
    public class CallListener extends NanoHTTPD {
        public CallListener() {
            super(8020);
            ServerRunner.run(CallListener.class);
        }

        @Override public Response serve(IHTTPSession session) {
            Method method = session.getMethod();
            String uri = session.getUri();
            String query = session.getQueryParameterString();
            Log.i("WebIntent", method + " " + uri + (query != null ? "?" + query : ""));

            String msg = "unknown path";
            if (uri.equals("/")) {
                msg =
                        "<html><body><h1>Android Web Intent Bridge</h1><form action='/intent' method='get'>\n" +
                                "  <li>Call Number: <input type='text' name='callNo'></li>\n" +
                                "  <li>Open Web Page: <input type='text' name='openUrl'></li>\n" +
                                "  <li>Launch Activity: <input type='text' name='launchActivity'></li>\n" +
                                "<input type='submit' value='Start!'></form></body></html>\n";
            }

            if (uri.equals("/intent")) {
                msg = "no intent";
                Intent intent = null;
                Map<String, String> parms = session.getParms();
                String callNo = parms.get("callNo");
                String openUrl = parms.get("openUrl");
                String launchActivity = parms.get("launchActivity");
                if (callNo != null && callNo.length() > 0) {
                    intent = new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+callNo));
                }
                if (openUrl != null && openUrl.length() > 0) {
                    intent = new Intent(Intent.ACTION_VIEW,Uri.parse(""+openUrl));
                }
                if (launchActivity != null && launchActivity.length() > 0) {
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setComponent(ComponentName.unflattenFromString(launchActivity));
                }
                if(intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    msg = "started: " + intent.toString();
                }
            }
            return new NanoHTTPD.Response(msg);
        }
    }
}
