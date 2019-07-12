/****************************************************************************
 Copyright (c) 2014 Chukong Technologies Inc.

 http://www.cocos2d-x.org

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 ****************************************************************************/
 
package org.cocos2dx.plugin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.aladinfun.nativeutil.BaseEntryActivity;
import com.facebook.AccessToken;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;

public class UserFacebook implements InterfaceUser{

    private static Activity mContext = null;
    private static InterfaceUser mAdapter = null;
    private static boolean bDebug = true;
    private final static String LOG_TAG = "UserFacebook";
    private static final List<String> allPublishPermissions = Arrays.asList(
            "publish_actions", "ads_management", "create_event", "rsvp_event",
            "manage_friendlists", "manage_notifications", "manage_pages");
    static String userIdStr = "";
    protected static void LogE(String msg, Exception e) {
        Log.e(LOG_TAG, msg, e);
        e.printStackTrace();
    }

    protected static void LogD(String msg) {
        if (bDebug) {
            Log.d(LOG_TAG, msg);
        }
    }
    
    public String getUserID(){
		return userIdStr;
	}
    
    public UserFacebook(Context context) {
        mContext = (Activity)context;
        mAdapter = this;
        FacebookWrapper.userFacebook = this;
    }

    @Override
    public void configDeveloperInfo(Hashtable<String, String> cpInfo) {
        LogD("not supported in Facebook pluign");
    }

    List<String> requestedReadPermissions = new ArrayList<String>();
    List<String> requestedPublishPermissions = new ArrayList<String>();

    @Override
    public void login() {
    	Log.d(LOG_TAG, "login()");
    	requestedReadPermissions.clear();
        requestedPublishPermissions.clear();
    	if(isLogined()) {
    		UserWrapper.onActionResult(this, UserWrapper.ACTION_RET_LOGIN_SUCCEED, FacebookWrapper.getAccessTokenMessage(AccessToken.getCurrentAccessToken()));
    	} else {
    		FacebookWrapper.isLoginRequested = true;
    		LoginManager.getInstance().logInWithReadPermissions(mContext, Arrays.asList("user_friends"));
    	}
    }
    
    public void login(final String permissions){
    	Log.d(LOG_TAG, "login() " + permissions);
    	requestedReadPermissions.clear();
        requestedPublishPermissions.clear();
        String[] permissionArray = permissions.split(",");
        for (int i = 0; i < permissionArray.length; i++) {
            String permission = permissionArray[i];
            if (allPublishPermissions.contains(permission)) {
            	requestedPublishPermissions.add(permission);
            } else {
            	requestedReadPermissions.add(permission);
            }
        }
        FacebookWrapper.isLoginRequested = true;
        if(!FacebookWrapper.requestUserPermissions()) {
			UserWrapper.onActionResult(this, UserWrapper.ACTION_RET_LOGIN_SUCCEED, FacebookWrapper.getAccessTokenMessage(AccessToken.getCurrentAccessToken()));
		}
    }

    @Override
    public void logout() {
    	Log.d(LOG_TAG, "logout()");
    	LoginManager.getInstance().logOut();
    	FacebookWrapper.isLoggedIn = false;
    }

    @Override
    public boolean isLogined() {
    	return FacebookWrapper.isFacebookLogined();
    }

    public boolean isLoggedIn() {
    	return isLogined();
    }
    
    @Override
    public String getSessionID() {
    	AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null ? accessToken.getToken() : "";
    }

    @Override
    public void setDebugMode(boolean debug) {
        bDebug = debug;     
    }

    @Override
    public String getPluginVersion() {
        return "4.9.0";
    }

    @Override
    public String getSDKVersion(){
        return FacebookSdk.getSdkVersion();
    }

    public void setSDKVersion(String version){
        //Settings.setSDKVersion(version);
    }
    
    public String getAccessToken(){
    	AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken == null ? "" : accessToken.getToken() == null ? "" : accessToken.getToken();
    }
    
    public String getPermissionList(){
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("{\"permissions\":[");
    	AccessToken accessToken = AccessToken.getCurrentAccessToken();
    	if(accessToken != null) {
    		Set<String> permissions = accessToken.getPermissions();
    		if(permissions != null && !permissions.isEmpty()) {
    			Iterator<String> it = permissions.iterator();
    			boolean isFirst = true;
    			while(it.hasNext()) {
    				if(isFirst) {
    					isFirst = false;
    				} else {
    					buffer.append(",");
    				}
    				buffer.append("\"")
    					.append(it.next())
    					.append("\"");
    			}
    		}
    	}
		buffer.append("]}");
    	return buffer.toString();
    }
    
