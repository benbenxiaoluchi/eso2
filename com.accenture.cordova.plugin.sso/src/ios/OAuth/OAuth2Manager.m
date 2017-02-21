//
//  CryptoPlugin.m
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

#import "OAuth2Manager.h"
#import "NXOAuth2AccountStore.h"
#import "OAuth2AuthorizationCodeRequestViewController.h"
#import "NXOAuth2AccessToken.h"
#import "NXOAuth2Account.h"
#import "NXOAuth2Account+Private.h"
#import "NXOAuth2Client.h"
#import "NXOAuth2Connection.h"

@interface OAuth2Manager() <NXOAuth2ConnectionDelegate>

@property (nonatomic, retain) NXOAuth2Account* account;

@property (nonatomic, retain) id accessTokenRefreshDidChangeNotificationObserver;
@property (nonatomic, retain) id accessTokenRefreshDidFailNotificationObserver;

@end

@implementation OAuth2Manager

- (void)requestAccess:(NSString*)responseType
         andGrantType:(NSString*)grantType
               scopes:(NSSet*)scopes
             clientId:(NSString*)clientId
         clientSecret:(NSString*)clientSecret
 authorizationCodeUri:(NSString*)authorizationCodeUrl
      tokenRequestUri:(NSString*)tokenRequestUrl
          redirectUri:(NSString*)redirectUrl {
    
    OAuth2AuthorizationCodeRequestViewController *oAuth2Controller = [[OAuth2AuthorizationCodeRequestViewController alloc] init];
    oAuth2Controller.redirectUrl = redirectUrl;
    oAuth2Controller.responseType = responseType;
    oAuth2Controller.grantType = grantType;
    oAuth2Controller.scope = scopes;
    oAuth2Controller.clientId = clientId;
    oAuth2Controller.clientSecret = clientSecret;
    oAuth2Controller.authorizationCodeUrl = authorizationCodeUrl;
    oAuth2Controller.tokenRequestUrl = tokenRequestUrl;
    oAuth2Controller.delegate = self;
    
    if (self.delegate) {
        [self.delegate presentOAuth2ViewController:oAuth2Controller];
    }
}

- (void)refreshToken:(NSString*)refreshToken
      andAccessToken:(NSString*)accessToken
              scopes:(NSSet*)scopes
            clientId:(NSString*)clientId
        clientSecret:(NSString*)clientSecret
authorizationCodeUri:(NSString*)authorizationCodeUrl
     tokenRequestUri:(NSString*)tokenRequestUrl
         redirectUri:(NSString*)redirectUrl {
    
    [[NXOAuth2AccountStore sharedStore] setClientID:clientId
                                             secret:clientSecret
                                              scope:scopes
                                   authorizationURL:[NSURL URLWithString:authorizationCodeUrl]
                                           tokenURL:[NSURL URLWithString:tokenRequestUrl]
                                        redirectURL:[NSURL URLWithString:redirectUrl]
                                      keyChainGroup:@"accentureSSO"
                                     forAccountType:@"accentureSSO"];
    
    NSMutableDictionary *configuration = [NSMutableDictionary dictionaryWithDictionary:[[NXOAuth2AccountStore sharedStore] configurationForAccountType:@"accentureSSO"]];
    NSDictionary *customHeaderFields = [NSDictionary dictionaryWithObject:@"application/x-www-form-urlencoded" forKey:@"Content-Type"];
    [configuration setObject:customHeaderFields forKey:kNXOAuth2AccountStoreConfigurationCustomHeaderFields];
    
    NXOAuth2AccessToken* anAccessToken = [[NXOAuth2AccessToken alloc] initWithAccessToken:accessToken refreshToken:refreshToken expiresAt:nil];
    
    [[NXOAuth2AccountStore sharedStore] setConfiguration:configuration forAccountType:@"accentureSSO"];
    
    self.account = [[NXOAuth2Account alloc] initAccountWithAccessToken:anAccessToken accountType:@"accentureSSO"];
    [[NXOAuth2AccountStore sharedStore] addAccount:self.account];
    
    NXOAuth2Client *client = [self.account oauthClient];
    client.desiredScope = scopes;
    
    [self registerForNotificaitons:self.account];
    
    NSString *authStr = [NSString stringWithFormat:@"%@:%@", clientId, clientSecret];
    NSData *authData = [authStr dataUsingEncoding:NSUTF8StringEncoding];
    NSString *authValue = [authData base64EncodedStringWithOptions:0];
    NSString *authHeader = [NSString stringWithFormat:@"Basic %@", authValue];
    
    [client refreshAccessTokenAndRetryConnection:nil withConentType:@"application/x-www-form-urlencoded" authorizationHeader:authHeader];
}


