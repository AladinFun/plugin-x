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

#import "UserFacebook.h"
#import <FBSDKCoreKit/FBSDKCoreKit.h>
#import <FBSDKLoginKit/FBSDKLoginKit.h>
#import "UserWrapper.h"
#import "ParseUtils.h"
#define OUTPUT_LOG(...)     if (self.debug) NSLog(__VA_ARGS__);

@implementation UserFacebook


@synthesize mUserInfo;
@synthesize debug = __debug;
bool _isLogin = false;
NSString *_userId = @"";
NSString *_accessToken = @"";

- (void) configDeveloperInfo : (NSMutableDictionary*) cpInfo{
}
- (void) login{
    [self _loginWithPermission:@[@"public_profile"]];
}
-(void) loginWithPermission:(NSString *)permissions{
    NSArray *permission = [permissions componentsSeparatedByString:@","];
    [self _loginWithPermission:permission];
}
-(void)_loginWithPermission:(NSArray *) permission{
    FBSDKAccessToken* accessToken = [FBSDKAccessToken currentAccessToken];
    NSMutableArray* readPermissionNeeded = [[NSMutableArray alloc] init];
    NSMutableArray* publishPermissionNeeded = [[NSMutableArray alloc] init];
    NSSet* PUBLISH_PERMISSIONS = [NSSet setWithObjects:@"publish_actions", @"ads_management", @"create_event",
                                                             @"rsvp_event",  @"manage_friendlists", @"manage_notifications",
                                                             @"manage_pages", nil];
    NSSet* grantedPermissions = nil;
    if(accessToken) {
        grantedPermissions = [accessToken permissions];
    }
    for(size_t i = 0; i < permission.count; i++) {
        NSString* pm = [permission objectAtIndex:i];
        if(!grantedPermissions || ![grantedPermissions containsObject:pm]) {
            if([PUBLISH_PERMISSIONS containsObject:pm]) {
                [publishPermissionNeeded addObject:pm];
            } else {
                [readPermissionNeeded addObject:pm];
            }
        }
    }
    
    if(accessToken && [readPermissionNeeded count] == 0 && [publishPermissionNeeded count] == 0) {
        NSMutableDictionary *result = [NSMutableDictionary dictionaryWithObjectsAndKeys:[[accessToken permissions] allObjects],@"permissions",[accessToken tokenString],@"accessToken", nil];
        NSString *msg = [ParseUtils NSDictionaryToNSString:result];
        [UserWrapper onActionResult:self withRet:kLoginSucceed withMsg:msg];
    } else if(!accessToken || [readPermissionNeeded count] > 0){
        FBSDKLoginManager* loginManager = [[FBSDKLoginManager alloc] init];
        UIViewController* rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
        [loginManager logInWithPermissions:readPermissionNeeded
                            fromViewController:rootViewController
                                       handler:^(FBSDKLoginManagerLoginResult *result, NSError *error) {
                                           if (error) {
                                               OUTPUT_LOG(@"Facebook login error:%ld %@", error.code, error.userInfo[FBSDKErrorDeveloperMessageKey]);
                                               NSString *msg = [ParseUtils MakeJsonStringWithObject:@"loginFailed" andKey:@"error_message"];
                                               [UserWrapper onActionResult:self withRet:kLoginFailed withMsg:msg];
                                           } else if([result isCancelled]) {
                                               NSString *msg = [ParseUtils MakeJsonStringWithObject:@"canceled" andKey:@"error_message"];
                                               [UserWrapper onActionResult:self withRet:kLoginFailed withMsg:msg];
                                           } else {
                                               FBSDKAccessToken* accessToken = [result token];
                                               _userId = [accessToken tokenString];
                                               _isLogin = true;
                                               if([publishPermissionNeeded count] == 0) {
                                                   NSMutableDictionary *result = [NSMutableDictionary dictionaryWithObjectsAndKeys:[[accessToken permissions] allObjects],@"permissions",[accessToken tokenString],@"accessToken", nil];
                                                   NSString *msg = [ParseUtils NSDictionaryToNSString:result];
                                                   [UserWrapper onActionResult:self withRet:kLoginSucceed withMsg:msg];
                                               } else {
                                                   [loginManager logInWithPermissions:publishPermissionNeeded
                                                                          fromViewController:rootViewController
                                                                                     handler:^(FBSDKLoginManagerLoginResult *result, NSError *error) {
                                                                                         if (error) {
                                                                                             OUTPUT_LOG(@"Facebook login error:%ld %@", error.code, error.userInfo[FBSDKErrorDeveloperMessageKey]);
                                                                                             NSString *msg = [ParseUtils MakeJsonStringWithObject:@"loginFailed" andKey:@"error_message"];
                                                                                             [UserWrapper onActionResult:self withRet:kLoginFailed withMsg:msg];
                                                                                         } else if([result isCancelled]) {
                                                                                             NSString *msg = [ParseUtils MakeJsonStringWithObject:@"canceled" andKey:@"error_message"];
                                                                                             [UserWrapper onActionResult:self withRet:kLoginFailed withMsg:msg];
                                                                                         } else {
                                                                                             FBSDKAccessToken* accessToken = [result token];
                                                                                             _userId = [accessToken tokenString];
                                                                                             _isLogin = true;
                                                                                             NSMutableDictionary *result = [NSMutableDictionary dictionaryWithObjectsAndKeys:[accessToken permissions],@"permissions",[accessToken tokenString],@"accessToken", nil];
                                                                                             NSString *msg = [ParseUtils NSDictionaryToNSString:result];
                                                                                             [UserWrapper onActionResult:self withRet:kLoginSucceed withMsg:msg];
                                                                                         }
                                                                                     }];
                                               }
                                           }
                                       }];

    } else {
        FBSDKLoginManager* loginManager = [[FBSDKLoginManager alloc] init];
        UIViewController* rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
        [loginManager logInWithPermissions:publishPermissionNeeded
                               fromViewController:rootViewController
                                          handler:^(FBSDKLoginManagerLoginResult *result, NSError *error) {
                                              if (error) {
                                                  OUTPUT_LOG(@"Facebook login error:%ld %@", error.code, error.userInfo[FBSDKErrorDeveloperMessageKey]);
                                                  NSString *msg = [ParseUtils MakeJsonStringWithObject:@"loginFailed" andKey:@"error_message"];
                                                  [UserWrapper onActionResult:self withRet:kLoginFailed withMsg:msg];
                                              } else if([result isCancelled]) {
                                                  NSString *msg = [ParseUtils MakeJsonStringWithObject:@"canceled" andKey:@"error_message"];
                                                  [UserWrapper onActionResult:self withRet:kLoginFailed withMsg:msg];
                                              } else {
                                                  FBSDKAccessToken* accessToken = [result token];
                                                  _userId = [accessToken tokenString];
                                                  _isLogin = true;
                                                  NSMutableDictionary *result = [NSMutableDictionary dictionaryWithObjectsAndKeys:[[accessToken permissions] allObjects],@"permissions",[accessToken tokenString],@"accessToken", nil];
                                                  NSString *msg = [ParseUtils NSDictionaryToNSString:result];
                                                  [UserWrapper onActionResult:self withRet:kLoginSucceed withMsg:msg];
                                              }
                                          }];

    }
    
}
- (void) logout{
    FBSDKLoginManager* loginManager = [[FBSDKLoginManager alloc] init];
    [loginManager  logOut];
}

