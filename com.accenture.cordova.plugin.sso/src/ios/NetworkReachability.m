//
//  NetworkReachability.m
//  Accenture SSO
//
//  Created by Joshua Lamkin on 9/18/15.
//  Copyright (c) 2015 Accenture. All rights reserved.
//

#import "NetworkReachability.h"
#import <SystemConfiguration/SCNetworkReachability.h>

@implementation NetworkReachability

+(bool)isNetworkAvailable
{
    SCNetworkReachabilityFlags flags;
    SCNetworkReachabilityRef address;
    address = SCNetworkReachabilityCreateWithName(NULL, "www.accenture.com" );
    Boolean success = SCNetworkReachabilityGetFlags(address, &flags);
    CFRelease(address);
    
    bool canReach = success
    && !(flags & kSCNetworkReachabilityFlagsConnectionRequired)
    && (flags & kSCNetworkReachabilityFlagsReachable);
    
    return canReach;
}

@end
