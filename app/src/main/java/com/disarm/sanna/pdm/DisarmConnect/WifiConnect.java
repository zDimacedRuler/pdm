package com.disarm.sanna.pdm.DisarmConnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by hridoy on 21/8/16.
 */
public class WifiConnect implements Runnable {
    private android.os.Handler handler;
    private Context context;
    private FileReader fr= null;
    private BufferedReader br = null;
    public int minDBLevel = 2;
    public WifiConnect(android.os.Handler handler, Context context) {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }

    @Override
    public void run() {
        List<ScanResult> allScanResults = MyService.wifi.getScanResults();
        Log.v(MyService.TAG2,"Running Autoconnector");
        MyService.wifiInfo = MyService.wifi.getConnectionInfo();
        String ssidName = MyService.wifiInfo.getSSID();
        Log.v(MyService.TAG2, ssidName);
        if(ssidName.contains("DisarmHotspotDB")) {
            Log.v(MyService.TAG2,"Already Connected DB ");
            Logger.addRecordToLog("Already DB Connected");

        }
        else if(ssidName.contains("DH-")) {
            Log.v(MyService.TAG2,"Already Connected");
            Logger.addRecordToLog("Already DH Connected");
            try {

                fr = new FileReader("/proc/net/arp");
                br = new BufferedReader(fr);
                String line;
                MyService.IpAddr = new ArrayList<String>();
                MyService.c = false;
                while ((line = br.readLine()) != null) {
                    String[] splitted = line.split(" +");
                    Log.v("Splitted:" , Arrays.deepToString(splitted));
                }
            }
            catch(Exception e)
            {}
        }

        else if(!ssidName.equals("<unknown ssid>")){
            Log.v(MyService.TAG2,"Checking For Disarm Hotspot");
            // Connecting to DisarmHotspot WIfi on Button Click

            if (allScanResults.toString().contains("DisarmHotspotDB")) {
                int level = findDBSignalLevel(allScanResults);
                if (level > minDBLevel)
                {
                    Log.v(MyService.TAG2, "Connecting DisarmDB");

                    String ssid = "DisarmHotspotDB";
                    WifiConfiguration wc = new WifiConfiguration();
                    wc.SSID = "\"" + ssid + "\""; //IMPORTANT! This should be in Quotes!!
                    wc.preSharedKey = "\""+ MyService.dbPass +"\"";
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                    int res = MyService.wifi.addNetwork(wc);

                    boolean b = MyService.wifi.enableNetwork(res, true);
                    Log.v(MyService.TAG2, "Connected");

                    Logger.addRecordToLog("DB Connected Successfully");
                }
            }
            else if (allScanResults.toString().contains("DH-")) {
                // Store all DH available in allDHAvailable
                Map allDHAvailable = new HashMap<String, Integer>();

                // Put all found DH to allDHAvailable Map
                for (ScanResult scanResult : allScanResults) {
                    if(scanResult.SSID.toString().contains("DH-")) {
                        allDHAvailable.put(scanResult.SSID,scanResult.level);
                    }
                }

                Log.v("AllDH Available:",Arrays.asList(allDHAvailable).toString());
                Logger.addRecordToLog("All DH available:" + Arrays.asList(allDHAvailable).toString());
                // Find key with the maximum value from allDHAvailable
                String bestFoundSSID="";
                int maxValueInMap = 0;
                try {
                    maxValueInMap = (int) Collections.max(allDHAvailable.values());  // This will return max value in the Hashmap
                    Iterator it = allDHAvailable.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Integer> pair = (Map.Entry) it.next();
                        if (pair.getValue() == maxValueInMap) {
                            Log.v("Best Found SSID:", pair.getKey());     // Print the key with max value
                            Logger.addRecordToLog("Best Found SSID"+ ',' + pair.getKey());
                            bestFoundSSID = pair.getKey().toString();
                        }
                    }
                }
                catch (Exception e)
                {}
                // Connect to the best found network
                String pass = "password123";
                WifiConfiguration wc = new WifiConfiguration();
                wc.SSID = "\"" + bestFoundSSID + "\""; //IMPORTANT! This should be in Quotes!!
                wc.preSharedKey = "\""+ pass +"\"";
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                //wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                int res = MyService.wifi.addNetwork(wc);
                boolean b = MyService.wifi.enableNetwork(res, true);
                Log.v(MyService.TAG2, "Connected");

                Logger.addRecordToLog("DH Connected Successfully," + bestFoundSSID);
            }
            else{
                Log.v(MyService.TAG2,"Disarm Not Available");

                Logger.addRecordToLog("no DH/DB network available");

            }

        }
        handler.postDelayed(this,10000);
    }
    public int findDBSignalLevel(List<ScanResult> allScanResults)
    {
        for (ScanResult scanResult : allScanResults) {
            if(scanResult.SSID.toString().equals(MyService.dbAPName)) {
                Log.v("SSID:",scanResult.SSID.toString());
                int level =  WifiManager.calculateSignalLevel(scanResult.level, 5);
                return level;
            }
        }
        return 0;
    }
}
