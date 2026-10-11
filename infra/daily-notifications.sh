#!/usr/bin/env bash
set -euo pipefail
umask 077
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
env_file="${BUDGET_ENV_FILE:-$project_dir/.env}"
if [[ ! -r "$env_file" ]]; then
    printf 'Environment file unavailable: %s\n' "$env_file" >&2
    exit 1
fi
job_token=""
while IFS='=' read -r key value; do
    if [[ "$key" == "JOB_TOKEN" ]]; then job_token="${value%$'\r'}"; fi
done < "$env_file"
if [[ ! "$job_token" =~ ^[A-Za-z0-9_-]{32,}$ ]]; then
    printf 'JOB_TOKEN must have at least 32 URL-safe characters.\n' >&2
    exit 1
fi
exec 9>"${BUDGET_JOB_LOCK:-/tmp/it-new-edu-daily.lock}"
flock -n 9 || exit 0
job_url="${BUDGET_JOB_URL:-http://127.0.0.1:8080/internal/jobs/daily-notifications}"
printf 'X-Job-Token: %s\n' "$job_token" |
    curl --silent --show-error --fail --connect-timeout 5 --max-time 120 \
        --request POST --header @- "$job_url"
printf '\n'
