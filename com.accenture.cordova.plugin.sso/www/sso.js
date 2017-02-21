/**
 *  Copyright 2015 Accenture
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

var exec = require("cordova/exec");

var SSO = function () {};

SSO.prototype.getToken = function (success, failure, clientId, clientSecret, clientScopes, clientRedirectUri, production) {
  cordova.exec(success, failure, 'SSOPlugin', 'getToken', [clientId, clientSecret, clientScopes, clientRedirectUri, production]);
};

SSO.prototype.logout = function (success, failure, clientId, clientSecret, clientScopes, clientRedirectUri, production) {
  cordova.exec(success, failure, 'SSOPlugin', 'logout', [clientId, clientSecret, clientScopes, clientRedirectUri, production]);
};

module.exports = new SSO();
