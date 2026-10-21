# In-app updates for the FairScan fork

The app checks the latest published release at `fidanci/FairScan`, downloads an APK matching the device ABI, validates its package and version, then opens Android's package installer. Android still asks the user to confirm installation.

To publish an update, configure these repository Actions secrets once:

- `FAIRSCAN_KEYSTORE_BASE64`: the base64-encoded Android release keystore
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Keep the keystore and passwords private and back them up. Every release must use the same signing key as the first installed fork build. If the key is lost or changes, Android will reject in-app upgrades; install the new build manually after removing the old one.

Create and push a version tag such as `fork-v2.3.2` to start a signed release build. The workflow publishes ABI-specific APK assets. Install the first build signed with this key manually; later versions can be installed from the app's Settings screen.

The repository's existing local debug APK may use a different key. In that case it cannot be upgraded by releases signed with a new key; install the release-signed APK once to establish the update signing identity.
