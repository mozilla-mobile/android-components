Adding our changes here instead of directly to the changelog so that if we need to rebase, we don't
have to stop on every commit for a merge conflict. This file will be removed at the end of this PR.

* **feature-pwa**
  * ⚠️ **This is a breaking change**: `WebAppUseCases`'s initializer argument, `WebAppShortcutManager`, is wrapped in a `LazyComponent`. The same applies to the classes defined inside `WebAppUseCases`.
