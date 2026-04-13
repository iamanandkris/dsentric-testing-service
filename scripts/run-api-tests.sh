#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-60}"
LOAD_TEST_CONCURRENCY="${LOAD_TEST_CONCURRENCY:-10}"
LOAD_TEST_REQUESTS="${LOAD_TEST_REQUESTS:-100}"
LOAD_TEST_METHOD="${LOAD_TEST_METHOD:-GET}"
MANAGE_DOCKER="${MANAGE_DOCKER:-0}"
KEEP_DOCKER_UP="${KEEP_DOCKER_UP:-0}"

for cmd in curl jq; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 1
  fi
done

if [[ "$MANAGE_DOCKER" == "1" ]] && ! command -v docker >/dev/null 2>&1; then
  echo "Missing required command: docker" >&2
  exit 1
fi

LAST_BODY=""
LAST_STATUS=""

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

log() {
  printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"
}

api_call() {
  local method="$1"
  local path="$2"
  local expected_status="$3"
  local body="${4:-}"
  local url="${BASE_URL}${path}"
  local response

  if [[ -n "$body" ]]; then
    response="$(curl -sS -X "$method" "$url" -H 'Content-Type: application/json' -d "$body" -w $'\n%{http_code}')"
  else
    response="$(curl -sS -X "$method" "$url" -H 'Content-Type: application/json' -w $'\n%{http_code}')"
  fi

  LAST_BODY="$(printf '%s' "$response" | sed '$d')"
  LAST_STATUS="$(printf '%s' "$response" | tail -n1)"

  if [[ "$LAST_STATUS" != "$expected_status" ]]; then
    echo "Request failed: $method $path" >&2
    echo "Expected status: $expected_status" >&2
    echo "Actual status:   $LAST_STATUS" >&2
    echo "Response body:" >&2
    printf '%s\n' "$LAST_BODY" >&2
    exit 1
  fi
}

json_field() {
  local filter="$1"
  if ! printf '%s' "$LAST_BODY" | jq -er "$filter"; then
    echo "JSON assertion failed: $filter" >&2
    echo "Response body:" >&2
    printf '%s\n' "$LAST_BODY" >&2
    exit 1
  fi
}

wait_for_health() {
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  log "Waiting for ${BASE_URL}/health"
  while (( SECONDS < deadline )); do
    if curl -sS "${BASE_URL}/health" >/tmp/dsentric-health.json 2>/dev/null; then
      if jq -e '.status == "UP"' /tmp/dsentric-health.json >/dev/null 2>&1; then
        log "Service is healthy"
        return 0
      fi
    fi
    sleep 2
  done
  echo "Timed out waiting for health endpoint" >&2
  exit 1
}

start_docker_stack() {
  log "Starting docker compose stack"
  compose_cmd up -d --build
}

stop_docker_stack() {
  if [[ "$KEEP_DOCKER_UP" != "1" ]]; then
    log "Stopping docker compose stack"
    compose_cmd down
  fi
}

