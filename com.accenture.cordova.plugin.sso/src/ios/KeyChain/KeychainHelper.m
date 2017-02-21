/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#import "KeychainHelper.h"
#import "UICKeyChainStore.h"

@implementation KeychainHelper

+ (NSString*) getForKey:(NSString*)key
         andServiceName:(NSString *)serviceName
                  error:(NSError**)error
{
    return [UICKeyChainStore stringForKey:key error:error];
}

+ (BOOL) setForKey:(NSString*)key
    andServiceName:(NSString*)serviceName
             value:(NSString*)value
             error:(NSError**)error
{
    return [UICKeyChainStore setString:value forKey:key error:error];
}

+ (BOOL) removeForKey:(NSString*)key
       andServiceName:(NSString*)serviceName
                error:(NSError**)error
{
    return [UICKeyChainStore removeItemForKey:key error:error];
}

#pragma mark - Shared

+ (NSString*) getForKeyShared:(NSString*)key
               andServiceName:(NSString*)serviceName
                  accessGroup:(NSString*)accessGroup
                        error:(NSError**)error
{
    return [UICKeyChainStore stringForKey:key service:serviceName accessGroup:accessGroup error:error];
}

+ (BOOL) setForKeyShared:(NSString*)key
          andServiceName:(NSString*)serviceName
                   value:(NSString*)value
             accessGroup:(NSString*)accessGroup
                   error:(NSError**)error
{
    return [UICKeyChainStore setString:value forKey:key service:serviceName accessGroup:accessGroup error:error];
}

+ (BOOL) removeForKeyShared:(NSString*)key
             andServiceName:(NSString*)serviceName
                accessGroup:(NSString*)accessGroup
                      error:(NSError**)error
{
    return [UICKeyChainStore removeItemForKey:key service:serviceName accessGroup:accessGroup error:error];
}

@end

