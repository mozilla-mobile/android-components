Adding our changes here instead of directly to the changelog so that if we need to rebase, we don't
have to stop on every commit for a merge conflict. This file will be removed at the end of this PR.

* **browser-search**
  * ⚠️ **This is a breaking change**: `SearchEngineManager.toDefaultSearchEngineProvider` has become `LazyComponent<SearchEngineManager>.toDefaultSearchEngineProvider`.

* **feature-pwa**
  * ⚠️ **This is a breaking change**: `WebAppUseCases`'s initializer argument, `WebAppShortcutManager`, is wrapped in a `LazyComponent`. The same applies to the classes defined inside `WebAppUseCases`.
  * ⚠️ **This is a breaking change**: `ManifestUpdateFeature`'s initializer argument, `WebAppShortcutManager`, is wrapped in a `LazyComponent`.
