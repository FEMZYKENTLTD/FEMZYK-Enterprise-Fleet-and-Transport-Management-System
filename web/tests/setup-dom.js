/**
 * Minimal browser-environment harness for the UI tests.
 *
 * There is no Chromium available in this environment (the Playwright CDN is
 * unreachable and there is no package-manager access), so these tests drive
 * the real React components inside jsdom instead: real state, real event
 * handling, real fetch calls against a stubbed server. That covers form
 * validation and interaction logic, but not layout, CSS or actual rendering.
 */
const { JSDOM } = require('jsdom');

function setupDom() {
  const dom = new JSDOM('<!doctype html><html><body></body></html>', {
    url: 'http://localhost/login',
    pretendToBeVisual: true,
  });

  const g = globalThis;
  g.window = dom.window;
  g.document = dom.window.document;
  g.navigator = dom.window.navigator;
  g.HTMLElement = dom.window.HTMLElement;
  g.HTMLInputElement = dom.window.HTMLInputElement;
  g.Node = dom.window.Node;
  g.Event = dom.window.Event;
  g.MouseEvent = dom.window.MouseEvent;
  g.KeyboardEvent = dom.window.KeyboardEvent;
  g.getComputedStyle = dom.window.getComputedStyle;
  g.IS_REACT_ACT_ENVIRONMENT = true;

  if (!g.requestAnimationFrame) {
    g.requestAnimationFrame = (cb) => setTimeout(() => cb(Date.now()), 0);
    g.cancelAnimationFrame = (id) => clearTimeout(id);
  }
  // Reset the document between tests: testing-library only auto-cleans when a
  // global afterEach exists, and this harness has no test runner globals.
  const { cleanup } = require('@testing-library/react');
  g.__cleanup = cleanup;
  if (!g.afterEach) {
    const originalIt = g.it;
    g.it = originalIt;
  }
  return dom;
}

/** Call before each test to start from an empty document. */
function resetDom() {
  if (globalThis.__cleanup) globalThis.__cleanup();
  document.body.innerHTML = '';
}

module.exports = { setupDom, resetDom };
