import http from "k6/http";
import { check } from "k6";

const TARGET_URL = __ENV.URL || "http://localhost:8088/api/v1/orders/1";
const RATE = Number(__ENV.RATE || 100);
const DURATION = __ENV.DURATION || "60s";
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || 200);

export const options = {
  discardResponseBodies: true,
  scenarios: {
    gateway_order_get_noauth: {
      executor: "constant-arrival-rate",
      rate: RATE,
      timeUnit: "1s",
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
    },
  },
  thresholds: {
    checks: ["rate>0.99"],
  },
};

http.setResponseCallback(http.expectedStatuses(401));

export default function () {
  const response = http.get(TARGET_URL, {
    tags: { endpoint: "gateway-order-get-noauth" },
  });

  check(response, {
    "status is 401": (res) => res.status === 401,
  });
}

