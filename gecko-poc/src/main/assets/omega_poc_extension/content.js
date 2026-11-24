(() => {
  document.documentElement.dataset.omegaGeckoExtension = "active";
  const originalTitle = document.title || "Example Domain";
  if (!originalTitle.includes("OmegA Extension Active")) {
    document.title = originalTitle + " | OmegA Extension Active";
  }
  const marker = document.createElement("div");
  marker.textContent = "OmegA WebExtension POC active";
  marker.setAttribute("role", "status");
  marker.style.cssText = "position:fixed;z-index:2147483647;right:12px;bottom:12px;padding:8px 12px;border-radius:10px;background:#21183d;color:#fff;font:12px sans-serif;box-shadow:0 2px 12px #0006";
  document.documentElement.appendChild(marker);
})();
