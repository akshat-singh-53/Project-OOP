// End-to-end lifecycle through the UI: employee reports -> technician works and resolves -> employee verifies and rates.
// Usage: start the backend on :8080 (with seed data), then `npm run e2e:lifecycle`.
import { chromium } from 'playwright-core';
import { mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = process.env.ITSM_URL ?? 'http://localhost:8080/';
const BROWSER = process.env.BROWSER_PATH ?? 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const OUT = join(dirname(fileURLToPath(import.meta.url)), 'screenshots');
mkdirSync(OUT, { recursive: true });

const USERNAMES = {
  'Rahul Verma': 'rahul', 'Priya Nair': 'priya', 'Arjun Singh': 'arjun',
  'Sneha Iyer': 'sneha', 'Vikram Rao': 'vikram', 'Meera Joshi': 'meera',
};

const browser = await chromium.launch({ executablePath: BROWSER, headless: true });
const problems = [];

async function session(username, password = 'pass123') {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  page.on('pageerror', (e) => problems.push(`[${username}] page error: ${e.message}`));
  page.on('response', (res) => {
    if (res.status() >= 400 && res.url().includes('/api/')) problems.push(`[${username}] HTTP ${res.status()} ${res.url()}`);
  });
  await page.goto(BASE);
  await page.fill('input:not([type=password])', username);
  await page.fill('input[type=password]', password);
  await page.click('button[type=submit]');
  await page.waitForSelector('.sidebar');
  return { ctx, page };
}

const step = (msg) => console.log('•', msg);
const clickButton = (page, text) => page.getByRole('button', { name: text }).first().click();
const status = (page) => page.locator('.page-head .b-status').first().innerText();

// 1. Employee reports an incident and sees live suggestions.
let { ctx, page } = await session('amit');
await page.goto(BASE + '#/report');
await page.waitForSelector('select');
const serviceValue = await page.$eval('select', (sel) =>
  [...sel.options].find((o) => o.text.startsWith('Email & Collaboration')).value);
await page.selectOption('select', serviceValue);
await page.fill('input[maxlength="150"]', 'Outlook crashes when opening PDF attachment');
await page.fill('textarea', 'Every time I open a PDF attachment in Outlook the application freezes and closes. Started after this morning update.');
await page.waitForSelector('.suggestion', { timeout: 8000 });
const suggested = await page.locator('.suggestion strong').first().innerText();
step(`live category suggestion: ${suggested}`);
await page.screenshot({ path: join(OUT, 'flow-1-report.png'), fullPage: true });
await clickButton(page, 'Submit incident');
await page.waitForSelector('text=created');
await page.screenshot({ path: join(OUT, 'flow-2-created.png'), fullPage: true });
await clickButton(page, 'Open incident');
await page.waitForURL(/#\/incidents\/\d+/);
const incidentUrl = page.url();
const technician = (await page.locator('dt:has-text("Technician") + dd').innerText()).replace(/\s*\(.*\)$/, '');
step(`incident ${incidentUrl.split('/').pop()} auto-assigned to ${technician}, status ${await status(page)}`);
await ctx.close();

// 2. Assigned technician acknowledges, starts work, adds a note and resolves.
const techUser = USERNAMES[technician];
if (!techUser) {
  problems.push(`incident was not auto-assigned (technician = "${technician}")`);
} else {
  ({ ctx, page } = await session(techUser));
  await page.goto(incidentUrl);
  await page.waitForSelector('text=Next steps');
  await clickButton(page, 'Acknowledge');
  await page.waitForSelector('.alert.success');
  await clickButton(page, 'Start work');
  await page.waitForSelector('.page-head .b-IN_PROGRESS');
  await page.fill('textarea', 'Reproduced with the latest Outlook build; the PDF previewer add-in is crashing.');
  await page.check('text=Internal');
  await clickButton(page, 'Add note');
  await page.waitForSelector('.note.internal');
  await clickButton(page, '✅ Resolve');
  await page.locator('label:has-text("Resolution —") + textarea').fill('Disabled the faulty PDF preview add-in and repaired the Office installation.');
  await page.locator('label:has-text("Root cause") + input').fill('Incompatible PDF previewer add-in after Office update');
  await clickButton(page, 'Mark resolved');
  await page.waitForSelector('.page-head .b-RESOLVED');
  step(`${techUser} resolved it, status ${await status(page)}`);
  await page.screenshot({ path: join(OUT, 'flow-3-resolved.png'), fullPage: true });
  await ctx.close();

  // 3. Employee confirms the fix and leaves feedback; internal note must be hidden.
  ({ ctx, page } = await session('amit'));
  await page.goto(incidentUrl);
  await page.waitForSelector('text=Next steps');
  if (await page.locator('.note.internal').count()) problems.push('employee can see an internal note');
  await clickButton(page, 'Confirm it is fixed');
  await page.waitForSelector('.page-head .b-VERIFIED');
  await page.locator('.stars button').nth(4).click();
  await clickButton(page, 'Send feedback');
  await page.waitForSelector('text=User rating');
  step(`amit verified and rated it, status ${await status(page)}`);
  await page.screenshot({ path: join(OUT, 'flow-4-verified.png'), fullPage: true });
  await ctx.close();
}

await browser.close();
console.log(problems.length ? 'PROBLEMS:\n' + problems.join('\n') : 'Lifecycle completed without errors');
process.exitCode = problems.length ? 1 : 0;