exercise_language() {
  local lang="$1"
  local lang_upper
  lang_upper="$(printf '%s' "$lang" | tr '[:lower:]' '[:upper:]')"
  local users="/users-${lang}"
  local products="/products-${lang}"
  local orders="/orders-${lang}"

  log "Running CRUD flow for ${lang}"

  api_call POST "$users" 201 '{
    "email":"api-test-'"${lang}"'@example.com",
    "name":"API Test '"${lang}"'",
    "age":31,
    "address":{"street":"1 Test St","city":"London","zipCode":"EC1A1AA"},
    "preferences":{"newsletter":true,"notifications":false}
  }'
  local user_id
  user_id="$(json_field '.data.id')"

  api_call GET "${users}/${user_id}" 200
  json_field '.language == "'"${lang}"'"' >/dev/null

  api_call PUT "${users}/${user_id}" 200 '{
    "email":"api-test-'"${lang}"'-updated@example.com",
    "name":"API Test '"${lang}"' Updated",
    "age":32,
    "address":{"street":"2 Test St","city":"Manchester","zipCode":"M11AE"},
    "preferences":{"newsletter":false,"notifications":true}
  }'

  api_call PATCH "${users}/${user_id}" 200 '{
    "age":33,
    "preferences":{"newsletter":true}
  }'

  api_call POST "$products" 201 '{
    "sku":"SKU-'"${lang_upper}"'-001",
    "name":"Product '"${lang}"'",
    "description":"Product created by API test",
    "category":"test",
    "price":{"amount":19.99,"currency":"USD"},
    "inventory":{"available":100,"reserved":5},
    "tags":["api-test","'"${lang}"'"]
  }'
  local product_id
  product_id="$(json_field '.data.id')"

  api_call GET "${products}/${product_id}" 200
  api_call PUT "${products}/${product_id}" 200 '{
    "sku":"SKU-'"${lang_upper}"'-001",
    "name":"Product '"${lang}"' Updated",
    "description":"Updated product created by API test",
    "category":"test",
    "price":{"amount":29.99,"currency":"USD"},
    "inventory":{"available":90,"reserved":7},
    "tags":["api-test","updated","'"${lang}"'"]
  }'
  api_call PATCH "${products}/${product_id}" 200 '{
    "price":{"amount":24.99},
    "inventory":{"available":95}
  }'

  api_call POST "$orders" 201 '{
    "userId": '"${user_id}"',
    "orderNumber": "ORD-'"${lang_upper}"'-001",
    "status": "pending",
    "items": [
      {"productId": '"${product_id}"', "sku":"SKU-'"${lang_upper}"'-001", "quantity":2, "unitPrice":24.99}
    ],
    "totals": {"subtotal":49.98,"tax":5.00,"shipping":10.00,"total":64.98},
    "shippingAddress":{"street":"3 Order St","city":"Bristol","zipCode":"BS11AA"},
    "paymentInfo":{"method":"credit_card","last4":"1234"}
  }'
  local order_id
  order_id="$(json_field '.data.id')"

  api_call GET "${orders}/${order_id}" 200
  api_call PUT "${orders}/${order_id}" 200 '{
    "userId": '"${user_id}"',
    "orderNumber": "ORD-'"${lang_upper}"'-001",
    "status": "pending",
    "items": [
      {"productId": '"${product_id}"', "sku":"SKU-'"${lang_upper}"'-001", "quantity":3, "unitPrice":24.99}
    ],
    "totals": {"subtotal":74.97,"tax":7.50,"shipping":10.00,"total":92.47},
    "shippingAddress":{"street":"3 Order St","city":"Bristol","zipCode":"BS11AA"},
    "paymentInfo":{"method":"credit_card","last4":"1234"}
  }'
  api_call PATCH "${orders}/${order_id}" 200 '{"status":"completed"}'

  api_call GET "$users" 200
  api_call GET "$products" 200
  api_call GET "$orders" 200

  api_call DELETE "${orders}/${order_id}" 204
  api_call DELETE "${products}/${product_id}" 204
  api_call DELETE "${users}/${user_id}" 204
}

