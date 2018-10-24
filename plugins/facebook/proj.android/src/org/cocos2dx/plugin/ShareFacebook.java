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

import java.io.File;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.Sharer.Result;
import com.facebook.share.internal.ShareFeedContent;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.model.GameRequestContent.ActionType;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareMessengerURLActionButton;
import com.facebook.share.model.ShareMessengerGenericTemplateElement;
import com.facebook.share.model.ShareMessengerGenericTemplateContent;
import com.facebook.share.widget.GameRequestDialog;
import com.facebook.share.widget.ShareDialog;
import com.facebook.share.widget.MessageDialog;

public class ShareFacebook implements InterfaceShare{

	private static Activity mContext = null;
	private static InterfaceShare mAdapter = null;
	private static boolean bDebug = true;
	private final static String LOG_TAG = "ShareFacebook";
	
	protected static void LogE(String msg, Exception e) {
        Log.e(LOG_TAG, msg, e);
        e.printStackTrace();
    }

    protected static void LogD(String msg) {
        if (bDebug) {
            Log.d(LOG_TAG, msg);
        }
    }
    
    public ShareFacebook(Context context) {
		mContext = (Activity)context;		
		mAdapter = this;
	}
    
	@Override
	public void configDeveloperInfo(Hashtable<String, String> cpInfo) {
		LogD("not supported in Facebook pluign");
	}

