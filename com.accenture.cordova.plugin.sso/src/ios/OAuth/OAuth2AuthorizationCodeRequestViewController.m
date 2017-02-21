        //
//  OAuth2AuthorizationCodeRequestViewController.m
//  HelloCordova
//
//  Created by Joshua Lamkin on 8/11/15.
//
//

#import "OAuth2AuthorizationCodeRequestViewController.h"
#import "NXOAuth2AccountStore.h"
#import "NXOAuth2AccessToken.h"
#import "NXOAuth2Account.h"

@interface OAuth2AuthorizationCodeRequestViewController ()

@property (nonatomic, retain) id accountDidChangeNotificationObserver;
@property (nonatomic, retain) id accountDidFailNotificationObserver;
@property (retain, nonatomic) IBOutlet UIWebView *privateWebView;
@property (retain, nonatomic) IBOutlet UIActivityIndicatorView *privateSpinner;
@end

@implementation OAuth2AuthorizationCodeRequestViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self clearCookies];
    
    if (self.webview == nil) {
        self.privateWebView = [[UIWebView alloc] initWithFrame:self.view.frame];
        self.privateWebView.delegate = self;
        [self.view addSubview:self.privateWebView];
    }
    
    if (self.spinner == nil) {
        self.privateSpinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
        CGRect spinnerFrame = self.privateSpinner.frame;
        spinnerFrame.origin.y = self.view.frame.size.height / 2;
        spinnerFrame.origin.x = self.view.frame.size.width / 2;
        self.privateSpinner.frame = spinnerFrame;
        [self.view addSubview:self.privateSpinner];
    }
    
    dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
        //Background Thread
        [[NXOAuth2AccountStore sharedStore] setClientID:self.clientId
                                                 secret:self.clientSecret
                                                  scope:self.scope
                                       authorizationURL:[NSURL URLWithString:self.authorizationCodeUrl]
                                               tokenURL:[NSURL URLWithString:self.tokenRequestUrl]
                                            redirectURL:[NSURL URLWithString:self.redirectUrl]
                                          keyChainGroup:@"accentureSSO"
                                         forAccountType:@"accentureSSO"];
        
        NSMutableDictionary *configuration = [NSMutableDictionary dictionaryWithDictionary:[[NXOAuth2AccountStore sharedStore] configurationForAccountType:@"accentureSSO"]];
        NSDictionary *customHeaderFields = [NSDictionary dictionaryWithObject:@"application/x-www-form-urlencoded" forKey:@"Content-Type"];
        [configuration setObject:customHeaderFields forKey:kNXOAuth2AccountStoreConfigurationCustomHeaderFields];
        [[NXOAuth2AccountStore sharedStore] setConfiguration:configuration forAccountType:@"accentureSSO"];
        
        [[NXOAuth2AccountStore sharedStore] requestAccessToAccountWithType:@"accentureSSO"
                                       withPreparedAuthorizationURLHandler:^(NSURL *preparedURL){
                                           dispatch_async(dispatch_get_main_queue(), ^(void){
                                               //Run UI Updates
                                               if (self.privateWebView != nil) {
                                                   [self.privateWebView loadRequest:[NSURLRequest requestWithURL:preparedURL]];
                                               }
                                               if (self.webview != nil) {
                                                   [self.webview loadRequest:[NSURLRequest requestWithURL:preparedURL]];
                                               }
                                           });
                                       }];
    });
    
  }

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self registerForNotificaitons];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(checkForCredentials:)
                                                 name:UIApplicationDidBecomeActiveNotification
                                               object:nil];
    
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(willResignActive:)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:nil];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidBecomeActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillResignActiveNotification object:nil];

}

