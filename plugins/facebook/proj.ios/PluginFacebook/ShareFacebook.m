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

#import "ShareFacebook.h"
#import "ShareWrapper.h"
#import "ParseUtils.h"
#define OUTPUT_LOG(...)     if (true) NSLog(__VA_ARGS__);

@implementation ShareFacebook

@synthesize mShareInfo;
@synthesize debug = __debug;

/**
 * A function for parsing URL parameters.
 */
- (NSDictionary*)parseURLParams:(NSString *)query {
    NSArray *pairs = [query componentsSeparatedByString:@"&"];
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    for (NSString *pair in pairs) {
        NSArray *kv = [pair componentsSeparatedByString:@"="];
        NSString *val =
        [kv[1] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        NSString *key = [kv[0] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        params[key] = val;
    }
    return params;
}

/**
 * shareInfo parameters support both AnySDK style and facebook style
 *  1. AnySDK style
 *      - title
 *      - site
 *      - siteUrl
 *      - text
 *      - imageUrl
 *      - imagePath
 *
 *  2. Facebook style
 *      - caption
 *      - name
 *      - link
 *      - description
 *      - picture
 */
- (void)convertParamsToFBParams:(NSMutableDictionary*) shareInfo {
    // Link type share info
    NSString *link = [shareInfo objectForKey:@"siteUrl"];
    if (!link) {
        link = [shareInfo objectForKey:@"link"];
    }
    else {
        [shareInfo setObject:link forKey:@"link"];
    }
    // Photo type share info
    NSString *photo = [shareInfo objectForKey:@"imageUrl"];
    if (!photo) {
        photo = [shareInfo objectForKey:@"imagePath"];
    }
    if (!photo) {
        photo = [shareInfo objectForKey:@"photo"];
    }
    else {
        [shareInfo setObject:photo forKey:@"photo"];
        [shareInfo setObject:photo forKey:@"picture"];
    }
    
    // Title
    NSString *caption = [shareInfo objectForKey:@"title"];
    if (!caption) {
        link = [shareInfo objectForKey:@"caption"];
    }
    else {
        [shareInfo setObject:caption forKey:@"caption"];
    }
    
    // Site name
    NSString *name = [shareInfo objectForKey:@"site"];
    if (!name) {
        link = [shareInfo objectForKey:@"name"];
    }
    else {
        [shareInfo setObject:name forKey:@"name"];
    }
    
    // Description
    NSString *desc = [shareInfo objectForKey:@"text"];
    if (!desc) {
        link = [shareInfo objectForKey:@"description"];
    }
    else {
        [shareInfo setObject:desc forKey:@"description"];
    }
}

- (void) configDeveloperInfo : (NSMutableDictionary*) cpInfo
{
}

- (void) share: (NSMutableDictionary*) shareInfo
{
    [self convertParamsToFBParams:shareInfo];
    NSString *link = [shareInfo objectForKey:@"link"];
    NSString *photo = [shareInfo objectForKey:@"photo"];
    UIViewController* rootViewController = [[[UIApplication sharedApplication]keyWindow]rootViewController];
    if (link) {
        // Link type share info
        NSString *link = [shareInfo objectForKey:@"link"];
        NSString *caption = [shareInfo objectForKey:@"caption"];
        //NSString *name = [shareInfo objectForKey:@"name"];
        NSString *desc = [shareInfo objectForKey:@"description"];
        NSString *photo = [shareInfo objectForKey:@"picture"];
        FBSDKShareLinkContent *content = [[FBSDKShareLinkContent alloc] init];
        content.contentTitle = caption;
        content.contentDescription = desc;
        content.contentURL = [NSURL URLWithString:link];
        content.imageURL = [NSURL URLWithString:photo];
        [FBSDKShareDialog showFromViewController:rootViewController
                                     withContent:content
                                        delegate:self];
    }
    else if (photo) {
        NSURL *photoUrl = [NSURL URLWithString:[shareInfo objectForKey:@"photo"]];
        UIImage *img = [UIImage imageWithData:[NSData dataWithContentsOfURL:photoUrl]];
        FBSDKSharePhotoContent* content = [[FBSDKSharePhotoContent alloc]init];
        content.photos = @[img];
        [FBSDKShareDialog showFromViewController:rootViewController
                                     withContent:content
                                        delegate:self];
    }
    else {
        NSString *msg = [ParseUtils MakeJsonStringWithObject:@"Share failed, share target absent or not supported, please add 'siteUrl' or 'imageUrl' in parameters" andKey:@"error_message"];
        [ShareWrapper onShareResult:self withRet:kShareFail withMsg:msg];
    }
}

- (void) setDebugMode: (BOOL) debug
{
    self.debug = debug;
}

- (NSString*) getSDKVersion
{
    return FBSDK_VERSION_STRING;
}

- (void) setSDKVersion: (NSString *)sdkVersion{
     //[FBSettings setSDKVersion:sdkVersion];
}

- (NSString*) getPluginVersion
{
    return @"0.2.0";
}

- (void) dialog: (NSMutableDictionary*) shareInfo
{
    [self convertParamsToFBParams:shareInfo];
    NSString *dialog_type = [shareInfo objectForKey:@"dialog"];
    UIViewController* rootViewController = [[[UIApplication sharedApplication]keyWindow]rootViewController];
    bool not_supported = false;
    
    if ([dialog_type hasSuffix:@"Link"]) {
        // Link type share info
        NSString *link = [shareInfo objectForKey:@"link"];
//        NSString *caption = [shareInfo objectForKey:@"caption"];
        NSString *name = [shareInfo objectForKey:@"name"];
        NSString *desc = [shareInfo objectForKey:@"description"];
        NSString *photo = [shareInfo objectForKey:@"picture"];
        
        // Additional properties
        NSString *place = [shareInfo objectForKey:@"place"];
        NSString *ref = [shareInfo objectForKey:@"reference"];
        NSString *to = [shareInfo objectForKey:@"to"];
        
        FBSDKShareLinkContent *content = [[FBSDKShareLinkContent alloc] init];
        content.contentTitle = name;
        content.contentDescription = desc;
        content.contentURL = [NSURL URLWithString:link];
        content.imageURL = [NSURL URLWithString:photo];
        if (place) {
            content.placeID = place;
        }
        if (ref) {
            content.ref = ref;
        }
        if(to){
            NSArray *friends = [to componentsSeparatedByString:@","];
            content.peopleIDs = friends;
        }
        if ([dialog_type isEqualToString:@"shareLink"]) {
            [FBSDKShareDialog showFromViewController:rootViewController
                                         withContent:content
                                            delegate:self];
        }
        else if ([dialog_type isEqualToString:@"messageLink"]) {
            [FBSDKMessageDialog showWithContent:content delegate:self];
        }
        else {
            not_supported = true;
        }
    }
    else if ([dialog_type hasSuffix:@"OpenGraph"]) {
        NSString *type = [shareInfo objectForKey:@"action_type"];
        NSString *previewProperty = [shareInfo objectForKey:@"preview_property_name"];
//        NSString *title = [shareInfo objectForKey:@"title"];
//        NSString *image = [shareInfo objectForKey:@"image"];
        NSString *link = [shareInfo objectForKey:@"link"];
//        NSString *desc = [shareInfo objectForKey:@"description"];
        
        NSString *place = [shareInfo objectForKey:@"place"];
        NSString *ref = [shareInfo objectForKey:@"reference"];
        NSString *to = [shareInfo objectForKey:@"to"];
        
        FBSDKShareOpenGraphAction* action =  [FBSDKShareOpenGraphAction actionWithType:type objectURL:[NSURL URLWithString:link] key:previewProperty];
        FBSDKShareOpenGraphContent* content = [[FBSDKShareOpenGraphContent alloc]init];
        content.action = action;
        content.previewPropertyName = previewProperty;
        if (place) {
            content.placeID = place;
        }
        if (ref) {
            content.ref = ref;
        }
        if(to){
            NSArray *friends = [to componentsSeparatedByString:@","];
            content.peopleIDs = friends;
        }
        if ([dialog_type isEqualToString:@"shareOpenGraph"]) {
            [FBSDKShareDialog showFromViewController:rootViewController
                                         withContent:content
                                            delegate:self];
        } else if ([dialog_type isEqualToString:@"messageOpenGraph"]) {
            [FBSDKMessageDialog showWithContent:content delegate:self];
        } else {
            not_supported = true;
        }
    }
    else if ([dialog_type hasSuffix:@"Photo"]) {
        UIImage *img = [[UIImage alloc] initWithContentsOfFile:[shareInfo objectForKey:@"photo"]];
        if(img ==nil){
            NSString *msg = [ParseUtils MakeJsonStringWithObject:@"Share failed, photo can't be found" andKey:@"error_message"];
            [ShareWrapper onShareResult:self withRet:kShareFail withMsg:msg];
            return;
        }
        NSURL *photoUrl = [NSURL URLWithString:[shareInfo objectForKey:@"photo"]];
        NSString *place = [shareInfo objectForKey:@"place"];
        NSString *ref = [shareInfo objectForKey:@"reference"];
        NSString *to = [shareInfo objectForKey:@"to"];
        UIImage *img1 = [UIImage imageWithData:[NSData dataWithContentsOfURL:photoUrl]];
        FBSDKSharePhotoContent* content = [[FBSDKSharePhotoContent alloc]init];
        content.photos = @[img1];
        if (place) {
            content.placeID = place;
        }
        if (ref) {
            content.ref = ref;
        }
        if(to){
            NSArray *friends = [to componentsSeparatedByString:@","];
            content.peopleIDs = friends;
        }
        
        if ([dialog_type isEqualToString:@"sharePhoto"]) {
            [FBSDKShareDialog showFromViewController:rootViewController
                                         withContent:content
                                            delegate:self];
        }
        else if ([dialog_type isEqualToString:@"messagePhoto"]) {
            [FBSDKMessageDialog showWithContent:content delegate:self];
        } else {
            not_supported = true;
        }
    }
    else if ([dialog_type isEqualToString:@"feedDialog"]) {
        NSString *link = [shareInfo objectForKey:@"link"];
//        NSString *caption = [shareInfo objectForKey:@"caption"];
        NSString *name = [shareInfo objectForKey:@"name"];
        NSString *desc = [shareInfo objectForKey:@"description"];
        NSString *photo = [shareInfo objectForKey:@"picture"];
        
        NSString *place = [shareInfo objectForKey:@"place"];
        NSString *ref = [shareInfo objectForKey:@"reference"];
        NSString *to = [shareInfo objectForKey:@"to"];
        
        FBSDKShareLinkContent *content = [[FBSDKShareLinkContent alloc] init];
        content.contentTitle = name;
        content.contentDescription = desc;
        content.contentURL = [NSURL URLWithString:link];
        content.imageURL = [NSURL URLWithString:photo];
        if (place) {
            content.placeID = place;
        }
        if (ref) {
            content.ref = ref;
        }
        if(to){
            NSArray *friends = [to componentsSeparatedByString:@","];
            content.peopleIDs = friends;
        }
        FBSDKShareDialog* dialog = [[FBSDKShareDialog alloc] init];
        dialog.shareContent = content;
        dialog.mode = FBSDKShareDialogModeFeedWeb;
        dialog.fromViewController = rootViewController;
        dialog.delegate = self;
        [dialog show];
    }
    else {
        not_supported = true;
    }
    
    if (not_supported) {
        NSString *error = [NSString stringWithFormat:@"Share failed, dialog not supported: %@", dialog_type];
        NSString *msg = [ParseUtils MakeJsonStringWithObject:error andKey:@"error_message"];
        [ShareWrapper onShareResult:self withRet:kShareFail withMsg:msg];
    }
}
-(BOOL) canPresentDialogWithParams:(NSMutableDictionary *)shareInfo{
    return true;
}

- (void) appRequest: (NSMutableDictionary*) shareInfo
{
    NSString *message = [shareInfo objectForKey:@"message"];
    NSString *title = [shareInfo objectForKey:@"title"];
    NSString *to = [shareInfo objectForKey:@"to"];
    
    FBSDKGameRequestContent* content = [[FBSDKGameRequestContent alloc] init];
    content.message = message;
    content.title = title;
    if(to){
        NSArray *friends = [to componentsSeparatedByString:@","];
        content.recipients = friends;
    }
    
    [FBSDKGameRequestDialog showWithContent:content delegate:self];
}

- (void)gameRequestDialog:(FBSDKGameRequestDialog *)gameRequestDialog didCompleteWithResults:(NSDictionary *)results {
    NSMutableArray* toArr = [[NSMutableArray alloc] init];
    NSArray* keyArr = results.allKeys;
    for(size_t i = 0; i < keyArr.count; i++) {
        NSString* key = [keyArr objectAtIndex:i];
        NSString* val = [results objectForKey:key];
        [toArr addObject:val];
    }
    NSMutableDictionary* dic = [[NSMutableDictionary alloc] init];
    [dic setObject:toArr forKey:@"to"];
    NSString *msg = [ParseUtils NSDictionaryToNSString:dic];
    [ShareWrapper onShareResult:self withRet:kShareSuccess withMsg:msg];
}


- (void)gameRequestDialog:(FBSDKGameRequestDialog *)gameRequestDialog didFailWithError:(NSError *)error {
    OUTPUT_LOG(@"request error:%ld %@", error.code, error.userInfo[FBSDKErrorDeveloperMessageKey]);
    NSString *msg = [ParseUtils MakeJsonStringWithObject:[NSString stringWithFormat:@"Share failed, %@", error.userInfo[FBSDKErrorLocalizedDescriptionKey]] andKey:@"error_message"];
    [ShareWrapper onShareResult:self withRet:kShareFail withMsg:msg];
}

- (void)gameRequestDialogDidCancel:(FBSDKGameRequestDialog *)gameRequestDialog {
    // User clicked the Cancel button
    NSString *msg = [ParseUtils MakeJsonStringWithObject:@"canceled" andKey:@"error_message"];
    [ShareWrapper onShareResult:self withRet:kShareCancel withMsg:msg];
}

- (void)sharer:(id<FBSDKSharing>)sharer didCompleteWithResults:(NSDictionary *)results {
    NSString *msg = [ParseUtils NSDictionaryToNSString:results];
    [ShareWrapper onShareResult:self withRet:kShareSuccess withMsg:msg];
}

- (void)sharer:(id<FBSDKSharing>)sharer didFailWithError:(NSError *)error {
    OUTPUT_LOG(@"share error:%ld %@", error.code, error.userInfo[FBSDKErrorDeveloperMessageKey]);
    NSString *msg = [ParseUtils MakeJsonStringWithObject:[NSString stringWithFormat:@"Share failed, %@", error.userInfo[FBSDKErrorLocalizedDescriptionKey]] andKey:@"error_message"];
    [ShareWrapper onShareResult:self withRet:kShareFail withMsg:msg];
}

- (void)sharerDidCancel:(id<FBSDKSharing>)sharer {
    NSString *msg = [ParseUtils MakeJsonStringWithObject:@"canceled" andKey:@"error_message"];
    [ShareWrapper onShareResult:self withRet:kShareCancel withMsg:msg];
}

@end