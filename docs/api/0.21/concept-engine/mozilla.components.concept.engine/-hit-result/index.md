---
title: HitResult - 
---

[mozilla.components.concept.engine](../index.html) / [HitResult](./index.html)

# HitResult

`sealed class HitResult`

Represents all the different supported types of data that can be found from long clicking
an element.

### Types

| [AUDIO](-a-u-d-i-o/index.html) | `class AUDIO : `[`HitResult`](./index.md)<br>If the HTML element was of type 'HTMLAudioElement'. |
| [EMAIL](-e-m-a-i-l/index.html) | `class EMAIL : `[`HitResult`](./index.md)<br>The type used if the URI is prepended with 'mailto:'. |
| [GEO](-g-e-o/index.html) | `class GEO : `[`HitResult`](./index.md)<br>The type used if the URI is prepended with 'geo:'. |
| [IMAGE](-i-m-a-g-e/index.html) | `class IMAGE : `[`HitResult`](./index.md)<br>If the HTML element was of type 'HTMLImageElement'. |
| [IMAGE_SRC](-i-m-a-g-e_-s-r-c/index.html) | `class IMAGE_SRC : `[`HitResult`](./index.md)<br>If the HTML element was of type 'HTMLImageElement' and contained a URI. |
| [PHONE](-p-h-o-n-e/index.html) | `class PHONE : `[`HitResult`](./index.md)<br>The type used if the URI is prepended with 'tel:'. |
| [UNKNOWN](-u-n-k-n-o-w-n/index.html) | `class UNKNOWN : `[`HitResult`](./index.md)<br>Default type if we're unable to match the type to anything. It may or may not have a src. |
| [VIDEO](-v-i-d-e-o/index.html) | `class VIDEO : `[`HitResult`](./index.md)<br>If the HTML element was of type 'HTMLVideoElement'. |

### Properties

| [src](src.html) | `val src: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Inheritors

| [AUDIO](-a-u-d-i-o/index.html) | `class AUDIO : `[`HitResult`](./index.md)<br>If the HTML element was of type 'HTMLAudioElement'. |
| [EMAIL](-e-m-a-i-l/index.html) | `class EMAIL : `[`HitResult`](./index.md)<br>The type used if the URI is prepended with 'mailto:'. |
| [GEO](-g-e-o/index.html) | `class GEO : `[`HitResult`](./index.md)<br>The type used if the URI is prepended with 'geo:'. |
| [IMAGE](-i-m-a-g-e/index.html) | `class IMAGE : `[`HitResult`](./index.md)<br>If the HTML element was of type 'HTMLImageElement'. |
| [IMAGE_SRC](-i-m-a-g-e_-s-r-c/index.html) | `class IMAGE_SRC : `[`HitResult`](./index.md)<br>If the HTML element was of type 'HTMLImageElement' and contained a URI. |
| [PHONE](-p-h-o-n-e/index.html) | `class PHONE : `[`HitResult`](./index.md)<br>The type used if the URI is prepended with 'tel:'. |
| [UNKNOWN](-u-n-k-n-o-w-n/index.html) | `class UNKNOWN : `[`HitResult`](./index.md)<br>Default type if we're unable to match the type to anything. It may or may not have a src. |
| [VIDEO](-v-i-d-e-o/index.html) | `class VIDEO : `[`HitResult`](./index.md)<br>If the HTML element was of type 'HTMLVideoElement'. |

