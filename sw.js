/* ============================================================
   Service worker — National Malaria Treatment Protocol
   ------------------------------------------------------------
   Purpose: guarantee the tool opens with no connection. Without
   this, "offline" relied on the browser's ordinary HTTP cache,
   which is evicted without warning — exactly the moment a CHW
   in the field needs it.

   Strategy is NETWORK-FIRST, not cache-first, and that is
   deliberate: this is a dosing tool. When a phone has signal it
   must get the current protocol, even at the cost of a slower
   load. The cache is the fallback, not the default. A 3-second
   timeout keeps a weak connection from stalling the page.

   ON EVERY CONTENT CHANGE bump CACHE to match APP_VERSION in
   index.html, or returning phones keep serving the old shell.
   ============================================================ */
const CACHE = "nmtp-v1.0.0";
const TIMEOUT_MS = 3000;

/* Relative so this works both at the domain root (Vercel) and in a
   subdirectory (GitHub Pages) without changes. */
const ASSETS = [
  "./",
  "./index.html",
  "./manifest.json",
  "./icon-192.png",
  "./icon-512.png"
];

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE)
      .then(c => c.addAll(ASSETS))
      .then(() => self.skipWaiting())
      .catch(() => self.skipWaiting())   // a missing optional asset must not block install
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

function fromNetwork(request){
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("timeout")), TIMEOUT_MS);
    fetch(request).then(response => {
      clearTimeout(timer);
      if (response && response.ok && request.method === "GET"){
        const copy = response.clone();
        caches.open(CACHE).then(c => c.put(request, copy)).catch(() => {});
      }
      resolve(response);
    }).catch(err => { clearTimeout(timer); reject(err); });
  });
}

self.addEventListener("fetch", event => {
  const req = event.request;

  /* Only same-origin GETs. The page makes no third-party requests by design;
     anything else is passed straight through. */
  if (req.method !== "GET" || new URL(req.url).origin !== self.location.origin) return;

  event.respondWith(
    fromNetwork(req)
      .catch(() => caches.match(req, { ignoreSearch: true }))
      .then(res => res || caches.match("./index.html") || caches.match("./"))
      .then(res => res || new Response(
        "<h1>Offline</h1><p>This page has not been saved for offline use yet. Open it once with an internet connection.</p>",
        { headers: { "Content-Type": "text/html; charset=utf-8" }, status: 503 }
      ))
  );
});