- (BOOL) isLogined{
    return [FBSDKAccessToken currentAccessToken] != nil;
}
-(NSString *)getUserID{
    FBSDKAccessToken* accessToken = [FBSDKAccessToken currentAccessToken];
    if(accessToken) {
        return [accessToken userID];
    } else {
        return @"";
    }
}
- (BOOL) isLoggedIn{
    return [FBSDKAccessToken currentAccessToken] != nil;
}

-(NSString *) getPermissionList{
    FBSDKAccessToken* accessToken = [FBSDKAccessToken currentAccessToken];
    NSString *msg;
    if(!accessToken) {
        msg =[ParseUtils MakeJsonStringWithObject:@"session closed please login first" andKey:@"error_message"];
    }else{
        msg = [ParseUtils MakeJsonStringWithObject:[[accessToken permissions] allObjects] andKey:@"permissions"];
    }
    return msg;
}
-(NSString *)getAccessToken{
    FBSDKAccessToken* accessToken = [FBSDKAccessToken currentAccessToken];
    if(accessToken) {
        return [accessToken tokenString];
    } else {
        return @"";
    }
}

- (NSString*) getSessionID{
    return [self getAccessToken];
}

- (void) setDebugMode: (BOOL) debug{
    __debug = debug;
}

- (NSString*) getSDKVersion{
    return FBSDK_VERSION_STRING;
}

