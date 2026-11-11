/**
 * Redirects `require('next/navigation')` to the test double so the bundled
 * components and the test file share one module instance.
 */
const Module = require('node:module');
const path = require('node:path');

const target = path.join(__dirname, 'next-navigation.js');
const original = Module._resolveFilename;

Module._resolveFilename = function (request, ...rest) {
  if (request === 'next/navigation') return target;
  return original.call(this, request, ...rest);
};
