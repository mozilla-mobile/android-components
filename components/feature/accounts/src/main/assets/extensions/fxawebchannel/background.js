/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
Establish communication with native application.
*/
let port = browser.runtime.connectNative("mozacWebchannelBackground");
/*
Handle messages from native application, register content script for specific url.
*/
port.onMessage.addListener( event => {
  if(event.type == "overrideFxAServer"){
    browser.contentScripts.register({
      "matches": [ event.url+"/*" ],
      "js": [{file: "fxawebchannel.js"}],
      "runAt": "document_start"
    });
    port.disconnect();
  }
});
