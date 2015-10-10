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

import org.json.JSONException;
import org.json.JSONObject;
import org.cocos2dx.lib.Cocos2dxActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AppEventsLogger;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.Session.OpenRequest;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;

public class UserFacebook implements InterfaceUser{

    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    private static Activity mContext = null;
    private static InterfaceUser mAdapter = null;
    private static Session session = null;
    private static boolean bDebug = true;
    private static boolean isLoggedIn = false;
    private static boolean loginRequested = false;
    private final static String LOG_TAG = "UserFacebook";
    private static final List<String> allPublishPermissions = Arrays.asList(
            "publish_actions", "ads_management", "create_event", "rsvp_event",
            "manage_friendlists", "manage_notifications", "manage_pages");
    private static String userIdStr = "";
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
        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        Settings.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
        Settings.addLoggingBehavior(LoggingBehavior.DEVELOPER_ERRORS);
        Settings.setIsLoggingEnabled(true);
        session = Session.getActiveSession();
        if (session == null) {
            session = new Session(context);
        
            Session.setActiveSession(session);
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                session.openForRead(new Session.OpenRequest((Activity) context).setCallback(statusCallback));
            }
        }
    }

    @Override
    public void configDeveloperInfo(Hashtable<String, String> cpInfo) {
        LogD("not supported in Facebook pluign");
    }

    private List<String> requestedReadPermissions = new ArrayList<String>();
    private List<String> requestedPublishPermissions = new ArrayList<String>();
    private int retryTimes = 5;

    @Override
    public void login() {
    	Log.d(LOG_TAG, "login()");
    	loginRequested = true;
        PluginWrapper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Session session = Session.getActiveSession();
                requestedReadPermissions.clear();
                requestedPublishPermissions.clear();
                if (!session.isOpened() && !session.isClosed()) {
					if(!session.isPermissionGranted("email") && !requestedReadPermissions.contains("email")) {
						requestedReadPermissions.add("email");
					}
					if(!session.isPermissionGranted("user_friends") && !requestedReadPermissions.contains("user_friends")) {
						requestedReadPermissions.add("user_friends");
					}
                    OpenRequest request = new Session.OpenRequest(mContext);
                    request.setCallback(statusCallback);
                    request.setPermissions(requestedReadPermissions);
                    Log.d(LOG_TAG, "login() openForRead");
                    retryTimes = 5;
                    session.openForRead(request);
                } else {
                	Log.d(LOG_TAG, "login() openActiveSession");
                    Session.openActiveSession(mContext, true, statusCallback);
                }
            } 
        });
    }
    
    public void login(final String permissions){
    	        
    	PluginWrapper.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "login with permissions:" + permissions);
                loginRequested = true;
                Session session = Session.getActiveSession();
                requestedReadPermissions.clear();
                requestedPublishPermissions.clear();
                retryTimes = 5;
                String[] permissionArray = permissions.split(",");
                boolean publishPermission = false;
                for (int i = 0; i < permissionArray.length; i++) {
                    String permission = permissionArray[i];
                    if (allPublishPermissions.contains(permission)) {
                        publishPermission = true;
                        if(!session.isPermissionGranted(permission)) {
                           requestedPublishPermissions.add(permission);
                        }
                    } else {
                       if(!session.isPermissionGranted(permission)) {
                           requestedReadPermissions.add(permission);
                       }
                    }
                }
                
                if(!session.isPermissionGranted("email") && !requestedReadPermissions.contains("email")) {
                   requestedReadPermissions.add("email");
                }
                if(!session.isPermissionGranted("user_friends") && !requestedReadPermissions.contains("user_friends")) {
                   requestedReadPermissions.add("user_friends");
                }
                if(session.isOpened()){
                	Log.d(LOG_TAG, "login(s) session is opened");
                	if(session.getPermissions().containsAll(Arrays.asList(permissionArray))){
                		LogD("login called when user is already connected");
                	}else{
                       if(!requestedReadPermissions.isEmpty()) {
                    	   Log.d(LOG_TAG, "login(s) req read permission->" + requestedReadPermissions.toString());
                           NewPermissionsRequest newPermissionsRequest = new NewPermissionsRequest(mContext, requestedReadPermissions);
                           newPermissionsRequest.setCallback(statusCallback);
                           session.requestNewReadPermissions(newPermissionsRequest);
                       } else if(!requestedPublishPermissions.isEmpty()){
                    	   Log.d(LOG_TAG, "login(s) req publish permission->" + requestedPublishPermissions.toString());
                           NewPermissionsRequest newPermissionsRequest = new NewPermissionsRequest(mContext, requestedPublishPermissions);
                           newPermissionsRequest.setCallback(statusCallback);
                           session.requestNewPublishPermissions(newPermissionsRequest);
                       } else {
                    	   Log.e(LOG_TAG, "login(s) requestedReadPermissions:" + requestedReadPermissions.toString() + " requestedPublishPermissions:" + requestedPublishPermissions.toString());
                       }
//                     NewPermissionsRequest newPermissionsRequest = new NewPermissionsRequest(mContext, Arrays.asList(permissionArray));
//                     newPermissionsRequest.setCallback(statusCallback);
//                     if(publishPermission)
//                         session.requestNewPublishPermissions(newPermissionsRequest);
//                     else
//                         session.requestNewReadPermissions(newPermissionsRequest);
                	}
                }else{
                	Log.d(LOG_TAG, "login(s) session is not opened");
                	if (!session.isClosed()) {
                        OpenRequest request = new Session.OpenRequest(mContext);
                        request.setCallback(statusCallback);
                        if(!requestedReadPermissions.isEmpty()) {
                        	Log.d(LOG_TAG, "login(s) open for read permission->" + requestedReadPermissions.toString());
                            request.setPermissions(requestedReadPermissions);
                            session.openForRead(request);
                        } else if(!requestedPublishPermissions.isEmpty()){
                        	Log.d(LOG_TAG, "login(s) open for publish permission->" + requestedPublishPermissions.toString());
                            request.setPermissions(requestedPublishPermissions);
                            session.openForPublish(request);
                        } else {
                        	Log.d(LOG_TAG, "login(s) open for read");
                            session.openForRead(request);
                        }
                    } else {
                    	Log.d(LOG_TAG, "login(s) openActiveSession");
                        Session.openActiveSession(mContext, true, statusCallback);
                    }
                }
                
            } 
        });
    }

    @Override
    public void logout() {
        Session session = Session.getActiveSession();
        if (!session.isClosed()) {
        	session.removeCallback(statusCallback);
            session.closeAndClearTokenInformation();
            isLoggedIn = false;
            session = new Session(mContext);
            session.addCallback(statusCallback);
            Session.setActiveSession(session);
        }
    }

    @Override
    public boolean isLogined() {
    	Session session = Session.getActiveSession();
        return isLoggedIn && session != null && session.isPermissionGranted("email") && session.isPermissionGranted("user_friends");
    }

    public boolean isLoggedIn() {
    	Session session = Session.getActiveSession();
        return isLoggedIn && session != null && session.isPermissionGranted("email") && session.isPermissionGranted("user_friends");
    }
    
    @Override
    public String getSessionID() {
        return null;
    }

    @Override
    public void setDebugMode(boolean debug) {
        bDebug = debug;     
    }

    @Override
    public String getPluginVersion() {
        return "0.2.0";
    }

    @Override
    public String getSDKVersion(){
        return Settings.getSdkVersion();
    }

    public void setSDKVersion(String version){
        //Settings.setSDKVersion(version);
    }
    
    public String getAccessToken(){
        return Session.getActiveSession().getAccessToken();
    }
    
    public String getPermissionList(){
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("{\"permissions\":[");
		List<String> list = Session.getActiveSession().getPermissions();
		for(int i = 0; i < list.size(); ++i){
			buffer.append("\"")
					.append(list.get(i))
					.append("\"");
			if(i != list.size() - 1)
				buffer.append(",");
		}
    	//    	.append(Session.getActiveSession().getPermissions().toString())
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
                    HttpMethod httpmethod = HttpMethod.values()[method];
                    
                    JSONObject jsonParameters = info.getJSONObject("Param3");
                    Bundle parameter = new Bundle();
                    Iterator<?> it = jsonParameters.keys();
                    while(it.hasNext()){
                        String key = it.next().toString();
                        String value = jsonParameters.getString(key);
                        parameter.putString(key, value);
                    }
                    
                    final int nativeCallback = info.getInt("Param4");
                    
                    Request request = new Request(Session.getActiveSession(), path, parameter, httpmethod, new Request.Callback() {
                        
                        @Override
                        public void onCompleted(Response response) {
                            LogD(response.toString());
                            
                            FacebookRequestError error = response.getError();
                            Cocos2dxActivity act = (Cocos2dxActivity) Cocos2dxActivity.getContext();
                            if(error == null){
                               final String data = response.getGraphObject().getInnerJSONObject().toString();
                               act.runOnGLThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       nativeRequestCallback(0, data, nativeCallback);
                                   }
                               });
                            }else{
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
                    request.executeAsync();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            
        });
                
    }
    
    public void activateApp(){
   	    AppEventsLogger.activateApp(mContext);
    	// com.facebook.Settings.publishInstallAsync(mContext, Settings.getApplicationId());
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
    
    
    private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
        	if(session == null || session != Session.getActiveSession()) {
        		if(session == null) {
        			Log.d(LOG_TAG, "SessionStatusCallback: session is null");
        		} else if(session != Session.getActiveSession()) {
        			Log.d(LOG_TAG, "SessionStatusCallback: old session");
        		}
        		return;
        	}
        	onSessionStateChange(session, state, exception);
        	Iterator<String> it = requestedReadPermissions.iterator();
            while(it.hasNext()) {
                String permission = it.next();
                if(session.isPermissionGranted(permission)) {
                	Log.d(LOG_TAG, "SessionStatusCallback: read permission granted -> " + permission);
                	it.remove();
                }
            }
            it = requestedPublishPermissions.iterator();
            while(it.hasNext()) {
                String permission = it.next();
                if(session.isPermissionGranted(permission)) {
                	Log.d(LOG_TAG, "SessionStatusCallback: publish permission granted -> " + permission);
                	it.remove();
                }
            }
            if(false == isLoggedIn){
                if(SessionState.OPENED == state){
                   if(requestedReadPermissions.isEmpty() && requestedPublishPermissions.isEmpty()) {
                	   Log.d(LOG_TAG, "SessionStatusCallback: login success");
                       isLoggedIn = true;
                       if(loginRequested) {
                    	   loginRequested = false;
                    	   UserWrapper.onActionResult(mAdapter, UserWrapper.ACTION_RET_LOGIN_SUCCEED, getSessionMessage(session));
                       }
                   } else if(loginRequested) {
                	   if(retryTimes > 0) {
	                	   if(!requestedReadPermissions.isEmpty()) {
		                	   Log.d(LOG_TAG, "SessionStatusCallback: req read permission->" + requestedReadPermissions.toString());
		                       NewPermissionsRequest newPermissionsRequest = new NewPermissionsRequest(mContext, requestedReadPermissions);
		                       newPermissionsRequest.setCallback(this);
		                       session.requestNewReadPermissions(newPermissionsRequest);
		                       retryTimes--;
		                   } else if(!requestedPublishPermissions.isEmpty()) {
		                	   Log.d(LOG_TAG, "SessionStatusCallback: req publish permission->" + requestedPublishPermissions.toString());
		                       NewPermissionsRequest newPermissionsRequest = new NewPermissionsRequest(mContext, requestedPublishPermissions);
		                       newPermissionsRequest.setCallback(this);
		                       session.requestNewPublishPermissions(newPermissionsRequest);
		                       retryTimes--;
		                   }
                	   } else if(!requestedReadPermissions.isEmpty() || !requestedPublishPermissions.isEmpty()) {
                		   loginRequested = false;
                     	   UserWrapper.onActionResult(mAdapter, UserWrapper.ACTION_RET_LOGIN_FAILED, "login failed");
                	   }
                   }
                }else if(SessionState.CLOSED_LOGIN_FAILED == state /*|| SessionState.CLOSED == state*/){      
                	Log.d(LOG_TAG, "SessionStatusCallback: login failed");
                	requestedPublishPermissions.clear();
                	requestedReadPermissions.clear();
                	if(loginRequested) {
                 	   loginRequested = false;
                 	   UserWrapper.onActionResult(mAdapter, UserWrapper.ACTION_RET_LOGIN_FAILED, getErrorMessage(exception, "login failed"));
                	}
                }
            }
            else{
                if(SessionState.OPENED_TOKEN_UPDATED == state){
                	if(requestedReadPermissions.isEmpty() && requestedPublishPermissions.isEmpty()) {
                 	   Log.d(LOG_TAG, "SessionStatusCallback(OPENED_TOKEN_UPDATED): login success");
                 	   if(loginRequested) {
                 		   loginRequested = false;
                 		   UserWrapper.onActionResult(mAdapter, UserWrapper.ACTION_RET_LOGIN_SUCCEED, getSessionMessage(session));
                 	   }
                    } else if(loginRequested) {
                    	if(retryTimes > 0) {
	                    	if(!requestedReadPermissions.isEmpty()) {
		                 	    Log.d(LOG_TAG, "SessionStatusCallback(OPENED_TOKEN_UPDATED): req read permission->" + requestedReadPermissions.toString());
		                        NewPermissionsRequest newPermissionsRequest = new NewPermissionsRequest(mContext, requestedReadPermissions);
		                        newPermissionsRequest.setCallback(statusCallback);
		                        session.requestNewReadPermissions(newPermissionsRequest);
		                        retryTimes--;
		                    } else if(!requestedPublishPermissions.isEmpty()) {
		                 	   	Log.d(LOG_TAG, "SessionStatusCallback(OPENED_TOKEN_UPDATED): req publish permission->" + requestedPublishPermissions.toString());
		                        NewPermissionsRequest newPermissionsRequest = new NewPermissionsRequest(mContext, requestedPublishPermissions);
		                        newPermissionsRequest.setCallback(statusCallback);
		                        session.requestNewPublishPermissions(newPermissionsRequest);
		                        retryTimes--;
		                    }
                    	} else if(!requestedReadPermissions.isEmpty() || !requestedPublishPermissions.isEmpty()) {
                 		   loginRequested = false;
                      	   UserWrapper.onActionResult(mAdapter, UserWrapper.ACTION_RET_LOGIN_FAILED, "login failed");
                 	   }
                    }
                }                   
                else if(SessionState.CLOSED == state || SessionState.CLOSED_LOGIN_FAILED == state){
                	requestedReadPermissions.clear();
                	requestedPublishPermissions.clear();
                	isLoggedIn = false;
                    UserWrapper.onActionResult(mAdapter, UserWrapper.ACTION_RET_LOGIN_FAILED, getErrorMessage(exception, "failed"));
                }                   
            }
        }
    }
    
	private void onSessionStateChange(Session session, SessionState state,
            Exception exception) {
        if (session != null && session.isOpened()) {
            // make request to the /me API
            Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user,
                        Response response) {
                    if (user != null) {
                    	userIdStr = user.getId();
                    }

                }
            }).executeAsync();
        }
    }
    
    private String getSessionMessage(Session session){
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("{\"accessToken\":\"").append(session.getAccessToken()).append("\",");
    	buffer.append("\"permissions\":[");
    	List<String> list = session.getPermissions();
		for(int i = 0; i < list.size(); ++i){
			buffer.append("\"")
					.append(list.get(i))
					.append("\"");
			if(i != list.size() - 1)
				buffer.append(",");
		}
		buffer.append("]}");
		System.out.println(buffer.toString());
    	return buffer.toString();
    }
    
    private String getErrorMessage(Exception exception, String message){
    	StringBuffer errorMessage = new StringBuffer();
    	errorMessage.append("{\"error_message\":\"")
			    	.append(null == exception ? message : exception.getMessage())
			    	.append("\"}");
    	
    	return errorMessage.toString();
    }
        
    private native void nativeRequestCallback(int ret, String msg,int cbIndex);
}
