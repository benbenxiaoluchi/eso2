//
//  SSOHelper.m
//  Accenture SSO
//
//  Created by Joshua Lamkin on 9/9/15.
//  Copyright (c) 2015 Accenture. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SSOHelper.h"
#import "KeychainHelper.h"
#import "OAuth2Manager.h"
#import "NetworkReachability.h"
#import "SSOExtensionHelpViewController.h"

@interface SSOHelper() <OAuth2ManagerDelegate>

@property (nonatomic, retain) OAuth2Manager* oAuthManager;
@property (nonatomic, copy) NSString* clientId;
@property (nonatomic, copy) NSString* clientSecret;
@property (nonatomic, copy) NSString* clientRedirectUri;
@property (nonatomic, copy) NSSet* clientScopes;
@property (nonatomic, copy) NSString* currentAccessToken;
@property (nonatomic, copy) NSString* currentRefreshToken;

@end

@implementation SSOHelper

NSString* const SSO_RESPONSE_TYPE = @"code";
NSString* const SSO_GRANT_TYPE = @"authorization_code";

NSString* const SSO_AUTORIZATION_CODE_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/authorize";
NSString* const SSO_TOKEN_REQUEST_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/token";
NSString* const SSO_TOKEN_VALIDATION_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/accesstokenvalidation";
NSString* const SSO_EXTENSION_TYPE_IDENTIFIER_STAGING = @"com.accenture.sso.login.staging";

NSString* const SSO_AUTORIZATION_CODE_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/authorize";
NSString* const SSO_TOKEN_REQUEST_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/token";
NSString* const SSO_TOKEN_VALIDATION_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/accesstokenvalidation";
NSString* const SSO_EXTENSION_TYPE_IDENTIFIER_PRODUCTION = @"com.accenture.sso.login";

NSString* const SSO_REFRESH_TOKEN_LOOKUP_KEY = @"refreshToken";
NSString* const SSO_ACCESS_TOKEN_LOOKUP_KEY = @"accessToken";
NSString* const SSO_HELP_TOKEN_LOOKUP_KEY = @"helpKey";

NSString* const SSO_GROUP = @"7N94X55M7E.com.accenture.sso";

NSString* const SSO_SERVICE_NAME_PRODUCTION = @"service";
NSString* const SSO_SERVICE_NAME_STAGING = @"serviceStaging";

- (instancetype)initWith:(NSString*)clientId
            clientSecret:(NSString*)clientSecret
            clientScopes:(NSSet*)clientScopes
       clientRedirectUri:(NSString*) clientRedirectUri {
    self = [self init];
    self.clientId = clientId;
    self.clientSecret = clientSecret;
    self.clientScopes = clientScopes;
    self.clientRedirectUri = clientRedirectUri;
    self.currentAccessToken = nil;
    self.currentRefreshToken = nil;
    self.inProduction = YES;
    
    return self;
}

- (instancetype)init {
    self.oAuthManager = [[OAuth2Manager alloc] init];
    self.oAuthManager.delegate = self;
    
    return self;
}

- (BOOL) shouldWriteToSharedKeyChain  {
#if defined(SSO_APP) || defined(SSO_EXTENSION)
    return YES;
#else
    return [[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:@"accenture://ssoApp"]];
#endif
}

- (BOOL) shouldShowExtension {
#if defined(SSO_APP) || defined(SSO_EXTENSION)
    return NO;
#else
    return [[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:@"accenture://ssoApp"]];
#endif
}

- (void) getToken {
    
    NSError* error = nil;
    NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
    NSString* accessToken = [KeychainHelper getForKey:SSO_ACCESS_TOKEN_LOOKUP_KEY andServiceName:serviceName error:&error];
    
    NSError* error2 = nil;
    NSString* refreshToken = [self readRefreshToken:&error2];
    
    if (error != nil || accessToken == nil || error2 != nil || refreshToken == nil) {
        self.currentAccessToken = @"";
        [self refreshToken];
    } else {
        [self validateAccessToken:accessToken];
    }
}

- (void) finishGetToken {
    self.currentAccessToken = @"";
    [self refreshToken];
}

- (void) validateAccessToken:(NSString*)accessToken {
    if ([NetworkReachability isNetworkAvailable]) {
        self.currentAccessToken = accessToken;
        NSString* authCodeUri = self.inProduction ? SSO_AUTORIZATION_CODE_URI_PRODUCTION : SSO_AUTORIZATION_CODE_URI_STAGING;
        NSString* validationUri = self.inProduction ? SSO_TOKEN_VALIDATION_URI_PRODUCTION : SSO_TOKEN_VALIDATION_URI_STAGING;
        NSString* tokenRequestUri = self.inProduction ? SSO_TOKEN_REQUEST_URI_PRODUCTION : SSO_TOKEN_REQUEST_URI_STAGING;
        [self.oAuthManager validateAccessToken:accessToken scopes:self.clientScopes clientId:self.clientId clientSecret:self.clientSecret authorizationCodeUri:authCodeUri tokenRequestUri:tokenRequestUri redirectUri:self.clientRedirectUri accessValidationUri:validationUri];
    } else {
        if (self.delegate != nil) {
            [self.delegate errorOccured:[NSError errorWithDomain:@"No Network Connection" code:0 userInfo:nil]];
            self.currentAccessToken = nil;
            self.currentRefreshToken = nil;
        }
    }
}

