---
layout: page
title: Use GeckoView reported scroll distances for dynamic app features
permalink: /rfc/0009-geckoview-dynamic-features
---

* Start date: 2022-05-19
* RFC PR: [#12198](https://github.com/mozilla-mobile/android-components/pull/12198)

## Summary

Have dynamic features like the dynamic toolbar or pull to refresh better integrate with GeckoView who will inform them about consumed or available scroll and fling distances.

## Motivation

Currently A-C infers what a user touch means (whether it is a scroll, on what direction, for what distance) in parallel with APZ for only later synchronizing through GeckoView - `InputResultDetail` on if and how that touch was handled by Gecko to actually scroll the webpage.

This means highly complex code is being duplicated in both projects and this allows for the dynamic behavior to be started and controlled by A-C but later possibly blocked by GeckoView whereas for a smooth UX the dynamic elements should always be in sync with the webpage and so the dynamic behavior should be triggered and controlled by GeckoView.

The current approach works okay for the most part with only a few edgecases still leading to issues:

- differences in touch inferring code may lead to differences in the distance or speed with which dynamic elements or the webpage is scrolled
- delay in the synchronization process may lead to some scrolls being omitted from animating the dynamic elements
- delay in the synchronization process may be perceived as a bug by users

This RFC proposes dropping all the touch inferring code from A-C and rely only on GeckoView to inform A-C about the distance a page is scrolled following user touch instead of GeckoView only informing whether the page is scrolled or not. This would then allow for all the dynamic elements to be in perfect sync with the webpage.

## Guide-level explanation

All of the functionality relies on Gecko to expose through GeckoView a complete stream of scroll and fling data.

To easily integrate this with all the platform Views (including the ones from Compose) GeckoView should expose that data following the contract defined by [NestedScrollingChild](https://developer.android.com/reference/androidx/core/view/NestedScrollingChild) which already exists in [NestedGeckoView](https://github.com/mozilla-mobile/android-components/blob/6c0fe91ab83614265f533b819bf406fc046b2227/components/browser/engine-gecko/src/main/java/mozilla/components/browser/engine/gecko/NestedGeckoView.kt#L29) but only as a shell, it [automatically forwards all scroll data](https://github.com/mozilla-mobile/android-components/blob/6c0fe91ab83614265f533b819bf406fc046b2227/components/browser/engine-gecko/src/main/java/mozilla/components/browser/engine/gecko/NestedGeckoView.kt#L159) but [from Android not from GeckoView](https://github.com/mozilla-mobile/android-components/blob/6c0fe91ab83614265f533b819bf406fc046b2227/components/browser/engine-gecko/src/main/java/mozilla/components/browser/engine/gecko/NestedGeckoView.kt#L63-L88).

This would allow GeckoView to integrate in a [NestedScrollingParent](https://developer.android.com/reference/androidx/core/view/NestedScrollingParent3) and then cooperate with other siblings to consume a scroll or fling event (their distance) through 4 main methods: [dispatchNestedPreScroll] (https://developer.android.com/reference/androidx/core/view/NestedScrollingChild2#dispatchNestedPreScroll(int,int,int[],int[],int)), [dispatchNestedScroll](https://developer.android.com/reference/androidx/core/view/NestedScrollingChild2#dispatchNestedScroll(int,int,int,int,int[],int)), [dispatchNestedPreFling](https://developer.android.com/reference/androidx/core/view/NestedScrollingChild#dispatchNestedPreFling(float,float)), [dispatchNestedFling](https://developer.android.com/reference/androidx/core/view/NestedScrollingChild#dispatchNestedFling(float,float,boolean)).

The cooperative nature of the above contract means that the following flow should be supported:

- `onTouchEvent(ev: MotionEvent)` is called for GeckoView. This is where it would need to infer everything about the `MotionEvent` and call the below `dispatch` methods.
- [dispatchNestedPreScroll](https://developer.android.com/reference/androidx/core/view/NestedScrollingChild2#dispatchNestedPreScroll(int,int,int[],int[],int)) is called for Geckoview to first inform the parent about a scroll starting and allowing it to react first.
    - [onNestedPreScroll](https://developer.android.com/reference/androidx/core/view/NestedScrollingParent#onNestedPreScroll(android.view.View,int,int,int[])) is called in the parent which can consume any scroll distance and report about this.
    - GeckoView can then consume any remainded of the scroll.
- [dispatchNestedScroll](https://developer.android.com/reference/androidx/core/view/NestedScrollingChild2#dispatchNestedScroll(int,int,int,int,int[],int)) is called for Geckoview to consume any scroll in progress and then inform the parent.
    - [onNestedScroll](https://developer.android.com/reference/androidx/core/view/NestedScrollingParent#onNestedScroll(android.view.View,int,int,int,int)) is called in the parent which can consume any remaining scroll distance GeckoView didn't consume

(The same flow exists for supporting flings)

--

Platform Views implementing using the data from the [NestedScrollingChild](https://developer.android.com/reference/androidx/core/view/NestedScrollingChild) callbacks already exists in many projects to this flow is already validated.

The commits from this PR show a proof of concept how the same can be used to drive the new features which will be implemented in Compose.

## Drawbacks

* ---

## Rationale and alternatives
- Gecko already has the scroll data and it is the source of truth for when and by how much the webpage is scrolled.
- By having Gecko (through Geckoview) inform A-C about the distance a webpage is scrolled at exactly the time the scroll happens all dynamic elements will be in perfect sync.
- Implementing the standard Android contract for participating in nested scrolls (cooperatively) will have GeckoView easily integrate with any platform Views depending on that data.
- Implementing the standard Android contract for participating in nested scrolls means a simpler and more straighforward api for applications only interested in "when" and "by how much" a webpage is scrolled with details about whether a touch event was handled or not / at the top level or in an iframe to be hidden.
- We can avoid keeping complex logic and doing extra work based on the same touch event that Gecko handles
- We can avoid the maintenance cost for customized Views interested in nested scrolls that have to infer the scroll distance on their own to then synchronize with Geckoview's `InputResultDetail `.

## Prior art

- On March 24th 2021 the [#9963 A-C PR](https://github.com/mozilla-mobile/android-components/pull/9963) started relying on GeckoView's new `InputResultDetail` for synchronizing the A-C functionality with how GeckoView handled the same touch event and only continue if we get that based on the touch the wepage was updated.
- On October 21st 2020 pull to refresh was enabled in Fenix Nightly following a better integration of the platform `SwipeRefreshLayout` with the webpage interactions. ([meta](https://github.com/mozilla-mobile/fenix/issues/9766)).
- [#6666 A-C PR](https://github.com/mozilla-mobile/android-components/pull/6666) added a `BrowserGestureDetector` for A-C to differentiate between zoom and scroll events to translate the toolbar (and later the pull to refresh throbber) only for vertical scrolls.
- [#6244 A-C PR](https://github.com/mozilla-mobile/android-components/pull/6244) added initial support for `EngineView` to synchronize with `GeckoView` on whether the user gesture did scroll the page or not (at this time we would not wait for the PZC inference to complete).
- On October 5th 2018 the [#1246 A-C PR](https://github.com/mozilla-mobile/android-components/pull/1246) added support for the `EngineView` to participate in participate in dynamic layouts and allow for other siblings to animate based on user scrolls by implementing `NestedScrollingChild`.
  This was a basic implementation to get the things going but there was no communication with GeckoView, all scrolling would happen automatically, just based on the `MotionEvent`s we got from Android.

## Unresolved questions

* Is the above flow from the `Guide-level explanation` possible? Can GeckoView cooperate with the parent and siblings in consuming a user scroll gesture?
* Is is needed for GeckoView to "cooperate" with the parent and other sibling and implement all methods about consumed and available distances?
* If the above is hard maybe in a V1 we can try only one callback from GeckoView: `dispatchNestedScroll ` through which GeckoView will only inform about the scroll and have all other features react to that. Any important drawbacks?
* How big the changes are. Both in A-C/Fenix/Focus but also in Gecko/GeckoView?
* Fenix has other features like snackbars / the download complete dialog / the find in bar page also dependent on the current the dynamic behavior. These "should" continue to work as before but need to be double-checked.