- (void) setSDKVersion: (NSString *)sdkVersion{
    //[FBSettings setSDKVersion:sdkVersion];
    /*
    [FBSDKSettings enableLoggingBehavior:FBSDKLoggingBehaviorDeveloperErrors];
    [FBSDKSettings enableLoggingBehavior:FBSDKLoggingBehaviorNetworkRequests];
    [FBSDKSettings enableLoggingBehavior:FBSDKLoggingBehaviorUIControlErrors];
    [FBSDKSettings enableLoggingBehavior:FBSDKLoggingBehaviorGraphAPIDebugInfo];
     */
}

- (NSString*) getPluginVersion{
    return @"";
}

-(void)activateApp{
    [FBSDKAppEvents activateApp];
}

-(void)logEventWithName:(NSString*) eventName{
    [FBSDKAppEvents logEvent:eventName];
}

-(void)logEvent:(NSMutableDictionary*) logInfo{
    if(logInfo.count == 2){
        NSString *eventName = [logInfo objectForKey:@"Param1"];
        id  param2 = [logInfo objectForKey:@"Param2"];
        if([param2 isKindOfClass:[NSDictionary class]]){
            NSDictionary *dic = (NSDictionary *)param2;
            [FBSDKAppEvents logEvent:eventName parameters:dic];
        }else{
            double floatval = [[logInfo objectForKey:@"Param2"] floatValue];
            [FBSDKAppEvents logEvent:eventName valueToSum:floatval];
        }
    }else if(logInfo.count == 3){
        NSString *eventName = [logInfo objectForKey:@"Param1"];
        double floatval = [[logInfo objectForKey:@"Param2"] floatValue];
        NSDictionary *para = [logInfo objectForKey:@"Param3"];
        [FBSDKAppEvents logEvent:eventName valueToSum:floatval parameters:para];
    }
}