- (void) signIn {
    [self signOut];
    
    if ([NetworkReachability isNetworkAvailable]) {
        if ([self shouldShowExtension]) {
            NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
            if ([KeychainHelper getForKeyShared:SSO_HELP_TOKEN_LOOKUP_KEY andServiceName:serviceName accessGroup:SSO_GROUP error:nil] == nil) {
                
                UIStoryboard *sb = [UIStoryboard storyboardWithName:@"SSOExtensionHelp" bundle:nil];
                UIViewController *vc = [sb instantiateInitialViewController];
                if ([vc isKindOfClass:[SSOExtensionHelpViewController class]]) {
                    ((SSOExtensionHelpViewController*)vc).delegate = self;
                }
            
                if (self.delegate != nil && [self.delegate respondsToSelector:@selector(presentOAuthViewController:)]) {
                    [self.delegate presentOAuthViewController:vc];
                }
            } else {
                [self launchSSOExtension];
            }
        } else {
            NSString* authCodeUri = self.inProduction ? SSO_AUTORIZATION_CODE_URI_PRODUCTION : SSO_AUTORIZATION_CODE_URI_STAGING;
            NSString* tokenRequestUri = self.inProduction ? SSO_TOKEN_REQUEST_URI_PRODUCTION : SSO_TOKEN_REQUEST_URI_STAGING;
            [self.oAuthManager requestAccess:SSO_RESPONSE_TYPE andGrantType:SSO_GRANT_TYPE scopes:self.clientScopes clientId:self.clientId clientSecret:self.clientSecret authorizationCodeUri:authCodeUri tokenRequestUri:tokenRequestUri redirectUri:self.clientRedirectUri];
        }
    } else {
        if (self.delegate != nil) {
            [self.delegate errorOccured:[NSError errorWithDomain:@"No Network Connection" code:0 userInfo:nil]];
            self.currentAccessToken = nil;
            self.currentRefreshToken = nil;
        }
    }

}

- (void) showSignIn {
    [self launchSSOExtension];
}

- (void) neverShowAgain {
    NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
    [KeychainHelper setForKeyShared:SSO_HELP_TOKEN_LOOKUP_KEY andServiceName:serviceName value:@"read" accessGroup:SSO_GROUP error:nil];
}

- (void) signOut {
    NSError* error;

    if ([self shouldWriteToSharedKeyChain]) {
        [self deleteSharedToken:SSO_REFRESH_TOKEN_LOOKUP_KEY error:error];
    } else {
        [self deleteToken:SSO_REFRESH_TOKEN_LOOKUP_KEY error:error];
    }
    
    [self deleteToken:SSO_ACCESS_TOKEN_LOOKUP_KEY error:error];
}

- (void) deleteToken:(NSString*)key
               error:(NSError*)error {
    NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
    [KeychainHelper removeForKey:key andServiceName:serviceName error:&error];
}

- (void) deleteSharedToken:(NSString*)key
                     error:(NSError*)error {
    NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
    [KeychainHelper removeForKeyShared:key andServiceName:serviceName accessGroup:SSO_GROUP error:&error];
}


- (void) launchSSOExtension {
    NSExtensionItem* item = [NSExtensionItem new];
    NSString* typeIdentifier = self.inProduction ? SSO_EXTENSION_TYPE_IDENTIFIER_PRODUCTION : SSO_EXTENSION_TYPE_IDENTIFIER_STAGING;
    NSItemProvider* provider = [[NSItemProvider alloc] initWithItem:@"notImportant" typeIdentifier:typeIdentifier];
    item.attachments = @[provider];
    UIActivityViewController* controller = [[UIActivityViewController alloc] initWithActivityItems:@[item] applicationActivities:nil];
    controller.completionWithItemsHandler = ^(NSString *activityType, BOOL completed, NSArray *returnedItems, NSError *activityError) {
        [self finishGetToken];
    };
    
    if (self.delegate != nil && [self.delegate respondsToSelector:@selector(presentExtensionViewController:)]) {
        [self.delegate presentExtensionViewController:controller];
    }
}

- (void) saveAccessToken:(NSString*)accessToken
         andRefreshToken:(NSString*)refreshToken {
    NSError* error = nil;

    if (refreshToken != nil) {
        if ([self shouldWriteToSharedKeyChain]) {
            [self saveSharedToken:refreshToken withKey:SSO_REFRESH_TOKEN_LOOKUP_KEY error:&error];
        } else {
            [self saveToken:refreshToken withKey:SSO_REFRESH_TOKEN_LOOKUP_KEY error:&error];
        }
    }
    
    if (error == nil) {
        [self saveToken:accessToken withKey:SSO_ACCESS_TOKEN_LOOKUP_KEY error:&error];
        if (error == nil) {
            if (self.delegate != nil) {
                [self.delegate onAccessTokenUpdate:accessToken];
                self.currentAccessToken = nil;
                self.currentRefreshToken = nil;
            }
        }
    }
    
    if (error != nil) {
        if (self.delegate != nil) {
            [self.delegate errorOccured:error];
            self.currentAccessToken = nil;
            self.currentRefreshToken = nil;
        }
    }
}

