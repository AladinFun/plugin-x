package org.cocos2dx.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import bolts.AppLinks;

import com.aladinfun.nativeutil.BaseEntryActivity;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.FacebookSdk.InitializeCallback;
import com.facebook.LoggingBehavior;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.applinks.AppLinkData;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

public class FacebookWrapper {
	static final String TAG = FacebookWrapper.class.getSimpleName();
	static boolean isLoggedIn = false;
	static boolean isLoginRequested = false;
	static UserFacebook userFacebook;
	static CallbackManager callbackManager;
	static FacebookCallback<LoginResult> loginCallback;
	
	
	static class MyLoginCallback implements FacebookCallback<LoginResult> {
		private String getErrorMessage(Exception exception, String message){
	    	StringBuffer errorMessage = new StringBuffer();
	    	errorMessage.append("{\"error_message\":\"")
				    	.append(null == exception ? message : exception.getMessage())
				    	.append("\"}");
	    	return errorMessage.toString();
	    }
		@Override
		public void onCancel() {
			Log.d(TAG, "loginCallback.onCancel");
			if(isLoginRequested) {
				isLoginRequested = false;
				UserWrapper.onActionResult(userFacebook, UserWrapper.ACTION_RET_LOGIN_FAILED, getErrorMessage(null, "canceled"));
			}
		}

		@Override
		public void onError(FacebookException facebookException) {
			Log.d(TAG, "loginCallback.onError " + facebookException.toString());
			if(isLoginRequested) {
				isLoginRequested = false;
				UserWrapper.onActionResult(userFacebook, UserWrapper.ACTION_RET_LOGIN_FAILED, getErrorMessage(facebookException, "error"));
			}
		}

		@Override
		public void onSuccess(LoginResult loginResult) {
			Log.d(TAG, "loginCallback.onSuccess");
			AccessToken accessToken = loginResult.getAccessToken();
			if(accessToken != null) {
				AccessToken.setCurrentAccessToken(accessToken);
			}
			String accessTokenMsg = getAccessTokenMessage(accessToken);
			Log.d(TAG, "accessToken " + accessTokenMsg);
			if(isLoginRequested) {
				if(!requestUserPermissions()) {
					isLoginRequested = false;
					isLoggedIn = true;
					UserWrapper.onActionResult(userFacebook, UserWrapper.ACTION_RET_LOGIN_SUCCEED, accessTokenMsg);
				}
			} else {
				isLoggedIn = true;
			}
		}
	};
	static AppEventsLogger appEventsLogger;
	static AccessTokenTracker accessTokenTracker;
	
