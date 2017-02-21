# ESO Plugin Integration guide

### Assumes working knowledge of Cordova and Git

#### Requirements

* The existing project must be target Android API 18+ and iOS8+
* Must be using Cordova Android 4.0.2 or higher
* Must be using Cordova iOS 3.8.0 or higher

#### Steps to add plugin
1.	Add Plugin to existing cordova project by calling ```Cordova plugin add https://innersource.accenture.com/cio-security-architecture/eso-library-for-cordova.git```
2.	Update java script to call SSO plugin 
	* clientId, clientSecret, callbackUri are all strings. The Uri should not be escaped.
	* clientScopes is an array of strings.
	* Production flag is a Boolean (true/false)
```
	sso.getToken(function(token) {
					//handle token
                }, function(error) {
					//handle error
                }, [CLIENT ID], [CLIENT SECRET], [Array of client scopes], [Callback URI],[ Production flag]);
```

**Please note that this plugin makes a network call to validate the token, so do not call it every time you need a token. The token should be cached in memory and this plugin called on application launch and whenever you receive a 401 HTTP error.**

Click here to see [sample application one] and [sample application two]

To obtain a clientId, clientSecret, callbackURI and scopes for your services [click this link and fill out the forms].

#### CI

#### iOS
There is an occasional issue with the hook not triggering with the plugin (we are working on it) but to ensure no issues add the following steps to your CI…

```
rm plugin
rm platform
add platform
add plugin
```

or

* Please ensure you are not adding and/or removing platforms inside the CI job
* Please ensure you are not adding and/or removing plugins inside the CI job


# ESO Plugin Maintenance guide

### 	How to update staging and production URLs

#### iOS

1.	Checkout [Plugin InnerSource Repo] into ```[directory]``` using [git]
2.	Open ```[directory]/src/ios/SSOHelper.m``` 
3.	There you will see
```
NSString* const SSO_AUTORIZATION_CODE_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/authorize";
NSString* const SSO_TOKEN_REQUEST_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/token";
NSString* const SSO_TOKEN_VALIDATION_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/accesstokenvalidation";

NSString* const SSO_AUTORIZATION_CODE_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/authorize";
NSString* const SSO_TOKEN_REQUEST_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/token";
NSString* const SSO_TOKEN_VALIDATION_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/accesstokenvalidation";
```
4.	Update them to the desired values
5.	Commit them using [git] and push them to the origin (innersource)

#### Android

1.	Checkout [Plugin InnerSource Repo] into ```[directory]``` using [git]
2.	Open ```[directory]/src/ios/SSOHelper.m``` 
3.	There you will see
```
NSString* const SSO_AUTORIZATION_CODE_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/authorize";
NSString* const SSO_TOKEN_REQUEST_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/token";
NSString* const SSO_TOKEN_VALIDATION_URI_STAGING = @"https://federation-sts-stage.accenture.com/oauth/ls/connect/accesstokenvalidation";

NSString* const SSO_AUTORIZATION_CODE_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/authorize";
NSString* const SSO_TOKEN_REQUEST_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/token";
NSString* const SSO_TOKEN_VALIDATION_URI_PRODUCTION = @"https://federation-sts.accenture.com/oauth/ls/connect/accesstokenvalidation";
```
4.	Update them to the desired values
5.	Commit them using [git] and push them to the origin (innersource)

### After you make changes

#### Update plugin version

1.	Checkout [Plugin InnerSource Repo] into ```[directory]``` using [git]
2.	Open ```[directory]/plugin.xml``` 
3. There you will see
```
<plugin xmlns="http://apache.org/cordova/ns/plugins/1.0"
	xmlns:android="http://schemas.android.com/apk/res/android"
	id="com.accenture.cordova.plugin.sso"
	version="1.0.0">
```
4. Update ```version="1.0.0"``` to desired value.
5.	Commit them using [git] and push them to the origin (innersource)
6. Update sample applications following integration steps from plugin readme.md
7. Then push updated sample applications using [git]

 [click this link and fill out the forms]:https://ts.accenture.com/sites/FIM/EnterpriseSignOn/_layouts/15/start.aspx#/Lists/Mobile%20Client%20Form/AllItems.aspx
 [sample application one]:https://innersource.accenture.com/cio-security-architecture/eso-cordova-sso-example-1/
 [sample application two]:https://innersource.accenture.com/cio-security-architecture/eso-cordova-sso-example-2/
[Plugin InnerSource Repo]:https://innersource.accenture.com/cio-security-architecture/eso-library-for-cordova.git
[git]:https://git-scm.com/documentation