run_dsentric_feature_checks() {
  log "Checking dsentric schema endpoints"
  api_call GET "/contracts/users/schema" 200
  json_field '.properties.registeredAt["x-immutable"] == true' >/dev/null
  json_field '.properties.internalNotes["x-internal"] == true' >/dev/null
  json_field '.properties.preferences.properties.internalSegment["x-reserved"] == true' >/dev/null

  api_call GET "/contracts/products/schema" 200
  json_field '.properties.createdAt["x-immutable"] == true' >/dev/null
  json_field '.properties.internalCost["x-internal"] == true' >/dev/null

  api_call GET "/contracts/orders/schema" 200
  json_field '.properties.orderNumber["x-immutable"] == true' >/dev/null
  json_field '.properties.internalStatus["x-reserved"] == true' >/dev/null
  json_field '.properties.paymentInfo.properties.last4["x-masked"] == "****"' >/dev/null

  log "Checking dsentric draft validation endpoints"
  api_call POST "/contracts/users/draft/scala" 200 '{"email":"draft-scala@example.com"}'
  json_field '.email == "draft-scala@example.com"' >/dev/null
  api_call POST "/contracts/products/draft/java" 200 '{"sku":"DRAFT-001"}'
  json_field '.sku == "DRAFT-001"' >/dev/null
  api_call POST "/contracts/orders/draft/kotlin" 200 '{"status":"pending"}'
  json_field '.status == "pending"' >/dev/null

  log "Checking dsentric immutable, reserved, masked, and validator behavior"

  api_call POST "/products-scala" 201 '{
    "sku":"DSENTRIC-IMMUT-001",
    "name":"Immutable Product",
    "price":{"amount":10.0,"currency":"USD"},
    "createdAt":"2026-04-11T00:00:00Z"
  }'
  local immutable_product_id
  immutable_product_id="$(json_field '.data.id')"

  api_call PATCH "/products-scala/${immutable_product_id}" 400 '{"createdAt":"2027-01-01T00:00:00Z"}'
  json_field '.details[0] | contains("immutable")' >/dev/null

  api_call POST "/users-scala" 400 '{
    "email":"reserved@example.com",
    "name":"Reserved User",
    "preferences":{"internalSegment":"secret"}
  }'
  json_field '.details[0] | contains("preferences")' >/dev/null

  api_call POST "/products-kotlin" 400 '{
    "sku":"BAD-INVENTORY-001",
    "name":"Bad Inventory",
    "description":"Validator probe",
    "category":"test",
    "price":{"amount":15.0,"currency":"USD"},
    "inventory":{"available":2,"reserved":5},
    "tags":["validator"]
  }'
  json_field '.details[0] | contains("inventory.reserved")' >/dev/null

  api_call POST "/users-scala" 201 '{
    "email":"order-check@example.com",
    "name":"Order Check User"
  }'
  local validator_user_id
  validator_user_id="$(json_field '.data.id')"

  api_call POST "/products-java" 201 '{
    "sku":"ORDER-CHECK-001",
    "name":"Order Check Product",
    "description":"Order validation helper",
    "category":"test",
    "price":{"amount":20.0,"currency":"USD"},
    "inventory":{"available":10,"reserved":1},
    "tags":["validator"]
  }'
  local validator_product_id
  validator_product_id="$(json_field '.data.id')"

  api_call POST "/orders-kotlin" 201 '{
    "userId": '"${validator_user_id}"',
    "orderNumber":"ORD-MASKED-001",
    "status":"pending",
    "items":[{"productId": '"${validator_product_id}"', "sku":"ORDER-CHECK-001", "quantity":2, "unitPrice":20.0}],
    "totals":{"subtotal":40.0,"tax":4.0,"shipping":1.0,"total":45.0},
    "shippingAddress":{"street":"4 Masked St","city":"Leeds","zipCode":"LS11AA"},
    "paymentInfo":{"method":"credit_card","last4":"1234","gatewayReference":"gw-123"}
  }'
  local validator_order_id
  validator_order_id="$(json_field '.data.id')"
  json_field '.data.paymentInfo.last4 == "****"' >/dev/null
  json_field '.data.paymentInfo.gatewayReference == null' >/dev/null

  api_call POST "/orders-kotlin" 400 '{
    "userId": '"${validator_user_id}"',
    "orderNumber":"ORD-BAD-TOTAL-001",
    "status":"pending",
    "items":[{"productId": '"${validator_product_id}"', "sku":"ORDER-CHECK-001", "quantity":2, "unitPrice":20.0}],
    "totals":{"subtotal":10.0,"tax":1.0,"shipping":1.0,"total":12.0},
    "shippingAddress":{"street":"4 Masked St","city":"Leeds","zipCode":"LS11AA"},
    "paymentInfo":{"method":"credit_card","last4":"1234","gatewayReference":"gw-123"}
  }'
  json_field '.details[0] | contains("subtotal")' >/dev/null

  api_call DELETE "/orders-kotlin/${validator_order_id}" 204
  api_call DELETE "/products-java/${validator_product_id}" 204
  api_call DELETE "/users-scala/${validator_user_id}" 204
  api_call DELETE "/products-scala/${immutable_product_id}" 204
}

run_metrics_checks() {
  log "Checking metrics endpoints"
  api_call GET "/metrics/stats" 200
  json_field '.count >= 0' >/dev/null

  api_call GET "/metrics/stats?endpoint=%2Fusers-scala&language=scala" 200
  json_field '.avgMs >= 0' >/dev/null
}

