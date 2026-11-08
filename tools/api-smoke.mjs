#!/usr/bin/env node
/**
 * Generic API smoke for the generated app.
 * Covers health/OpenAPI plus the employee list/create/update/conflict flow.
 */
const BACKEND = process.env.APP_BACKEND_URL || 'http://127.0.0.1:8080';
const FRONTEND = process.env.APP_FRONTEND_URL || 'http://127.0.0.1:5173';

async function call(path, options = {}) {
  const response = await fetch(BACKEND + path, {
    ...options,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...(options.headers || {}) },
  });
  const text = await response.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* non-JSON */ }
  return { status: response.status, json, text };
}

let failed = false;
try {
  const health = await call('/actuator/health');
  console.log('health:', health.status, health.json?.status || health.text);
  if (health.status !== 200 || health.json?.status !== 'UP') failed = true;

  const openapi = await call('/v3/api-docs');
  const paths = Object.keys(openapi.json?.paths || {});
  console.log('openapi:', openapi.status, 'paths:', paths.length, paths.slice(0, 8).join(', '));
  if (openapi.status !== 200) failed = true;

  const employeeNo = `API${Date.now()}`;
  const list = await call('/api/employees?page=1&size=10');
  const listOk = list.status === 200 && list.json?.code === 0 && Array.isArray(list.json?.data?.records);
  console.log('employees list:', list.status, 'records:', list.json?.data?.records?.length, 'total:', list.json?.data?.total);
  if (!listOk) failed = true;

  const created = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify({ employeeNo, name: '接口烟测员工', status: 'ACTIVE' }),
  });
  const createdId = created.json?.data?.id;
  console.log('employees create:', created.status, 'id:', createdId);
  if (created.status !== 201 || !createdId) failed = true;

  const updated = await call(`/api/employees/${createdId}`, {
    method: 'PUT',
    body: JSON.stringify({ employeeNo, name: '接口烟测员工改', status: 'INACTIVE' }),
  });
  console.log('employees update:', updated.status, updated.json?.data?.name);
  if (updated.status !== 200 || updated.json?.data?.name !== '接口烟测员工改') failed = true;

  const conflict = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify({ employeeNo, name: '重复工号' }),
  });
  console.log('employees duplicate:', conflict.status, conflict.json?.message);
  if (conflict.status !== 409) failed = true;

  const root = await fetch(FRONTEND + '/');
  console.log('frontend:', root.status);
  if (root.status !== 200) failed = true;
} catch (error) {
  console.error('API smoke failed:', error.message);
  failed = true;
}

if (failed) process.exitCode = 1;
else console.log('API smoke: PASS');
