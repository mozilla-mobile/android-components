/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
Establish communication with native application
*/
let port = browser.runtime.connectNative("mozacWebchannel");
port.onMessage.addListener((event) => {
  window.dispatchEvent(new CustomEvent('WebChannelMessageToContent', {
    detail: JSON.stringify(event)
  }));
});

window.addEventListener("unload", (event) => { port.disconnect() }, false);

/*
Handle messages from FxA
*/
window.addEventListener('WebChannelMessageToChrome', function (e) {
  const detail = JSON.parse(e.detail);
  port.postMessage(detail);
});
