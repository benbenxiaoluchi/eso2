//
//  ACEPlugin.m
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

#import "SSOPlugin.h"
#import "SSOHelper.h"

@interface SSOPlugin () <SSOHelperDelegate>

@property (nonatomic, retain) SSOHelper* ssoHelper;
@property (nonatomic, retain) CDVInvokedUrlCommand* command;

@end

@implementation SSOPlugin

- (void)getToken:(CDVInvokedUrlCommand *)command { 
    self.command = command;

    NSString *clientId = [command.arguments objectAtIndex:0];
    NSString *clientSecret = [command.arguments objectAtIndex:1];
    NSArray *scopeArray = [command.arguments objectAtIndex:2];
    NSString *redirectUrl = [command.arguments objectAtIndex:3];
    BOOL inProduction = [[command.arguments objectAtIndex:4] boolValue];
    
    self.ssoHelper = [[SSOHelper alloc] initWith:clientId clientSecret:clientSecret clientScopes:[NSSet setWithArray:scopeArray] clientRedirectUri:redirectUrl];
    self.ssoHelper.delegate = self;
    self.ssoHelper.inProduction = inProduction;
    [self.ssoHelper getToken];
}

- (void)logout:(CDVInvokedUrlCommand *)command { 
    self.command = command;
    
    NSString *clientId = [command.arguments objectAtIndex:0];
    NSString *clientSecret = [command.arguments objectAtIndex:1];
    NSArray *scopeArray = [command.arguments objectAtIndex:2];
    NSString *redirectUrl = [command.arguments objectAtIndex:3];
    BOOL inProduction = [[command.arguments objectAtIndex:4] boolValue];
    
    self.ssoHelper = [[SSOHelper alloc] initWith:clientId clientSecret:clientSecret clientScopes:[NSSet setWithArray:scopeArray] clientRedirectUri:redirectUrl];
    self.ssoHelper.delegate = self;
    self.ssoHelper.inProduction = inProduction;
    [self.ssoHelper signOut];
}

#pragma mark SSOHelperDelegate

- (void)onAccessTokenUpdate:(NSString*)accessToken {
    CDVPluginResult *pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:accessToken];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
}

- (void)presentOAuthViewController:(UIViewController*)viewController {
    dispatch_async(dispatch_get_main_queue(), ^(void){
        [self.viewController presentViewController:viewController animated:YES completion:nil];
    });
}

- (void)presentExtensionViewController:(UIViewController *)viewController {
    dispatch_async(dispatch_get_main_queue(), ^(void){
        [self.viewController presentViewController:viewController animated:YES completion:nil];
    });
}

- (void)errorOccured:(NSError*)error {
    CDVPluginResult *pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
}

- (void)errorOccuredMessage:(NSString*)error {
    CDVPluginResult *pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
}

@end