- (void) saveSharedToken:(NSString*)token
                 withKey:(NSString*)key
                   error:(NSError**)error {
    NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
    [KeychainHelper setForKeyShared:key andServiceName:serviceName value:token accessGroup:SSO_GROUP error:error];
}

- (void) saveToken:(NSString*)token
           withKey:(NSString*)key
             error:(NSError**)error {
    NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
    [KeychainHelper setForKey:key andServiceName:serviceName value:token error:error];
}

#pragma mark OAuth2ManagerDelegate

- (void)onOAuth2Result:(NSString*)accessToken
       andRefreshToken:(NSString*)refreshToken {
    [self saveAccessToken:accessToken andRefreshToken:refreshToken];
}

- (void)onAccessTokenUpdate:(NSString*)accessToken {
    [self saveAccessToken:accessToken andRefreshToken:self.currentRefreshToken];
}

- (void)tokenValidationResult:(BOOL)valid {
    if (valid) {
        if (self.delegate != nil) {
            [self.delegate onAccessTokenUpdate:self.currentAccessToken];
            self.currentAccessToken = nil;
            self.currentRefreshToken = nil;
        }
    } else {
        [self refreshToken];
    }
}

- (NSString*) readRefreshToken:(NSError**)error {
    
    NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
    if ([self shouldWriteToSharedKeyChain]) {
        return [KeychainHelper getForKeyShared:SSO_REFRESH_TOKEN_LOOKUP_KEY andServiceName:serviceName accessGroup:SSO_GROUP error:error];
    } else {
        return [KeychainHelper getForKey:SSO_REFRESH_TOKEN_LOOKUP_KEY andServiceName:serviceName error:error];
    }
    
}

- (void)setAccessTokenAndRefreshToken {
    
    NSError* error = nil;
    NSString* serviceName = self.inProduction ? SSO_SERVICE_NAME_PRODUCTION : SSO_SERVICE_NAME_STAGING;
    NSString* accessToken = [KeychainHelper getForKey:SSO_ACCESS_TOKEN_LOOKUP_KEY andServiceName:serviceName error:&error];
    
    if (error == nil && accessToken != nil) {
        self.currentAccessToken = accessToken;
    } else {
        self.currentAccessToken = @"";
    }
    
    [self refreshToken];
}

- (void)refreshToken {
    NSError* error = nil;
    NSString* refreshToken = [self readRefreshToken:&error];
    
    if (error != nil || refreshToken == nil) {
        [self signIn];
    } else {
        if ([NetworkReachability isNetworkAvailable]) {
            self.currentRefreshToken = refreshToken;
            NSString* authCodeUri = self.inProduction ? SSO_AUTORIZATION_CODE_URI_PRODUCTION : SSO_AUTORIZATION_CODE_URI_STAGING;
            NSString* tokenRequestUri = self.inProduction ? SSO_TOKEN_REQUEST_URI_PRODUCTION : SSO_TOKEN_REQUEST_URI_STAGING;
            [self.oAuthManager refreshToken:refreshToken andAccessToken:self.currentAccessToken scopes:self.clientScopes clientId:self.clientId clientSecret:self.clientSecret authorizationCodeUri:authCodeUri tokenRequestUri:tokenRequestUri redirectUri:self.clientRedirectUri];
        } else {
            if (self.delegate != nil) {
                [self.delegate errorOccured:[NSError errorWithDomain:@"No Network Connection" code:0 userInfo:nil]];
                self.currentAccessToken = nil;
                self.currentRefreshToken = nil;
            }
        }
    }
}

- (void)presentOAuth2ViewController:(UIViewController*)viewController {
    if (self.delegate != nil && [self.delegate respondsToSelector:@selector(presentOAuthViewController:)]) {
        [self.delegate presentOAuthViewController:viewController];
    }
}

- (void)failedToRefreshToken:(NSError*) error {
    [self signIn];
}

- (void)errorOccured:(NSError*)error {
    if (self.delegate != nil) {
        [self.delegate errorOccured:error];
        self.currentAccessToken = nil;
        self.currentRefreshToken = nil;
    }
}

- (void) networkErrorOccurred:(NSString *)error {
    if (self.delegate != nil) {
        [self.delegate errorOccuredMessage:error];
        self.currentAccessToken = nil;
        self.currentRefreshToken = nil;
    }
}

- (BOOL)doCredentialsExist {
    NSError* error = nil;
    NSString* refreshToken = [self readRefreshToken:&error];
    
    return error == nil && refreshToken != nil;
}

- (void)useExistingCredentials {
    [self getToken];
}

@end