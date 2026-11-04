# Google Play release checklist

## Accessibility declaration
Play reviews AccessibilityService use by hand. The app sets `isAccessibilityTool="false"`, so it
must pass the prominent disclosure and permission declaration requirements.

- Declared purpose: detect when a user-selected app opens, to show the user's saved videos instead.
- With no cooldown set, the service also observes the launcher to detect when a session ends.
- Provide a short video of: the disclosure screen, enabling the service, and a prompt appearing.
- The in-app disclosure is the Permission step of onboarding. It must stay before the system settings screen.
- Check the current Play policy wording before submitting, because it changes.

## Data safety form (suggested answers, confirm before submitting)
- Data collected: none. Data is processed on the device and the developer receives nothing.
- Data shared: links are sent to the linked site or its embed endpoint only at the user's request
  to save them. Review how Play's current definitions treat this.
- Encryption in transit: metadata requests use HTTPS only; cleartext traffic is disabled in the
  manifest, so `http://` links are saved but get no title or thumbnail.
- Account or data deletion: no account exists; uninstalling deletes all data.

## Store listing
Privacy policy URL, screenshots, feature graphic, content rating, target audience.
