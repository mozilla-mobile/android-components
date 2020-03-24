/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Handles the parsing of the ErrorPages URI and then passes them to injectValues
 */
function parseQuery(queryString) {
   if (queryString[0] === '?') {
       queryString = queryString.substr(1);
   }
   const query = Object.fromEntries(new URLSearchParams(queryString).entries());
   injectValues(query)
   updateShowSSL(query)
};

/**
 * Updates the HTML elements based on the queryMap
 */
function injectValues(queryMap) {
   // Go through each element and inject the values
   document.title = queryMap.title
   document.getElementById('errorTitleText').innerHTML = queryMap.title
   document.getElementById('errorShortDesc').innerHTML = queryMap.description
   document.getElementById('errorTryAgain').innerHTML = queryMap.button
   document.getElementById('advancedButton').innerHTML = queryMap.badCertAdvanced
   document.getElementById('badCertTechnicalInfo').innerHTML = queryMap.badCertTechInfo
   document.getElementById('advancedPanelBackButton').innerHTML = queryMap.badCertGoBack
   document.getElementById('advancedPanelAcceptButton').innerHTML = queryMap.badCertAcceptTemporary

   // If no image is passed in, remove the element so as not to leave an empty iframe
   const errorImage = document.getElementById('errorImage')
   if (!queryMap.image) {
      errorImage.remove()
   } else  {
      errorImage.src = "resource://android/assets/" + queryMap.image
   }
}

var advancedVisible = false;

/**
 * Used to show or hide the "advanced" button based on the validity of the SSL certificate
 */
function updateShowSSL(queryMap) {
    /** @type {'true' | 'false'} */
    const showSSL = queryMap.showSSL;
    if (typeof document.addCertException === "undefined") {
        document.getElementById('advancedButton').style.display='none';
    } else {
        if (showSSL === 'true') {
            document.getElementById('advancedButton').style.display='block';
        } else {
            document.getElementById('advancedButton').style.display='none';
        }
    }
}

/**
 * Used to display information about the SSL certificate in `error_pages.html`
 */
function toggleAdvanced() {
    if (advancedVisible) {
        document.getElementById('badCertAdvancedPanel').style.display='none';
    } else {
        document.getElementById('badCertAdvancedPanel').style.display='block';
    }
    advancedVisible = !advancedVisible;
}

/**
 * Used to bypass an SSL pages in `error_pages.html`
 */
async function acceptAndContinue(temporary) {
    try {
        await document.addCertException(temporary);
        location.reload();
    } catch (error) {
        console.error("Unexpected error: " + error)
    }
}

parseQuery(document.documentURI);