run_load_test() {
  local endpoint
  for endpoint in /users-scala /users-java /users-kotlin; do
    log "Running load test against ${endpoint}"
    api_call POST "/load-test" 200 "{
      \"endpoint\": \"${endpoint}\",
      \"method\": \"${LOAD_TEST_METHOD}\",
      \"concurrency\": ${LOAD_TEST_CONCURRENCY},
      \"requests\": ${LOAD_TEST_REQUESTS},
      \"body\": null
    }"
    printf '%s\n' "$LAST_BODY" | jq .
  done

  for endpoint in /users-scala /users-java /users-kotlin; do
    log "Running POST load test against ${endpoint}"
    api_call POST "/load-test" 200 "{
      \"endpoint\": \"${endpoint}\",
      \"method\": \"POST\",
      \"concurrency\": ${LOAD_TEST_CONCURRENCY},
      \"requests\": ${LOAD_TEST_REQUESTS},
      \"body\": {
        \"email\": \"loadtest-${endpoint##*/}-user@example.com\",
        \"name\": \"Load Test User\",
        \"age\": 34,
        \"preferences\": {
          \"newsletter\": false,
          \"notifications\": true
        }
      }
    }"
    printf '%s\n' "$LAST_BODY" | jq .
  done

  for endpoint in /products-scala /products-java /products-kotlin; do
    log "Running POST load test against ${endpoint}"
    api_call POST "/load-test" 200 "{
      \"endpoint\": \"${endpoint}\",
      \"method\": \"POST\",
      \"concurrency\": ${LOAD_TEST_CONCURRENCY},
      \"requests\": ${LOAD_TEST_REQUESTS},
      \"body\": {
        \"sku\": \"loadtest-${endpoint##*/}-sku\",
        \"name\": \"Load Test Product\",
        \"description\": \"Benchmark product payload\",
        \"category\": \"benchmark\",
        \"price\": {
          \"amount\": 19.99,
          \"currency\": \"USD\"
        },
        \"inventory\": {
          \"available\": 50,
          \"reserved\": 5
        },
        \"tags\": [\"load-test\", \"${endpoint##*/}\"]
      }
    }"
    printf '%s\n' "$LAST_BODY" | jq .
  done

  for endpoint in /orders-scala /orders-java /orders-kotlin; do
    log "Running POST load test against ${endpoint}"
    api_call POST "/load-test" 200 "{
      \"endpoint\": \"${endpoint}\",
      \"method\": \"POST\",
      \"concurrency\": ${LOAD_TEST_CONCURRENCY},
      \"requests\": ${LOAD_TEST_REQUESTS},
      \"body\": {
        \"userId\": 1,
        \"orderNumber\": \"LOADTEST-${endpoint##*/}-ORDER\",
        \"status\": \"pending\",
        \"items\": [
          {
            \"productId\": 1,
            \"sku\": \"SKU-0000\",
            \"quantity\": 2,
            \"unitPrice\": 20.0
          }
        ],
        \"totals\": {
          \"subtotal\": 40.0,
          \"tax\": 4.0,
          \"shipping\": 1.0,
          \"total\": 45.0
        },
        \"paymentInfo\": {
          \"method\": \"credit_card\",
          \"last4\": \"1234\"
        }
      }
    }"
    printf '%s\n' "$LAST_BODY" | jq .
  done

  for endpoint in /users-scala/1 /users-java/1 /users-kotlin/1; do
    log "Running PATCH load test against ${endpoint}"
    api_call POST "/load-test" 200 "{
      \"endpoint\": \"${endpoint}\",
      \"method\": \"PATCH\",
      \"concurrency\": ${LOAD_TEST_CONCURRENCY},
      \"requests\": ${LOAD_TEST_REQUESTS},
      \"body\": {
        \"age\": 34,
        \"preferences\": {
          \"newsletter\": false
        }
      }
    }"
    printf '%s\n' "$LAST_BODY" | jq .
  done

  for endpoint in /products-scala/1 /products-java/1 /products-kotlin/1; do
    log "Running PATCH load test against ${endpoint}"
    api_call POST "/load-test" 200 "{
      \"endpoint\": \"${endpoint}\",
      \"method\": \"PATCH\",
      \"concurrency\": ${LOAD_TEST_CONCURRENCY},
      \"requests\": ${LOAD_TEST_REQUESTS},
      \"body\": {
        \"price\": {
          \"amount\": 24.99,
          \"currency\": \"USD\"
        },
        \"inventory\": {
          \"available\": 60,
          \"reserved\": 5
        }
      }
    }"
    printf '%s\n' "$LAST_BODY" | jq .
  done

  for endpoint in /orders-scala/1 /orders-java/1 /orders-kotlin/1; do
    log "Running PATCH load test against ${endpoint}"
    api_call POST "/load-test" 200 "{
      \"endpoint\": \"${endpoint}\",
      \"method\": \"PATCH\",
      \"concurrency\": ${LOAD_TEST_CONCURRENCY},
      \"requests\": ${LOAD_TEST_REQUESTS},
      \"body\": {
        \"status\": \"completed\"
      }
    }"
    printf '%s\n' "$LAST_BODY" | jq .
  done
}

run_reseed() {
  log "Triggering reseed"
  api_call POST "/seed" 200
  json_field '.usersCreated >= 0' >/dev/null
}

main() {
  if [[ "$MANAGE_DOCKER" == "1" ]]; then
    trap stop_docker_stack EXIT
    start_docker_stack
  fi
  wait_for_health
  exercise_language scala
  exercise_language java
  exercise_language kotlin
  run_dsentric_feature_checks
  run_metrics_checks
  run_load_test
  run_reseed
  log "All API checks completed successfully"
}

main "$@"