-(void)requestPermissions:(NSString *)permision{
    FBSDKAccessToken* accessToken = [FBSDKAccessToken currentAccessToken];
    if(!accessToken){
        NSString *msg = [ParseUtils MakeJsonStringWithObject:@"Session closed please login first" andKey:@"error_message"];
        [UserWrapper onPermissionsResult:self withRet:kPermissionFailed withMsg:msg];
        return;
    }
    NSArray *permission = [permision componentsSeparatedByString:@","];
    NSSet* grantedPermissions = [accessToken permissions];
    NSMutableArray* readPermissionNeeded = [[NSMutableArray alloc] init];
    NSMutableArray* publishPermissionNeeded = [[NSMutableArray alloc] init];
    NSSet* PUBLISH_PERMISSIONS = [NSSet setWithObjects:@"publish_actions", @"ads_management", @"create_event",
                                  @"rsvp_event",  @"manage_friendlists", @"manage_notifications",
                                  @"manage_pages", nil];
    for(size_t i = 0; i < permission.count; i++) {
        NSString* pm = [permission objectAtIndex:i];
        if(!grantedPermissions || ![grantedPermissions containsObject:pm]) {
            if([PUBLISH_PERMISSIONS containsObject:pm]) {
                [publishPermissionNeeded addObject:pm];
            } else {
                [readPermissionNeeded addObject:pm];
            }
        }
    }
    
    if([readPermissionNeeded count] == 0 && [publishPermissionNeeded count] == 0) {
        NSString *msg =[ParseUtils MakeJsonStringWithObject:[[accessToken permissions] allObjects] andKey:@"permissions"];
        if(msg!=nil){
            [UserWrapper onPermissionsResult:self withRet:kPermissionSucceed withMsg:msg];
        }else{
            msg = [ParseUtils MakeJsonStringWithObject:@"parse permission data fail" andKey:@"error_message"];
            [UserWrapper onPermissionsResult:self withRet:kPermissionFailed withMsg:msg];
        }
    } else if([readPermissionNeeded count] > 0) {
        FBSDKLoginManager* loginManager = [[FBSDKLoginManager alloc] init];
        UIViewController* rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
        [loginManager logInWithPermissions:readPermissionNeeded
                            fromViewController:rootViewController
                                       handler:^(FBSDKLoginManagerLoginResult *result, NSError *error) {
                                           if (error) {
                                               OUTPUT_LOG(@"Facebook login with read permission error:%ld %@", error.code, error.userInfo[FBSDKErrorDeveloperMessageKey]);
                                               NSString *msg = [ParseUtils MakeJsonStringWithObject:error.description andKey:@"error_message"];
                                               [UserWrapper onPermissionsResult:self withRet:(int)error.code withMsg:msg];
                                           } else if([result isCancelled]) {
                                               NSString *msg = [ParseUtils MakeJsonStringWithObject:@"canceled" andKey:@"error_message"];
                                               [UserWrapper onPermissionsResult:self withRet:kPermissionFailed withMsg:msg];
                                           } else {
                                               FBSDKAccessToken* accessToken = [result token];
                                               if([publishPermissionNeeded count] == 0) {
                                                   NSString *msg =[ParseUtils MakeJsonStringWithObject:[[accessToken permissions] allObjects] andKey:@"permissions"];
                                                   if(msg!=nil){
                                                       [UserWrapper onPermissionsResult:self withRet:kPermissionSucceed withMsg:msg];
                                                   }else{
                                                       msg = [ParseUtils MakeJsonStringWithObject:@"parse permission data fail" andKey:@"error_message"];
                                                       [UserWrapper onPermissionsResult:self withRet:kPermissionFailed withMsg:msg];
                                                   }
                                               } else {
                                                   [loginManager logInWithPermissions:publishPermissionNeeded
                                                                          fromViewController:rootViewController
                                                                                     handler:^(FBSDKLoginManagerLoginResult *result, NSError *error) {
                                                                                         if (error) {
                                                                                             OUTPUT_LOG(@"Facebook login with publish permission error:%ld %@", error.code, error.userInfo[FBSDKErrorDeveloperMessageKey]);
                                                                                             NSString *msg = [ParseUtils MakeJsonStringWithObject:error.description andKey:@"error_message"];
                                                                                             [UserWrapper onPermissionsResult:self withRet:(int)error.code withMsg:msg];
                                                                                         } else if([result isCancelled]) {
                                                                                             NSString *msg = [ParseUtils MakeJsonStringWithObject:@"canceled" andKey:@"error_message"];
                                                                                             [UserWrapper onPermissionsResult:self withRet:kPermissionFailed withMsg:msg];
                                                                                         } else {
                                                                                             NSString *msg =[ParseUtils MakeJsonStringWithObject:[[accessToken permissions] allObjects] andKey:@"permissions"];
                                                                                             if(msg!=nil){
                                                                                                 [UserWrapper onPermissionsResult:self withRet:kPermissionSucceed withMsg:msg];
                                                                                             }else{
                                                                                                 msg = [ParseUtils MakeJsonStringWithObject:@"parse permission data fail" andKey:@"error_message"];
                                                                                                 [UserWrapper onPermissionsResult:self withRet:kPermissionFailed withMsg:msg];
                                                                                             }
                                                                                         }
                                                                                     }];
                                               }
                                           }
                                       }];
        
    } else {
        FBSDKLoginManager* loginManager = [[FBSDKLoginManager alloc] init];
        UIViewController* rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
        [loginManager logInWithPermissions:publishPermissionNeeded
                               fromViewController:rootViewController
                                          handler:^(FBSDKLoginManagerLoginResult *result, NSError *error) {
                                              if (error) {
                                                  OUTPUT_LOG(@"Facebook login error:%ld %@", error.code, error.userInfo[FBSDKErrorDeveloperMessageKey]);
                                                  NSString *msg = [ParseUtils MakeJsonStringWithObject:error.description andKey:@"error_message"];
                                                  [UserWrapper onPermissionsResult:self withRet:(int)error.code withMsg:msg];
                                              } else if([result isCancelled]) {
                                                  NSString *msg = [ParseUtils MakeJsonStringWithObject:@"canceled" andKey:@"error_message"];
                                                  [UserWrapper onPermissionsResult:self withRet:kPermissionFailed withMsg:msg];
                                              } else {
                                                  FBSDKAccessToken* accessToken = [result token];
                                                  NSMutableDictionary *result = [NSMutableDictionary dictionaryWithObjectsAndKeys:[accessToken permissions],@"permissions",[accessToken tokenString],@"accessToken", nil];
                                                  NSString *msg = [ParseUtils NSDictionaryToNSString:result];
                                                  [UserWrapper onPermissionsResult:self withRet:kPermissionSucceed withMsg:msg];
                                              }
                                          }];
        
    }
}

