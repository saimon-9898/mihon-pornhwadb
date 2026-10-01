# Pornhwa DB extension for Mihon

A [Mihon](https://github.com/mihonapp/mihon) source for the
[Pornhwa DB](https://pornhwadb.com) catalogue API.

## Install

Grab `pornhwadb-extension-apk` from the [Actions run](https://github.com/saimon-9898/mihon-pornhwadb/actions),
install the APK, then in Mihon go to **Extensions → Repositories** and refresh.

You also need an API key from
[pornhwadb.com/profile?tab=settings&section=developer](https://pornhwadb.com/profile?tab=settings&section=developer).
Enter it under **Extensions → Pornhwa DB → Preferences → API key**.

## Why there are no chapters

The API is a catalogue, not a reader. Its `Chapter` resource is a *scene record* — a chapter range
plus tags and characters — and no endpoint exposes page image URLs. The site says so itself at
[/llms.txt](https://pornhwadb.com/llms.txt): "it does not host full works for reading".

So this source implements search, browsing and details only. The details page links out to the
official listings (AniList, MangaBaka and others) so you can read there. It deliberately exposes no
chapters, because a chapter entry here could only ever dead-end in the reader.

## Auth

`X-API-Key` header and nothing else — the API has no user accounts and no login endpoint. The key is
stored in the host app's preferences and is never compiled into this APK.

## Build

Pushed to `main`, Actions builds it. Locally: `gradle assembleDebug`.
The output is `pornhwadb/build/outputs/apk/debug/pornhwadb-debug.apk`.

Built against `tachiyomix` 1.6.0 (extension lib) and JDK 17.

## Not a Mihon-official extension

This is a standalone single-source repo, not a contribution to
[keiyoushi/extensions-source](https://github.com/keiyoushi/extensions-source). It is debug-signed, so
it cannot be updated in place alongside a differently-signed build.