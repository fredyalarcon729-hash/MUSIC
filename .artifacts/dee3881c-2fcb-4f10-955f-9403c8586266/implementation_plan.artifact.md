# Critical YouTube Stream Extraction Fix

The current extraction methods are being blocked by YouTube's latest security measures, causing the app to fallback to demo tracks. This plan implements a more robust extraction logic using the YouTube Music Web client configuration.

## User Review Required

> [!CAUTION]
> YouTube's bot detection is extremely aggressive. I will switch the extraction engine to mimic a web browser (YouTube Music Web client), which currently has higher success rates for audio extraction. I will also add more diverse Piped instances.

## Proposed Changes

### [Component Name] Core Layer

#### [MODIFY] [YouTubeAudioResolver.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/core/source/youtube/YouTubeAudioResolver.kt)
- **Switch to WEB_REMIX client**: Update `resolveViaInnertube` to use the YouTube Music Web client configuration. This includes specific `clientName`, `clientVersion`, and a browser-like `User-Agent`.
- **Add Required Headers**: Include `Origin` and `Referer` headers which are often mandatory for the Web client.
- **Improved Parsing**: Handle cases where the stream might be encrypted or requires specific parameters from the `streamingData`.
- **Expanded Piped List**: Add more public Piped instances as fallbacks.

## Verification Plan

### Manual Verification
- Monitor Logcat for `YouTubeAudioResolver` logs to see the raw response status if it fails.
- Play several different YouTube tracks to ensure consistent behavior.
