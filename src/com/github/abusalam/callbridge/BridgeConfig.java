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
            super(8080);
            ServerRunner.run(CallListener.class);
        }

        @Override public Response serve(IHTTPSession session) {
            Method method = session.getMethod();
            String uri = session.getUri();
            System.out.println(method + " '" + uri + "' ");

            String msg = "<html><body>";
            Map<String, String> parms = session.getParms();
            if (parms.get("cellNo") == null)
                msg +=
                        "<form action='?' method='get'>\n" +
                                "  <p>Mobile No: <input type='text' name='cellNo'></p>\n" +
                                "</form>\n";
            else
            {
                msg += "<p>Calling: " + parms.get("cellNo") + "</p>";
            }
            msg += "</body></html>\n";
            Intent callIntent = new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+parms.get("cellNo")));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);
            return new NanoHTTPD.Response(msg);
        }
    }

}