	static class MyAccessTokenTracker extends AccessTokenTracker {
        @Override
        protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
        	Log.d(TAG, "onCurrentAccessTokenChanged old_access_token:" + getAccessTokenMessage(oldAccessToken));
        	Log.d(TAG, "onCurrentAccessTokenChanged new_access_token:" + getAccessTokenMessage(currentAccessToken));
        }
    };
    static ProfileTracker profileTracker;
    
    static class MyProfileTracker extends ProfileTracker {
		@Override
		protected void onCurrentProfileChanged(Profile oldProfile, Profile newProfile) {
			if(oldProfile == null) {
				Log.d(TAG, "user profile change old: null");
			} else {
				Log.d(TAG, "user profile change old:");
				Log.d(TAG, "id:" + oldProfile.getId());
				Log.d(TAG, "firstName:" + oldProfile.getFirstName());
				Log.d(TAG, "lastName:" + oldProfile.getLastName());
				Log.d(TAG, "midName:" + oldProfile.getMiddleName());
				Log.d(TAG, "name:" + oldProfile.getName());
			}
			if(newProfile == null) {
				Log.d(TAG, "user profile change new: null");
				UserFacebook.userIdStr = null;
			} else {
				Log.d(TAG, "user profile change new:");
				Log.d(TAG, "id:" + newProfile.getId());
				Log.d(TAG, "firstName:" + newProfile.getFirstName());
				Log.d(TAG, "lastName:" + newProfile.getLastName());
				Log.d(TAG, "midName:" + newProfile.getMiddleName());
				Log.d(TAG, "name:" + newProfile.getName());
				UserFacebook.userIdStr = newProfile.getId();
			}
		}
	};
	
	static boolean requestUserPermissions() {
		AccessToken accessToken = AccessToken.getCurrentAccessToken();
		Activity ctx = (Activity) BaseEntryActivity.getContext();
		if(userFacebook != null) {
			if(accessToken == null || accessToken.isExpired() || accessToken.getToken() == null || accessToken.getToken().trim().length() == 0) {
				Log.d(TAG, "login with read permission:" + Arrays.toString(userFacebook.requestedReadPermissions.toArray()));
				LoginManager.getInstance().logInWithReadPermissions(ctx, userFacebook.requestedReadPermissions);
			} else {
				Set<String> grantedPermissions = accessToken.getPermissions();
				if(grantedPermissions.containsAll(userFacebook.requestedReadPermissions)) {
					if(grantedPermissions.containsAll(userFacebook.requestedPublishPermissions)) {
						Log.d(TAG, "all permission granted");
						return false;
					} else {
						List<String> toRequest = new ArrayList<String>();
						toRequest.addAll(userFacebook.requestedPublishPermissions);
						toRequest.removeAll(grantedPermissions);
						Log.d(TAG, "login with publish permission:" + Arrays.toString(toRequest.toArray()));
						LoginManager.getInstance().logInWithPublishPermissions(ctx, toRequest);
					}
				} else {
					List<String> toRequest = new ArrayList<String>();
					toRequest.addAll(userFacebook.requestedReadPermissions);
					toRequest.removeAll(grantedPermissions);
					Log.d(TAG, "login with read permission:" + Arrays.toString(toRequest.toArray()));
					LoginManager.getInstance().logInWithReadPermissions(ctx, toRequest);
				}
			}
		} else {
			Log.d(TAG, "userFacebook is null");
		}
		return true;
	}
	
	static String getAccessTokenMessage(AccessToken accessToken){
	    if(accessToken != null) {
	    	StringBuffer buffer = new StringBuffer();
	    	buffer.append("{\"accessToken\":\"").append(accessToken.getToken()).append("\",");
	    	buffer.append("\"permissions\":[");
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
			buffer.append("]}");
	    	return buffer.toString();
	    }
	    return "null";
    }
	
	private static String linkString = null;
    
	public static void onCreate(Activity activity){
		isLoginRequested = false;
		
		FacebookSdk.setIsDebugEnabled(true);
		FacebookSdk.addLoggingBehavior(LoggingBehavior.REQUESTS);
		FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
		FacebookSdk.sdkInitialize(activity.getApplicationContext(), new InitializeCallback() {
			@Override
			public void onInitialized() {
				isFacebookLogined();
			}
		});
		appEventsLogger = AppEventsLogger.newLogger(activity);
		callbackManager = CallbackManager.Factory.create();
		loginCallback = new MyLoginCallback();
		LoginManager.getInstance().registerCallback(callbackManager, loginCallback);
		
		accessTokenTracker = new MyAccessTokenTracker();
		if(!accessTokenTracker.isTracking()) {
			accessTokenTracker.startTracking();
		}
		profileTracker = new MyProfileTracker();
		if(!profileTracker.isTracking()) {
			profileTracker.startTracking();
		}
		
		isFacebookLogined();
		Uri targetUrl = AppLinks.getTargetUrlFromInboundIntent(activity, activity.getIntent());
		if (targetUrl != null) {
			linkString = targetUrl.toString();
			Log.i(TAG, "App Link Target URL1: " + linkString);
		} else {
			AppLinkData.fetchDeferredAppLinkData(activity, new AppLinkData.CompletionHandler() {
				@Override
				public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
					if(appLinkData != null) {
						Uri targetUrl = appLinkData.getTargetUri();
						if(targetUrl != null) {
							linkString = targetUrl.toString();
							Log.i(TAG, "App Link Target URL2: " + linkString);
						} else {
							Log.i(TAG, "targetUrl is null");
						}
					} else {
						Log.i(TAG, "appLinkData is null");
					}
				}
			});
		}
	}
	
	public static boolean isFacebookLogined() {
		Profile.getCurrentProfile();
		AccessToken accessToken = AccessToken.getCurrentAccessToken();
		isLoggedIn = (accessToken != null && !accessToken.isExpired() && accessToken.getToken() != null && accessToken.getToken().trim().length() > 0);
		return isLoggedIn;
	}
	
	public static void onPause(Activity activity) {
		// if(activity != null)
		// {
		// 	AppEventsLogger.deactivateApp(activity);
		// }
		// else
		// {
		// 	Log.i(TAG, "onPause activity is null");
		// }
	}
	
	public static void onResume(Activity activity) {
		// if(activity != null)
		// {
		// 	AppEventsLogger.activateApp(activity);
		// }
		// else
		// {
		// 	Log.i(TAG, "onResume activity is null");
		// }
	}
	
	public static void onDestroy(Activity activity) {
		if(accessTokenTracker != null && accessTokenTracker.isTracking()) {
			accessTokenTracker.stopTracking();
		}
		if(profileTracker != null && profileTracker.isTracking()) {
			profileTracker.stopTracking();
		}
	}
	
	public static void onAcitivityResult(int requestCode, int resultCode, Intent data){
		if(callbackManager != null) {
			callbackManager.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	public static AppEventsLogger getAppEventsLogger(){
		return appEventsLogger;
	}
}
