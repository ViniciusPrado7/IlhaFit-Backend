import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const PROFILE = (__ENV.PROFILE || "50").toLowerCase();
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || "admin@ilhafit.com";
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || "Adm@1234";

const THINK_MIN_MS = Number(__ENV.THINK_MIN_MS || 1000);
const THINK_MAX_MS = Number(__ENV.THINK_MAX_MS || 3000);

const PUBLIC_WEIGHT = Number(__ENV.PUBLIC_WEIGHT || 80);
const ADMIN_WEIGHT = Number(__ENV.ADMIN_WEIGHT || 20);
const FIND_LIMIT_MAX_VUS = Number(__ENV.FIND_LIMIT_MAX_VUS || 600);
const FIND_LIMIT_STEP_VUS = Number(__ENV.FIND_LIMIT_STEP_VUS || 50);
const FIND_LIMIT_STAGE_DURATION = __ENV.FIND_LIMIT_STAGE_DURATION || "90s";
const FIND_LIMIT_ABORT_DELAY = __ENV.FIND_LIMIT_ABORT_DELAY || "45s";
const FIND_LIMIT_MAX_HTTP_FAILED_RATE = Number(__ENV.FIND_LIMIT_MAX_HTTP_FAILED_RATE || 0.05);
const FIND_LIMIT_MIN_SUCCESS_RATE = Number(__ENV.FIND_LIMIT_MIN_SUCCESS_RATE || 0.95);
const FIND_LIMIT_MAX_PUBLIC_ERROR_RATE = Number(__ENV.FIND_LIMIT_MAX_PUBLIC_ERROR_RATE || 0.05);
const FIND_LIMIT_MAX_ADMIN_ERROR_RATE = Number(__ENV.FIND_LIMIT_MAX_ADMIN_ERROR_RATE || 0.05);

const PUBLIC_ENDPOINTS = [
  "/api/categorias/categorias",
  "/api/profissionais/profissionais",
  "/api/estabelecimentos/estabelecimentos",
];

const successRate = new Rate("app_success_rate");
const publicErrorRate = new Rate("public_error_rate");
const adminErrorRate = new Rate("admin_error_rate");
const status2xx = new Counter("http_status_2xx");
const status4xx = new Counter("http_status_4xx");
const status5xx = new Counter("http_status_5xx");
const publicDuration = new Trend("public_duration", true);
const adminDuration = new Trend("admin_duration", true);

export const options = buildOptions(PROFILE);

export function setup() {
  const loginPayload = JSON.stringify({
    email: ADMIN_EMAIL,
    senha: ADMIN_PASSWORD,
  });

  const response = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
    headers: { "Content-Type": "application/json" },
    timeout: "10s",
  });

  check(response, {
    "setup login status 200": (r) => r.status === 200,
    "setup login token presente": (r) => !!r.json("token"),
  });

  return {
    adminToken: response.json("token"),
  };
}

export function mixedTraffic(data) {
  const authHeaders = {
    headers: {
      Authorization: `Bearer ${data.adminToken}`,
      "Content-Type": "application/json",
    },
    timeout: "10s",
  };

  const chance = Math.random() * 100;
  if (chance < PUBLIC_WEIGHT) {
    runPublicBrowse();
  } else if (chance < PUBLIC_WEIGHT + ADMIN_WEIGHT) {
    runAdminRead(authHeaders);
  } else {
    runPublicBrowse();
  }

  sleep(randomThinkTimeSeconds());
}

function runPublicBrowse() {
  const endpoint = pickRandom(PUBLIC_ENDPOINTS);
  const response = http.get(`${BASE_URL}${endpoint}`, {
    timeout: "10s",
    tags: { endpoint, flow: "public" },
  });

  recordResultMetrics(response, "public");

  check(response, {
    "public status aceitavel": (r) => r.status === 200,
  });
}

function runAdminRead(authHeaders) {
  const adminResponse = http.get(`${BASE_URL}/api/admin/users`, {
    ...authHeaders,
    tags: { endpoint: "/api/admin/users", flow: "admin" },
  });

  recordResultMetrics(adminResponse, "admin");

  check(adminResponse, {
    "admin status aceitavel": (r) => r.status === 200,
  });
}