	@Override
	public void share(final Hashtable<String, String> cpInfo) {
		LogD("share invoked " + cpInfo.toString());
		if (networkReachable()) {
			PluginWrapper.runOnMainThread(new Runnable() {
				@Override
				public void run() {
//					String caption = cpInfo.get("title");
					String url = cpInfo.get("link");
//					String text = cpInfo.get("description");
//					String picture = cpInfo.get("imageUrl");
					
					ShareDialog shareDialog = new ShareDialog(mContext);
					shareDialog.registerCallback(FacebookWrapper.callbackManager, new FacebookCallback<Sharer.Result>() {
						@Override
						public void onCancel() {
							Log.d(LOG_TAG, "share() onCancel");
							ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_CANCEL, "{ \"error_message\" : \"canceled\"}");
						}
						@Override
						public void onError(FacebookException facebookException) {
							Log.d(LOG_TAG, "share() onError", facebookException);
							ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \"share failed\"}");
						}
						@Override
						public void onSuccess(Result result) {
							Log.d(LOG_TAG, "share() onSuccess " + result.getPostId());
							ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_SUCCESS, "{\"id\":\"" + result.getPostId() + "\"}");
						}
					});
					if(ShareDialog.canShow(ShareLinkContent.class) || true) {
						Log.d(LOG_TAG, "share link");
//						ShareLinkContent linkContent = new ShareLinkContent.Builder()
//							.setContentDescription(text)
//							.setContentTitle(caption)
//							.setContentUrl(Uri.parse(url))
//							.setImageUrl(Uri.parse(picture))
//							.build();
						ShareLinkContent linkContent = new ShareLinkContent.Builder()
							.setContentUrl(Uri.parse(url))
							.build();
						Log.d(LOG_TAG, "shareDialog.show(linkContent)");
						shareDialog.show(linkContent);
					}
//					else {
//						Log.d(LOG_TAG, "ShareDialog.canShow(ShareLinkContent.class) return  false");
//					}
				}
			});
		}		
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
	public String getSDKVersion() {
		return FacebookSdk.getSdkVersion();
	}

	public void setSDKVersion(String version){
//        Settings.setSDKVersion(version);
    }

	private boolean networkReachable() {
		boolean bRet = false;
		try {
			ConnectivityManager conn = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = conn.getActiveNetworkInfo();
			bRet = (null == netInfo) ? false : netInfo.isAvailable();
		} catch (Exception e) {
			LogE("Fail to check network status", e);
		}
		LogD("NetWork reachable : " + bRet);
		return bRet;
	}
	
	public boolean canPresentDialogWithParams(final JSONObject cpInfo){ 
		try {
			String dialogType = cpInfo.getString("dialog");
			if("shareLink".equals(dialogType)){
				return ShareDialog.canShow(ShareLinkContent.class);
			}
			else if("shareOpenGraph".equals(dialogType)){
				return ShareDialog.canShow(ShareOpenGraphContent.class);
				
			}
			else if("sharePhoto".equals(dialogType)){
				return ShareDialog.canShow(SharePhotoContent.class);
				
			}
			else if("apprequests".equals(dialogType)){
				return GameRequestDialog.canShow();
			}
			else if("messageLink".equals(dialogType)){
				return ShareDialog.canShow(ShareFeedContent.class);
			}
			else if("messageOpenGraph".equals(dialogType)){
				return ShareDialog.canShow(ShareOpenGraphContent.class);
			}
			else if("messagePhoto".equals(dialogType)){
				return ShareDialog.canShow(SharePhotoContent.class);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void webDialog(final JSONObject cpInfo){ 
		PluginWrapper.runOnMainThread(new Runnable(){
			@Override
			public void run() {
				try {
					String dialogType = cpInfo.getString("dialog");
					if("shareLink".equals(dialogType)){
						WebFeedDialog(cpInfo);
					}
					else if("shareOpenGraph".equals(dialogType)){
						WebFeedDialog(cpInfo);
					}
					else {
						String errMsgString = "{\"error_message\" : \"do not support this type!\"}";
						ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, errMsgString);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
		});
	}

	public void dialog(final JSONObject cpInfo){
		PluginWrapper.runOnMainThread(new Runnable(){

			@Override
			public void run() {
				try {
					String dialogType = cpInfo.getString("dialog");
					Log.d(LOG_TAG, "dialog " + dialogType);
					if("shareLink".equals(dialogType)){
						FBShareDialog(cpInfo);
					}
					else if("feedDialog".equals(dialogType)){
						WebFeedDialog(cpInfo);
					}
					else if("shareOpenGraph".equals(dialogType)){
						FBShareOpenGraphDialog(cpInfo);
					}
					else if("sharePhoto".equals(dialogType)){
						FBSharePhotoDialog(cpInfo);
					}
					else if("apprequests".equals(dialogType)){
						WebRequestDialog(cpInfo);
					}
					else if("messageLink".equals(dialogType)){
						FBMessageDialog(cpInfo);
					}
					else if("messageOpenGraph".equals(dialogType)){
						FBMessageOpenGraphDialog(cpInfo);
					}
					else if("messagePhoto".equals(dialogType)){
						FBMessagePhotoDialog(cpInfo);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	
	private void FBShareOpenGraphDialog(JSONObject info) throws JSONException{
		String type = info.has("action_type")?info.getString("action_type"):info.getString("actionType");
		String previewProperty = info.has("preview_property_name")?info.getString("preview_property_name"):info.getString("previewPropertyName");

		ShareDialog shareDialog = new ShareDialog(mContext);
		shareDialog.registerCallback(FacebookWrapper.callbackManager, new FacebookCallback<Sharer.Result>() {
			@Override
			public void onCancel() {
				Log.d(LOG_TAG, "FBShareOpenGraphDialog() onCancel");
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_CANCEL, "{ \"error_message\" : \"canceled\"}");
			}
			@Override
			public void onError(FacebookException facebookException) {
				Log.d(LOG_TAG, "FBShareOpenGraphDialog() onError", facebookException);
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \"share failed\"}");
			}
			@Override
			public void onSuccess(Result result) {
				Log.d(LOG_TAG, "FBShareOpenGraphDialog() onSuccess " + result.getPostId());
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_SUCCESS, "{\"id\":\"" + result.getPostId() + "\"}");
			}
		});
		if(ShareDialog.canShow(ShareOpenGraphContent.class) || true) {
			Log.d(LOG_TAG, "share ShareOpenGraphContent");
			ShareOpenGraphContent ogContent = new ShareOpenGraphContent.Builder()
				.setAction(
						new ShareOpenGraphAction.Builder()
							.setActionType(type)
							.putPhoto("image",
									new SharePhoto.Builder()
										.setImageUrl(Uri.parse(info.getString("image")))
										.build())
							.putString("description", info.getString("description"))
							.build())
				.setPreviewPropertyName(previewProperty)
				.setContentUrl(Uri.parse(info.getString("url")))
				.build();
			Log.d(LOG_TAG, "shareDialog.show(ogContent)");
			shareDialog.show(ogContent);
		}
//		else {
//			Log.d(LOG_TAG, "ShareDialog.canShow(ShareOpenGraphContent.class) return false");
//		}
	}
	
	private void FBSharePhotoDialog(JSONObject info) throws JSONException{
		String filepath = info.getString("photo");
		if("".equals(filepath)){
			LogD("Must specify one photo");
			return;
		}
		
		ShareDialog shareDialog = new ShareDialog(mContext);
		shareDialog.registerCallback(FacebookWrapper.callbackManager, new FacebookCallback<Sharer.Result>() {
			@Override
			public void onCancel() {
				Log.d(LOG_TAG, "FBSharePhotoDialog() onCancel");
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_CANCEL, "{ \"error_message\" : \"canceled\"}");
			}
			@Override
			public void onError(FacebookException facebookException) {
				Log.d(LOG_TAG, "FBSharePhotoDialog() onError", facebookException);
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \"share failed\"}");
			}
			@Override
			public void onSuccess(Result result) {
				Log.d(LOG_TAG, "FBSharePhotoDialog() onSuccess " + result.getPostId());
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_SUCCESS, "{\"id\":\"" + result.getPostId() + "\"}");
			}
		});
		if(ShareDialog.canShow(SharePhotoContent.class) || true) {
			Log.d(LOG_TAG, "share photo " + filepath);
			File file = new File(filepath);
			SharePhotoContent photoContent = new SharePhotoContent.Builder()
						.addPhoto(new SharePhoto.Builder()
							.setImageUrl(Uri.fromFile(file))
							.build())
						.build();
			Log.d(LOG_TAG, "shareDialog.show(photoContent)");
			shareDialog.show(photoContent);
		}
//		else {
//			Log.d(LOG_TAG, "ShareDialog.canShow(SharePhotoContent.class) return false");
//		}
	}
	public void appRequest(final JSONObject info){
		PluginWrapper.runOnMainThread(new Runnable(){

			@Override
			public void run() {
				try{
					WebRequestDialog(info);
				}catch(JSONException e){
					e.printStackTrace();
				}
			}
		});
	}
	
	private void WebRequestDialog(JSONObject info) throws JSONException{
//		if(info.has("appLinkUrl")) {
//			if (AppInviteDialog.canShow()) {
//				AppInviteContent.Builder builder = new AppInviteContent.Builder();
//				builder.setApplinkUrl(safeGetJsonString(info, "appLinkUrl"));
//				if(info.has("previewImageUrl")) {
//					builder.setPreviewImageUrl(safeGetJsonString(info, "previewImageUrl"));
//				}
//				
//				AppInviteContent content = builder.build();
//				AppInviteDialog dialog = new AppInviteDialog(mContext);
//				dialog.registerCallback(FacebookWrapper.callbackManager, callback);
//				return;
//			}
//		}
		
		String message;
		if ((message = safeGetJsonString(info, "message")) == null)
		{
			ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \" need to add property 'message' \"}");
			return;
		}
		String strActionType = safeGetJsonString(info, "action_type");
		ActionType actionType = null;
		if(strActionType != null && strActionType.trim().length() > 0) {
			actionType = ActionType.valueOf(strActionType);
		}
		String to = safeGetJsonString(info, "to");
		String[] toIds = null;
		List<String> toList = new ArrayList<String>();

		if (to != null) {
			toIds = to.split(",");
			if(toIds.length == 1 && to.length() > 15) {
				toIds = to.split(" ");
			}
			for(int i = 0; i < toIds.length; i++) {
				toList.add(toIds[i]);
			}
		}

		String filters = safeGetJsonString(info, "filters");
		GameRequestContent requestContent = null;
		if (filters != null && filters.equals("AppNonUsers")) {
			requestContent = new GameRequestContent.Builder()
			.setActionType(actionType)
			.setMessage(message)
			.setTitle(safeGetJsonString(info, "title"))
			.setData(safeGetJsonString(info, "data"))
			.setFilters(GameRequestContent.Filters.APP_NON_USERS)
			.build();
		}else {
			requestContent = new GameRequestContent.Builder()
			.setActionType(actionType)
			.setMessage(message)
			.setRecipients(toList)
			.setTitle(safeGetJsonString(info, "title"))
			.setData(safeGetJsonString(info, "data"))
			.build();
		}

		GameRequestDialog requestDialog = new GameRequestDialog(mContext);
		requestDialog.registerCallback(FacebookWrapper.callbackManager, new FacebookCallback<GameRequestDialog.Result>() {
			@Override
			public void onCancel() {
				StringBuffer buffer = new StringBuffer();
				buffer.append("{\"error_message\":\"")
					.append("canceled")
					.append("\"}");
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, buffer.toString());
			}
			@Override
			public void onError(FacebookException error) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("{\"error_message\":\"")
					.append(error.getMessage())
					.append("\"}");
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, buffer.toString());
			}
			@Override
			public void onSuccess(com.facebook.share.widget.GameRequestDialog.Result result) {
				
				StringBuffer buffer = new StringBuffer();
				buffer.append("{\"request\":\"");
				buffer.append(result.getRequestId());
				buffer.append("\", \"to\":[");
				Iterator<String> it = result.getRequestRecipients().iterator();
				while(it.hasNext()){
					String key = it.next();
					buffer.append("\"");
					buffer.append(key);
					buffer.append("\"");
					if(it.hasNext()) {
						buffer.append(",");
					}
				}
				buffer.append("]}");
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_SUCCESS, buffer.toString());
			}
		});
		requestDialog.show(requestContent);
	}
	
	private String safeGetJsonString(JSONObject info, String key) {
		try {
			return info.getString(key);
		} catch (Exception e) {
			return null;
		}
	}
	
	private void FBShareDialog(JSONObject info) throws JSONException{
		String link = null;
		// some property need to add
		if ((link = safeGetJsonString(info, "link")) == null)
		{
			ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \" need to add property 'link' \"}");
			return;
		}
		
		ShareDialog shareDialog = new ShareDialog(mContext);
		shareDialog.registerCallback(FacebookWrapper.callbackManager, new FacebookCallback<Sharer.Result>() {
			@Override
			public void onCancel() {
				Log.d(LOG_TAG, "FBShareDialog() onCancel");
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_CANCEL, "{ \"error_message\" : \"canceled\"}");
			}
			@Override
			public void onError(FacebookException facebookException) {
				Log.d(LOG_TAG, "FBShareDialog() onError", facebookException);
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \"share failed\"}");
			}
			@Override
			public void onSuccess(Result result) {
				Log.d(LOG_TAG, "FBShareDialog() onSuccess " + result.getPostId());
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_SUCCESS, "{\"id\":\"" + result.getPostId() + "\"}");
			}
		});
		if(ShareDialog.canShow(ShareFeedContent.class) || true) {
			ShareFeedContent.Builder builder = new ShareFeedContent.Builder();
			builder.setLink(link);
			
			// some property can be choose
			String name = null;
			if ((name = safeGetJsonString(info, "name")) != null)
				builder.setLinkName(name);
			
			String caption = null;
			if ((caption = safeGetJsonString(info, "caption")) != null)
				builder.setLinkCaption(caption);
			
			String description = null;
			if ((description = safeGetJsonString(info, "description")) != null)
				builder.setLinkDescription(description);
			
			String picture = null;
			if ((picture = safeGetJsonString(info, "picture")) != null)
				builder.setPicture(picture);
			
			String friendStr = null;
			if ((friendStr = safeGetJsonString(info, "to")) != null)
			{
				builder.setToId(friendStr);
			}
			
			String place = null;
			if ((place = safeGetJsonString(info, "place")) != null)
			{
				builder.setPlaceId(place);
			}
			
			String ref = null;
			if ((ref = safeGetJsonString(info, "reference")) != null)
			{
				builder.setRef(ref);
			}
			String mediaSource = null;
			if ((mediaSource = safeGetJsonString(info, "media_source")) != null)
			{
				builder.setMediaSource(mediaSource);
			}
			
			ShareFeedContent feedContent = builder.build();
			Log.d(LOG_TAG, "shareDialog.show(feedContent)");
			shareDialog.show(feedContent);
		}