- (void)checkForCredentials:(NSNotification*)notif {
    if (self.delegate != nil && [self.delegate doCredentialsExist]) {
        [self.delegate useExistingCredentials];
#ifndef SSO_EXTENSION
        [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
#endif
    } else {
        [self registerForNotificaitons];
    }
}

- (void)willResignActive:(NSNotification*)notif {
    [self unregisterForNotifications];
}

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType {
    NSRange range = [request.URL.absoluteString rangeOfString:self.redirectUrl options:NSCaseInsensitiveSearch];
    NSRange range2 = [request.URL.absoluteString rangeOfString:@"code=" options:NSCaseInsensitiveSearch];
    if (range.location != NSNotFound && range.location == 0) {
        if (range2.location != NSNotFound) {
            [[NXOAuth2AccountStore sharedStore] handleRedirectURL:[NSURL URLWithString:request.URL.absoluteString]];
            return NO;
        } else {
            NSRange range3 = [request.URL.absoluteString rangeOfString:@"error=" options:NSCaseInsensitiveSearch];
            NSString* errorMessage = @"ESO issue";
            if (range3.location != NSNotFound) {
                errorMessage = [request.URL.absoluteString substringFromIndex:range3.location+6];
            }
            NSError *error = [NSError errorWithDomain:errorMessage code:0 userInfo:nil];
            
            if (self.delegate != nil) {
                [self.delegate errorOccured:error];
            }
            
            [self unregisterForNotifications];
#ifndef SSO_EXTENSION
            [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
#endif
        }
    }
    return YES;
}

- (void)webViewDidStartLoad:(UIWebView *)webView {
    if (self.privateSpinner != nil) {
        [self.privateSpinner startAnimating];
    }
    if (self.spinner != nil) {
        [self.spinner startAnimating];
    }
}

- (void)webViewDidFinishLoad:(UIWebView *)webView {
    if (self.privateSpinner != nil) {
        [self.privateSpinner stopAnimating];
    }
    if (self.spinner != nil) {
        [self.spinner stopAnimating];
    }
}

- (void)webView:(UIWebView *)webView didFailLoadWithError:(NSError *)error {
    if ([error.domain isEqual: @"WebKitErrorDomain"]) {
        NSURL* failingUrl = [error.userInfo objectForKey:@"NSErrorFailingURLKey"];
        NSRange range = [failingUrl.absoluteString rangeOfString:self.redirectUrl options:NSCaseInsensitiveSearch];
        if (range.location == NSNotFound) {
            if (self.delegate != nil) {
                [self.delegate errorOccured:error];
            }
            [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
        }
    } else {
        if (self.delegate != nil) {
            [self.delegate errorOccured:error];
        }
        [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
    }
}

- (void)registerForNotificaitons {

    self.accountDidChangeNotificationObserver = [[NSNotificationCenter defaultCenter] addObserverForName:NXOAuth2AccountStoreAccountsDidChangeNotification
                                                      object:[NXOAuth2AccountStore sharedStore]
                                                       queue:nil
                                                  usingBlock:^(NSNotification *aNotification){
                                                      
                                                      if ([aNotification.userInfo objectForKey:NXOAuth2AccountStoreNewAccountUserInfoKey] != nil) {
                                                          NXOAuth2Account *account = [aNotification.userInfo objectForKey:NXOAuth2AccountStoreNewAccountUserInfoKey];
                                                          NXOAuth2AccessToken *token = account.accessToken;
                                                          
                                                          if (self.delegate != nil) {
                                                              [self.delegate sendOAuth2Result:token.accessToken andRefreshToken:token.refreshToken];
                                                          }
                                                          
                                                          [self unregisterForNotifications];
                                                          [self clearCookies];
#ifndef SSO_EXTENSION
                                                          [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
#endif
                                                      }
                                                      
                                                  }];
    
    self.accountDidFailNotificationObserver = [[NSNotificationCenter defaultCenter] addObserverForName:NXOAuth2AccountStoreDidFailToRequestAccessNotification
                                                      object:[NXOAuth2AccountStore sharedStore]
                                                       queue:nil
                                                  usingBlock:^(NSNotification *aNotification){
                                                      NSError *error = [aNotification.userInfo objectForKey:NXOAuth2AccountStoreErrorKey];
                                                      
                                                      if (self.delegate != nil) {
                                                          [self.delegate errorOccured:error];
                                                      }
                                                      
                                                      [self unregisterForNotifications];
                                                      [self clearCookies];
#ifndef SSO_EXTENSION
                                                      [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
#endif

                                                  }];
}

- (void)unregisterForNotifications {
    if (self.accountDidChangeNotificationObserver != nil) {
        [[NSNotificationCenter defaultCenter] removeObserver:self.accountDidChangeNotificationObserver];
        self.accountDidChangeNotificationObserver = nil;
    }
    if (self.accountDidFailNotificationObserver != nil) {
        [[NSNotificationCenter defaultCenter] removeObserver:self.accountDidFailNotificationObserver];
        self.accountDidFailNotificationObserver = nil;
    }
}

- (void)clearCookies {
    NSHTTPCookie *cookie;
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for (cookie in [storage cookies]) {
        [storage deleteCookie:cookie];
    }
}

@end