    public void request(final JSONObject info /*String path, int method, JSONObject params, int nativeCallback*/ ){
        PluginWrapper.runOnMainThread(new Runnable(){
            @Override
            public void run() {
                try {
                    String path = info.getString("Param1");
                    
                    int method = info.getInt("Param2");
                    HttpMethod httpMethod = HttpMethod.values()[method];
                    
                    JSONObject jsonParameters = info.getJSONObject("Param3");
                    Bundle parameter = new Bundle();
                    Iterator<?> it = jsonParameters.keys();
                    while(it.hasNext()){
                        String key = it.next().toString();
                        String value = jsonParameters.getString(key);
                        parameter.putString(key, value);
                    }
                    
                    final int nativeCallback = info.getInt("Param4");
                    
                    GraphRequest request = GraphRequest.newGraphPathRequest(AccessToken.getCurrentAccessToken(), path, new GraphRequest.Callback() {
						@Override
						public void onCompleted(GraphResponse response) {
							FacebookRequestError error = response.getError();
							BaseEntryActivity act = (BaseEntryActivity) BaseEntryActivity.getContext();
							if(error == null){
								final String data = response.getJSONObject().toString();
								if(act != null) {
									act.runOnGLThread(new Runnable() {
										@Override
										public void run() {
											nativeRequestCallback(0, data, nativeCallback);
										}
									});
								}
							} else {
								final int errCode = error.getErrorCode();
								final String msg = "{\"error_message\":\""+error.getErrorMessage()+"\"}";
								act.runOnGLThread(new Runnable() {
									@Override
									public void run() {
										nativeRequestCallback(errCode, msg, nativeCallback);
									}
								});
							}
						}
					});
                    request.setHttpMethod(httpMethod);
                    request.setParameters(parameter);
                    request.executeAsync();
                } catch (JSONException e) {
                    Log.d(LOG_TAG, e.getMessage(), e);
                }
            }
            
        });
                
    }
    
    public void activateApp(){
   	    // AppEventsLogger.activateApp(mContext);
//    	com.facebook.Settings.publishInstallAsync(mContext, Settings.getApplicationId());
    }    
    
    public void logEvent(String eventName){
    	FacebookWrapper.getAppEventsLogger().logEvent(eventName);
    }
    
    public void logEvent(JSONObject info){
    	int length = info.length();
    	if(3 == length){
    		try {
    			String eventName = info.getString("Param1");
    			Double valueToSum = info.getDouble("Param2");
    			
    			JSONObject params = info.getJSONObject("Param3");
    			Iterator<?> keys = params.keys();
    			Bundle bundle = new Bundle();
    			while(keys.hasNext()){
    				String key = keys.next().toString();
    				bundle.putString(key, params.getString(key));
    			}
    			
    			FacebookWrapper.getAppEventsLogger().logEvent(eventName, valueToSum, bundle);
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    	}else if(2 == length){
    		try {
    			String eventName = info.getString("Param1");
				Double valueToSum = info.getDouble("Param2");
				FacebookWrapper.getAppEventsLogger().logEvent(eventName, valueToSum);
			} catch (JSONException e) {
				try {
					String eventName = info.getString("Param1");
					JSONObject params = info.getJSONObject("Param2");
	    			Iterator<?> keys = params.keys();
	    			Bundle bundle = new Bundle();
	    			while(keys.hasNext()){
	    				String key = keys.next().toString();
	    				bundle.putString(key, params.getString(key));
	    			}
	    			FacebookWrapper.getAppEventsLogger().logEvent(eventName, bundle);
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			}
    	}
    	
    }
    
    public void logPurchase(JSONObject info)
    {
    	int length = info.length();
    	if(3 == length){
    		try {
    			Double purchaseNum = info.getDouble("Param1");
    			String currency= info.getString("Param2");
    			
    			JSONObject params = info.getJSONObject("Param3");
    			Iterator<?> keys = params.keys();
    			Bundle bundle = new Bundle();
    			while(keys.hasNext()){
    				String key = keys.next().toString();
    				bundle.putString(key, params.getString(key));
    			}
    			Currency currencyStr = null;
    			try {
    				currencyStr = Currency.getInstance(currency);
				} catch (IllegalArgumentException e) {
					currencyStr = Currency.getInstance(Locale.getDefault());
					e.printStackTrace();
				}
    			
    			FacebookWrapper.getAppEventsLogger().logPurchase(new BigDecimal(purchaseNum), currencyStr, bundle);
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    	}else if(2 == length){
    		try {
    			Double purchaseNum = info.getDouble("Param1");
    			String  currency= info.getString("Param2");
				FacebookWrapper.getAppEventsLogger().logPurchase(new BigDecimal(purchaseNum), Currency.getInstance(currency));
	    	} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    }
    
    private native void nativeRequestCallback(int ret, String msg,int cbIndex);
}