//		else {
//			Log.d(LOG_TAG, "ShareDialog.canShow(ShareFeedContent.class) return false");
//		}
	}
	
	private void WebFeedDialog(JSONObject info) throws JSONException{
		String link = null;
		// some property need to add
		if ((link = safeGetJsonString(info, "link")) == null)
		{
			ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \" need to add property 'link' \"}");
			return;
		}
		
		ShareDialog shareDialog = new ShareDialog(mContext);
		shareDialog.registerCallback(FacebookWrapper.callbackManager, new FacebookCallback<Sharer.Result>() {
			@Override
			public void onCancel() {
				Log.d(LOG_TAG, "WebFeedDialog() onCancel");
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_CANCEL, "{ \"error_message\" : \"canceled\"}");
			}
			@Override
			public void onError(FacebookException facebookException) {
				Log.d(LOG_TAG, "WebFeedDialog() onError", facebookException);
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \"share failed\"}");
			}
			@Override
			public void onSuccess(Result result) {
				Log.d(LOG_TAG, "WebFeedDialog() onSuccess " + result.getPostId());
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_SUCCESS, "{\"id\":\"" + result.getPostId() + "\"}");
			}
		});
		if(ShareDialog.canShow(ShareFeedContent.class) || true) {
			ShareFeedContent.Builder builder = new ShareFeedContent.Builder();
			builder.setLink(link);
			
			// some property can be choose
			String name = null;
			if ((name = safeGetJsonString(info, "name")) != null)
				builder.setLinkName(name);
			
			String caption = null;
			if ((caption = safeGetJsonString(info, "caption")) != null)
				builder.setLinkCaption(caption);
			
			String description = null;
			if ((description = safeGetJsonString(info, "description")) != null)
				builder.setLinkDescription(description);
			
			String picture = null;
			if ((picture = safeGetJsonString(info, "picture")) != null)
				builder.setPicture(picture);
			
			String friendStr = null;
			if ((friendStr = safeGetJsonString(info, "to")) != null)
			{
				builder.setToId(friendStr);
			}
			
			String place = null;
			if ((place = safeGetJsonString(info, "place")) != null)
			{
				builder.setPlaceId(place);
			}
			
			String ref = null;
			if ((ref = safeGetJsonString(info, "reference")) != null)
			{
				builder.setRef(ref);
			}
			String mediaSource = null;
			if ((mediaSource = safeGetJsonString(info, "media_source")) != null)
			{
				builder.setMediaSource(mediaSource);
			}
			
			ShareFeedContent feedContent = builder.build();
			Log.d(LOG_TAG, "shareDialog.show(feedContent)");
			shareDialog.show(feedContent);
		}
