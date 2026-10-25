// The color of the bar of the browser and of the installed app from the start:
// the one of the accent of the last visit, which src/hooks/useAccent.ts keeps.
// Some browsers take it only while the page loads, before the app runs. A file
// of its own, the CSP allows no inline scripts.
try {
  var color = localStorage.getItem('themeColor');
  if (color && /^#[0-9a-f]{6}$/i.test(color)) {
    document
      .querySelector('meta[name="theme-color"]')
      .setAttribute('content', color);
  }
} catch (e) {
  // no storage, e.g. blocked: the color of the build
}
