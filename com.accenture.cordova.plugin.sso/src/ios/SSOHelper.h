//
//  SSOHelper.h
//  Accenture SSO
//
//  Created by Joshua Lamkin on 9/9/15.
//  Copyright (c) 2015 Accenture. All rights reserved.
//

#import <UIKit/UIKit.h>

@protocol SSOHelperDelegate <NSObject>
@required
- (void)onAccessTokenUpdate:(NSString*)accessToken;
- (void)errorOccured:(NSError*)error;
- (void)errorOccuredMessage:(NSString*)error;
@optional
- (void)presentOAuthViewController:(UIViewController*)viewController;
- (void)presentExtensionViewController:(UIViewController*)viewController;

@end

extern NSString* const SSO_RESPONSE_TYPE;
extern NSString* const SSO_GRANT_TYPE;
extern NSString* const SSO_STATE;
extern NSString* const SSO_AUTORIZATION_CODE_URI_STAGING;
extern NSString* const SSO_TOKEN_REQUEST_URI_STAGING;
extern NSString* const SSO_TOKEN_VALIDATION_URI_STAGING;
extern NSString* const SSO_AUTORIZATION_CODE_URI_PRODUCTION;
extern NSString* const SSO_TOKEN_REQUEST_URI_PRODUCTION;
extern NSString* const SSO_TOKEN_VALIDATION_URI_PRODUCTION;


@interface SSOHelper : NSObject

@property (nonatomic, weak) id <SSOHelperDelegate> delegate;
@property (nonatomic) BOOL inProduction;


- (instancetype)initWith:(NSString*)clientId
            clientSecret:(NSString*)clientSecret
            clientScopes:(NSSet*)clientScopes
       clientRedirectUri:(NSString*)clientRedirectUri;

- (void) getToken;

- (void) saveAccessToken:(NSString*)accessToken
         andRefreshToken:(NSString*)refreshToken;

- (void) signOut;

@end