- (void)registerForNotificaitons:(NXOAuth2Account* )account {
    
    self.accessTokenRefreshDidChangeNotificationObserver = [[NSNotificationCenter defaultCenter] addObserverForName:NXOAuth2AccountDidChangeAccessTokenNotification
                                                                                                             object:account
                                                                                                              queue:nil
                                                                                                         usingBlock:^(NSNotification *aNotification){
                                                                                                             
                                                                                                             if (self.delegate != nil) {
                                                                                                                 [self.delegate onAccessTokenUpdate:self.account.accessToken.accessToken];
                                                                                                             }
                                                                                                             
                                                                                                             [self unregisterForNotifications];
                                                                                                             [self removeAccount];
                                                                                                         }];
    
    self.accessTokenRefreshDidFailNotificationObserver = [[NSNotificationCenter defaultCenter] addObserverForName:NXOAuth2AccountDidFailToGetAccessTokenNotification
                                                                                                           object:account
                                                                                                            queue:nil
                                                                                                       usingBlock:^(NSNotification *aNotification){
                                                                                                           
                                                                                                           NSError *error = [aNotification.userInfo objectForKey:NXOAuth2AccountStoreErrorKey];
                                                                                                           if (self.delegate != nil) {
                                                                                                                if ([self isNetworkError:error]) {
                                                                                                                    [self.delegate networkErrorOccurred:[NSString stringWithFormat:@"Network Error %@",error.localizedDescription]];
                                                                                                                }
                                                                                                                else {
                                                                                                                    [self.delegate failedToRefreshToken:error];
                                                                                                                }
                                                                                                           }
                                                          
                                                                                                           [self unregisterForNotifications];
                                                                                                           [self removeAccount];
                                                                                                       }];
}

- (void)unregisterForNotifications {
    [[NSNotificationCenter defaultCenter] removeObserver:self.accessTokenRefreshDidChangeNotificationObserver];
    [[NSNotificationCenter defaultCenter] removeObserver:self.accessTokenRefreshDidFailNotificationObserver];
}

- (void)removeAccount {
    NSArray *accounts = [[NXOAuth2AccountStore sharedStore] accountsWithAccountType:@"accentureSSO"];
    if (accounts != nil && accounts.count > 0) {
        NXOAuth2Account *account = [accounts objectAtIndex:0];
        [[NXOAuth2AccountStore sharedStore] removeAccount:account];
    }
}

