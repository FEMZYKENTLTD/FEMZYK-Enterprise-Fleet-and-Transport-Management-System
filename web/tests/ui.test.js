/**
 * Browser-level interaction tests for the sign-in and password-reset screens.
 *
 * These drive the real components in jsdom: typing into fields, submitting
 * forms, and asserting on what the user would see. The server is stubbed at
 * the fetch boundary, so the tests stay hermetic and never touch the network.
 *
 * Run: npm run test:ui
 */
require('./stubs/register');
const { setupDom, resetDom } = require('./setup-dom');
setupDom();
globalThis.__resetDom = resetDom;

const { describe, it, expect, run } = require('./harness');
const React = require('react');
const { render, screen, waitFor, cleanup } = require('@testing-library/react');
const userEvent = require('@testing-library/user-event').default;
const { LoginForm, ResetForm } = require('./.build/ui.cjs');
const { navigation } = require('./stubs/next-navigation.js');

/* ------------------------------------------------------------------ fetch -- */

const calls = [];
let responder = () => ({ status: 200, body: {} });

function stubFetch() {
  calls.length = 0;
  globalThis.fetch = async (url, init = {}) => {
    const body = init.body ? JSON.parse(init.body) : undefined;
    calls.push({ url: String(url), method: init.method ?? 'GET', body });
    const { status, body: payload } = responder(String(url), body);
    return {
      ok: status >= 200 && status < 300,
      status,
      text: async () => JSON.stringify(payload),
      blob: async () => new Blob([JSON.stringify(payload)]),
    };
  };
}

function field(label) {
  // Field renders as <label><span>Label *</span><input/></label>, so the input
  // is found by walking the label whose text starts with the wanted label.
  const wanted = label.trim().toLowerCase();
  const match = Array.from(document.querySelectorAll('label')).find((el) => {
    const span = el.querySelector('span');
    const text = (span ? span.textContent : '').replace('*', '').trim().toLowerCase();
    return text === wanted;
  });
  if (!match) throw new Error(`No field labelled "${label}"`);
  return match.querySelector('input');
}

function typeInto(label, value) {
  return userEvent.type(field(label), value);
}

/* ----------------------------------------------------------------- tests -- */

describe('Sign-in form', () => {
  it('renders both fields and keeps submit disabled until filled', async () => {
    render(React.createElement(LoginForm, { demoMode: false }));
    expect(field('Username')).toBeTruthy();
    expect(field('Password')).toBeTruthy();
    const submit = screen.getByRole('button', { name: /sign in/i });
    expect(submit.disabled).toBe(true);

    await typeInto('username', 'admin');
    expect(screen.getByRole('button', { name: /sign in/i }).disabled).toBe(true);

    await typeInto('password', 'Admin@2024');
    expect(screen.getByRole('button', { name: /sign in/i }).disabled).toBe(false);
    cleanup();
  });

  it('posts credentials and navigates to the dashboard on success', async () => {
    navigation.reset();
    stubFetch();
    responder = () => ({ status: 200, body: { user: { id: 1, role: 'ADMINISTRATOR' }, mustChangePassword: false } });
    render(React.createElement(LoginForm, { demoMode: false }));

    await typeInto('username', 'admin');
    await typeInto('password', 'Admin@2024');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(navigation.pushes).toContain('/dashboard'));
    expect(calls[0].url).toBe('/api/auth/login');
    expect(calls[0].method).toBe('POST');
    expect(calls[0].body).toEqual({ username: 'admin', password: 'Admin@2024' });
    cleanup();
  });

  it('shows the server error and stays on the page when credentials are rejected', async () => {
    navigation.reset();
    stubFetch();
    responder = () => ({ status: 401, body: { error: 'Invalid username or password.' } });
    render(React.createElement(LoginForm, { demoMode: false }));

    await typeInto('username', 'admin');
    await typeInto('password', 'wrong-password');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    const alert = await screen.findByRole('alert');
    expect(alert.textContent).toContain('Invalid username or password.');
    expect(navigation.pushes).toEqual([]);
    cleanup();
  });

  it('surfaces a network failure instead of silently doing nothing', async () => {
    navigation.reset();
    globalThis.fetch = async () => { throw new Error('Network unreachable'); };
    render(React.createElement(LoginForm, { demoMode: false }));

    await typeInto('username', 'admin');
    await typeInto('password', 'Admin@2024');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    const alert = await screen.findByRole('alert');
    expect(alert.textContent).toContain('Network unreachable');
    expect(navigation.pushes).toEqual([]);
    cleanup();
  });

  it('offers a link to the password reset page', () => {
    render(React.createElement(LoginForm, { demoMode: false }));
    const link = screen.getByRole('link', { name: /forgot your password/i });
    expect(link.getAttribute('href')).toBe('/login/reset');
    cleanup();
  });

  it('hides the demo account chips when demo mode is off', () => {
    render(React.createElement(LoginForm, { demoMode: false }));
    expect(screen.queryByText(/demo accounts/i)).toBeFalsy();
    cleanup();
  });

  it('fills the form when a demo chip is clicked', async () => {
    render(React.createElement(LoginForm, { demoMode: true }));
    await userEvent.click(screen.getByText('Fleet Manager'));
    expect(field('Username').value).toBe('manager');
    expect(field('Password').value).toBe('Fleet@2026');
    cleanup();
  });
});

