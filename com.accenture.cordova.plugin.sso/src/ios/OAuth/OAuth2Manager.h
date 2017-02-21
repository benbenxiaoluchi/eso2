//
//  CryptoPlugin.h
//
//  Copyright 2015 Accenture
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

#import <UIKit/UIKit.h>
#import "OAuth2AuthorizationCodeRequestViewController.h"

@protocol OAuth2ManagerDelegate <NSObject>
@required
- (void)onOAuth2Result:(NSString*)accessToken andRefreshToken:(NSString*)refreshToken;
- (void)onAccessTokenUpdate:(NSString*)accessToken;
- (void)tokenValidationResult:(BOOL)valid;
- (void)presentOAuth2ViewController:(UIViewController*)viewController;
- (void)failedToRefreshToken:(NSError*)error;
- (void)errorOccured:(NSError*)error;
- (BOOL)doCredentialsExist;
- (void)useExistingCredentials;
- (void)networkErrorOccurred:(NSString*)error;

@end

@interface OAuth2Manager : NSObject <OAuth2AuthorizationCodeRequestDelegate>

@property (nonatomic, weak) id <OAuth2ManagerDelegate> delegate;

- (void)requestAccess:(NSString*)responseType
         andGrantType:(NSString*)grantType
               scopes:(NSSet*)scopes
             clientId:(NSString*)clientId
         clientSecret:(NSString*)clientSecret
 authorizationCodeUri:(NSString*)authorizationCodeUrl
      tokenRequestUri:(NSString*)tokenRequestUrl
          redirectUri:(NSString*)redirectUrl;

- (void)refreshToken:(NSString*)refreshToken
      andAccessToken:(NSString*)accessToken
              scopes:(NSSet*)scopes
            clientId:(NSString*)clientId
        clientSecret:(NSString*)clientSecret
authorizationCodeUri:(NSString*)authorizationCodeUrl
     tokenRequestUri:(NSString*)tokenRequestUrl
         redirectUri:(NSString*)redirectUrl;

- (void) validateAccessToken:(NSString*)accessToken
                      scopes:(NSSet*)scopes
                    clientId:(NSString*)clientId
                clientSecret:(NSString*)clientSecret
        authorizationCodeUri:(NSString*)authorizationCodeUrl
             tokenRequestUri:(NSString*)tokenRequestUrl
                 redirectUri:(NSString*)redirectUrl
         accessValidationUri:(NSString*)accessValidationUrl;


@end