- (void) validateAccessToken:(NSString*)accessToken
                      scopes:(NSSet*)scopes
                    clientId:(NSString*)clientId
                clientSecret:(NSString*)clientSecret
        authorizationCodeUri:(NSString*)authorizationCodeUrl
             tokenRequestUri:(NSString*)tokenRequestUrl
                 redirectUri:(NSString*)redirectUrl
         accessValidationUri:(NSString*)accessValidationUrl  {
    
    [[NXOAuth2AccountStore sharedStore] setClientID:clientId
                                             secret:clientSecret
                                              scope:scopes
                                   authorizationURL:[NSURL URLWithString:authorizationCodeUrl]
                                           tokenURL:[NSURL URLWithString:tokenRequestUrl]
                                        redirectURL:[NSURL URLWithString:redirectUrl]
                                      keyChainGroup:@"accentureSSO"
                                     forAccountType:@"accentureSSO"];
    
    NSMutableDictionary *configuration = [NSMutableDictionary dictionaryWithDictionary:[[NXOAuth2AccountStore sharedStore] configurationForAccountType:@"accentureSSO"]];
    NSDictionary *customHeaderFields = [NSDictionary dictionaryWithObject:@"application/x-www-form-urlencoded" forKey:@"Content-Type"];
    [configuration setObject:customHeaderFields forKey:kNXOAuth2AccountStoreConfigurationCustomHeaderFields];
    
    [[NXOAuth2AccountStore sharedStore] setConfiguration:configuration forAccountType:@"accentureSSO"];
    
    NXOAuth2Account* account = [[NXOAuth2Account alloc] initAccountWithAccessToken:nil accountType:@"accentureSSO"];
    [[NXOAuth2AccountStore sharedStore] addAccount:account];
    NXOAuth2Client *client = [account oauthClient];
    
    NSMutableURLRequest *tokenRequest = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:accessValidationUrl]];
    [tokenRequest setHTTPMethod:@"GET"];
    
    NSMutableDictionary *parameters = [NSMutableDictionary dictionaryWithObjectsAndKeys:
                                       accessToken, @"token",
                                       nil];
    
    NSString *authStr = [NSString stringWithFormat:@"%@:%@", clientId, clientSecret];
    NSData *authData = [authStr dataUsingEncoding:NSUTF8StringEncoding];
    NSString *authValue = [authData base64EncodedStringWithOptions:0];
    NSString *authHeader = [NSString stringWithFormat:@"Basic %@", authValue];
    [tokenRequest setValue:authHeader forHTTPHeaderField:@"Authorization"];
    
    (void) [[NXOAuth2Connection alloc] initWithRequest:tokenRequest
                                     requestParameters:parameters
                                           oauthClient:client
                                              delegate:self];
}


#pragma mark NXOAuth2ConnectionDelegate

- (void)oauthConnection:(NXOAuth2Connection *)connection didFinishWithData:(NSData *)data
{
    [self removeAccount];
    if (self.delegate) {
        [self.delegate tokenValidationResult:YES];
    }
}

- (void)oauthConnection:(NXOAuth2Connection *)connection didFailWithError:(NSError *)error
{
    [self removeAccount];
    if (self.delegate != nil) {
        if ([self isNetworkError:error]) {
            [self.delegate networkErrorOccurred:[NSString stringWithFormat:@"Network Error %@",error.localizedDescription]];
        }
        else {
            [self.delegate tokenValidationResult:NO];
        }
    }
}

#pragma mark OAuth2AuthorizationCodeRequestDelegate

- (void)sendOAuth2Result:(NSString*)accessToken andRefreshToken:(NSString*)refreshToken {
    [self removeAccount];
    if (self.delegate) {
        [self.delegate onOAuth2Result:accessToken andRefreshToken:refreshToken];
    }
}

- (void)errorOccured:(NSError*)error {
    [self removeAccount];
    if (self.delegate != nil) {
        if ([self isNetworkError:error]) {
            [self.delegate networkErrorOccurred:[NSString stringWithFormat:@"Network Error %@",error.localizedDescription]];
        }
        else {
            [self.delegate errorOccured:error];
        }
    }
}

- (BOOL)doCredentialsExist {
    BOOL exist = NO;
    
    if (self.delegate != nil) {
        exist = [self.delegate doCredentialsExist];
    }
    
    return exist;
}

- (BOOL)isNetworkError:(NSError *)error
{
    if ([error.domain isEqualToString:NSURLErrorDomain]) {
        return (   error.code == NSURLErrorTimedOut
                || error.code == NSURLErrorCannotConnectToHost
                || error.code == NSURLErrorNetworkConnectionLost
                || error.code == NSURLErrorDNSLookupFailed
                || error.code == NSURLErrorNotConnectedToInternet
                || error.code == NSURLErrorSecureConnectionFailed
                || error.code == NSURLErrorCannotLoadFromNetwork);
    }
    return false;
}

- (void)useExistingCredentials {
    if (self.delegate != nil) {
        [self.delegate useExistingCredentials];
    }
}

@end