-(void)api:(NSMutableDictionary *)params{
    NSString *graphPath = [params objectForKey:@"Param1"];
    int methodID = [[params objectForKey:@"Param2"] intValue];
    NSString * method = methodID == 0? @"GET":methodID == 1?@"POST":@"DELETE";
    NSDictionary *param = [params objectForKey:@"Param3"];
    int cbId = [[params objectForKey:@"Param4"] intValue];
    [[[FBSDKGraphRequest alloc] initWithGraphPath:graphPath parameters:param HTTPMethod:method] startWithCompletionHandler:^(FBSDKGraphRequestConnection *connection, id result, NSError *error) {
         if(!error){
             NSString *msg = [ParseUtils NSDictionaryToNSString:(NSDictionary *)result];
             if(nil == msg){
                 NSString *msg = [ParseUtils MakeJsonStringWithObject:@"parse result failed" andKey:@"error_message"];
                 [UserWrapper onGraphResult:self withRet:kGraphResultFail withMsg:msg withCallback:cbId];
             }else{
                 OUTPUT_LOG(@"success");
                 [UserWrapper onGraphResult:self withRet:kGraphResultSuccess withMsg:msg withCallback:cbId];
             }
         }else{
             NSString *msg = [ParseUtils MakeJsonStringWithObject:error.description andKey:@"error_message"];
             [UserWrapper onGraphResult:self withRet:(int)error.code withMsg:msg withCallback:cbId];
             OUTPUT_LOG(@"error %@", error.description);
         }
         
     }];
}

-(void)logPurchase:(NSMutableDictionary *)purchaseInfo{
    if(purchaseInfo.count == 2){
        NSNumber *count = [purchaseInfo objectForKey:@"Param1"];
        NSString *currency = [purchaseInfo objectForKey:@"Param2"];
        [FBSDKAppEvents logPurchase:[count floatValue] currency:currency];
    }else if(purchaseInfo.count == 3){
        NSNumber *count = [purchaseInfo objectForKey:@"Param1"];
        NSString *currency = [purchaseInfo objectForKey:@"Param2"];
        NSDictionary *dict = [purchaseInfo objectForKey:@"Param3"];
        [FBSDKAppEvents logPurchase:[count floatValue] currency:currency parameters:dict];
    }
}
@end
