/// <reference lib="webworker" />
import { cleanupOutdatedCaches, createHandlerBoundToURL, precacheAndRoute } from 'workbox-precaching'
import { NavigationRoute, registerRoute } from 'workbox-routing'
import { initializeApp } from 'firebase/app'
import { getMessaging } from 'firebase/messaging/sw'

declare let self: ServiceWorkerGlobalScope

self.skipWaiting()
cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)
// SPA: serve index.html for navigations so the app works offline and on deep links.
try {
  registerRoute(new NavigationRoute(createHandlerBoundToURL('index.html')))
} catch { /* index.html not precached in dev */ }

// Firebase Cloud Messaging: background pushes are displayed automatically from the
// "notification" payload sent by the backend; clicks open the payload's link.
const config = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}
if (config.apiKey) getMessaging(initializeApp(config))