//		else {
//			Log.d(LOG_TAG, "ShareDialog.canShow(ShareFeedContent.class) return false");
//		}
	}

	private void FBMessageDialog(JSONObject info) throws JSONException{
		// FBShareDialog(info);
		String link = null;
		if ((link = safeGetJsonString(info, "link")) == null) {
			ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\": \"FBMessageDialog need to add property 'link'\"}");
			return;
		}

		MessageDialog msgDialog = new MessageDialog(mContext);
		msgDialog.registerCallback(FacebookWrapper.callbackManager, new FacebookCallback<Sharer.Result>() {
			@Override
			public void onSuccess(Result result) {
				Log.d(LOG_TAG, "FBMessageDialog() onSuccess " + result.getPostId());
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_SUCCESS, "{\"FBMessageDialog id\":\"" + result.getPostId() + "\"}");
			}
			@Override
			public void onCancel() {
				Log.d(LOG_TAG, "FBMessageDialog() onCancel");
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_CANCEL, "{ \"error_message\" : \"FBMessageDialog canceled\"}");
			}
			@Override
			public void onError(FacebookException facebookException) {
				Log.d(LOG_TAG, "FBMessageDialog() onError", facebookException);
				ShareWrapper.onShareResult(mAdapter, ShareWrapper.SHARERESULT_FAIL, "{ \"error_message\" : \"FBMessageDialog share failed\"}");
			}
		});

		Log.d(LOG_TAG, "MessageDialog canShow ShareLinkContent: " + MessageDialog.canShow(ShareLinkContent.class));
		if (MessageDialog.canShow(ShareLinkContent.class) || true) {
			ShareLinkContent linkContent = new ShareLinkContent.Builder()
				.setContentUrl(Uri.parse(link))
				.build();
			// msgDialog.show(linkContent);
			MessageDialog.show(mContext, linkContent);
		}
	}

	private void FBMessageOpenGraphDialog(JSONObject info) throws JSONException{
		FBShareOpenGraphDialog(info);
	}
	
	private void FBMessagePhotoDialog(JSONObject info) throws JSONException{
		FBSharePhotoDialog(info);
	}

}
