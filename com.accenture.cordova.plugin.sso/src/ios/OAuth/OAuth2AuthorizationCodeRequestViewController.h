//
//  OAuth2AuthorizationCodeRequestViewController.h
//  HelloCordova
//
//  Created by Joshua Lamkin on 8/11/15.
//
//

#import <UIKit/UIKit.h>


@protocol OAuth2AuthorizationCodeRequestDelegate <NSObject>
@required
- (void)sendOAuth2Result:(NSString*)accessToken andRefreshToken:(NSString*)refreshToken;
- (void)errorOccured:(NSError*)error;
- (BOOL)doCredentialsExist;
- (void)useExistingCredentials;

@end


@interface OAuth2AuthorizationCodeRequestViewController : UIViewController <UIWebViewDelegate>

@property (weak, nonatomic) IBOutlet UIWebView *webview;
@property (weak, nonatomic) IBOutlet UIActivityIndicatorView *spinner;
@property (nonatomic, weak) id <OAuth2AuthorizationCodeRequestDelegate> delegate;

@property (nonatomic, readwrite) NSString* responseType;
@property (nonatomic, readwrite) NSString* grantType;
@property (nonatomic, readwrite) NSSet* scope;
@property (nonatomic, readwrite) NSString* clientId;
@property (nonatomic, readwrite) NSString* clientSecret;
@property (nonatomic, readwrite) NSString* authorizationCodeUrl;
@property (nonatomic, readwrite) NSString* tokenRequestUrl;
@property (nonatomic, readwrite) NSString* redirectUrl;

@end

