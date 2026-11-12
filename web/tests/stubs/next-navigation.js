/** Test double for next/navigation: records navigations instead of performing them. */
const navigation = {
  pushes: [],
  refreshes: 0,
  replace: [],
  reset() {
    this.pushes.length = 0;
    this.replace.length = 0;
    this.refreshes = 0;
  },
};

function useRouter() {
  return {
    push: (href) => { navigation.pushes.push(href); },
    replace: (href) => { navigation.replace.push(href); },
    refresh: () => { navigation.refreshes += 1; },
    back: () => {},
    forward: () => {},
    prefetch: () => {},
  };
}

function usePathname() {
  return '/login';
}

function useSearchParams() {
  return new URLSearchParams();
}

const _push = () => {};
const redirect = _push;
const notFound = _push;

module.exports = { navigation, useRouter, usePathname, useSearchParams, redirect, notFound };
