// UI smoke test: logs in as each role, opens every page, and reports browser errors and failed API calls.
// Usage: start the backend on :8080, then `npm run e2e`. Screenshots are written to e2e/screenshots.
import { chromium } from 'playwright-core';
import { mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = process.env.ITSM_URL ?? 'http://localhost:8080/';
const BROWSER = process.env.BROWSER_PATH ?? 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const OUT = join(dirname(fileURLToPath(import.meta.url)), 'screenshots');
mkdirSync(OUT, { recursive: true });

const ROLES = [
  { user: 'amit', pass: 'pass123', pages: ['', 'report', 'incidents', 'notifications', 'profile'] },
  { user: 'rahul', pass: 'pass123', pages: ['', 'incidents?mine=1', 'incidents?unassigned=1', 'incidents', 'intelligence', 'sla', 'profile'] },
  { user: 'admin', pass: 'admin123', pages: ['', 'incidents', 'intelligence', 'sla', 'admin/users', 'admin/catalog', 'admin/settings', 'admin/audit', 'admin/outbox'] },
];

const browser = await chromium.launch({ executablePath: BROWSER, headless: true });
const problems = [];
for (const r of ROLES) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  page.on('console', (m) => { if (m.type() === 'error') problems.push(`[${r.user}] console: ${m.text()}`); });
  page.on('pageerror', (e) => problems.push(`[${r.user}] page error: ${e.message}`));
  page.on('response', (res) => {
    if (res.status() >= 400 && res.url().includes('/api/')) problems.push(`[${r.user}] HTTP ${res.status()} ${res.url()}`);
  });

  await page.goto(BASE);
  if (r.user === ROLES[0].user) await page.screenshot({ path: join(OUT, '00-login.png') });
  await page.fill('input:not([type=password])', r.user);
  await page.fill('input[type=password]', r.pass);
  await page.click('button[type=submit]');
  await page.waitForSelector('.sidebar');

  for (const p of r.pages) {
    await page.goto(BASE + '#/' + p);
    await page.waitForTimeout(1400);
    const name = `${r.user}-${(p || 'dashboard').replace(/[/?=]/g, '_')}.png`;
    await page.screenshot({ path: join(OUT, name), fullPage: true });
  }

  await page.goto(BASE + '#/incidents');
  await page.waitForSelector('tr.clickable', { timeout: 5000 }).catch(() => problems.push(`[${r.user}] incident list is empty`));
  const rows = await page.$$('tr.clickable');
  if (rows.length) {
    await rows[0].click();
    await page.waitForTimeout(1800);
    await page.screenshot({ path: join(OUT, `${r.user}-incident-detail.png`), fullPage: true });
  }
  await ctx.close();
}
await browser.close();

console.log(problems.length ? problems.join('\n') : 'All pages rendered without errors');
process.exitCode = problems.length ? 1 : 0;
