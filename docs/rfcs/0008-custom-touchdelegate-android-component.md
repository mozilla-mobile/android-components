---
layout: page
title: Working on a new custom Android component which uses TouchDelegate API to enhance accessibility
permalink: /rfc/0008-custom-touchdelegate-android-component
---

* Start date: 2021-07-08
* RFC PR: [#10573](https://github.com/mozilla-mobile/android-components/pull/10573)

## Summary

Improving how **easy-to-click** certain buttons in Fenix are by implementing a new Android component that uses the TouchDelegate API. This way, buttons/clickable items can retain the same size (thus not disrupting the rest of the layout), while having a **larger touch area**. 

## Motivation

* Google Accessibility Scanner suggests an increase in touch target size for some important buttons/clickable items to enhance accessibility. For example -  
![Image 1](https://user-images.githubusercontent.com/67039214/121003149-d19c9600-c7aa-11eb-990d-dd100cef51bf.png)  

* However, it is not always possible to increase the size of these clickable items, as it *might disrupt the entire layout* (especially when it is an important clickable item like the search bar). Even padding/margin alterations might end up disrupting rest of the layout.  

* So, a clickable item with the **same size but larger touch area is ideal**, as mentioned in this comment [Bug(a11y): Consider increasing the "Scan" and "Search engine" options #16829 (comment)_](https://github.com/mozilla-mobile/fenix/issues/16829#issuecomment-801464840).

## Explanation

### About TouchDelegate API

*  The TouchDelegate API can be used to increase touch area of a button without resizing i.e. without changing visible view bounds.  

![Img](https://user-images.githubusercontent.com/67039214/124944840-47ef1b00-e02b-11eb-89a3-caf3690e81c3.png)

* However, to use the TouchDelegate API for a button/clickable item, this class should be used by an ancestor of the delegate (delegate view is view whose touch area's to be changed).

* Also, as far as I could make out from various articles/usage examples and trying this out myself, _an ancestor which uses TouchDelegate can only handle one child button's touch events_. So, for each clickable item whose touch area we want to increase, we might need to define some immediate ancestor (say, a RelativeLayout) with which TouchDelegate class can be used. (We can explore this further to see if there's some way of having _multiple delegates for a single parent_.)

* It might be good to have a custom Android component to fulfil this need - a button or navigation bar with a certain size, wrapped in a RecyclerView (or some other custom view) which implements TouchDelegate. This way, for any project, user can increase the touch area for a button without altering its intended size with ease.

* We might be able to proceed along the lines of [this PR](https://github.com/mozilla-mobile/fenix/pull/19676).  

       
        search_engines_shortcut_layout.post {
            val delegateArea = Rect()
            search_engines_shortcut_button.apply { isEnabled = true }
            search_engines_shortcut_layout.getHitRect(delegateArea)
            delegateArea.top = search_engines_shortcut_button.top - 16.dpToPx()
            delegateArea.bottom = search_engines_shortcut_button.bottom + 16.dpToPx()
            delegateArea.left = search_engines_shortcut_button.left
            delegateArea.right = search_engines_shortcut_button.right

            (search_engines_shortcut_button.parent as? View)?.apply {
                touchDelegate = TouchDelegate(delegateArea, search_engines_shortcut_button)
            }

        }

        qr_scan_button_layout.post {
            val delegateArea = Rect()
            qr_scan_button.apply { isEnabled = true }
            qr_scan_button_layout.getHitRect(delegateArea)
            delegateArea.top = qr_scan_button.top - 16.dpToPx()
            delegateArea.bottom = qr_scan_button.bottom + 16.dpToPx()
            delegateArea.left = qr_scan_button.left
            delegateArea.right = qr_scan_button.right

            (qr_scan_button.parent as? View)?.apply {
                touchDelegate = TouchDelegate(delegateArea, qr_scan_button)
            }

        }

In this code snippet, `qr_scan_button_layout` and `search_engines_shortcut_layout` are the ancestor RecyclerViews that are implementing TouchDelegate APIs. 

### Relevant Issues

* [Consider increasing the "Scan" and "Search engine" options #16829](https://github.com/mozilla-mobile/fenix/issues/16829)
* [Consider increasing touch target size in 'Add to Home screen' dialog box #18765](https://github.com/mozilla-mobile/fenix/issues/18765) 
* [URL bar - "Cancel" & microphone buttons are recommended to be resized #9765](https://github.com/mozilla-mobile/fenix/issues/9765)  

### Questions to Explore

* Is there a way of handling multiple delegates under the same ancestor layout? 
* Does the TouchDelegate API work properly under all conditions?

## Potential Drawbacks

* _Difficulty in migrating existing buttons_:  It might not be easy to make all existing buttons/clickable items in Fenix use this new Android component. Analysis of which buttons qualify to use this component for enhanced accessibility may be required.

* _Nesting of buttons required_: To use the TouchDelegate API with a button, the TouchDelegate class should be used by an **ancestor of the delegate** (delegate view is view whose touch area's to be changed). This means that the button has to be placed inside, say, a RelativeLayout, which serves just as a wrapper to enable the TouchDelegate API's use. This extra nesting of multiple buttons might not be desirable. 

## Alternatives

* _Increase size of buttons_: Eventhough the existing layout might be disrupted, simply increasing the size of these buttons or modifying the padding/margins can fix the accessibility issues.

## Prior art & resources

* [Documentation: Extend a child view's touchable area](https://developer.android.com/training/gestures/viewgroup#delegate)
* [Android change touch area of View by TouchDelegate](https://medium.com/android-news/android-change-touch-area-of-view-by-touchdelegate-fc19f2a34021)
* [ListView Tips & Tricks #5: Enlarged Touchable Areas](https://cyrilmottier.com/2012/02/16/listview-tips-tricks-5-enlarged-touchable-areas/) - Might help with multiple delegate views under a parent view. 
* [Gist: TouchDelegateComposite.java](https://gist.github.com/kaiwinter/b277d4ccc2dfb3c15eb6) - Might help with multiple delegate views under a parent view. 
* [brendanw/Touch-Delegates](https://github.com/brendanw/Touch-Delegates/blob/master/src/com/brendan/touchdelegates/MyTouchDelegate.java)