function recordResultMetrics(response, flow) {
  const ok = response.status >= 200 && response.status < 300;

  successRate.add(ok);
  if (flow === "public") {
    publicErrorRate.add(!ok);
    publicDuration.add(response.timings.duration);
  } else {
    adminErrorRate.add(!ok);
    adminDuration.add(response.timings.duration);
  }

  if (response.status >= 200 && response.status < 300) {
    status2xx.add(1);
  } else if (response.status >= 400 && response.status < 500) {
    status4xx.add(1);
  } else if (response.status >= 500) {
    status5xx.add(1);
  }
}

function randomThinkTimeSeconds() {
  const waitMs = THINK_MIN_MS + Math.random() * (THINK_MAX_MS - THINK_MIN_MS);
  return waitMs / 1000;
}

function pickRandom(items) {
  return items[Math.floor(Math.random() * items.length)];
}

function buildOptions(profile) {
  const baseThresholds = {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<2000", "p(99)<5000"],
    app_success_rate: ["rate>0.95"],
    public_error_rate: ["rate<0.05"],
    admin_error_rate: ["rate<0.05"],
  };

  const commonScenario = {
    executor: "ramping-vus",
    exec: "mixedTraffic",
    gracefulRampDown: "30s",
  };

  switch (profile) {
    case "50":
      return {
        thresholds: baseThresholds,
        scenarios: {
          fixed_50: {
            ...commonScenario,
            stages: [
              { duration: "30s", target: 10 },
              { duration: "30s", target: 50 },
              { duration: "4m", target: 50 },
              { duration: "30s", target: 0 },
            ],
          },
        },
      };
    case "100":
      return {
        thresholds: baseThresholds,
        scenarios: {
          fixed_100: {
            ...commonScenario,
            stages: [
              { duration: "30s", target: 10 },
              { duration: "60s", target: 100 },
              { duration: "5m", target: 100 },
              { duration: "60s", target: 0 },
            ],
          },
        },
      };
    case "1000":
      return {
        thresholds: {
          http_req_failed: ["rate<0.25"],
          http_req_duration: ["p(95)<10000", "p(99)<12000"],
          app_success_rate: ["rate>0.75"],
          public_error_rate: ["rate<0.25"],
          admin_error_rate: ["rate<0.25"],
        },
        scenarios: {
          fixed_1000: {
            ...commonScenario,
            stages: [
              { duration: "60s", target: 20 },
              { duration: "6m", target: 1000 },
              { duration: "4m", target: 1000 },
              { duration: "90s", target: 0 },
            ],
          },
        },
      };
    case "find-limit":
      return {
        thresholds: {
          http_req_failed: [{
            threshold: `rate<${FIND_LIMIT_MAX_HTTP_FAILED_RATE}`,
            abortOnFail: true,
            delayAbortEval: FIND_LIMIT_ABORT_DELAY,
          }],
          app_success_rate: [{
            threshold: `rate>${FIND_LIMIT_MIN_SUCCESS_RATE}`,
            abortOnFail: true,
            delayAbortEval: FIND_LIMIT_ABORT_DELAY,
          }],
          public_error_rate: [{
            threshold: `rate<${FIND_LIMIT_MAX_PUBLIC_ERROR_RATE}`,
            abortOnFail: true,
            delayAbortEval: FIND_LIMIT_ABORT_DELAY,
          }],
          admin_error_rate: [{
            threshold: `rate<${FIND_LIMIT_MAX_ADMIN_ERROR_RATE}`,
            abortOnFail: true,
            delayAbortEval: FIND_LIMIT_ABORT_DELAY,
          }],
        },
        scenarios: {
          progressive_limit: {
            ...commonScenario,
            stages: buildFindLimitStages(),
          },
        },
      };
    default:
      throw new Error(`Perfil de carga desconhecido: ${profile}`);
  }
}

function buildFindLimitStages() {
  const stages = [{ duration: "60s", target: 100 }];

  for (let target = 100 + FIND_LIMIT_STEP_VUS; target <= FIND_LIMIT_MAX_VUS; target += FIND_LIMIT_STEP_VUS) {
    stages.push({ duration: FIND_LIMIT_STAGE_DURATION, target });
  }

  stages.push({ duration: "60s", target: 0 });
  return stages;
}