describe('Password reset form', () => {
  it('blocks submission when the two passwords differ', async () => {
    stubFetch();
    render(React.createElement(ResetForm));

    await typeInto('username', 'manager');
    await typeInto('registered e-mail', 'manager@femzyk.local');
    await typeInto('new password', 'NewPass123');
    await typeInto('confirm new password', 'Different123');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));

    const alert = await screen.findByRole('alert');
    expect(alert.textContent).toContain('do not match');
    expect(calls.length).toBe(0);
    cleanup();
  });

  it('keeps submit disabled until every field is filled', async () => {
    render(React.createElement(ResetForm));
    expect(screen.getByRole('button', { name: /reset password/i }).disabled).toBe(true);
    await typeInto('username', 'viewer');
    await typeInto('registered e-mail', 'viewer@femzyk.local');
    expect(screen.getByRole('button', { name: /reset password/i }).disabled).toBe(true);
    await typeInto('new password', 'View@2027');
    await typeInto('confirm new password', 'View@2027');
    expect(screen.getByRole('button', { name: /reset password/i }).disabled).toBe(false);
    cleanup();
  });

  it('posts the reset and confirms success', async () => {
    stubFetch();
    responder = () => ({ status: 200, body: { ok: true } });
    render(React.createElement(ResetForm));

    await typeInto('username', 'manager');
    await typeInto('registered e-mail', 'manager@femzyk.local');
    await typeInto('new password', 'NewPass123');
    await typeInto('confirm new password', 'NewPass123');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => expect(screen.getByText(/password updated/i)).toBeTruthy());
    expect(calls[0].url).toBe('/api/auth/reset');
    expect(calls[0].body).toEqual({ username: 'manager', email: 'manager@femzyk.local', newPassword: 'NewPass123' });
    expect(screen.getByRole('link', { name: /back to sign in/i }).getAttribute('href')).toBe('/login');
    cleanup();
  });

  it('shows the server rejection when the username and e-mail do not match', async () => {
    stubFetch();
    responder = () => ({ status: 400, body: { error: 'Username and e-mail address do not match any account.' } });
    render(React.createElement(ResetForm));

    await typeInto('username', 'manager');
    await typeInto('registered e-mail', 'wrong@example.com');
    await typeInto('new password', 'NewPass123');
    await typeInto('confirm new password', 'NewPass123');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));

    const alert = await screen.findByRole('alert');
    expect(alert.textContent).toContain('do not match any account');
    expect(screen.queryByText(/password updated/i)).toBeFalsy();
    cleanup();
  });
});

run();
