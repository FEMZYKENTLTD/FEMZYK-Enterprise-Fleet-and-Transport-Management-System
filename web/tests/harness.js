/**
 * A deliberately tiny test harness: describe/it/expect with no dependencies.
 * Kept small so the web tier gains a regression suite without pulling in a
 * test framework and a build step of its own.
 */
const tests = [];
let prefix = [];

function describe(name, fn) {
  prefix.push(name);
  fn();
  prefix.pop();
}

function it(name, fn) {
  tests.push({ name: [...prefix, name].join(' › '), fn });
}

function fail(message) {
  throw new Error(message);
}

/** JSON.stringify throws on DOM nodes; describe them instead. */
function show(value) {
  if (value && typeof value === 'object' && value.nodeType) {
    return `<${value.tagName?.toLowerCase() ?? 'node'}> ${(value.textContent ?? '').slice(0, 60)}`;
  }
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

const matchers = (actual, negate) => {
  const assert = (pass, msg) => {
    if (negate ? pass : !pass) fail(negate ? `expected NOT: ${msg}` : msg);
  };
  return {
    toBe: (expected) => assert(Object.is(actual, expected), `expected ${show(actual)} to be ${show(expected)}`),
    toEqual: (expected) => assert(
      show(actual) === show(expected),
      `expected ${show(actual)} to equal ${show(expected)}`,
    ),
    toBeTruthy: () => assert(Boolean(actual), `expected ${show(actual)} to be truthy`),
    toBeFalsy: () => assert(!actual, `expected ${show(actual)} to be falsy`),
    toContain: (needle) => assert(
      typeof actual === 'string' ? actual.includes(needle) : Array.from(actual ?? []).includes(needle),
      `expected ${show(actual)} to contain ${JSON.stringify(needle)}`,
    ),
    toBeGreaterThan: (n) => assert(actual > n, `expected ${actual} to be greater than ${n}`),
    toBeLessThan: (n) => assert(actual < n, `expected ${actual} to be less than ${n}`),
  };
};

function expect(actual) {
  return { ...matchers(actual, false), not: matchers(actual, true) };
}

async function run() {
  let passed = 0;
  const failures = [];
  const started = Date.now();

  for (const test of tests) {
    if (globalThis.__resetDom) globalThis.__resetDom();
    try {
      await test.fn();
      passed += 1;
      console.log(`  \x1b[32m✓\x1b[0m ${test.name}`);
    } catch (error) {
      failures.push({ name: test.name, error });
      console.log(`  \x1b[31m✗\x1b[0m ${test.name}`);
      console.log(`      ${error.message.split('\n')[0]}`);
    }
  }

  const ms = Date.now() - started;
  console.log('');
  if (failures.length) {
    console.log(`\x1b[31m${failures.length} failed\x1b[0m, ${passed} passed (${ms}ms)`);
    for (const f of failures) console.log(`\n${f.name}\n${f.error.stack}`);
    process.exitCode = 1;
  } else {
    console.log(`\x1b[32m${passed} passed\x1b[0m, 0 failed (${ms}ms)`);
  }
  return failures.length;
}

module.exports = { describe, it, expect, run };
