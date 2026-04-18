import http from "tests/http";

const RATE = Number(__ENV.RATE || 30);
const DURATION = __ENV.DURATION || "24h";
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 20);
const MAX_VUS = Number(__ENV.MAX_VUS || 200);
const MODE = (__ENV.MODE || "chaos").toLowerCase();

const ERROR_4XX_RATIO_RAW = Number(__ENV.ERROR_4XX_RATIO || 0.05);
const ERROR_5XX_RATIO_RAW = Number(__ENV.ERROR_5XX_RATIO || 0.05);

const GATEWAY_BASE_URL = __ENV.GATEWAY_BASE_URL || "http://localhost:8088";
const AUTH_BASE_URL = __ENV.AUTH_BASE_URL || "http://localhost:8084";
const USER_BASE_URL = __ENV.USER_BASE_URL || "http://localhost:8082";
const ORDER_BASE_URL = __ENV.ORDER_BASE_URL || "http://localhost:8081";

function clamp01(value) {
  if (Number.isNaN(value)) {
    return 0;
  }

  if (value < 0) {
    return 0;
  }

  if (value > 1) {
    return 1;
  }

  return value;
}

const error4xxRatioBase = clamp01(ERROR_4XX_RATIO_RAW);
const error5xxRatioBase = clamp01(ERROR_5XX_RATIO_RAW);
const errorRatioSum = error4xxRatioBase + error5xxRatioBase;
const ratioNormalizer = errorRatioSum > 0.95 ? 0.95 / errorRatioSum : 1;
const ERROR_4XX_RATIO = error4xxRatioBase * ratioNormalizer;
const ERROR_5XX_RATIO = error5xxRatioBase * ratioNormalizer;
const LATENCY_RATIO = 1 - ERROR_4XX_RATIO - ERROR_5XX_RATIO;

const URLS = {
  gateway: {
    chaos: `${GATEWAY_BASE_URL}/api/v1/gateway/test/status/chaos`,
    latency: `${GATEWAY_BASE_URL}/api/v1/gateway/test/status/latency`,
    status400: `${GATEWAY_BASE_URL}/api/v1/gateway/test/status/400`,
    status500: `${GATEWAY_BASE_URL}/api/v1/gateway/test/status/500`,
  },
  auth: {
    chaos: `${AUTH_BASE_URL}/api/v1/auth/test/status/chaos`,
    latency: `${AUTH_BASE_URL}/api/v1/auth/test/status/latency`,
    status400: `${AUTH_BASE_URL}/api/v1/auth/test/status/400`,
    status500: `${AUTH_BASE_URL}/api/v1/auth/test/status/500`,
  },
  user: {
    chaos: `${USER_BASE_URL}/api/v1/users/test/status/chaos`,
    latency: `${USER_BASE_URL}/api/v1/users/test/status/latency`,
    status400: `${USER_BASE_URL}/api/v1/users/test/status/400`,
    status500: `${USER_BASE_URL}/api/v1/users/test/status/500`,
  },
  order: {
    chaos: `${ORDER_BASE_URL}/api/v1/orders/test/status/chaos`,
    latency: `${ORDER_BASE_URL}/api/v1/orders/test/status/latency`,
    status400: `${ORDER_BASE_URL}/api/v1/orders/test/status/400`,
    status500: `${ORDER_BASE_URL}/api/v1/orders/test/status/500`,
  },
};

export const options = {
  discardResponseBodies: true,
  scenarios: {
    gateway_traffic: {
      executor: "constant-arrival-rate",
      rate: RATE,
      timeUnit: "1s",
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      exec: "gatewayTraffic",
    },
    auth_traffic: {
      executor: "constant-arrival-rate",
      rate: RATE,
      timeUnit: "1s",
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      exec: "authTraffic",
    },
    user_traffic: {
      executor: "constant-arrival-rate",
      rate: RATE,
      timeUnit: "1s",
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      exec: "userTraffic",
    },
    order_traffic: {
      executor: "constant-arrival-rate",
      rate: RATE,
      timeUnit: "1s",
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      exec: "orderTraffic",
    },
  },
};

http.setResponseCallback(http.expectedStatuses(200, 400, 404, 500));

function pickRoute(domainUrls) {
  const random = Math.random();

  if (random < LATENCY_RATIO) {
    return {
      url: domainUrls.latency,
      route: "latency",
      expectedStatus: "200",
    };
  }

  if (random < LATENCY_RATIO + ERROR_4XX_RATIO) {
    return {
      url: domainUrls.status400,
      route: "status_400",
      expectedStatus: "400",
    };
  }

  return {
    url: domainUrls.status500,
    route: "status_500",
    expectedStatus: "500",
  };
}

function hit(domainUrls, domain) {
  const selected =
    MODE === "mixed"
      ? pickRoute(domainUrls)
      : {
          url: domainUrls.chaos,
          route: "chaos",
          expectedStatus: "200|400|500",
        };

  http.get(selected.url, {
    tags: {
      domain,
      route: selected.route,
      expected_status: selected.expectedStatus,
      mode: MODE,
    },
  });
}

export function gatewayTraffic() {
  hit(URLS.gateway, "gateway");
}

export function authTraffic() {
  hit(URLS.auth, "auth");
}

export function userTraffic() {
  hit(URLS.user, "user");
}

export function orderTraffic() {
  hit(URLS.order, "order");
}
